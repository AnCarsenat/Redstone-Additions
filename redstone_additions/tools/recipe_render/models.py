"""Turn an item id into something drawable.

Vanilla decides how an item looks in three hops:

    assets/minecraft/items/<id>.json      item definition — which model to use
    assets/minecraft/models/<model>.json  the model, with a parent chain
    assets/minecraft/textures/...         the textures the chain names

An item whose model chain ends in `item/generated` is a flat sprite. Anything else
is a block model and gets rendered from its actual elements, the way the game and
misode's previews (deepslate) do it:

  * every element is a box from `from` to `to` in 0..16 model space, optionally
    rotated around an axis;
  * the model's `display.gui` transform is applied about the block centre — for
    `block/block` that is rotation [30, 225, 0] and scale 0.625;
  * the result is projected orthographically, so a box face stays a
    parallelogram and its texture can be mapped with a plain affine transform;
  * faces pointing away from the viewer are dropped, the rest drawn far to near;
  * each face is shaded by its facing, with vanilla's constants (up 1.0, down 0.5,
    north/south 0.8, east/west 0.6).

That means stairs look like stairs and a dropper's front face lands where the game
puts it, instead of everything being approximated as a full cube.
"""

from __future__ import annotations

import math
import os

from PIL import Image, ImageChops, ImageDraw

from assets import AssetRoot, strip_namespace

# Chain ancestors that mean "flat sprite".
FLAT_PARENTS = {
    "minecraft:item/generated",
    "minecraft:item/handheld",
    "minecraft:item/handheld_rod",
    "minecraft:item/handheld_mace",
    "minecraft:item/generated_custom_model_data",
    "item/generated",
    "item/handheld",
    "builtin/generated",
}

# Vanilla diffuse shading per face, from LightTexture / Direction#getShade.
SHADE = {
    "up": 1.0,
    "down": 0.5,
    "north": 0.8,
    "south": 0.8,
    "east": 0.6,
    "west": 0.6,
}

# Outward normals in model space.
NORMALS = {
    "up": (0, 1, 0),
    "down": (0, -1, 0),
    "north": (0, 0, -1),
    "south": (0, 0, 1),
    "east": (1, 0, 0),
    "west": (-1, 0, 0),
}

# Fallback when a model has no display transform of its own: what block/block uses.
DEFAULT_GUI_DISPLAY = {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}


class Unrenderable(Exception):
    """The item has no model we can draw (chests, banners, shields, heads)."""


# --- model loading ---------------------------------------------------------


def _first_model(node) -> str | None:
    """Walk an item definition's model tree down to the first plain model id."""
    if not isinstance(node, dict):
        return None
    kind = node.get("type", "")
    if kind in ("minecraft:model", "model"):
        return node["model"]
    # Ordered by "which branch is the normal one".
    for key in ("on_false", "fallback", "base"):
        if key in node:
            found = _first_model(node[key]) if isinstance(node[key], dict) else node[key]
            if isinstance(found, str):
                return found
            if found:
                return found
    for key in ("cases", "entries", "models"):
        for case in node.get(key, []):
            found = _first_model(case.get("model", case))
            if found:
                return found
    return None


def item_model_id(root: AssetRoot, item_id: str) -> str:
    """The model id an item definition points at."""
    path = root.item_definition_path(item_id)
    if not os.path.isfile(path):
        raise Unrenderable(f"no item definition for {item_id}")
    model = _first_model(root.read_json(path).get("model"))
    if not model:
        raise Unrenderable(f"item {item_id} has no plain model")
    return model


def load_model(root: AssetRoot, model_id: str) -> dict:
    """Flatten a model and its parent chain.

    Textures merge child-first, which is how `block/dropper` overrides
    `block/orientable`'s placeholders. Elements and display do not merge: the
    nearest definition in the chain wins, as in the game.
    """
    chain: list[str] = []
    textures: dict = {}
    elements = None
    display: dict = {}

    current = model_id
    seen = set()
    while current and current not in seen:
        seen.add(current)
        chain.append(current)
        path = root.model_path(current)
        if not os.path.isfile(path):
            break
        data = root.read_json(path)
        for key, value in data.get("textures", {}).items():
            textures.setdefault(key, value)
        if elements is None and "elements" in data:
            elements = data["elements"]
        for key, value in data.get("display", {}).items():
            display.setdefault(key, value)
        current = data.get("parent")

    return {"chain": chain, "textures": textures, "elements": elements, "display": display}


def _deref(textures: dict, value, depth: int = 0) -> str | None:
    """Follow `#side`-style references to a real texture id.

    A texture entry is usually a plain id, but glass and redstone dust write it as
    `{"sprite": "...", "force_translucent": true}`. Only the sprite matters here.
    """
    while depth < 8:
        if isinstance(value, dict):
            value = value.get("sprite", "")
        if not isinstance(value, str) or not value:
            return None
        if not value.startswith("#"):
            return value
        value = textures.get(value[1:], "")
        depth += 1
    return None


def load_texture(root: AssetRoot, texture_id: str) -> Image.Image:
    path = root.texture_path(texture_id)
    if not os.path.isfile(path):
        raise Unrenderable(f"missing texture {texture_id}")
    image = Image.open(path).convert("RGBA")
    # Animated textures are a vertical strip of frames; the first one will do.
    if image.height > image.width and image.height % image.width == 0:
        image = image.crop((0, 0, image.width, image.width))
    return image


# --- geometry --------------------------------------------------------------


def _rotate(point, axis: str, degrees: float, origin):
    """Rotate a point around one axis through `origin`."""
    angle = math.radians(degrees)
    cos, sin = math.cos(angle), math.sin(angle)
    x, y, z = (point[i] - origin[i] for i in range(3))
    if axis == "x":
        y, z = y * cos - z * sin, y * sin + z * cos
    elif axis == "y":
        x, z = x * cos + z * sin, -x * sin + z * cos
    else:
        x, y = x * cos - y * sin, x * sin + y * cos
    return (x + origin[0], y + origin[1], z + origin[2])


def _display_transform(display: dict):
    """Build the vertex transform for the GUI display context.

    Same shape as deepslate's: translate to the block centre, then the display
    translation, rotation and scale, then back — with rotation applied X, Y, Z.
    """
    gui = display.get("gui", DEFAULT_GUI_DISPLAY)
    rx, ry, rz = gui.get("rotation", [0, 0, 0])
    tx, ty, tz = gui.get("translation", [0, 0, 0])
    scale = gui.get("scale", [1, 1, 1])

    def transform(point):
        x, y, z = (point[i] - 8 for i in range(3))
        x, y, z = x * scale[0], y * scale[1], z * scale[2]
        # The matrix is Rx·Ry·Rz (JOML's rotationXYZ), so applied to a vector the
        # Z rotation happens first and the X rotation last. Doing it the other way
        # round spins the block before tilting it, which leaves every icon leaning
        # over instead of sitting square.
        x, y, z = _rotate((x, y, z), "z", rz, (0, 0, 0))
        x, y, z = _rotate((x, y, z), "y", ry, (0, 0, 0))
        x, y, z = _rotate((x, y, z), "x", rx, (0, 0, 0))
        return (x + tx + 8, y + ty + 8, z + tz + 8)

    return transform


# Corner order per face, from deepslate's BlockModel. Corners run
# bottom-left, bottom-right, top-right, top-left in texture space.
def _face_corners(face: str, x0, y0, z0, x1, y1, z1):
    return {
        "up": [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)],
        "down": [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
        "south": [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
        "north": [(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)],
        "east": [(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)],
        "west": [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)],
    }[face]


def _default_uv(face: str, x0, y0, z0, x1, y1, z1):
    """Vanilla FaceBakery.defaultFaceUV — the uv a face gets when it names none."""
    return {
        "down": [x0, 16 - z1, x1, 16 - z0],
        "up": [x0, z0, x1, z1],
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }[face]


def _shade(image: Image.Image, factor: float) -> Image.Image:
    if factor >= 1.0:
        return image
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            pixels[x, y] = (int(r * factor), int(g * factor), int(b * factor), a)
    return image


def _paste_quad(canvas: Image.Image, patch: Image.Image, quad) -> None:
    """Map a texture patch onto a screen-space parallelogram.

    Orthographic projection keeps a rectangle a parallelogram, so three corner
    pairs pin the whole mapping. `quad` runs bottom-left, bottom-right, top-right,
    top-left, matching the patch's corners.
    """
    width, height = patch.size
    # patch (0,0) is top-left, which is quad[3].
    (dx0, dy0), (dx1, dy1), (dx3, dy3) = quad[3], quad[2], quad[0]

    a11, a12 = (dx1 - dx0) / width, (dx3 - dx0) / height
    a21, a22 = (dy1 - dy0) / width, (dy3 - dy0) / height
    det = a11 * a22 - a12 * a21
    if abs(det) < 1e-9:
        return  # edge-on, nothing to draw

    p, q = a22 / det, -a12 / det
    r, s = -a21 / det, a11 / det
    coeffs = (p, q, -(p * dx0 + q * dy0), r, s, -(r * dx0 + s * dy0))

    warped = patch.transform(canvas.size, Image.AFFINE, coeffs, resample=Image.NEAREST)

    mask = Image.new("L", canvas.size, 0)
    # Outline as well as fill: neighbouring faces meet on exact edges, and a
    # fill-only mask leaves a one-pixel seam of background showing through.
    ImageDraw.Draw(mask).polygon([tuple(point) for point in quad], fill=255, outline=255, width=2)

    # The warped texture is transparent right on the edge, where the transform
    # samples outside the source, so the seam would still show as a hairline of
    # background between two faces. Lay the face's average colour underneath.
    alpha = warped.getchannel("A")
    if patch.getchannel("A").getextrema()[0] > 0:
        average = patch.resize((1, 1), Image.BOX).getpixel((0, 0))
        canvas.paste(Image.new("RGBA", canvas.size, average), (0, 0), mask)

    canvas.paste(warped, (0, 0), ImageChops.darker(alpha, mask))


def render_block_model(root: AssetRoot, model: dict, size: int) -> Image.Image:
    """Draw a block model as the game draws it in a GUI slot."""
    transform = _display_transform(model["display"])
    scale = size / 16.0
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    drawn = []

    for element in model["elements"] or []:
        x0, y0, z0 = element["from"]
        x1, y1, z1 = element["to"]
        rotation = element.get("rotation")

        for face, spec in element.get("faces", {}).items():
            if face not in NORMALS:
                continue
            texture_id = _deref(model["textures"], spec.get("texture", ""))
            if not texture_id:
                continue

            corners = _face_corners(face, x0, y0, z0, x1, y1, z1)
            if rotation:
                corners = [
                    _rotate(corner, rotation["axis"], rotation["angle"], rotation["origin"])
                    for corner in corners
                ]
            corners = [transform(corner) for corner in corners]

            # Cull anything facing away. The camera looks down -Z, so a visible
            # face has a normal with a positive Z component after transforming.
            normal = NORMALS[face]
            tip = transform(tuple(8 + normal[i] * 8 for i in range(3)))
            centre = transform((8, 8, 8))
            if tip[2] - centre[2] <= 0.001:
                continue

            texture = load_texture(root, texture_id)
            unit = texture.width / 16.0
            uv = spec.get("uv") or _default_uv(face, x0, y0, z0, x1, y1, z1)
            box = (
                int(round(min(uv[0], uv[2]) * unit)),
                int(round(min(uv[1], uv[3]) * unit)),
                max(int(round(max(uv[0], uv[2]) * unit)), int(round(min(uv[0], uv[2]) * unit)) + 1),
                max(int(round(max(uv[1], uv[3]) * unit)), int(round(min(uv[1], uv[3]) * unit)) + 1),
            )
            patch = texture.crop(box)
            turns = (spec.get("rotation", 0) // 90) % 4
            if turns:
                patch = patch.rotate(-90 * turns, expand=True)
            patch = _shade(patch.copy(), SHADE[face])

            # Model space to screen: x right, y up (so flipped), z toward viewer.
            quad = [(point[0] * scale, (16 - point[1]) * scale) for point in corners]
            depth = sum(point[2] for point in corners) / 4.0
            drawn.append((depth, patch, quad))

    if not drawn:
        raise Unrenderable("model has no visible faces")

    # Painter's algorithm: far faces first.
    for _, patch, quad in sorted(drawn, key=lambda item: item[0]):
        _paste_quad(canvas, patch, quad)

    return canvas


def render_item(root: AssetRoot, item_id: str, size: int, item_model: str | None = None) -> Image.Image:
    """Draw one item at size x size pixels.

    `item_model` is the `minecraft:item_model` component, which overrides the
    picture without changing the item — every Redstone Additions block is a bat
    spawn egg or a command block wearing someone else's model.
    """
    model_id = item_model_id(root, item_model or item_id)
    model = load_model(root, model_id)

    flat = any(parent in FLAT_PARENTS for parent in model["chain"]) or "layer0" in model["textures"]
    if flat:
        layers = []
        index = 0
        while f"layer{index}" in model["textures"]:
            layer = _deref(model["textures"], model["textures"][f"layer{index}"])
            if layer:
                layers.append(load_texture(root, layer))
            index += 1
        if not layers:
            raise Unrenderable(f"{item_id}: flat model with no layers")
        sprite = layers[0].copy()
        for extra in layers[1:]:
            sprite.alpha_composite(extra.resize(sprite.size, Image.NEAREST))
        return sprite.resize((size, size), Image.NEAREST)

    if not model["elements"]:
        raise Unrenderable(f"{item_id}: model {model_id} has no elements")
    return render_block_model(root, model, size)

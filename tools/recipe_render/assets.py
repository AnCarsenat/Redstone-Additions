"""Vanilla asset acquisition for the recipe renderer.

Resolves a Minecraft version through Mojang's version manifest, downloads that
version's client jar and keeps the handful of things a recipe picture needs:
item and block textures, the item/model definitions that say which texture goes
with which item, the crafting table GUI, the ASCII font, and the vanilla item
tags so a `#minecraft:planks` ingredient can be resolved to something drawable.

Everything lands under one cache directory, one subdirectory per version, with
the jar's own paths preserved so a texture can be found by its namespaced id:

    assets/26.2/assets/minecraft/textures/item/netherite_scrap.png
    assets/26.2/assets/minecraft/models/block/dropper.json
    assets/26.2/data/minecraft/tags/item/planks.json
    assets/26.2/.version           marker: only written once extraction finished

The jar itself is deleted after extraction — it is 30 MB of code and sounds we
have no use for.
"""

from __future__ import annotations

import hashlib
import json
import os
import shutil
import urllib.request
import zipfile

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

# Directory prefixes worth keeping out of the jar.
KEEP_PREFIXES = (
    "assets/minecraft/textures/item/",
    "assets/minecraft/textures/block/",
    "assets/minecraft/items/",
    "assets/minecraft/models/item/",
    "assets/minecraft/models/block/",
    "data/minecraft/tags/item/",
)

# Single files worth keeping: one GUI per recipe screen, plus the font.
KEEP_FILES = (
    "assets/minecraft/textures/gui/container/crafting_table.png",
    "assets/minecraft/textures/gui/container/furnace.png",
    "assets/minecraft/textures/gui/container/blast_furnace.png",
    "assets/minecraft/textures/gui/container/smoker.png",
    "assets/minecraft/textures/gui/container/stonecutter.png",
    "assets/minecraft/textures/gui/container/smithing.png",
    "assets/minecraft/textures/font/ascii.png",
)


def _fetch_json(url: str) -> dict:
    with urllib.request.urlopen(url) as response:
        return json.load(response)


def resolve_version(spec: str = "latest") -> tuple[str, str, str]:
    """Return (version_id, jar_url, jar_sha1) for 'latest', 'snapshot' or an id."""
    manifest = _fetch_json(MANIFEST)
    if spec in ("latest", "release"):
        spec = manifest["latest"]["release"]
    elif spec == "snapshot":
        spec = manifest["latest"]["snapshot"]

    for entry in manifest["versions"]:
        if entry["id"] == spec:
            meta = _fetch_json(entry["url"])
            client = meta["downloads"]["client"]
            return spec, client["url"], client["sha1"]
    raise SystemExit(f"no such Minecraft version: {spec}")


def _download(url: str, dest: str, expect_sha1: str | None = None) -> None:
    print(f"downloading {url}")
    with urllib.request.urlopen(url) as response, open(dest, "wb") as out:
        shutil.copyfileobj(response, out)
    if expect_sha1:
        digest = hashlib.sha1(open(dest, "rb").read()).hexdigest()
        if digest != expect_sha1:
            raise SystemExit(f"sha1 mismatch on {dest}: {digest} != {expect_sha1}")


def _extract(jar_path: str, dest: str) -> int:
    with zipfile.ZipFile(jar_path) as jar:
        members = [
            name
            for name in jar.namelist()
            if name in KEEP_FILES or name.startswith(KEEP_PREFIXES)
        ]
        jar.extractall(dest, members=members)
    return len(members)


class AssetRoot:
    """One extracted version, addressed by namespaced id."""

    def __init__(self, path: str, version: str):
        self.path = path
        self.version = version

    # -- paths --------------------------------------------------------------
    def _asset(self, *parts: str) -> str:
        return os.path.join(self.path, "assets", "minecraft", *parts)

    def texture_path(self, texture_id: str) -> str:
        """`minecraft:block/dropper_front` -> .../textures/block/dropper_front.png"""
        return self._asset("textures", *strip_namespace(texture_id).split("/")) + ".png"

    def model_path(self, model_id: str) -> str:
        return self._asset("models", *strip_namespace(model_id).split("/")) + ".json"

    def item_definition_path(self, item_id: str) -> str:
        return self._asset("items", strip_namespace(item_id) + ".json")

    def gui_path(self, name: str) -> str:
        return self._asset("textures", "gui", "container", name + ".png")

    def font_path(self, name: str = "ascii") -> str:
        return self._asset("textures", "font", name + ".png")

    def item_tag_path(self, tag_id: str) -> str:
        return os.path.join(
            self.path, "data", "minecraft", "tags", "item",
            strip_namespace(tag_id) + ".json",
        )

    # -- reads --------------------------------------------------------------
    def read_json(self, path: str) -> dict:
        with open(path) as handle:
            return json.load(handle)

    def resolve_item_tag(self, tag_id: str) -> list[str]:
        """Flatten a vanilla item tag to a list of item ids."""
        out: list[str] = []
        for value in self.read_json(self.item_tag_path(tag_id))["values"]:
            entry = value["id"] if isinstance(value, dict) else value
            if entry.startswith("#"):
                out.extend(self.resolve_item_tag(entry[1:]))
            else:
                out.append(entry)
        return out


def strip_namespace(identifier: str) -> str:
    return identifier.split(":", 1)[-1]


def ensure_assets(cache_dir: str, version_spec: str = "latest", refresh: bool = False) -> AssetRoot:
    """Make sure a version's assets are on disk, and return a handle to them."""
    if version_spec not in ("latest", "snapshot", "release") and not refresh:
        # A pinned version that is already extracted needs no network at all.
        candidate = os.path.join(cache_dir, version_spec)
        if os.path.isfile(os.path.join(candidate, ".version")):
            return AssetRoot(candidate, version_spec)

    version, jar_url, jar_sha1 = resolve_version(version_spec)
    dest = os.path.join(cache_dir, version)
    marker = os.path.join(dest, ".version")

    if os.path.isfile(marker) and not refresh:
        return AssetRoot(dest, version)

    if os.path.isdir(dest):
        shutil.rmtree(dest)
    os.makedirs(dest, exist_ok=True)

    jar_path = os.path.join(cache_dir, f"{version}-client.jar")
    try:
        _download(jar_url, jar_path, jar_sha1)
        count = _extract(jar_path, dest)
    finally:
        if os.path.isfile(jar_path):
            os.remove(jar_path)

    with open(marker, "w") as handle:
        handle.write(version + "\n")
    print(f"kept {count} files for {version} in {dest}")
    return AssetRoot(dest, version)

"""Beet plugin that builds the 26.3 overlay (`overlay_121`) out of the base pack.

26.3 (data pack format 121) changed the shape of three kinds of file the pack
ships, and every one of them is a hard error at load time -- the world refuses
to open, not just the one file:

- predicates and loot conditions: the type key `condition` became `type`, a
  list of conditions is no longer accepted at the root (use `all_of`), and the
  `conditions` list on functions, pools and entries became a single `condition`
- item modifiers and loot functions: the type key `function` became `type`, a
  list at the root is no longer accepted (use `sequence`), and a pool's or an
  entry's `functions` list became `modifier`
- advancements: the `recipe_unlocked` trigger takes `recipes` instead of
  `recipe`
- block states in NBT: `Name`/`Properties` became `id`/`properties`

The old shapes are the only ones 1.21.9 - 26.2 understand, so the base pack
keeps them and 26.3 gets a converted copy as an overlay. Converting at build
time rather than keeping a hand-written copy in src means a new recipe unlock,
predicate or modifier is ported the moment it is added, and the two can never
drift apart.

Functions are ported too, but only where they inline one of those structures as
SNBT -- `loot ... loot {...}`, `item modify ... {...}`, `if predicate {...}` --
or spell out a block state. Inline loot is parsed when the function loads, so a
stale key takes the whole function down; a stale block state loads fine and
summons air. Shapes this plugin cannot rewrite mechanically fail the build
instead of shipping something that silently does not work.

The overlay is built from what the game would see on format 121 *before* this
overlay: the base pack with `overlay_102` applied on top. `overlay_102` stops at
format 120 so the two never both supply the same file on one version and the
overlay order never matters.
"""

import json
import re
from copy import deepcopy
from typing import Any

from beet import Context, DataPack, Function, ItemModifier, Predicate, Advancement

OVERLAY = "overlay_121"
FORMAT = 121
PREVIOUS_OVERLAYS = ["overlay_102"]


class PortError(Exception):
    pass


# ---------------------------------------------------------------------------
# JSON conversion
# ---------------------------------------------------------------------------


def condition_list(value: Any, where: str) -> Any:
    """A `conditions` list (or a list-shaped predicate) as one 26.3 condition."""
    if isinstance(value, dict):
        return condition(value, where)
    if not isinstance(value, list):
        raise PortError(f"{where}: expected a condition or a list of them")
    terms = [condition(term, where) for term in value]
    if len(terms) == 1:
        return terms[0]
    return {"type": "minecraft:all_of", "terms": terms}


def condition(value: Any, where: str) -> Any:
    if isinstance(value, str):  # a reference to a predicate file
        return value
    if isinstance(value, list):
        return condition_list(value, where)
    if not isinstance(value, dict):
        raise PortError(f"{where}: expected a condition object")
    result = dict(value)
    if "condition" in result:
        result = {"type": result.pop("condition"), **result}
    if "terms" in result:
        result["terms"] = [condition(term, where) for term in result["terms"]]
    if "term" in result:
        result["term"] = condition(result["term"], where)
    return result


def modifier_list(value: Any, where: str) -> Any:
    """A `functions` list (or a list-shaped item modifier) as one 26.3 modifier."""
    if isinstance(value, dict):
        return modifier(value, where)
    if not isinstance(value, list):
        raise PortError(f"{where}: expected a function or a list of them")
    functions = [modifier(item, where) for item in value]
    if len(functions) == 1:
        return functions[0]
    return {"type": "minecraft:sequence", "functions": functions}


def modifier(value: Any, where: str) -> Any:
    if isinstance(value, str):
        return value
    if isinstance(value, list):
        return modifier_list(value, where)
    if not isinstance(value, dict):
        raise PortError(f"{where}: expected a function object")
    result = dict(value)
    if "function" in result:
        result = {"type": result.pop("function"), **result}
    if "conditions" in result:
        result["condition"] = condition_list(result.pop("conditions"), where)
    if result.get("type") in ("sequence", "minecraft:sequence"):
        result["functions"] = [modifier(item, where) for item in result["functions"]]
    return result


def advancement(value: dict, where: str) -> dict:
    result = deepcopy(value)
    for criterion in result.get("criteria", {}).values():
        trigger = criterion.get("trigger", "").removeprefix("minecraft:")
        conditions = criterion.get("conditions", {})
        if trigger == "recipe_unlocked" and "recipe" in conditions:
            conditions["recipes"] = conditions.pop("recipe")
        if isinstance(conditions.get("player"), list):
            conditions["player"] = condition_list(conditions["player"], where)
    return result


# ---------------------------------------------------------------------------
# Inline SNBT in functions
# ---------------------------------------------------------------------------

# Only lines that hand the game an inline structure are touched; a plain
# `function ra:foo` call never matches these.
INLINE = re.compile(r"\b(loot|item\s+modify|predicate)\b[^\n]*\{")
RENAMES = [
    (re.compile(r"\bfunctions:\["), "modifier:["),
    (re.compile(r"\bfunction:(\"|')"), r"type:\1"),
    (re.compile(r"\bcondition:(\"|')"), r"type:\1"),
]
UNSUPPORTED = re.compile(r"\bconditions:\[")

# Block states in NBT: `{Name:..., Properties:{...}}` became
# `{id:..., properties:{...}}`. Nothing fails to load over this -- a block
# display summoned with the old keys just shows air -- so every skin, wire and
# pipe display would vanish without a single error.
BLOCK_STATE = re.compile(r"\bblock_state\b")
BLOCK_STATE_RENAMES = [
    (re.compile(r"\bblock_state:\{Name:"), "block_state:{id:"),
    (re.compile(r"\bProperties:\{"), "properties:{"),
    (re.compile(r"\bblock_state\.Name\b"), "block_state.id"),
    (re.compile(r"\bblock_state\.Properties\b"), "block_state.properties"),
]
# Anything still spelt the old way after the renames is a shape this plugin
# does not know, e.g. `block_state:{Properties:{...},Name:...}`.
BLOCK_STATE_LEFTOVER = re.compile(r"\b(Name|Properties)\b(?=\s*[:.])")


def function_text(text: str, where: str) -> str | None:
    """The 26.3 version of a function, or None when nothing in it changes."""
    changed = False
    lines = text.split("\n")
    for index, line in enumerate(lines):
        if line.lstrip().startswith("#"):
            continue
        new = line
        if INLINE.search(line):
            if UNSUPPORTED.search(line):
                raise PortError(
                    f"{where}:{index + 1}: inline `conditions:[...]` has no mechanical "
                    "26.3 port (it became a single `condition`); rewrite it by hand"
                )
            for pattern, replacement in RENAMES:
                new = pattern.sub(replacement, new)
        if BLOCK_STATE.search(new):
            for pattern, replacement in BLOCK_STATE_RENAMES:
                new = pattern.sub(replacement, new)
            if BLOCK_STATE_LEFTOVER.search(new):
                raise PortError(
                    f"{where}:{index + 1}: block state still uses `Name`/`Properties` "
                    "after the 26.3 renames; port it by hand"
                )
        if new != line:
            lines[index] = new
            changed = True
    return "\n".join(lines) if changed else None


# ---------------------------------------------------------------------------
# Plugin
# ---------------------------------------------------------------------------


def effective(ctx: Context, file_type: type) -> dict:
    """What format 121 sees for one file type before this overlay is applied."""
    files = dict(ctx.data[file_type])
    for name in PREVIOUS_OVERLAYS:
        if name in ctx.data.overlays:
            files.update(ctx.data.overlays[name][file_type])
    return files


def beet_default(ctx: Context):
    overlay: DataPack = ctx.data.overlays.setdefault(OVERLAY)
    overlay.min_format = FORMAT
    overlay.max_format = FORMAT
    errors: list[str] = []

    def port(file_type, convert, label):
        for name, file in effective(ctx, file_type).items():
            where = f"{label} {name}"
            try:
                # Parsed from the text rather than through `file.data`: touching
                # `.data` makes beet re-serialise the base file, reformatting
                # every JSON file in the pack for no change in content.
                original = json.loads(file.text)
                ported = convert(original, where)
            except PortError as error:
                errors.append(str(error))
                continue
            if ported != original:
                overlay[file_type][name] = file_type(deepcopy(ported))

    port(Predicate, condition, "predicate")
    port(ItemModifier, modifier, "item modifier")
    port(Advancement, advancement, "advancement")

    for name, function in effective(ctx, Function).items():
        try:
            ported = function_text(function.text, f"function {name}")
        except PortError as error:
            errors.append(str(error))
            continue
        if ported is not None:
            overlay.functions[name] = Function(ported)

    if errors:
        raise SystemExit(
            "26.3 port failed:\n" + "\n".join(f"  {error}" for error in errors)
        )

    counts = {
        "predicates": len(overlay.predicates),
        "item modifiers": len(overlay.item_modifiers),
        "advancements": len(overlay.advancements),
        "functions": len(overlay.functions),
    }
    print(
        f"port_26_3: {OVERLAY} -- "
        + ", ".join(f"{count} {label}" for label, count in counts.items())
    )

#!/usr/bin/env python3
"""Compare the Kore output against the last beet build, namespace by namespace.

The reference is what 26.3 actually loads from the beet build: `data/`, with `overlay_102/`
and then `overlay_121/` laid over it (an overlay replaces whole files). The Kore output is
`build/kore/redstone_additions/data/`, written by `./gradlew koreBuild`.

Functions are compared after normalisation: comment and blank lines dropped, runs of
whitespace collapsed. Kore moves multi-command `execute ... run` blocks into
`generated_scopes/`, so a line calling one is expanded back into the called body before
comparing, which is what makes a ported function comparable to the hand-written one.

JSON files are compared as parsed values.

Usage:
    python3 tools/compare_reference.py ra_gates            # one namespace
    python3 tools/compare_reference.py ra_gates ra_sensors # several
    python3 tools/compare_reference.py --all
    python3 tools/compare_reference.py ra_gates --diff     # show a unified diff per mismatch

Exit code 0 only when nothing is missing, extra or different.
"""

from __future__ import annotations

import argparse
import difflib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BEET = ROOT.parent / "redstone_additions" / "build" / "redstone_additions_data_pack"
KORE = ROOT / "build" / "kore" / "redstone_additions"
OVERLAYS = ["overlay_102", "overlay_121"]
GENERATED = "generated_scopes"


def reference_files() -> dict[str, Path]:
    files: dict[str, Path] = {}
    for base in [BEET] + [BEET / o for o in OVERLAYS]:
        data = base / "data"
        if not data.is_dir():
            continue
        for p in data.rglob("*"):
            if p.is_file():
                files[p.relative_to(data).as_posix()] = p
    return files


def kore_files() -> dict[str, Path]:
    data = KORE / "data"
    return {p.relative_to(data).as_posix(): p for p in data.rglob("*") if p.is_file()} if data.is_dir() else {}


STRICT = False

SELECTOR = re.compile(r"@[aenprs]\[")
BARE_QUOTED = re.compile(r'"([A-Za-z0-9_.+-]+)"')
DOUBLE_SUFFIX = re.compile(r"(?<=[0-9])d(?=[,}\]\s]|$)")


def split_top(body: str) -> list[str]:
    parts, depth, cur = [], 0, ""
    for ch in body:
        if ch in "{[(":
            depth += 1
        elif ch in "}])":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    parts.append(cur)
    return parts


def sort_selectors(line: str) -> str:
    """Kore writes selector arguments sorted; hand-written ones are in any order."""
    out, i = "", 0
    for m in SELECTOR.finditer(line):
        if m.start() < i:
            continue
        start = m.end()
        depth, j = 1, start
        while j < len(line) and depth:
            depth += {"[": 1, "]": -1}.get(line[j], 0)
            j += 1
        out += line[i:start] + ",".join(sorted(split_top(line[start : j - 1]))) + "]"
        i = j
    return out + line[i:]


def canon(line: str) -> str:
    """Formatting differences that never change what the game does."""
    if STRICT:
        return line
    line = line.replace("minecraft:", "")
    line = sort_selectors(line)
    line = BARE_QUOTED.sub(r"\1", line)
    line = DOUBLE_SUFFIX.sub("", line)
    return line


def norm_lines(text: str) -> list[str]:
    out = []
    for line in text.splitlines():
        s = " ".join(line.split())
        if not s or s.startswith("#"):
            continue
        out.append(canon(s))
    return out


CALL = re.compile(r"function ([a-z0-9_.-]+):(" + GENERATED + r"/[a-z0-9_./-]+)$")
CALL_CANON = re.compile(r"function (?:([a-z0-9_.-]+):)?(" + GENERATED + r"/[a-z0-9_./-]+)$")


def expand(lines: list[str], kore: dict[str, Path], depth: int = 0) -> list[str]:
    """Inline calls to generated scopes so `execute ... run function x:generated_scopes/y` compares
    against the original multi-line form, each body line prefixed with the execute head."""
    if depth > 8:
        return lines
    out = []
    for line in lines:
        m = CALL_CANON.search(line)
        if not m:
            out.append(line)
            continue
        key = f"{m.group(1) or 'minecraft'}/function/{m.group(2)}.mcfunction"
        body = kore.get(key)
        if body is None:
            out.append(line)
            continue
        head = line[: m.start()].strip()  # "execute ... run" or ""
        for sub in expand(norm_lines(body.read_text()), kore, depth + 1):
            out.append(f"{head} {sub}".strip() if head else sub)
    return out


def canon_json(v):
    """Same normalisation as [canon] for JSON: `minecraft:` dropped, and a one-element list of
    ids equal to the bare id (vanilla accepts both for item / block / entity lists)."""
    if STRICT:
        return v
    if isinstance(v, dict):
        return {k.replace("minecraft:", ""): canon_json(x) for k, x in v.items()}
    if isinstance(v, list):
        items = [canon_json(x) for x in v]
        return items[0] if len(items) == 1 and isinstance(items[0], str) else items
    if isinstance(v, str):
        return v.replace("minecraft:", "")
    return v


def load_json(p: Path):
    try:
        return canon_json(json.loads(p.read_text()))
    except Exception as e:  # noqa: BLE001
        return f"<invalid json: {e}>"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("namespaces", nargs="*")
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--diff", action="store_true", help="print a unified diff for each mismatch")
    ap.add_argument("--strict", action="store_true", help="no normalisation beyond whitespace and comments")
    args = ap.parse_args()
    global STRICT
    STRICT = args.strict

    ref, kore = reference_files(), kore_files()
    if not kore:
        print(f"No Kore output at {KORE}; run ./gradlew koreBuild first.")
        return 2
    wanted = set(args.namespaces)

    def selected(key: str) -> bool:
        return args.all or key.split("/", 1)[0] in wanted

    ref_keys = {k for k in ref if selected(k)}
    kore_keys = {k for k in kore if selected(k) and f"/{GENERATED}/" not in k}

    missing = sorted(ref_keys - kore_keys)
    extra = sorted(kore_keys - ref_keys)
    different = []
    for key in sorted(ref_keys & kore_keys):
        a, b = ref[key], kore[key]
        if key.endswith(".mcfunction"):
            la = norm_lines(a.read_text())
            lb = expand(norm_lines(b.read_text()), kore)
            if la != lb:
                different.append((key, la, lb))
        elif key.endswith(".json"):
            ja, jb = load_json(a), load_json(b)
            if ja != jb:
                different.append(
                    (key, json.dumps(ja, indent=1, sort_keys=True).splitlines(), json.dumps(jb, indent=1, sort_keys=True).splitlines())
                )
        elif a.read_bytes() != b.read_bytes():
            different.append((key, ["<binary>"], ["<binary differs>"]))

    print(f"reference files: {len(ref_keys)}  kore files: {len(kore_keys)}")
    print(f"identical: {len(ref_keys & kore_keys) - len(different)}  different: {len(different)}  missing: {len(missing)}  extra: {len(extra)}")
    for k in missing:
        print(f"  MISSING  {k}")
    for k in extra:
        print(f"  EXTRA    {k}")
    for k, la, lb in different:
        print(f"  DIFFERS  {k}")
        if args.diff:
            for line in difflib.unified_diff(la, lb, "beet", "kore", lineterm="", n=1):
                print("      " + line)
    return 0 if not (missing or extra or different) else 1


if __name__ == "__main__":
    sys.exit(main())

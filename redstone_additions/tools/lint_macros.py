"""Beet plugin that fails the build on the two ways a function silently will not load.

A line starting with `$` is a macro line, and Minecraft requires it to contain
at least one `$(...)` substitution. One that does not raises

    java.lang.IllegalArgumentException: No variables in macro

at load time, and the WHOLE FUNCTION is rejected -- not just that line. Nothing
in the build catches it, because the file is valid text and beet has no opinion
about command syntax, so the first sign is a feature silently doing nothing in
game and an error buried in latest.log.

That is exactly how the Big Torch shipped without its torch: the summon was
written as a macro, then the one value it interpolated was replaced with a
constant, and the leftover `$` took the whole function down with it.

The second check is calls to functions that are not there. `function <id>` is
resolved when the pack loads, so a call to something that has been renamed or
deleted takes down the function that calls it -- again wholesale, again silently
as far as the build is concerned. That happened once already in this branch: a
helper was inlined and deleted, a stray `git checkout` brought back the caller
without it, and beet built the result without a word.

Both are cheap to check and impossible to argue with, so both run on every build.

RUNS LAST IN THE PIPELINE, ON PURPOSE
tools.settings_gen generates a whole function tree from tools/settings/*.json and
the rest of the pack calls into it. Linting before it ran reported every one of
those calls as missing. Last also means the generated functions are checked like
everything else rather than trusted.
"""

import re

from beet import Context

MACRO = re.compile(r"\$\([A-Za-z_][A-Za-z_0-9]*\)")
CALL = re.compile(r"\bfunction\s+([a-z0-9_.-]+:[a-z0-9_./-]+)")


def beet_default(ctx: Context):
    bad = []

    for name, function in ctx.data.functions.items():
        for number, line in enumerate(function.lines, 1):
            stripped = line.strip()
            if stripped.startswith("$") and not MACRO.search(stripped):
                bad.append(f"  {name}:{number}\n    {stripped[:120]}")

    if bad:
        raise ValueError(
            "macro line with no $(variable) — Minecraft refuses to load the "
            "whole function:\n" + "\n".join(bad)
        )

    known = set(ctx.data.functions)
    tags = {f"#{name}" for name in ctx.data.function_tags}
    missing = []

    for name, function in ctx.data.functions.items():
        for number, line in enumerate(function.lines, 1):
            stripped = line.strip()
            if stripped.startswith("#"):
                continue
            for call in CALL.findall(stripped):
                # A macro builds its target at runtime and cannot be resolved here.
                if "$(" in stripped:
                    continue
                if call in known or call in tags:
                    continue
                missing.append(f"  {name}:{number} calls {call}")

    if missing:
        raise ValueError(
            "call to a function that does not exist — the calling function will "
            "not load:\n" + "\n".join(missing)
        )

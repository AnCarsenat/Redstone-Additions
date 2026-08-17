"""Beet plugin that keeps the files beet does not recognise as pack content.

Beet builds the output pack out of the files it knows how to load, so anything
else in src — the changelog, the module READMEs, updater.py — is silently left
behind. This copies every one of them into the built pack at the same relative
path, so what ships matches what is in src.
"""

from beet import BinaryFile, Context

SOURCE = "src"
IGNORE = {"__pycache__", ".DS_Store", "desktop.ini"}


def beet_default(ctx: Context):
    source = ctx.directory / SOURCE
    loaded = {path for path, _ in ctx.data.list_files()}

    for path in sorted(source.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(source)
        if any(part in IGNORE or part.startswith(".") for part in relative.parts):
            continue
        key = relative.as_posix()
        if key in loaded:
            continue
        # Copied byte for byte, so this works for text and binaries alike.
        ctx.data.extra[key] = BinaryFile(source_path=path)

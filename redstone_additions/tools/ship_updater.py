"""Beet plugin that ships src/updater.py at the root of the built data pack.

Beet only loads files it recognises as pack contents, so a loose script in src
is dropped on the way out. This puts it back, next to pack.mcmeta, which is
where the updater expects to find itself.
"""

from beet import Context, TextFile


def beet_default(ctx: Context):
    source = ctx.directory / "src" / "updater.py"
    ctx.data.extra["updater.py"] = TextFile(source.read_text(encoding="utf-8"))

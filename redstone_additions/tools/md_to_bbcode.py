#!/usr/bin/env python3
"""Turn a Markdown file into the BBCode Planet Minecraft accepts.

PMC's editor speaks its own small BBCode dialect — the tag list is the one its
own site guide documents (bold, italic, underline, quote, code, list, list=,
bullets, img, simg, url, font size, text colour, center, hr, sliced, spoiler,
youtube), plus the member mention `[mn=id]name[/mn]` its embed boxes emit. There
is no heading tag and no table tag, so headings become sized bold text and tables
are flattened.

    # readme.md -> readme.bbcode, next to the input
    python3 tools/md_to_bbcode.py ../readme.md

    # relative links and pictures need an absolute URL to hang off
    python3 tools/md_to_bbcode.py ../readme.md \
        --base-url https://github.com/AnCarsenat/Redstone-Additions/raw/main/

    # straight to the clipboard buffer of your choice
    python3 tools/md_to_bbcode.py ../docs/jetpacks.md -o -

Sizes are the pixel numbers PMC's editor itself writes — 6, 8, 10, 12 (normal),
14, 18, 24, 36 — not the 1-6 scale other boards use.

Markdown in, and what comes out:

    # heading            [size=24][b]heading[/b][/size]
    **bold** *italic*    [b]bold[/b] [i]italic[/i]
    ~~struck~~           [s]struck[/s]
    `code`               [b]code[/b]        (--inline-code picks the style)
    ```fenced```         [code]fenced[/code]
    - item               [list][*]item[/list]
    1. item              [list=1][*]item[/list]
    > quoted             [quote]quoted[/quote]
    ---                  [hr]
    [text](url)          [url=url]text[/url]
    ![alt](url)          [img]url[/img]     ([img width=.. height=..] with a size)
    | a | b |            a flattened list, or aligned text in [code]
    <p align="center">   [center]
    <details>            [spoiler]
    a YouTube link       [yt]video_id[/yt]

A picture keeps the size the source gave it — `[img width=220 height=101]` — with
the height read out of the image file, since Markdown only ever writes a width and
a width on its own squashes the picture. `--source-dir` says where those files are.

Anchor links (`[text](#section)`) lose their target — PMC has no anchors — and
come out as plain text. Anything else the file leaves behind is reported on
stderr rather than silently dropped.
"""

from __future__ import annotations

import argparse
import os
import re
import struct
import sys
from html.parser import HTMLParser
from urllib.parse import parse_qs, urljoin, urlparse

# The sizes PMC's own editor emits, in the pixel numbers it stores them as.
# 12 is body text, so a heading mapped to 12 is written as bold alone.
BODY_SIZE = 12
PMC_SIZES = (6, 8, 10, 12, 14, 18, 24, 36)
DEFAULT_HEADING_SIZES = (24, 18, 14, 12, 12, 12)

BADGE_HOSTS = {"img.shields.io", "shields.io", "badgen.net", "badge.fury.io"}

FENCE_RE = re.compile(r"^(\s{0,3})(`{3,}|~{3,})\s*([^\s`]*)\s*$")
HEADING_RE = re.compile(r"^\s{0,3}(#{1,6})\s+(.*?)\s*#*\s*$")
HR_RE = re.compile(r"^\s{0,3}([-*_])\s*(?:\1\s*){2,}$")
QUOTE_RE = re.compile(r"^\s{0,3}>\s?(.*)$")
LIST_RE = re.compile(r"^(\s*)([-*+]|\d+[.)])\s+(.*)$")
HTML_BLOCK_RE = re.compile(r"^\s{0,3}</?[a-zA-Z][^>]*>")
TABLE_DELIM_RE = re.compile(r"^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$")
ADMONITION_RE = re.compile(r'^(\s{0,3})(!!!|\?\?\?\+?)\s+(\w+)(?:\s+"([^"]*)")?\s*$')
LINK_DEF_RE = re.compile(r'^\s{0,3}\[([^\]]+)\]:\s*(\S+)(?:\s+"[^"]*")?\s*$')
ATTR_LIST_RE = re.compile(r"(?<=[)\x00])\s*\{[^{}\n]*\}")
SETEXT_H1_RE = re.compile(r"^\s{0,3}=+\s*$")
SETEXT_H2_RE = re.compile(r"^\s{0,3}-+\s*$")

YOUTUBE_HOSTS = {
    "youtube.com",
    "www.youtube.com",
    "m.youtube.com",
    "youtu.be",
    "www.youtu.be",
}

SENTINEL = "\x00%d\x00"
SENTINEL_RE = re.compile(r"\x00(\d+)\x00")


class Options:
    """Everything the conversion is allowed to decide differently."""

    def __init__(
        self,
        base_url: str | None = None,
        tables: str = "list",
        inline_code: str = "bold",
        images: str = "auto",
        heading_sizes: tuple[int, ...] = DEFAULT_HEADING_SIZES,
        badges: bool = True,
        youtube: bool = True,
        width: int = 78,
        source_dir: str = ".",
    ):
        self.base_url = base_url
        self.tables = tables
        self.inline_code = inline_code
        self.images = images
        self.heading_sizes = heading_sizes
        self.badges = badges
        self.youtube = youtube
        self.width = width
        # Where a picture's file is looked for, to work out the height that goes
        # with a width the source only gave one half of.
        self.source_dir = source_dir
        self.warnings: list[str] = []

    def warn(self, message: str) -> None:
        if message not in self.warnings:
            self.warnings.append(message)


# --------------------------------------------------------------------------
# URLs and pictures


def resolve_url(url: str, opts: Options) -> str:
    url = url.strip().strip("<>")
    if not url or url.startswith(("#", "mailto:", "data:")):
        return url
    if urlparse(url).scheme or url.startswith("//"):
        return url
    if opts.base_url:
        return urljoin(opts.base_url, url)
    opts.warn(f"relative link left as is (no --base-url): {url}")
    return url


def is_badge(url: str) -> bool:
    parts = urlparse(url)
    return parts.netloc.lower() in BADGE_HOSTS or "/badge/" in parts.path


def youtube_id(url: str) -> str | None:
    parts = urlparse(url)
    host = parts.netloc.lower()
    if host not in YOUTUBE_HOSTS:
        return None
    if host.endswith("youtu.be"):
        video = parts.path.strip("/").split("/")[0]
        return video or None
    if parts.path.startswith(("/embed/", "/shorts/", "/v/")):
        return parts.path.split("/")[2] or None
    values = parse_qs(parts.query).get("v")
    return values[0] if values else None


def local_path(src: str, opts: Options) -> str | None:
    """The file a picture's src points at, if it is one this repo holds."""
    src = src.strip().strip("<>")
    if not src:
        return None
    if not urlparse(src).scheme and not src.startswith("//"):
        candidate = os.path.join(opts.source_dir, src)
        return candidate if os.path.isfile(candidate) else None
    # An absolute URL under --base-url is a file in the repo the readme lives in.
    if opts.base_url and src.startswith(opts.base_url):
        candidate = os.path.join(opts.source_dir, src[len(opts.base_url) :].lstrip("/"))
        return candidate if os.path.isfile(candidate) else None
    return None


def image_size(path: str) -> tuple[int, int] | None:
    """Pixel size straight out of a PNG, GIF or JPEG header. No Pillow needed."""
    try:
        with open(path, "rb") as handle:
            head = handle.read(26)
            if head[:8] == b"\x89PNG\r\n\x1a\n":
                return struct.unpack(">II", head[16:24])
            if head[:6] in (b"GIF87a", b"GIF89a"):
                return struct.unpack("<HH", head[6:10])
            if head[:2] != b"\xff\xd8":
                return None
            # JPEG: walk the segments to the frame header that carries the size.
            handle.seek(2)
            while True:
                marker = handle.read(2)
                if len(marker) < 2 or marker[0] != 0xFF:
                    return None
                length = struct.unpack(">H", handle.read(2))[0]
                if 0xC0 <= marker[1] <= 0xCF and marker[1] not in (0xC4, 0xC8, 0xCC):
                    body = handle.read(5)
                    height, width = struct.unpack(">HH", body[1:5])
                    return width, height
                handle.seek(length - 2, os.SEEK_CUR)
    except (OSError, struct.error):
        return None


def image_tag(
    src: str, opts: Options, width: str | None = None, height: str | None = None
) -> str:
    url = resolve_url(src, opts)
    if not url:
        return ""
    if not opts.badges and is_badge(url):
        return ""
    if opts.images == "simg":
        return f"[simg]{url}[/simg]"
    if opts.images == "img" or not (width or height):
        return f"[img]{url}[/img]"

    # PMC sizes a picture on the tag itself: [img width=200 height=200]. A source
    # that gave only a width gets the matching height off the file, so nothing is
    # squashed; without the file there is no honest height to write.
    if width and not height:
        path = local_path(src, opts)
        size = image_size(path) if path else None
        if size and size[0]:
            height = str(round(int(width) * size[1] / size[0]))
        else:
            opts.warn(f"height guessed from no file, width only: {url}")
    attributes = "".join(
        f" {name}={value}"
        for name, value in (("width", width), ("height", height))
        if value
    )
    return f"[img{attributes}]{url}[/img]"


def attr_width(attributes: str) -> str | None:
    """Width out of an mkdocs attr_list, `{ width="220" }`."""
    match = re.search(r'width\s*=\s*"?(\d+)"?', attributes or "")
    return match.group(1) if match else None


def link_tag(href: str, text: str, opts: Options) -> str:
    url = resolve_url(href, opts)
    if url.startswith("#"):
        # PMC has no in-page anchors, so the target is worthless; keep the words.
        opts.warn(f"anchor link flattened to text: [{text}]({href})")
        return text
    if not url:
        return text
    if opts.youtube:
        video = youtube_id(url)
        if video:
            return f"[yt]{video}[/yt]"
    if not text or text == url:
        return f"[url]{url}[/url]"
    return f"[url={url}]{text}[/url]"


def code_span(text: str, opts: Options) -> str:
    style = opts.inline_code
    if style == "bold":
        return f"[b]{text}[/b]"
    if style == "italic":
        return f"[i]{text}[/i]"
    if style == "code":
        return f"[code]{text}[/code]"
    return f"`{text}`"


def heading_bbcode(level: int, text: str, opts: Options) -> str:
    size = opts.heading_sizes[min(level, len(opts.heading_sizes)) - 1]
    body = f"[b]{text}[/b]"
    if size and size != BODY_SIZE:
        body = f"[size={size}]{body}[/size]"
    return body


# --------------------------------------------------------------------------
# HTML, both whole blocks of it and the odd tag inside a paragraph


class HtmlToBBCode(HTMLParser):
    """Rewrites the HTML tags PMC has an equivalent for and drops the rest."""

    def __init__(self, opts: Options):
        super().__init__(convert_charrefs=True)
        self.opts = opts
        self.out: list[str] = []
        self.stack: list[tuple[str, str]] = []

    # -- tag table

    def _map(self, tag: str, attrs: dict[str, str]) -> tuple[str, str]:
        opts = self.opts
        if tag in ("b", "strong"):
            return "[b]", "[/b]"
        if tag in ("i", "em"):
            return "[i]", "[/i]"
        if tag in ("u", "ins"):
            return "[u]", "[/u]"
        if tag in ("s", "del", "strike"):
            return "[s]", "[/s]"
        if tag in ("code", "kbd", "tt", "samp"):
            piece = code_span("\x01", opts).split("\x01")
            return piece[0], piece[1]
        if tag == "pre":
            return "[code]", "[/code]"
        if tag == "br":
            return "\n", ""
        if tag == "hr":
            return "\n[hr]\n", ""
        if tag == "img":
            width = attrs.get("width") or attr_width(attrs.get("style", ""))
            return (
                image_tag(
                    attrs.get("src", ""),
                    opts,
                    width or None,
                    attrs.get("height") or None,
                ),
                "",
            )
        if tag == "a":
            href = resolve_url(attrs.get("href", ""), opts)
            if not href or href.startswith("#"):
                return "", ""
            return f"[url={href}]", "[/url]"
        if tag == "center":
            return "[center]", "[/center]"
        if tag in ("p", "div"):
            if _is_centered(attrs):
                return "[center]", "[/center]\n"
            return "", "\n"
        if tag in ("h1", "h2", "h3", "h4", "h5", "h6"):
            level = int(tag[1])
            open_, close = heading_bbcode(level, "\x01", opts).split("\x01")
            if _is_centered(attrs):
                return "[center]" + open_, close + "[/center]\n"
            return open_, close + "\n"
        if tag == "blockquote":
            return "[quote]", "[/quote]\n"
        if tag == "ul":
            return "\n[list]\n", "[/list]\n"
        if tag == "ol":
            return "\n[list=1]\n", "[/list]\n"
        if tag == "li":
            return "[*]", "\n"
        if tag == "details":
            return "[spoiler]", "[/spoiler]\n"
        if tag == "summary":
            return "[b]", "[/b]\n"
        if tag == "span":
            return _span_style(attrs.get("style", ""))
        if tag in ("table", "thead", "tbody", "tr", "td", "th"):
            opts.warn("HTML table flattened — PMC has no table tag")
            if tag == "tr":
                return "", "\n"
            if tag in ("td", "th"):
                return "", " "
            return "", "\n"
        return "", ""

    # -- parser hooks

    def handle_starttag(self, tag, attrs):
        # A valueless attribute (`<p align>`) parses to None, not "".
        open_, close = self._map(tag, {k: (v or "") for k, v in attrs})
        self.out.append(open_)
        if tag in ("br", "hr", "img"):
            return
        self.stack.append((tag, close))

    def handle_startendtag(self, tag, attrs):
        # A valueless attribute (`<p align>`) parses to None, not "".
        open_, close = self._map(tag, {k: (v or "") for k, v in attrs})
        self.out.append(open_ + close)

    def handle_endtag(self, tag):
        for index in range(len(self.stack) - 1, -1, -1):
            if self.stack[index][0] == tag:
                for _, close in reversed(self.stack[index:]):
                    self.out.append(close)
                del self.stack[index:]
                return

    def handle_data(self, data):
        self.out.append(data)

    def result(self) -> str:
        for _, close in reversed(self.stack):
            self.out.append(close)
        self.stack.clear()
        return "".join(self.out)


def _is_centered(attrs: dict[str, str]) -> bool:
    if attrs.get("align", "").lower() == "center":
        return True
    style = attrs.get("style", "").replace(" ", "").lower()
    return "text-align:center" in style


def _span_style(style: str) -> tuple[str, str]:
    style = style.replace(" ", "").lower()
    open_, close = "", ""
    if "font-weight:bold" in style or "font-weight:700" in style:
        open_, close = open_ + "[b]", "[/b]" + close
    if "font-style:italic" in style:
        open_, close = open_ + "[i]", "[/i]" + close
    if "text-decoration:underline" in style:
        open_, close = open_ + "[u]", "[/u]" + close
    if "text-decoration:line-through" in style:
        open_, close = open_ + "[s]", "[/s]" + close
    colour = re.search(r"(?<!-)color:([#\w]+)", style)
    if colour:
        open_, close = open_ + f"[color={colour.group(1)}]", "[/color]" + close
    return open_, close


def convert_html(fragment: str, opts: Options) -> str:
    parser = HtmlToBBCode(opts)
    parser.feed(fragment)
    parser.close()
    out = parser.result()
    # <details><summary>X</summary> is a titled spoiler, which PMC writes as one
    # tag; the summary only becomes bold text because it is parsed on its own.
    return re.sub(r"\[spoiler\]\s*\[b\](.+?)\[/b\]\n?", r"[spoiler=\1]\n", out)


# --------------------------------------------------------------------------
# Inline markdown


def inline(text: str, opts: Options, refs: dict[str, str]) -> str:
    kept: list[str] = []

    def keep(value: str) -> str:
        kept.append(value)
        return SENTINEL % (len(kept) - 1)

    # Code spans first: nothing inside one is markup.
    def code_repl(match: re.Match) -> str:
        return keep(code_span(match.group(2).strip(), opts))

    text = re.sub(r"(?<!`)(`+)(.+?)\1(?!`)", code_repl, text, flags=re.S)

    # Escaped punctuation, held aside so it cannot be read as emphasis.
    text = re.sub(r"\\([\\`*_{}\[\]()#+\-.!~|])", lambda m: keep(m.group(1)), text)

    def img_repl(match: re.Match) -> str:
        attributes = match.group(3) or ""
        height = re.search(r'height\s*=\s*"?(\d+)"?', attributes)
        return keep(
            image_tag(
                match.group(2),
                opts,
                attr_width(attributes),
                height.group(1) if height else None,
            )
        )

    # The trailing group is an mkdocs attr_list — `![x](y){ width="220" }`.
    text = re.sub(
        r"!\[([^\]]*)\]\(\s*<?([^)\s>]+)>?(?:\s+\"[^\"]*\")?\s*\)(\s*\{[^{}\n]*\})?",
        img_repl,
        text,
    )

    def ref_img_repl(match: re.Match) -> str:
        target = refs.get(match.group(2).lower() or match.group(1).lower())
        if target is None:
            return match.group(0)
        return keep(image_tag(target, opts))

    text = re.sub(r"!\[([^\]]*)\]\[([^\]]*)\]", ref_img_repl, text)

    def link_repl(match: re.Match) -> str:
        label = match.group(1)
        return keep(link_tag(match.group(2), label, opts))

    text = re.sub(
        r"\[((?:[^\[\]]|\[[^\]]*\])*)\]\(\s*<?([^)\s>]+)>?(?:\s+\"[^\"]*\")?\s*\)",
        link_repl,
        text,
    )

    # Whatever attr_list is left once the images have taken their width from it.
    # A converted link or picture ends in a sentinel rather than the `)` the
    # attr_list followed in the source.
    text = ATTR_LIST_RE.sub("", text)

    def ref_link_repl(match: re.Match) -> str:
        label = match.group(1)
        target = refs.get((match.group(2) or label).lower())
        if target is None:
            return match.group(0)
        return keep(link_tag(target, label, opts))

    text = re.sub(r"\[((?:[^\[\]])*)\]\[([^\]]*)\]", ref_link_repl, text)

    def autolink_repl(match: re.Match) -> str:
        url = match.group(1)
        return keep(link_tag(url, url, opts))

    text = re.sub(r"<((?:https?|ftp)://[^>\s]+)>", autolink_repl, text)

    text = re.sub(r"\*\*(?!\s)(.+?)(?<!\s)\*\*", r"[b]\1[/b]", text, flags=re.S)
    text = re.sub(r"(?<![\w_])__(?!\s)(.+?)(?<!\s)__(?![\w_])", r"[b]\1[/b]", text, flags=re.S)
    text = re.sub(r"~~(?!\s)(.+?)(?<!\s)~~", r"[s]\1[/s]", text, flags=re.S)
    text = re.sub(r"(?<![\w*])\*(?!\s)([^*]+?)(?<!\s)\*(?![\w*])", r"[i]\1[/i]", text)
    text = re.sub(r"(?<![\w_])_(?!\s)([^_]+?)(?<!\s)_(?![\w_])", r"[i]\1[/i]", text)

    if re.search(r"<[a-zA-Z/!]", text):
        text = convert_html(text, opts)

    # Bare URLs are left alone: PMC links them itself, except a YouTube one on
    # its own, which is worth an embed.
    if opts.youtube:
        stripped = text.strip()
        if re.fullmatch(r"https?://\S+", stripped):
            video = youtube_id(stripped)
            if video:
                text = f"[yt]{video}[/yt]"

    return SENTINEL_RE.sub(lambda m: kept[int(m.group(1))], text)


# --------------------------------------------------------------------------
# Block markdown


def convert(text: str, opts: Options) -> str:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = re.sub(r"(?s)<!--.*?-->", "", text)

    refs: dict[str, str] = {}
    kept_lines: list[str] = []
    in_fence = False
    for line in text.split("\n"):
        fence = FENCE_RE.match(line)
        if fence:
            in_fence = not in_fence
        if not in_fence:
            match = LINK_DEF_RE.match(line)
            if match:
                refs[match.group(1).lower()] = match.group(2)
                continue
        kept_lines.append(line)

    body = render_blocks(kept_lines, opts, refs)
    # `code` inside **bold**, or a bold first table column holding `code`, ends
    # up wrapped twice in the same tag.
    for tag in ("b", "i", "u", "s"):
        pattern = re.compile(rf"\[{tag}\]\[{tag}\](.*?)\[/{tag}\]\[/{tag}\]", re.S)
        while True:
            body, count = pattern.subn(rf"[{tag}]\1[/{tag}]", body)
            if not count:
                break
    body = re.sub(r"\[center\][ \t]+", "[center]", body)
    body = re.sub(r"[ \t]+\[/center\]", "[/center]", body)
    body = re.sub(r"\n{3,}", "\n\n", body).strip()
    return body + "\n"


def render_blocks(lines: list[str], opts: Options, refs: dict[str, str]) -> str:
    blocks: list[str] = []
    index = 0
    while index < len(lines):
        line = lines[index]
        if not line.strip():
            index += 1
            continue

        fence = FENCE_RE.match(line)
        if fence:
            block, index = read_fence(lines, index, fence.group(2))
            blocks.append(block)
            continue

        heading = HEADING_RE.match(line)
        if heading:
            level = len(heading.group(1))
            blocks.append(
                heading_bbcode(level, inline(heading.group(2), opts, refs), opts)
            )
            index += 1
            continue

        if HR_RE.match(line):
            blocks.append("[hr]")
            index += 1
            continue

        admonition = ADMONITION_RE.match(line)
        if admonition:
            block, index = read_admonition(lines, index, admonition, opts, refs)
            blocks.append(block)
            continue

        if QUOTE_RE.match(line):
            block, index = read_quote(lines, index, opts, refs)
            blocks.append(block)
            continue

        if is_table(lines, index):
            block, index = read_table(lines, index, opts, refs)
            blocks.append(block)
            continue

        if LIST_RE.match(line):
            block, index = read_list(lines, index, opts, refs)
            blocks.append(block)
            continue

        if HTML_BLOCK_RE.match(line):
            block, index = read_html_block(lines, index, opts)
            blocks.append(block)
            continue

        block, index = read_paragraph(lines, index, opts, refs)
        blocks.append(block)

    return "\n\n".join(part for part in blocks if part.strip())


def read_fence(lines: list[str], index: int, marker: str) -> tuple[str, int]:
    closing = re.compile(r"^\s{0,3}" + marker[0] + "{" + str(len(marker)) + r",}\s*$")
    index += 1
    body: list[str] = []
    while index < len(lines) and not closing.match(lines[index]):
        body.append(lines[index])
        index += 1
    index += 1  # step over the closing fence, or past the end of the file
    return "[code]\n" + "\n".join(body).rstrip() + "\n[/code]", index


def read_paragraph(
    lines: list[str], index: int, opts: Options, refs: dict[str, str]
) -> tuple[str, int]:
    body: list[str] = []
    while index < len(lines):
        line = lines[index]
        if not line.strip():
            break
        if body and SETEXT_H1_RE.match(line):
            text = inline(" ".join(body).strip(), opts, refs)
            return heading_bbcode(1, text, opts), index + 1
        if body and SETEXT_H2_RE.match(line) and not HR_RE.match(line):
            text = inline(" ".join(body).strip(), opts, refs)
            return heading_bbcode(2, text, opts), index + 1
        if body and (
            HEADING_RE.match(line)
            or HR_RE.match(line)
            or FENCE_RE.match(line)
            or QUOTE_RE.match(line)
            or LIST_RE.match(line)
            or HTML_BLOCK_RE.match(line)
            or is_table(lines, index)
        ):
            break
        # A soft wrap is a space in Markdown, but PMC turns every newline into a
        # line break, so a wrapped paragraph would come out broken where the
        # source file happened to end its lines. Only Markdown's own hard break —
        # two trailing spaces, or a trailing backslash — keeps the newline.
        hard = line.endswith("  ") or line.rstrip().endswith("\\")
        body.append(line.strip().rstrip("\\") + ("\n" if hard else " "))
        index += 1
    return inline("".join(body).strip(), opts, refs), index


def read_quote(
    lines: list[str], index: int, opts: Options, refs: dict[str, str]
) -> tuple[str, int]:
    body: list[str] = []
    while index < len(lines):
        match = QUOTE_RE.match(lines[index])
        if match:
            body.append(match.group(1))
        elif lines[index].strip() and body:
            body.append(lines[index].strip())  # lazy continuation
        else:
            break
        index += 1
    return "[quote]\n" + render_blocks(body, opts, refs) + "\n[/quote]", index


def read_admonition(
    lines: list[str],
    index: int,
    match: re.Match,
    opts: Options,
    refs: dict[str, str],
) -> tuple[str, int]:
    marker, kind, title = match.group(2), match.group(3), match.group(4)
    title = title if title is not None else kind.replace("-", " ").title()
    index += 1
    body: list[str] = []
    while index < len(lines):
        line = lines[index]
        if not line.strip():
            body.append("")
            index += 1
            continue
        if not line.startswith(("    ", "\t")):
            break
        body.append(line[4:] if line.startswith("    ") else line[1:])
        index += 1
    inner = render_blocks(body, opts, refs)
    if marker.startswith("???"):
        return f"[spoiler={title}]\n{inner}\n[/spoiler]", index
    heading = f"[b]{inline(title, opts, refs)}[/b]" if title else ""
    return f"[quote]\n{heading}\n{inner}\n[/quote]".replace("\n\n\n", "\n\n"), index


def read_html_block(lines: list[str], index: int, opts: Options) -> tuple[str, int]:
    body: list[str] = []
    while index < len(lines) and lines[index].strip():
        body.append(lines[index].strip())
        index += 1
    # The line breaks HTML ignores would become real ones on PMC, so the gaps
    # between tags collapse to the single space the browser would have shown.
    fragment = re.sub(r">\s+<", "> <", "\n".join(body))
    converted = convert_html(fragment, opts)
    return "\n".join(line.strip() for line in converted.split("\n")).strip(), index


def read_list(
    lines: list[str], index: int, opts: Options, refs: dict[str, str]
) -> tuple[str, int]:
    first = LIST_RE.match(lines[index])
    assert first is not None
    base_indent = len(first.group(1))
    ordered = first.group(2)[0].isdigit()
    items: list[list[str]] = []

    while index < len(lines):
        line = lines[index]
        if not line.strip():
            # A blank line ends the list unless the next line continues it.
            following = index + 1
            while following < len(lines) and not lines[following].strip():
                following += 1
            if following >= len(lines):
                break
            nxt = LIST_RE.match(lines[following])
            continues = (nxt and len(nxt.group(1)) >= base_indent) or (
                len(lines[following]) - len(lines[following].lstrip()) > base_indent
            )
            if not continues:
                break
            if items:
                items[-1].append("")
            index = following
            continue

        match = LIST_RE.match(line)
        indent = len(line) - len(line.lstrip())
        if match and len(match.group(1)) <= base_indent:
            if len(match.group(1)) < base_indent:
                break
            if match.group(2)[0].isdigit() != ordered:
                break
            items.append([match.group(3)])
            index += 1
            continue
        if indent > base_indent or (items and line.strip()):
            if not items:
                break
            items[-1].append(line[base_indent + 2 :] if indent > base_indent else line.strip())
            index += 1
            continue
        break

    rendered = []
    for item in items:
        inner = render_blocks(item, opts, refs).strip()
        # A nested list reads as part of its item, so it does not want the blank
        # line render_blocks puts between blocks.
        inner = re.sub(r"\n\n(\[list)", r"\n\1", inner)
        rendered.append("[*]" + inner)
    opening = "[list=1]" if ordered else "[list]"
    return opening + "\n" + "\n".join(rendered) + "\n[/list]", index


def is_table(lines: list[str], index: int) -> bool:
    return (
        "|" in lines[index]
        and index + 1 < len(lines)
        and "|" in lines[index + 1]
        and TABLE_DELIM_RE.match(lines[index + 1]) is not None
    )


def split_row(line: str) -> list[str]:
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]
    cells: list[str] = []
    current: list[str] = []
    in_code = False
    escaped = False
    for char in line:
        if escaped:
            current.append(char)
            escaped = False
            continue
        if char == "\\":
            escaped = True
            current.append(char)
            continue
        if char == "`":
            in_code = not in_code  # a pipe inside `code` is text, not a cell wall
        if char == "|" and not in_code:
            cells.append("".join(current).strip())
            current = []
            continue
        current.append(char)
    cells.append("".join(current).strip())
    return cells


def read_table(
    lines: list[str], index: int, opts: Options, refs: dict[str, str]
) -> tuple[str, int]:
    header = split_row(lines[index])
    index += 2
    rows: list[list[str]] = []
    while index < len(lines) and lines[index].strip() and "|" in lines[index]:
        rows.append(split_row(lines[index]))
        index += 1

    if opts.tables == "drop":
        opts.warn(f"table dropped ({len(rows)} rows)")
        return "", index
    if opts.tables == "code":
        return table_as_code(header, rows), index
    rendered = [[inline(cell, opts, refs) for cell in row] for row in rows]
    # A picture inside a [*] item wraps around the bullet and the row's text ends
    # up pinned to the bottom of the picture, so a table of pictures is written
    # as labelled blocks instead of a list.
    if any(has_picture(cell) for row in rendered for cell in row):
        return table_as_blocks(rendered), index
    return table_as_list(header, rendered, opts, refs), index


def has_picture(cell: str) -> bool:
    return "[img" in cell or "[simg]" in cell


def table_as_blocks(rows: list[list[str]]) -> str:
    blocks = []
    for cells in rows:
        if not cells:
            continue
        lines = [f"[b]{cells[0]}[/b]"] if cells[0] else []
        lines += [cell for cell in cells[1:] if cell]
        blocks.append("\n".join(lines))
    return "\n\n".join(blocks)


def table_as_code(header: list[str], rows: list[str]) -> str:
    columns = max([len(header)] + [len(row) for row in rows])
    grid = [header + [""] * (columns - len(header))]
    grid += [row + [""] * (columns - len(row)) for row in rows]
    widths = [max(len(row[col]) for row in grid) for col in range(columns)]
    out = []
    for position, row in enumerate(grid):
        out.append("  ".join(cell.ljust(widths[col]) for col, cell in enumerate(row)).rstrip())
        if position == 0:
            out.append("  ".join("-" * widths[col] for col in range(columns)))
    return "[code]\n" + "\n".join(out) + "\n[/code]"


def table_as_list(
    header: list[str], rows: list[list[str]], opts: Options, refs: dict[str, str]
) -> str:
    items = []
    for cells in rows:  # already through inline(), so the pictures can be spotted
        if not cells:
            continue
        if len(cells) == 1:
            items.append("[*]" + cells[0])
        elif len(cells) == 2:
            items.append(f"[*][b]{cells[0]}[/b] — {cells[1]}")
        else:
            titles = [inline(cell, opts, refs) for cell in header[1:]]
            details = []
            for position, cell in enumerate(cells[1:]):
                label = titles[position] if position < len(titles) else ""
                details.append(f"[*]{label}: {cell}" if label else f"[*]{cell}")
            items.append(
                f"[*][b]{cells[0]}[/b]\n[list]\n" + "\n".join(details) + "\n[/list]"
            )
    return "[list]\n" + "\n".join(items) + "\n[/list]"


# --------------------------------------------------------------------------
# CLI


def parse_sizes(value: str) -> tuple[int, ...]:
    sizes = [int(part) for part in value.split(",")]
    if len(sizes) != 6:
        raise argparse.ArgumentTypeError("--heading-sizes wants six numbers, h1 to h6")
    for size in sizes:
        if size not in PMC_SIZES:
            raise argparse.ArgumentTypeError(
                f"{size} is not a PMC font size; pick from {', '.join(map(str, PMC_SIZES))}"
            )
    return tuple(sizes)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Convert Markdown to Planet Minecraft BBCode.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("input", help="markdown file, or - for stdin")
    parser.add_argument(
        "-o",
        "--output",
        help="output file, - for stdout; default is the input named .bbcode",
    )
    parser.add_argument(
        "--base-url",
        help="absolute URL that relative links and pictures hang off",
    )
    parser.add_argument(
        "--tables",
        choices=("list", "code", "drop"),
        default="list",
        help="how to flatten tables, since PMC has no table tag (default: list)",
    )
    parser.add_argument(
        "--inline-code",
        choices=("bold", "italic", "code", "backticks"),
        default="bold",
        help="what `code` becomes; [code] is a box, not inline (default: bold)",
    )
    parser.add_argument(
        "--images",
        choices=("auto", "img", "simg"),
        default="auto",
        help="auto keeps the source's width as [img width=.. height=..]; img "
        "drops every size; simg scales to the post width (default: auto)",
    )
    parser.add_argument(
        "--source-dir",
        help="where the picture files live, for reading the height that goes "
        "with a width; default is the input file's directory",
    )
    parser.add_argument(
        "--heading-sizes",
        type=parse_sizes,
        default=DEFAULT_HEADING_SIZES,
        metavar="H1,..,H6",
        help="PMC font sizes for h1 to h6 (default: 24,18,14,12,12,12)",
    )
    parser.add_argument(
        "--no-badges",
        action="store_true",
        help="drop shields.io-style badge pictures",
    )
    parser.add_argument(
        "--no-youtube",
        action="store_true",
        help="keep YouTube links as links instead of [yt] embeds",
    )
    parser.add_argument(
        "--quiet", action="store_true", help="do not report what was flattened"
    )
    args = parser.parse_args(argv)

    if args.input == "-":
        text = sys.stdin.read()
        default_output = "-"
        source_dir = "."
    else:
        with open(args.input, encoding="utf-8") as handle:
            text = handle.read()
        default_output = os.path.splitext(args.input)[0] + ".bbcode"
        source_dir = os.path.dirname(os.path.abspath(args.input))

    opts = Options(
        base_url=args.base_url,
        tables=args.tables,
        inline_code=args.inline_code,
        images=args.images,
        heading_sizes=args.heading_sizes,
        badges=not args.no_badges,
        youtube=not args.no_youtube,
        source_dir=args.source_dir or source_dir,
    )
    bbcode = convert(text, opts)

    target = args.output or default_output
    if target == "-":
        sys.stdout.write(bbcode)
    else:
        with open(target, "w", encoding="utf-8") as handle:
            handle.write(bbcode)
        if not args.quiet:
            print(f"wrote {target}", file=sys.stderr)

    if opts.warnings and not args.quiet:
        for warning in opts.warnings:
            print(f"note: {warning}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

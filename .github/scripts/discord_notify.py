"""Post a Discord embed for a push or a published release.

Reads the GitHub event payload from GITHUB_EVENT_PATH and posts to the webhook
in DISCORD_WEBHOOK. Building the JSON here rather than in shell means commit
messages containing quotes, backticks or newlines cannot break the payload.
"""

import json
import os
import sys
import urllib.error
import urllib.request

MAX_COMMITS = 10
DESCRIPTION_LIMIT = 4096
REDSTONE_RED = 0xB02E26

WEBHOOK = os.environ.get("DISCORD_WEBHOOK", "").strip()


def truncate(text, limit):
    return text if len(text) <= limit else text[: limit - 1] + "…"


def push_embed(event):
    commits = [c for c in event.get("commits", []) if c.get("distinct", True)]
    if not commits:
        return None

    ref = os.environ.get("GITHUB_REF_NAME", "")
    lines = []
    for commit in commits[:MAX_COMMITS]:
        sha = commit.get("id", "")[:7]
        message = commit.get("message", "")
        title = truncate(message.splitlines()[0] if message else "(no message)", 100)
        author = (commit.get("author") or {}).get("username") or (
            commit.get("author") or {}
        ).get("name", "")
        suffix = f" — {author}" if author else ""
        lines.append(f"[`{sha}`]({commit.get('url', '')}) {title}{suffix}")

    hidden = len(commits) - len(lines)
    if hidden > 0:
        lines.append(f"…and {hidden} more commit{'s' if hidden > 1 else ''}.")

    plural = "s" if len(commits) > 1 else ""
    return {
        "title": f"{len(commits)} new commit{plural} on {ref}",
        "url": event.get("compare", ""),
        "description": truncate("\n".join(lines), DESCRIPTION_LIMIT),
        "color": REDSTONE_RED,
        "footer": {"text": f"pushed by {(event.get('pusher') or {}).get('name', 'someone')}"},
    }


def release_embed(event):
    release = event.get("release") or {}
    name = release.get("name") or release.get("tag_name") or "a new release"
    body = release.get("body") or "No release notes."
    return {
        "title": f"Release: {name}",
        "url": release.get("html_url", ""),
        "description": truncate(body, DESCRIPTION_LIMIT),
        "color": REDSTONE_RED,
        "footer": {"text": f"released by {(release.get('author') or {}).get('login', 'someone')}"},
    }


def main():
    if not WEBHOOK:
        print("DISCORD_WEBHOOK is empty — nothing to post.", file=sys.stderr)
        return 0

    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path or not os.path.exists(event_path):
        print("No event payload — nothing to post.", file=sys.stderr)
        return 0

    with open(event_path, encoding="utf-8") as handle:
        event = json.load(handle)

    event_name = os.environ.get("GITHUB_EVENT_NAME", "")
    if event_name == "release":
        embed = release_embed(event)
    elif event_name == "push":
        embed = push_embed(event)
    else:
        print(f"Nothing to say about a {event_name!r} event.")
        return 0

    if embed is None:
        print("No distinct commits in this push — nothing to post.")
        return 0

    repo = os.environ.get("GITHUB_REPOSITORY", "")
    embed["author"] = {
        "name": repo,
        "url": (event.get("repository") or {}).get("html_url", ""),
    }

    payload = json.dumps({"username": "Redstone Additions", "embeds": [embed]}).encode("utf-8")
    request = urllib.request.Request(
        WEBHOOK,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "User-Agent": "redstone-additions-ci",
        },
    )
    try:
        with urllib.request.urlopen(request) as response:
            print(f"Discord responded {response.status}")
    except urllib.error.HTTPError as error:
        print(
            f"Discord rejected the post: {error.code} {error.read().decode(errors='replace')}",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

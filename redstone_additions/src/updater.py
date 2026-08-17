#!/usr/bin/env python3
"""Check Modrinth for a newer Redstone Additions data pack and install it.

Run it with no arguments and it finds every copy of the pack installed under
your Minecraft saves, compares each one against the latest release on
Modrinth, asks before touching anything, then downloads and swaps the zip.

    python src/updater.py                 # scan saves, prompt per world
    python src/updater.py --check         # report only, never write
    python src/updater.py --yes           # no prompt, install straight away
    python src/updater.py --target ~/.minecraft/saves/World/datapacks

Standard library only, so it runs anywhere Python 3.9+ does.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

API = "https://api.modrinth.com/v2"
SLUG = "redstone-additions"
USER_AGENT = "AnCarsenat/Redstone-Additions/updater (github.com/AnCarsenat/Redstone-Additions)"

# redstone_additions_v5.1.5(RELEASE)-0000.zip -> 5.1.5
FILENAME_VERSION = re.compile(r"redstone[_-]additions[_-]?v?(\d+(?:\.\d+)*)", re.I)
# " v5.1.5\n" inside the pack.mcmeta description
MCMETA_VERSION = re.compile(r"v(\d+(?:\.\d+)*(?:[-+][0-9A-Za-z.-]+)?)")


# --------------------------------------------------------------------------- #
# versions
# --------------------------------------------------------------------------- #


def parse_version(text: str) -> tuple:
    """Turn "5.1.5" or "5.2.0-beta.1" into something orderable.

    Release sorts above a prerelease of the same number, which is what
    Modrinth's own ordering does.
    """
    text = text.strip().lstrip("vV").split("+", 1)[0]
    core, _, pre = text.partition("-")
    numbers = tuple(int(part) for part in re.findall(r"\d+", core))
    numbers += (0,) * (4 - len(numbers))
    return numbers, (0, pre) if pre else (1, "")


# --------------------------------------------------------------------------- #
# Modrinth
# --------------------------------------------------------------------------- #


def fetch_json(url: str):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def latest_datapack_version(slug: str, mc_version: str | None, allow_prerelease: bool):
    """Newest data pack release on Modrinth, or None if the project has none.

    The project also publishes a mod build of every release; those carry the
    fabric/forge loaders and a "+mod" version number, so they are dropped here.
    """
    versions = fetch_json(f"{API}/project/{slug}/version")

    candidates = []
    for version in versions:
        if "datapack" not in version.get("loaders", []):
            continue
        if not allow_prerelease and version.get("version_type") != "release":
            continue
        if mc_version and mc_version not in version.get("game_versions", []):
            continue
        files = version.get("files") or []
        primary = next((f for f in files if f.get("primary")), files[0] if files else None)
        if primary is None:
            continue
        candidates.append((parse_version(version["version_number"]), version, primary))

    if not candidates:
        return None, None
    _, version, primary = max(candidates, key=lambda item: item[0])
    return version, primary


def download(url: str, expected_sha512: str | None, destination: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    digest = hashlib.sha512()
    with urllib.request.urlopen(request, timeout=120) as response, destination.open("wb") as out:
        while chunk := response.read(65536):
            digest.update(chunk)
            out.write(chunk)
    if expected_sha512 and digest.hexdigest() != expected_sha512:
        destination.unlink(missing_ok=True)
        raise RuntimeError("downloaded file does not match the checksum Modrinth published")


# --------------------------------------------------------------------------- #
# finding what is installed
# --------------------------------------------------------------------------- #


def minecraft_dir() -> Path | None:
    system = platform.system()
    if system == "Windows":
        appdata = os.environ.get("APPDATA")
        candidate = Path(appdata) / ".minecraft" if appdata else None
    elif system == "Darwin":
        candidate = Path.home() / "Library" / "Application Support" / "minecraft"
    else:
        candidate = Path.home() / ".minecraft"
    return candidate if candidate and candidate.is_dir() else None


def read_mcmeta_version(raw: bytes) -> str | None:
    try:
        meta = json.loads(raw)
    except (json.JSONDecodeError, UnicodeDecodeError):
        return None
    description = meta.get("pack", {}).get("description", "")
    if isinstance(description, list):
        text = "".join(
            part.get("text", "") if isinstance(part, dict) else str(part) for part in description
        )
    elif isinstance(description, dict):
        text = description.get("text", "")
    else:
        text = str(description)
    match = MCMETA_VERSION.search(text)
    return match.group(1) if match else None


def installed_version(path: Path) -> str | None:
    """Version of the pack at `path`, which is either a zip or a folder.

    The pack.mcmeta description is the authority because a user can rename the
    zip; the filename is only a fallback for a pack.mcmeta that has no version
    in it.
    """
    try:
        if path.is_dir():
            mcmeta = path / "pack.mcmeta"
            raw = mcmeta.read_bytes() if mcmeta.is_file() else b""
        else:
            with zipfile.ZipFile(path) as archive:
                raw = archive.read("pack.mcmeta")
    except (OSError, KeyError, zipfile.BadZipFile):
        raw = b""

    version = read_mcmeta_version(raw) if raw else None
    if version:
        return version
    match = FILENAME_VERSION.search(path.name)
    return match.group(1) if match else None


def looks_like_our_pack(path: Path) -> bool:
    if FILENAME_VERSION.search(path.name):
        return True
    try:
        if path.is_dir():
            return (path / "data" / "ra").is_dir()
        with zipfile.ZipFile(path) as archive:
            return any(name.startswith("data/ra/") for name in archive.namelist()[:5000])
    except (OSError, zipfile.BadZipFile):
        return False


def find_installs(targets: list[Path]) -> list[Path]:
    """Every copy of the pack inside the given datapacks folders."""
    found = []
    for folder in targets:
        if not folder.is_dir():
            continue
        for entry in sorted(folder.iterdir()):
            if entry.name.startswith(".") or entry.name.endswith((".old", ".bak")):
                continue
            if entry.is_dir() or entry.suffix.lower() == ".zip":
                if looks_like_our_pack(entry):
                    found.append(entry)
    return found


def datapack_folders(explicit: list[str]) -> list[Path]:
    """Resolve --target arguments, or fall back to scanning every world."""
    if explicit:
        folders = []
        for raw in explicit:
            path = Path(raw).expanduser()
            # Accept either a world folder or its datapacks folder.
            folders.append(path / "datapacks" if (path / "datapacks").is_dir() else path)
        return folders

    root = minecraft_dir()
    if root is None:
        return []
    saves = root / "saves"
    if not saves.is_dir():
        return []
    return [world / "datapacks" for world in sorted(saves.iterdir()) if (world / "datapacks").is_dir()]


# --------------------------------------------------------------------------- #
# installing
# --------------------------------------------------------------------------- #


def install(current: Path, downloaded: Path, filename: str, keep_old: bool) -> Path:
    """Replace the pack at `current` with the downloaded zip.

    The old copy is moved aside first and only deleted once the new one is in
    place, so a failure half way through leaves the world with a working pack.
    """
    folder = current.parent
    backup = folder / (current.name + ".bak")
    if backup.exists():
        shutil.rmtree(backup) if backup.is_dir() else backup.unlink()

    was_folder = current.is_dir()
    destination = folder / (current.name if was_folder else filename)

    current.rename(backup)
    try:
        if was_folder:
            # Installed unzipped, so keep it unzipped: same folder name, new contents.
            destination.mkdir()
            with zipfile.ZipFile(downloaded) as archive:
                archive.extractall(destination)
        else:
            shutil.copy2(downloaded, destination)
    except Exception:
        if destination.exists():
            shutil.rmtree(destination) if destination.is_dir() else destination.unlink()
        backup.rename(current)
        raise

    if keep_old:
        backup.rename(folder / (current.name + ".old"))
    else:
        shutil.rmtree(backup) if backup.is_dir() else backup.unlink()
    return destination


def confirm(question: str, assume_yes: bool) -> bool:
    if assume_yes:
        return True
    if not sys.stdin.isatty():
        print("  not a terminal and --yes was not given, so nothing was changed")
        return False
    try:
        answer = input(f"{question} [y/N] ").strip().lower()
    except (EOFError, KeyboardInterrupt):
        print()
        return False
    return answer in ("y", "yes")


# --------------------------------------------------------------------------- #


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--target", action="append", default=[], metavar="PATH",
                        help="a world folder or datapacks folder to update; repeatable. "
                             "Default: every world under your Minecraft saves")
    parser.add_argument("--slug", default=SLUG, help=f"Modrinth project slug (default: {SLUG})")
    parser.add_argument("--mc-version", metavar="X.Y.Z",
                        help="only consider releases that list this Minecraft version")
    parser.add_argument("--prerelease", action="store_true",
                        help="also consider alpha and beta releases")
    parser.add_argument("--check", action="store_true", help="report only, never write anything")
    parser.add_argument("-y", "--yes", action="store_true", help="install without asking")
    parser.add_argument("--keep-old", action="store_true",
                        help="rename the replaced pack to .old instead of deleting it")
    args = parser.parse_args(argv)

    try:
        latest, file_info = latest_datapack_version(args.slug, args.mc_version, args.prerelease)
    except urllib.error.HTTPError as error:
        print(f"Modrinth returned {error.code} for project '{args.slug}'", file=sys.stderr)
        return 2
    except (urllib.error.URLError, TimeoutError) as error:
        print(f"could not reach Modrinth: {error}", file=sys.stderr)
        return 2

    if latest is None:
        print("no matching data pack release found on Modrinth")
        return 1

    latest_number = latest["version_number"]
    print(f"Latest on Modrinth: {latest_number} ({latest['version_type']}, "
          f"{latest['date_published'][:10]})")

    folders = datapack_folders(args.target)
    installs = find_installs(folders)
    if not installs:
        where = ", ".join(str(f) for f in folders) if folders else "your Minecraft saves"
        print(f"no installed copy of the pack found in {where}")
        return 1

    exit_code = 0
    for path in installs:
        current = installed_version(path)
        label = f"{path.parent.parent.name}: {path.name}"
        if current is None:
            print(f"\n{label}\n  version unreadable, skipping")
            exit_code = 1
            continue

        if parse_version(current) >= parse_version(latest_number):
            print(f"\n{label}\n  v{current} is up to date")
            continue

        print(f"\n{label}\n  v{current} -> v{latest_number}")
        if args.check:
            exit_code = 1
            continue
        if not confirm("  Update this world?", args.yes):
            print("  skipped")
            continue

        with tempfile.TemporaryDirectory() as tmp:
            downloaded = Path(tmp) / file_info["filename"]
            print(f"  downloading {file_info['filename']} "
                  f"({file_info['size'] / 1_048_576:.1f} MiB)")
            try:
                download(file_info["url"], file_info.get("hashes", {}).get("sha512"), downloaded)
                installed = install(path, downloaded, file_info["filename"], args.keep_old)
            except Exception as error:
                print(f"  failed: {error}", file=sys.stderr)
                exit_code = 2
                continue
        print(f"  installed {installed.name}")
        print("  run /reload in that world, or restart it, to load the new version")

    return exit_code


if __name__ == "__main__":
    sys.exit(main())

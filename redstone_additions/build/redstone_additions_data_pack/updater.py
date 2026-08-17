#!/usr/bin/env python3
"""Check Modrinth for a newer Redstone Additions and update this copy of it.

This script ships inside the data pack, so it already knows what it is looking
at: the folder it sits in is the pack it updates. Drop into that folder and run

    python updater.py            # check, ask, install
    python updater.py --check    # report only, never write
    python updater.py --yes      # install without asking

If you installed the pack as a zip, pull this file out next to the zip inside
your world's datapacks folder and run it there; it will find the zip itself.

Standard library only, so it runs anywhere Python 3.9+ does.
"""

from __future__ import annotations

import argparse
import hashlib
import json
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

    A release sorts above a prerelease of the same number, which is the order
    Modrinth itself uses.
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
    """Newest data pack release on Modrinth, or (None, None) if there is none.

    Every release is also published as a mod; those carry the fabric/forge
    loaders and a "+mod" version number, so they are dropped here.
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
        raise RuntimeError("the download does not match the checksum Modrinth published")


# --------------------------------------------------------------------------- #
# working out what is installed
# --------------------------------------------------------------------------- #


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
    """Version of the pack at `path`, which is either a folder or a zip.

    The pack.mcmeta description is the authority because a zip can be renamed;
    the filename is only a fallback for a pack.mcmeta with no version in it.
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


def locate_pack(here: Path) -> Path | None:
    """The pack this script belongs to.

    Normally that is the folder the script sits in, because the script is
    shipped inside the pack. If there is no pack.mcmeta beside it the script
    was pulled out into the datapacks folder, so look for the pack there.
    """
    if (here / "pack.mcmeta").is_file():
        return here

    for entry in sorted(here.iterdir()):
        if entry.name.startswith(".") or entry.name.endswith((".old", ".bak")):
            continue
        if entry.is_dir() or entry.suffix.lower() == ".zip":
            if looks_like_our_pack(entry):
                return entry
    return None


# --------------------------------------------------------------------------- #
# installing
# --------------------------------------------------------------------------- #


def install(current: Path, downloaded: Path, filename: str, keep_old: bool) -> Path:
    """Replace the pack at `current` with the downloaded zip.

    The old copy is moved aside first and only deleted once the new one is in
    place, so a failure half way through leaves the world with a working pack.
    A folder keeps its name, so the world's enabled-pack list still matches.
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
            # A release older than this script does not carry a copy of it, and
            # the extract above has just replaced the one that was running.
            # Put it back so the pack can still update itself next time.
            running = Path(__file__).resolve()
            if running.parent == current.resolve() and not (destination / running.name).exists():
                shutil.copy2(backup / running.name, destination / running.name)
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
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("--check", action="store_true", help="report only, never write anything")
    parser.add_argument("-y", "--yes", action="store_true", help="install without asking")
    parser.add_argument("--keep-old", action="store_true",
                        help="rename the replaced pack to .old instead of deleting it")
    parser.add_argument("--mc-version", metavar="X.Y.Z",
                        help="only consider releases that list this Minecraft version")
    parser.add_argument("--prerelease", action="store_true",
                        help="also consider alpha and beta releases")
    parser.add_argument("--slug", default=SLUG, help=f"Modrinth project slug (default: {SLUG})")
    args = parser.parse_args(argv)

    here = Path(__file__).resolve().parent
    pack = locate_pack(here)
    if pack is None:
        print(f"no Redstone Additions pack found in {here}", file=sys.stderr)
        print("run this script from inside the pack folder, or next to the pack zip "
              "in your world's datapacks folder", file=sys.stderr)
        return 2

    current = installed_version(pack)
    if current is None:
        print(f"cannot read the version of {pack.name}", file=sys.stderr)
        return 2
    print(f"Installed: v{current}  ({pack})")

    try:
        latest, file_info = latest_datapack_version(args.slug, args.mc_version, args.prerelease)
    except urllib.error.HTTPError as error:
        print(f"Modrinth returned {error.code} for project '{args.slug}'", file=sys.stderr)
        return 2
    except (urllib.error.URLError, TimeoutError) as error:
        print(f"could not reach Modrinth: {error}", file=sys.stderr)
        return 2

    if latest is None:
        print("no matching data pack release found on Modrinth", file=sys.stderr)
        return 2

    latest_number = latest["version_number"]
    print(f"Latest:    v{latest_number}  ({latest['version_type']}, "
          f"{latest['date_published'][:10]})")

    if parse_version(current) >= parse_version(latest_number):
        print("Already up to date.")
        return 0

    print(f"\nUpdate available: v{current} -> v{latest_number}")
    if args.check:
        return 1
    if not confirm("Download and install it?", args.yes):
        print("Nothing was changed.")
        return 1

    with tempfile.TemporaryDirectory() as tmp:
        downloaded = Path(tmp) / file_info["filename"]
        print(f"Downloading {file_info['filename']} "
              f"({file_info['size'] / 1_048_576:.1f} MiB)")
        try:
            download(file_info["url"], file_info.get("hashes", {}).get("sha512"), downloaded)
            installed = install(pack, downloaded, file_info["filename"], args.keep_old)
        except Exception as error:
            print(f"failed: {error}", file=sys.stderr)
            return 2

    print(f"Installed {installed.name}")
    print("Run /reload in that world, or restart it, to load the new version.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

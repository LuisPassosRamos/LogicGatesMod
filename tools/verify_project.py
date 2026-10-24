"""Check documentation, versions, generated assets and the production JAR without rewriting sources."""
import argparse
import hashlib
import json
import posixpath
import re
import subprocess
import sys
import zipfile
from pathlib import Path
from urllib.parse import unquote

from project_config import ROOT, configuration, production_jar
from build_info import identity
from gate_catalog import CATALOG, model_count
from verify_languages import check as check_languages

REMOVED_CLASSES = ("EnergyNodeComponent", "ToggleSourceComponent", "BlockContextAware")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def repository():
    config = configuration()
    require(re.fullmatch(r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?", config["version"]), "Version must follow SemVer")
    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    require(f"## [{config['version']}]" in changelog, "Current version missing from CHANGELOG.md")
    for name in REMOVED_CLASSES:
        require(not list((ROOT / "src/main/java").rglob(f"{name}.java")), f"Unused placeholder returned: {name}")
    # Verify local links in maintained docs, rather than stale copies of remote documentation.
    documents = [ROOT / "README.md", ROOT / "CHANGELOG.md", *(ROOT / "docs").rglob("*.md"),
                 *(ROOT / "assets").rglob("*.md")]
    for document in documents:
        text = document.read_text(encoding="utf-8")
        for target in re.findall(r"\]\(([^\s)]+)(?:\s+\"[^\"]*\")?\)", text):
            if target.startswith(("http:", "https:", "mailto:", "#")):
                continue
            target = unquote(target.split("#", 1)[0].strip("<>"))
            require(not re.match(r"[A-Za-z]:", target), f"Machine-specific link in {document.relative_to(ROOT)}")
            require((document.parent / target).exists(), f"Broken link in {document.relative_to(ROOT)}: {target}")
    keys = check_languages(ROOT)
    print(f"Repository checked: version {config['version']}, {len(documents)} documents, {keys} translation keys")


def assets():
    for name in ("generate_cable_assets.py", "generate_terminal_assets.py", "generate_gate_assets.py",
                 "generate_workbench_assets.py", "generate_inventory_assets.py", "sync_gate_projects.py", "model_icons.py"):
        subprocess.run([sys.executable, str(ROOT / "tools" / name), "--check"], cwd=ROOT, check=True)
    from verify_workshop import check
    check()


def distribution(path):
    config = configuration()
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        manifest = json.loads(archive.read("manifest.json"))
        info = dict(line.split("=", 1) for line in archive.read("logicgates-build.properties").decode().splitlines())
        require(manifest["Version"] == config["version"] == info["version"], "Packaged version mismatch")
        require(manifest["ServerVersion"] == "=" + config["hytaleVersion"], "Packaged Hytale target mismatch")
        require(info["hytaleVersion"] == config["hytaleVersion"], "Build identity Hytale target mismatch")
        require(re.fullmatch(r"[a-f0-9]{64}", info["sourceFingerprint"]), "Missing source fingerprint")
        require(info["sourceFingerprint"] == identity()["sourceFingerprint"], "Packaged sources are stale; rebuild the JAR")
        require(info["buildId"] == f"{config['version']}+{info['sourceFingerprint'][:12]}", "Incorrect build ID")
        require("sisso/logicsim/LogicSimPlugin.class" in names, "Plugin entry class missing")
        resource_root = ROOT / "src/main/resources"
        ui_resources = {source.relative_to(resource_root).as_posix()
                        for source in (resource_root / "Common/UI/Custom").rglob("*.ui")}
        require(ui_resources.issubset(names), f"Missing Custom UI documents: {sorted(ui_resources - set(names))}")
        for name in names:
            require(not name.endswith((".bbmodel", ".py", ".bak")), f"Authoring file packaged: {name}")
            require("/integration/" not in name and not name.endswith("Test.class"), f"Test class packaged: {name}")
            require(not any(name.endswith(f"/{old}.class") for old in REMOVED_CLASSES), f"Placeholder packaged: {name}")
        expected = {f"Common/Items/LogicGates/{gate['folder']}/{gate['itemId']}_{state}_Connections_{mask}.blockymodel"
                    for gate in CATALOG["gates"] for state in ("On", "Off") for mask in range(16)
                    if mask & ~gate["allowedMask"] == 0}
        require(len(expected) == model_count(), "Gate catalog has duplicate model identifiers")
        require(expected.issubset(names), f"Missing generated gate models: {sorted(expected - set(names))}")
        originals = {"Common/Items/LogicGates/" + source.relative_to(ROOT / "assets/models").as_posix()
                     for source in (ROOT / "assets/models").rglob("*.blockymodel")}
        require(not originals.intersection(names), "Authoring model packaged")
        from verify_workshop import packaged
        packaged(archive)
    print(f"Distribution checked: {path.name}, build {info['buildId']}")


def release(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), "Duplicate files in release ZIP")
        data = json.loads(archive.read("release.json"))
        require(data["file"] == production_jar().name, "Unexpected production JAR in release")
        jar = archive.read(data["file"])
        require(jar == production_jar().read_bytes(), "Release does not contain the verified production JAR")
        checksum = hashlib.sha256(jar).hexdigest()
        require(data["sha256"] == checksum and data["sizeBytes"] == len(jar), "Release metadata does not match JAR")
        require(archive.read(data["file"] + ".sha256").decode() == f"{checksum}  {data['file']}\n", "Release checksum mismatch")
        require("README.md" in names and "CHANGELOG.md" in names and "docs/README.md" in names, "Release documentation missing")
        for name in names:
            if not name.endswith(".md"):
                continue
            for target in re.findall(r"\]\(([^\s)]+)(?:\s+\"[^\"]*\")?\)", archive.read(name).decode("utf-8")):
                if target.startswith(("http:", "https:", "mailto:", "#")):
                    continue
                target = unquote(target.split("#", 1)[0].strip("<>"))
                resolved = posixpath.normpath(posixpath.join(posixpath.dirname(name), target))
                require(resolved in names, f"Broken release documentation link: {name} -> {target}")
    print(f"Release ZIP checked: {path.name}, documentation and SHA-256 match")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("repository", "assets", "distribution", "release"))
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--zip", type=Path)
    args = parser.parse_args()
    try:
        if args.mode == "repository": repository()
        elif args.mode == "assets": assets()
        elif args.mode == "release":
            require(args.zip is not None, "--zip is required")
            release(args.zip)
        else:
            require(args.jar is not None, "--jar is required")
            distribution(args.jar)
    except (ValueError, OSError, subprocess.CalledProcessError, KeyError, zipfile.BadZipFile) as error:
        raise SystemExit(str(error)) from error


if __name__ == "__main__":
    main()

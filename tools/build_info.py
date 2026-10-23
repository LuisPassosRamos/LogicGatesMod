"""Produce deterministic build identity and distributable checksums (standard library only)."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

from project_config import ROOT, configuration


def write_if_changed(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists() or path.read_bytes() != content:
        path.write_bytes(content)


def git(*arguments):
    try:
        result = subprocess.run(["git", *arguments], cwd=ROOT, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


def identity():
    digest = hashlib.sha256()
    paths = [path for path in (ROOT / "src/main").rglob("*") if path.is_file()]
    paths += [ROOT / name for name in ("build.gradle.kts", "settings.gradle.kts", "gradle.properties",
                                      "tools/build_info.py", "tools/project_config.py")]
    for path in sorted(paths, key=lambda p: p.relative_to(ROOT).as_posix()):
        digest.update(path.relative_to(ROOT).as_posix().encode("utf-8") + b"\0")
        digest.update(path.read_bytes() + b"\0")
    values = dict(configuration())
    status = git("status", "--porcelain")
    values.update(revision=git("rev-parse", "--short=12", "HEAD"),
                  dirty="unknown" if status == "unknown" else ("true" if status else "false"),
                  sourceFingerprint=digest.hexdigest())
    values["buildId"] = f"{values['version']}+{values['sourceFingerprint'][:12]}"
    return values


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--resources", type=Path)
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.resources:
        data = identity()
        content = "".join(f"{key}={value}\n" for key, value in sorted(data.items())).encode("utf-8")
        write_if_changed(args.resources / "logicgates-build.properties", content)
        print(f"Build identity: {data['buildId']}")
    elif args.jar and args.output:
        with zipfile.ZipFile(args.jar) as archive:
            data = dict(line.split("=", 1) for line in archive.read("logicgates-build.properties").decode().splitlines())
        checksum = hashlib.sha256(args.jar.read_bytes()).hexdigest()
        data.update(file=args.jar.name, sha256=checksum, sizeBytes=args.jar.stat().st_size)
        write_if_changed(args.output / "release.json", (json.dumps(data, indent=2) + "\n").encode("utf-8"))
        write_if_changed(args.jar.with_suffix(".jar.sha256"), f"{checksum}  {args.jar.name}\n".encode("ascii"))
        print(f"Release: {args.jar.name}, SHA-256 {checksum}")
    else:
        parser.error("Use --resources, or --jar together with --output")


if __name__ == "__main__":
    main()

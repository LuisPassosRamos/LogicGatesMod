"""Shared, dependency-free project paths and canonical release configuration."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def configuration():
    manifest = json.loads((ROOT / "src/main/resources/manifest.json").read_text(encoding="utf-8"))
    return {"version": manifest["Version"], "hytaleVersion": manifest["ServerVersion"].removeprefix("=")}


def production_jar():
    return ROOT / "build/libs" / f"LogicGates-{configuration()['version']}.jar"

"""Authoring exports live outside the runtime resource pack."""
import json
from project_config import ROOT


def read_model(folder, name):
    path = ROOT / "assets/models" / folder / name
    return json.loads(path.read_text(encoding="utf-8"))

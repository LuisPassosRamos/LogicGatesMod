"""Map quiet pixel material swatches to boxes without changing their physical size.

Imagegen owns the source pixels. This module only copies the PNG and changes model UVs.
The atlas has sixteen material cells; sampling their centers tolerates painted seams.
"""
from functools import lru_cache
import json
import struct

from project_config import ROOT

CATALOG = json.loads((ROOT / "assets/workshop/catalog.json").read_text(encoding="utf-8"))
if CATALOG["schemaVersion"] != 1:
    raise ValueError("Unsupported presentation catalog")
ITEMS = CATALOG["items"]
PATCH_SIZE = 256
FACES = ("back", "right", "front", "left", "top", "bottom")


def atlas(item_id):
    return ROOT / f"assets/textures/workshop/Logic_{ITEMS[item_id]['atlas']}_Atlas.png"


@lru_cache(maxsize=None)
def dimensions(item_id):
    data = atlas(item_id).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    return struct.unpack(">II", data[16:24])


def tile(item_id, column, row, patch_size=PATCH_SIZE):
    width, height = dimensions(item_id)
    return (round((column + .5) * width / 4) - patch_size // 2,
            round((row + .5) * height / 4) - patch_size // 2)


def apply_surface(shape, item_id, role="paint", on=False):
    # The source has sparse, broad pixel highlights. Keep enough of each swatch
    # to show those shades, without crossing cell boundaries. The bench keeps
    # its original authored UVs/materials; its decorative module is independent.
    patch_size = 64 if role == "indicator" else PATCH_SIZE
    for axis in "xyz":
        shape["stretch"][axis] *= shape["settings"]["size"][axis] / patch_size
    shape["settings"]["size"] = dict.fromkeys("xyz", patch_size)
    sides, top, bottom = (2, 0), (1, 0), (3, 0)
    if role == "iron":
        sides, top, bottom = (1, 1), (0, 1), (3, 3)
    elif role in ("curve", "inversion"):
        sides, top, bottom = (2, 0), (0, 0), (3, 0)
    elif role == "wood":
        sides = top = bottom = (3, 1)
    elif role == "timber":
        sides, top, bottom = (2, 0), (0, 0), (3, 0)
    elif role == "module":
        sides = top = bottom = (3, 1)
    elif role == "indicator":
        sides, top = (3 if on else 1, 2), (2 if on else 1, 2)
        bottom = sides
    elif role == "wire":
        sides, top, bottom = (1, 3), (0, 3), (1, 3)
    elif role == "label":
        sides = top = bottom = (2, 3)
    elif role == "dark":
        sides = top = bottom = (3, 3)
    cells = dict.fromkeys(FACES, sides)
    cells.update(top=top, bottom=bottom)
    shape["textureLayout"] = {
        face: {"offset": dict(zip("xy", tile(item_id, *cell, patch_size))),
               "mirror": {"x": False, "y": False}, "angle": 0}
        for face, cell in cells.items()
    }
    shape["shadingMode"] = "fullbright" if role == "indicator" and on else "flat"


def gate_role(name):
    if name.startswith("indicator"):
        return "indicator"
    if name.startswith("extra_curve"):
        return "curve"
    if name == "inversion_mark":
        return "inversion"
    if name.startswith("input_") or name == "output":
        return "wire"
    if name.startswith("port_"):
        return "label"
    # The body uses one color family. Shape, labels and subtle shades identify it.
    return "casing" if name == "body_1" else "paint"

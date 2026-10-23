"""UV mapping for the generated metallic XOR/XNOR atlas; never edits PNG pixels."""
from functools import lru_cache
import struct

from generate_cable_assets import ROOT

ATLAS = ROOT / "assets/textures/Logic_Parity_Metal_Texture.png"
PATCH_SIZE = 64
FACES = ("back", "right", "front", "left", "top", "bottom")


@lru_cache(maxsize=1)
def atlas_size():
    data = ATLAS.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    return struct.unpack(">II", data[16:24])


def tile(column, row):
    width, height = atlas_size()
    # Center patches stay clear of cell borders and any unrelated material.
    return (round((column + .5) * width / 4) - PATCH_SIZE // 2,
            round((row + .5) * height / 4) - PATCH_SIZE // 2)


def apply_surface(shape, name, on=False):
    # UV dimensions and physical dimensions are independent in Blockymodel.
    # Preserve the latter while mapping a real brushed-metal patch to each face.
    for axis in "xyz":
        shape["stretch"][axis] *= shape["settings"]["size"][axis] / PATCH_SIZE
    shape["settings"]["size"] = dict.fromkeys("xyz", PATCH_SIZE)
    if name == "indicator":
        cells = dict.fromkeys(FACES, (3 if on else 1, 2))
        cells["top"] = (2 if on else 0, 2)
    elif name.startswith("input_") or name == "output":
        cells = dict.fromkeys(FACES, (1, 3))
        cells["top"] = (0, 3)
    else:
        cells = {"top": (0, 0), "front": (1, 0), "back": (2, 0),
                 "left": (3, 0), "right": (2, 0), "bottom": (3, 0)}
        if name.startswith("extra_curve_") or name == "inversion_mark":
            cells["top"] = (0, 1)
            cells["front"] = (2, 1)
        elif name in ("body_2", "body_3", "body_5", "body_6"):
            cells["top"] = (1, 0)
    shape["textureLayout"] = {
        face: {"offset": dict(zip("xy", tile(*cell))), "mirror": {"x": False, "y": False}, "angle": 0}
        for face, cell in cells.items()
    }
    shape["shadingMode"] = "fullbright" if name == "indicator" and on else "flat"

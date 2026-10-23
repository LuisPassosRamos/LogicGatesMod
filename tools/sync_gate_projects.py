"""Create portable Blockbench projects from the editable XOR/XNOR Hytale sources.

The .blockymodel files are authoritative exports. After editing/exporting in Blockbench,
run this command and generate_gate_assets.py. --check never rewrites project files.
"""
import argparse
import base64
import json
import struct
import uuid

from generate_cable_assets import ROOT, RESOURCES
from gate_catalog import GATES
from art_sources import read_model
from asset_io import write_asset

FACES = {"north": "back", "south": "front", "west": "left", "east": "right",
         "up": "top", "down": "bottom"}


def project(kind):
    folder = GATES[kind]["folder"]
    directory = RESOURCES / f"Common/Items/LogicGates/{folder}"
    source = read_model(folder, f"Logic_{kind}_Gate.blockymodel")
    texture = directory / f"Logic_{kind}_Gate_Texture.png"
    return portable(source, texture, f"Logic_{kind}_Gate", kind)


def portable(source, texture, name, kind):
    """Keep author boxes/UVs/stretch and embed the actual source texture."""
    elements = []
    for node in source["nodes"]:
        # These prop sources intentionally use independent, unrotated boxes.
        assert not node.get("children") and node["orientation"] == dict(x=0, y=0, z=0, w=1)
        shape = node["shape"]
        size = [shape["settings"]["size"][a] for a in "xyz"]
        center = [node["position"][a] + shape["offset"][a] for a in "xyz"]
        faces = {}
        for bb_face, hytale_face in FACES.items():
            uv = shape["textureLayout"][hytale_face]
            assert uv["angle"] == 0
            w = size[2] if hytale_face in ("left", "right") else size[0]
            h = size[2] if hytale_face in ("top", "bottom") else size[1]
            u, v = uv["offset"]["x"], uv["offset"]["y"]
            faces[bb_face] = {"uv": [u, v, u + (-w if uv["mirror"]["x"] else w),
                                                v + (-h if uv["mirror"]["y"] else h)], "texture": 0}
        elements.append({
            "name": node["name"], "type": "cube", "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"logicgates/{kind}/{node['id']}")),
            "box_uv": False, "export": True, "autouv": 0,
            "from": [center[i] - size[i] / 2 for i in range(3)],
            "to": [center[i] + size[i] / 2 for i in range(3)],
            "origin": [node["position"][a] for a in "xyz"],
            "stretch": [shape["stretch"][a] for a in "xyz"],
            "shading_mode": shape["shadingMode"], "double_sided": shape["doubleSided"],
            "faces": faces
        })
    width, height = struct.unpack(">II", texture.read_bytes()[16:24])
    return {
        "meta": {"format_version": "5.0", "model_format": "hytale_prop", "box_uv": False},
        "name": name, "resolution": {"width": width, "height": height},
        "visible_box": [1, 1, 0], "elements": elements, "groups": [],
        "outliner": [element["uuid"] for element in elements],
        "textures": [{"name": texture.name, "id": "0", "width": width, "height": height,
                      "uv_width": width, "uv_height": height, "use_as_default": True,
                      "internal": True, "saved": False,
                      "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"logicgates/{kind}/texture")),
                      "source": "data:image/png;base64," + base64.b64encode(texture.read_bytes()).decode("ascii")}]
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    projects = {ROOT / f"assets/blockbench/{kind}/Logic_{kind}_Gate.bbmodel": project(kind) for kind in ("XOR", "XNOR")}
    source = read_model("Workbench", "Logic_Workbench.blockymodel")
    texture = RESOURCES / "Common/Items/LogicGates/Workbench/Logic_Workbench_Texture.png"
    projects[ROOT / "assets/blockbench/Workbench/Logic_Workbench.bbmodel"] = portable(source, texture, "Logic_Workbench", "Workbench")
    for path, data in projects.items():
        content = (json.dumps(data, indent=2) + "\n").encode("utf-8")
        if args.check:
            if not path.exists() or path.read_bytes() != content:
                raise SystemExit(f"Outdated Blockbench project: {path}")
        else:
            write_asset(path, content)
    print(f"{len(projects)} editable Blockbench projects {'checked' if args.check else 'generated'}")


if __name__ == "__main__":
    main()

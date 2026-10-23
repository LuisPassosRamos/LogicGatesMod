"""Generate enlarged lever/lamp bodies, connected models and matching compound hitboxes.

Original models remain the art sources. Physical geometry stays approved while UV sizes
select workshop materials. Child positions are relative to the parent's shape center.
"""
import argparse
from copy import deepcopy
from itertools import product
import json
import math

from generate_cable_assets import RESOURCES, DIRECTIONS, THICKNESS, box
from art_sources import read_model
from workshop_material import atlas, apply_surface
from asset_io import write_asset

BODY_SCALE = 2
AXES = "xyz"


def read(relative):
    return json.loads((RESOURCES / relative).read_text(encoding="utf-8"))


def body_nodes(kind, on):
    suffix = "_On" if kind == "Lever" and on else ""
    original = read_model(kind, f"Logic_{kind}{suffix}.blockymodel")
    if kind == "Lever":
        nodes = [deepcopy(n) for n in original["nodes"] if n["id"] != "4"]
    else:
        nodes = []

        def flatten(node, parent_center):
            # All nodes in the source lamp are unrotated. Flatten before discarding its wire.
            assert node["orientation"] == {"x": 0, "y": 0, "z": 0, "w": 1}
            origin = {a: parent_center[a] + node["position"][a] for a in AXES}
            center = {a: origin[a] + node["shape"]["offset"][a] for a in AXES}
            if node["id"] != "1":
                flat = deepcopy(node)
                flat.pop("children", None)
                flat["position"] = origin
                nodes.append(flat)
            for child in node.get("children", []):
                flatten(child, center)

        for root in original["nodes"]:
            flatten(root, dict.fromkeys(AXES, 0))
    for i, node in enumerate(nodes, 1):
        node["id"], node["name"] = str(i), f"body_{i}"
        for vector in (node["position"], node["shape"]["offset"], node["shape"]["stretch"]):
            for axis in AXES:
                vector[axis] *= BODY_SCALE
        role = ("iron" if i == 1 else "copper" if i == 2 else "wood") if kind == "Lever" \
            else ("indicator" if i == 4 else "iron")
        apply_surface(node["shape"], "Logic_" + kind, role, on)
    return nodes


def model(kind, on, mask):
    nodes = body_nodes(kind, on)
    for bit, name, x, z, width, depth in DIRECTIONS:
        if not mask & bit:
            continue
        arm = box(len(nodes) + 1, name, x, z,
                  THICKNESS if width == 2 else width,
                  THICKNESS if depth == 2 else depth)
        apply_surface(arm["shape"], "Logic_" + kind, "wire")
        nodes.append(arm)
    return {"nodes": nodes, "format": "prop", "lod": "auto"}


def rotate(vector, quaternion):
    norm = math.sqrt(sum(v * v for v in quaternion.values()))
    x, y, z, w = (quaternion[a] / norm for a in "xyzw")
    vx, vy, vz = vector
    tx, ty, tz = 2 * (y * vz - z * vy), 2 * (z * vx - x * vz), 2 * (x * vy - y * vx)
    return (vx + w * tx + y * tz - z * ty,
            vy + w * ty + z * tx - x * tz,
            vz + w * tz + x * ty - y * tx)


def hitboxes(generated_model):
    boxes = []
    for node in generated_model["nodes"]:
        shape = node["shape"]
        corners = []
        for signs in product((-1, 1), repeat=3):
            point = [shape["offset"][a] + s * shape["settings"]["size"][a]
                     * shape["stretch"][a] / 2 for a, s in zip(AXES, signs)]
            rotated = rotate(point, node["orientation"])
            corners.append([(node["position"][a] + value) / 32 + (0 if a == "y" else .5)
                            for a, value in zip(AXES, rotated)])
        boxes.append({
            "Min": {a.upper(): round(min(p[i] for p in corners), 9) for i, a in enumerate(AXES)},
            "Max": {a.upper(): round(max(p[i] for p in corners), 9) for i, a in enumerate(AXES)}
        })
    return {"Boxes": boxes}


def visual(kind, on, mask):
    state = f"{'On' if on else 'Off'}_Connections_{mask}"
    variant = state if kind == "Lever" else f"Connections_{mask}"
    result = {
        "CustomModel": f"Items/LogicGates/{kind}/Logic_{kind}_{variant}.blockymodel",
        "HitboxType": f"Logic_{kind}_{variant}",
        "InteractionHitboxType": f"Logic_{kind}_{variant}",
        "InteractionHint": f"server.interactionHints.turn{'off' if on else 'on'}",
        "InteractionSoundEventId": f"SFX_Logic_{kind}",
        "Light": {"Color": "#f00"} if kind == "Lamp" and on else None
    }
    if kind == "Lamp":
        # Preserve the existing off-model paths. On models have identical physical
        # geometry and select the red cells in the same authored material atlas.
        suffix = "On_" if on else ""
        result["CustomModel"] = f"Items/LogicGates/Lamp/Logic_Lamp_{suffix}Connections_{mask}.blockymodel"
        result["CustomModelTexture"] = [{"Texture": "Items/LogicGates/Lamp/Logic_Lamp_Texture.png", "Weight": 1}]
    return result


def generated_files():
    files = {}
    for kind in ("Lever", "Lamp"):
        texture_path = f"Common/Items/LogicGates/{kind}/Logic_{kind}_Texture.png"
        files[texture_path] = atlas("Logic_" + kind).read_bytes()
        item_path = f"Server/Item/Items/Deco/Logic_{kind}.json"
        item = read(item_path)
        block = item["BlockType"]
        block.update(visual(kind, False, 0))
        definitions = {}
        for on in (False, True):
            # Keep the old saved state names resolvable; the evaluator adds connections after loading.
            definitions["On" if on else "Off"] = visual(kind, on, 0)
            for mask in range(16):
                state = visual(kind, on, mask)
                definitions[f"{'On' if on else 'Off'}_Connections_{mask}"] = state
                model_path = "Common/" + state["CustomModel"]
                if model_path not in files:
                    generated = model(kind, on, mask)
                    files[model_path] = generated
                    files[f"Server/Item/Block/Hitboxes/{state['HitboxType']}.json"] = hitboxes(generated)
        block["State"] = {"Definitions": definitions}
        files[item_path] = item
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = generated_files()
    stale = []
    for relative, data in files.items():
        path = RESOURCES / relative
        content = data if isinstance(data, bytes) else (json.dumps(data, indent=2) + "\n").encode("utf-8")
        if args.check:
            if not path.exists() or path.read_bytes() != content:
                stale.append(relative)
        else:
            write_asset(path, content)
    if stale:
        raise SystemExit("Outdated generated assets: " + ", ".join(stale))
    print(f"{len(files)} terminal assets {'checked' if args.check else 'generated'}")


if __name__ == "__main__":
    main()

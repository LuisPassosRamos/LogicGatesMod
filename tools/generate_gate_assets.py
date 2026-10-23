"""Generate directional gates from original art, with connected wires and matching hitboxes."""
import argparse
from copy import deepcopy
import json

from generate_cable_assets import RESOURCES, THICKNESS, box
from generate_terminal_assets import BODY_SCALE, hitboxes, read
from workshop_material import atlas, apply_surface, gate_role, ITEMS

from gate_catalog import GATES
from art_sources import read_model
from port_markings import markings
from asset_io import write_asset


def body_nodes(kind, on):
    settings = GATES[kind]
    original = read_model(settings["folder"], f"Logic_{kind}_Gate.blockymodel")
    result = []

    def flatten(node, parent_center):
        assert node["orientation"] == {"x": 0, "y": 0, "z": 0, "w": 1}
        origin = {a: parent_center[a] + node["position"][a] for a in "xyz"}
        center = {a: origin[a] + node["shape"]["offset"][a] for a in "xyz"}
        # Keep source art untouched; generate new connectors and inversion markers where needed.
        if node["id"] not in settings.get("remove", ("2", "3", "4")):
            flat = deepcopy(node)
            flat.pop("children", None)
            flat["position"] = origin
            flat["id"] = str(len(result) + 1)
            if settings.get("named_nodes"):
                is_indicator = node["name"] == "indicator"
            else:
                is_indicator = node["id"] in settings.get("indicators", (settings["indicator"],))
                flat["name"] = ("indicator" if node["id"] == settings["indicator"] else f"indicator_{node['id']}") \
                    if is_indicator else f"body_{flat['id']}"
            for vector in (flat["position"], flat["shape"]["offset"], flat["shape"]["stretch"]):
                for axis in "xyz":
                    vector[axis] *= BODY_SCALE
            if node["id"] == settings.get("inversion_node"):
                flat["name"] = "inversion_mark"
            apply_surface(flat["shape"], settings["item_id"], gate_role(flat["name"]), on)
            result.append(flat)
        for child in node.get("children", []):
            flatten(child, center)

    for root in original["nodes"]:
        flatten(root, dict.fromkeys("xyz", 0))
    if settings.get("add_inversion"):
        mark = box(len(result) + 1, "inversion_mark", 12, 0, 4, 4)
        mark["shape"]["offset"]["y"] = 2
        mark["shape"]["stretch"]["y"] = 2
        apply_surface(mark["shape"], settings["item_id"], "inversion")
        result.append(mark)
    return result


def model(kind, on, mask):
    nodes = body_nodes(kind, on)
    settings = GATES[kind]
    # Pale geometric port markings remain non-emissive over every paint color.
    labels = markings(kind, len(nodes) + 1, (0, 0))
    for label in labels:
        apply_surface(label["shape"], settings["item_id"], "label")
    nodes.extend(labels)
    # Attach at the actual silhouette, including the OR gate's concave rear center.
    arms = ((1, "input_a", 0, -13, THICKNESS, 6),
            (2, "output", (16 + settings["front"]) / 2, 0, 16 - settings["front"], THICKNESS),
            (4, "input_b", 0, 13, THICKNESS, 6),
            (8, "input_c", -(16 + settings["rear"]) / 2, 0, 16 - settings["rear"], THICKNESS))
    for bit, name, x, z, width, depth in arms:
        if mask & bit & settings.get("allowed_mask", 15):
            arm = box(len(nodes) + 1, name, x, z, width, depth)
            apply_surface(arm["shape"], settings["item_id"], "wire")
            nodes.append(arm)
    return {"nodes": nodes, "format": "prop", "lod": "auto"}


def visual(kind, on, mask):
    item = GATES[kind]["item_id"]
    state = f"{'On' if on else 'Off'}_Connections_{mask}"
    return {
        "CustomModel": f"Items/LogicGates/{GATES[kind]['folder']}/{item}_{state}.blockymodel",
        "HitboxType": f"{item}_Connections_{mask}",
        "InteractionHitboxType": f"{item}_Connections_{mask}",
        "Light": {"Color": "#f00"} if on else None
    }


def generated_files():
    files = {}
    for kind, settings in GATES.items():
        item_id = settings["item_id"]
        texture = f"Items/LogicGates/{settings['folder']}/{item_id}_Texture.png"
        files["Common/" + texture] = atlas(item_id).read_bytes()
        definitions = {}
        for on in (False, True):
            for mask in range(16):
                if mask & ~settings.get("allowed_mask", 15):
                    continue
                state = f"{'On' if on else 'Off'}_Connections_{mask}"
                properties = visual(kind, on, mask)
                generated = model(kind, on, mask)
                definitions[state] = properties
                files["Common/" + properties["CustomModel"]] = generated
                if not on:
                    files[f"Server/Item/Block/Hitboxes/{properties['HitboxType']}.json"] = hitboxes(generated)
        files[f"Server/Item/Items/Deco/{item_id}.json"] = {
            "TranslationProperties": {"Name": f"server.items.logic_{kind.lower()}_gate.name",
                                      "Description": f"server.items.logic_{kind.lower()}_gate.description"},
            "Categories": ["Blocks.Deco"],
            "Model": visual(kind, False, settings.get("allowed_mask", 15))["CustomModel"], "Texture": texture,
            "BlockType": {
                "Material": "Solid", "DrawType": "Model", "Opacity": "Transparent",
                **visual(kind, False, 0),
                "CustomModelTexture": [{"Texture": texture, "Weight": 1}],
                "CustomModelScale": 1, "VariantRotation": "NESW",
                "PlacementSettings": {"RotationMode": "FacingPlayer", "AllowRotationKey": True},
                "RotationYawPlacementOffset": "Ninety",
                "Gathering": {"Soft": {"IsWeaponBreakable": False, "ItemId": item_id}},
                "BlockParticleSetId": "Metal", "BlockSoundSetId": "Metal", "PhysicalMaterialId": "Metal",
                "TextureComputedColor": ITEMS[item_id]["color"],
                "ParticleColor": ITEMS[item_id]["color"],
                "Support": {"Down": [{"FaceType": "Full"}]},
                "BlockEntity": {"Components": {"LogicGates_Gate": {}}},
                "State": {"Definitions": definitions}
            },
            "PlayerAnimationsId": "Block", "ItemSoundSetId": "ISS_Items_Metal",
            "Tags": {"Type": ["Deco"]}
        }
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
    print(f"{len(files)} gate assets {'checked' if args.check else 'generated'}")


if __name__ == "__main__":
    main()

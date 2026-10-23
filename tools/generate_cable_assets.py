"""Generate the 16 horizontal cable models and their item states (no image edits)."""
import argparse
import json
from pathlib import Path
from workshop_material import atlas, apply_surface
from asset_io import write_asset

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
MODEL_DIR = "Items/LogicGates/Cable"
# Arms start at the 6-unit hub's edge and end at +/-16, without coplanar overlap.
DIRECTIONS = ((1, "north", 0, -9.5, 2, 13),
              (2, "east", 9.5, 0, 13, 2),
              (4, "south", 0, 9.5, 2, 13),
              (8, "west", -9.5, 0, 13, 2))
THICKNESS = 3  # 50% thicker, with the same endpoints at the block edges.

# Placeholder UVs for box construction; workshop_material remaps every published face.
FACE_TILES = {"back": (0, 0), "right": (0, 2), "front": (2, 0),
              "left": (2, 2), "top": (0, 4), "bottom": (2, 4)}


def box(node_id, name, x, z, width, depth):
    return {
        "id": str(node_id), "name": name,
        "position": {"x": x, "y": 0, "z": z},
        "orientation": {"x": 0, "y": 0, "z": 0, "w": 1},
        "shape": {
            "type": "box", "offset": {"x": 0, "y": THICKNESS / 2, "z": 0},
            "stretch": {"x": width / 2, "y": THICKNESS / 2, "z": depth / 2},
            "settings": {"isPiece": False, "size": {"x": 2, "y": 2, "z": 2},
                         "isStaticBox": True},
            "textureLayout": {
                face: {"offset": {"x": uv[0], "y": uv[1]},
                       "mirror": {"x": False, "y": False}, "angle": 0}
                for face, uv in FACE_TILES.items()
            },
            "unwrapMode": "custom", "visible": True,
            "doubleSided": False, "shadingMode": "flat"
        }
    }


def generated_files():
    result = {}
    for mask in range(16):
        nodes = [box(1, "junction", 0, 0, 6, 6)]
        for bit, name, x, z, width, depth in DIRECTIONS:
            if mask & bit:
                nodes.append(box(len(nodes) + 1, name, x, z,
                                 THICKNESS if width == 2 else width,
                                 THICKNESS if depth == 2 else depth))
        for node in nodes:
            apply_surface(node["shape"], "Logic_Cable", "copper" if node["name"] == "junction" else "wire")
        result[f"Common/{MODEL_DIR}/Logic_Cable_{mask}.blockymodel"] = {
            "nodes": nodes, "format": "prop", "lod": "auto"
        }
    result[f"Common/{MODEL_DIR}/Logic_Cable_Texture.png"] = atlas("Logic_Cable").read_bytes()
    result["Server/Item/Items/Deco/Logic_Cable.json"] = {
        "TranslationProperties": {"Name": "server.items.logic_cable.name"},
        "Icon": "Icons/ItemsGenerated/logic_cable.png",
        "Categories": ["Blocks.Deco"],
        "BlockType": {
            "Material": "Solid", "DrawType": "Model", "Opacity": "Transparent",
            "CustomModel": f"{MODEL_DIR}/Logic_Cable_0.blockymodel",
            "CustomModelTexture": [{"Texture": f"{MODEL_DIR}/Logic_Cable_Texture.png", "Weight": 1}],
            "HitboxType": "Logic_Cable", "InteractionHitboxType": "Logic_Cable",
            "Gathering": {"Soft": {"IsWeaponBreakable": False, "ItemId": "Logic_Cable"}},
            "BlockParticleSetId": "Metal", "BlockSoundSetId": "Metal",
            "PhysicalMaterialId": "Metal",
            "Support": {"Down": [{"FaceType": "Full"}]},
            "BlockEntity": {"Components": {"LogicGates_Cable": {}}},
            "State": {
                "Definitions": {
                    f"Connections_{mask}": {"CustomModel": f"{MODEL_DIR}/Logic_Cable_{mask}.blockymodel"}
                    for mask in range(16)
                }
            },
            "CustomModelScale": 1,
            "TextureComputedColor": "#7e7e7e", "ParticleColor": "#7e7e7e"
        },
        "PlayerAnimationsId": "Block", "ItemSoundSetId": "ISS_Items_Metal"
    }
    result["Server/Item/Block/Hitboxes/Logic_Cable.json"] = {
        "Boxes": [{"Min": {"X": 0, "Y": 0, "Z": 0},
                   "Max": {"X": 1, "Y": THICKNESS / 32, "Z": 1}}]
    }
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="check generated files without changing them")
    args = parser.parse_args()
    stale = []
    for relative, data in generated_files().items():
        path = RESOURCES / relative
        content = data if isinstance(data, bytes) else (json.dumps(data, indent=2) + "\n").encode("utf-8")
        if args.check:
            if not path.exists() or path.read_bytes() != content:
                stale.append(relative)
        else:
            write_asset(path, content)
    if stale:
        raise SystemExit("Outdated generated assets: " + ", ".join(stale))
    print(f"{len(generated_files())} cable assets {'checked' if args.check else 'generated'}")


if __name__ == "__main__":
    main()

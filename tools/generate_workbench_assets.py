"""Package the editable workshop bench with native Hytale crafting and piece hitboxes."""
import argparse
import json

from project_config import ROOT
from generate_terminal_assets import hitboxes
from workshop_material import atlas
from item_presentation import decorate
from asset_io import write_asset

RESOURCE = ROOT / "src/main/resources"
MODEL = "Items/LogicGates/Workbench/Logic_Workbench_Placed.blockymodel"
TEXTURE = "Items/LogicGates/Workbench/Logic_Workbench_Texture.png"


def generated_files():
    source = json.loads((ROOT / "assets/models/Workbench/Logic_Workbench.blockymodel").read_text(encoding="utf-8"))
    item = {
        "TranslationProperties": {"Name": "server.items.logic_workbench.name", "Description": "server.items.logic_workbench.description"},
        "Icon": "Icons/ItemsGenerated/LogicGates/Logic_Workbench.png", "Categories": ["LogicGates.Benches"],
        "Model": MODEL, "Texture": TEXTURE, "MaxStack": 1, "ItemLevel": 1,
        "PlayerAnimationsId": "Block", "ItemSoundSetId": "ISS_Blocks_Wood", "Tags": {"Type": ["Bench"]},
        "BlockType": {
            "Material": "Solid", "DrawType": "Model", "Opacity": "Transparent",
            "CustomModel": MODEL, "CustomModelTexture": [{"Texture": TEXTURE, "Weight": 1}],
            "CustomModelScale": 1, "HitboxType": "Logic_Workbench", "InteractionHitboxType": "Logic_Workbench",
            "VariantRotation": "NESW", "PlacementSettings": {"RotationMode": "FacingPlayer", "AllowRotationKey": True},
            "Bench": {"Type": "Crafting", "Id": "LogicGates_Workbench",
                      "Categories": [{"Id": "LogicGates_" + name, "Icon": f"Icons/CraftingCategories/LogicGates/{name}.png",
                                      "Name": "server.logicgates.categories." + name.lower()} for name in ("Components", "Gates")],
                      "LocalOpenSoundEventId": "SFX_Workbench_Open", "LocalCloseSoundEventId": "SFX_Workbench_Close",
                      "CompletedSoundEventId": "SFX_Workbench_Craft", "FailedSoundEventId": "SFX_Generic_Crafting_Failed"},
            "BlockEntity": {"Components": {"BenchBlock": {}}},
            "State": {"Definitions": {"CraftCompleted": {"Looping": True}, "CraftCompletedInstant": {}}},
            "Gathering": {"Breaking": {"GatherType": "Benches"}},
            "Support": {"Down": [{"FaceType": "Full"}]},
            "BlockParticleSetId": "Wood", "BlockSoundSetId": "Wood", "PhysicalMaterialId": "Wood",
            "TextureComputedColor": "#97653e", "ParticleColor": "#97653e"
        }
    }
    decorate("Logic_Workbench", item)
    return {"Common/" + MODEL: source, "Common/" + TEXTURE: atlas("Logic_Workbench").read_bytes(),
            "Server/Item/Block/Hitboxes/Logic_Workbench.json": hitboxes(source),
            "Server/Item/Items/Bench/Logic_Workbench.json": item}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = generated_files()
    for relative, data in files.items():
        path = RESOURCE / relative
        content = data if isinstance(data, bytes) else (json.dumps(data, indent=2) + "\n").encode("utf-8")
        if args.check:
            if not path.exists() or path.read_bytes() != content: raise SystemExit(f"Outdated bench asset: {relative}")
        else:
            write_asset(path, content)
    print(f"{len(files)} workbench assets {'checked' if args.check else 'generated'}")


if __name__ == "__main__":
    main()

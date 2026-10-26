"""Require complete presentation/crafting registration, including future mod items."""
import json
from project_config import ROOT
from workshop_material import ITEMS
from gate_catalog import CATALOG

RESOURCE = ROOT / "src/main/resources"
INTERNAL_ITEMS = {"Logic_Classroom_Select"}


def internal(item_id, item):
    """Internal capability tools are deliberately absent from recipes and creative categories."""
    if (item_id not in INTERNAL_ITEMS or any(key in item for key in ("Recipe", "Categories", "BuilderTool"))
            or item.get("MaxStack") != 1
            or item.get("Interactions") != {
                action: {"Interactions": [{"Type": "LogicGates_ClassroomSelection"}]}
                for action in ("Primary", "Secondary")}
            or item.get("Icon") != "Icons/Items/EditorTools/Selection.png"
            or item.get("Model") != "Items/Block_Spawner_Block.blockymodel"
            or item.get("Texture") != "Items/Block_Spawner_Block_Large_Texture.png"):
        raise ValueError(f"Invalid internal selection tool: {item_id}")


def definitions():
    return {path.stem: json.loads(path.read_text(encoding="utf-8"))
            for path in (RESOURCE / "Server/Item/Items").rglob("*.json")}


def check():
    items = definitions()
    expected = ITEMS.keys() | INTERNAL_ITEMS
    if items.keys() != expected:
        raise ValueError(f"Uncatalogued/missing mod items: {items.keys() ^ expected}")
    if not {gate["itemId"] for gate in CATALOG["gates"]}.issubset(items):
        raise ValueError("Gate without workshop registration")
    category = json.loads((RESOURCE / "Server/Item/Category/CreativeLibrary/LogicGates.json").read_text(encoding="utf-8"))
    children = {entry["Id"] for entry in category["Children"]}
    for item_id, item in items.items():
        if item_id in INTERNAL_ITEMS:
            internal(item_id, item)
            continue
        settings = ITEMS[item_id]
        if settings["category"] not in children or item.get("Categories") != ["LogicGates." + settings["category"]]:
            raise ValueError(f"Missing creative category: {item_id}")
        if not item.get("Icon") or not (RESOURCE / "Common" / item["Icon"]).is_file():
            raise ValueError(f"Missing icon: {item_id}")
        if item.get("Recipe") != settings.get("recipe") or not item.get("Recipe"):
            raise ValueError(f"Missing/stale crafting recipe: {item_id}")
        recipe = item["Recipe"]
        invalid_time = recipe.get("TimeSeconds", 0) != 0 if item_id == "Logic_Workbench" else recipe.get("TimeSeconds", 0) <= 0
        if recipe.get("OutputQuantity", 1) < 1 or invalid_time or not recipe.get("Input"):
            raise ValueError(f"Invalid recipe quantities/time: {item_id}")
        for material in recipe["Input"]:
            if material["Quantity"] < 1 or ("ItemId" in material) == ("ResourceTypeId" in material):
                raise ValueError(f"Invalid ingredient: {item_id}/{material}")
            if material.get("ItemId") in ITEMS:
                raise ValueError(f"This direct recipe must not depend on another mod component: {item_id}")
        bench = recipe["BenchRequirement"]
        expected = "Fieldcraft" if item_id == "Logic_Workbench" else "LogicGates_Workbench"
        if len(bench) != 1 or bench[0]["Id"] != expected or bench[0]["Type"] != "Crafting":
            raise ValueError(f"Wrong crafting station: {item_id}")
    print(f"Workshop checked: {len(ITEMS)} craftable items and {len(INTERNAL_ITEMS)} restricted internal tool")


def packaged(archive):
    names = set(archive.namelist())
    if not any(name.startswith("Server/Item/Items/") for name in names):
        raise ValueError("Item pack missing from distribution")
    for item_id in INTERNAL_ITEMS:
        candidates = [name for name in names if name.startswith("Server/Item/Items/") and name.endswith("/" + item_id + ".json")]
        if len(candidates) != 1: raise ValueError(f"Internal tool missing from distribution: {item_id}")
        internal(item_id, json.loads(archive.read(candidates[0])))
    for item_id in ITEMS:
        candidates = [name for name in names if name.startswith("Server/Item/Items/") and name.endswith("/" + item_id + ".json")]
        if len(candidates) != 1: raise ValueError(f"Item missing from distribution: {item_id}")
        item = json.loads(archive.read(candidates[0]))
        if "Common/" + item["Icon"] not in names: raise ValueError(f"Icon missing from distribution: {item_id}")
        def references(value):
            if isinstance(value, dict):
                for key, entry in value.items():
                    if key in ("CustomModel", "Model", "Texture", "Icon") and isinstance(entry, str):
                        if "Common/" + entry not in names: raise ValueError(f"Missing packaged asset: {entry}")
                    references(entry)
            elif isinstance(value, list):
                for entry in value: references(entry)
        references(item)
    if "Server/Item/Category/CreativeLibrary/LogicGates.json" not in names:
        raise ValueError("Creative category missing from distribution")

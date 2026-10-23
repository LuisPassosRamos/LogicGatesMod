"""Common item registration: every component has an icon, category and description."""
from copy import deepcopy
from workshop_material import ITEMS


def decorate(item_id, item):
    settings = ITEMS[item_id]
    item["Icon"] = f"Icons/ItemsGenerated/LogicGates/{item_id}.png"
    item["Categories"] = ["LogicGates." + settings["category"]]
    item["TranslationProperties"]["Description"] = item["TranslationProperties"]["Name"].removesuffix(".name") + ".description"
    item.setdefault("Model", item["BlockType"]["CustomModel"])
    item.setdefault("Texture", item["BlockType"]["CustomModelTexture"][0]["Texture"])
    item["Recipe"] = deepcopy(settings["recipe"])
    item["BlockType"]["TextureComputedColor"] = settings["color"]
    item["BlockType"]["ParticleColor"] = settings["color"]
    return item

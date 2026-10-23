"""Generate the mod's own creative library category without changing vanilla assets."""
import argparse
import json
from project_config import ROOT


def definition():
    return {"Name": "server.logicgates.categories.title", "Icon": "Icons/ItemCategories/LogicGates/Category.png", "Order": 4,
            "Children": [{"Id": name, "Name": "server.logicgates.categories." + name.lower(),
                          "Icon": f"Icons/ItemCategories/LogicGates/{name}.png", "Order": order}
                         for order, name in enumerate(("Components", "Gates", "Benches"))]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    path = ROOT / "src/main/resources/Server/Item/Category/CreativeLibrary/LogicGates.json"
    content = (json.dumps(definition(), indent=2) + "\n").encode()
    if args.check:
        if not path.exists() or path.read_bytes() != content: raise SystemExit("Outdated LogicGates creative category")
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
    print("LogicGates creative category " + ("checked" if args.check else "generated"))


if __name__ == "__main__":
    main()

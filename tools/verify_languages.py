"""Validate locale keys, duplicates, parameters and literal Java/Custom UI references."""
import json
import re


def parse(path):
    messages = {}
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid translation at {path}:{number}")
        key, value = (part.strip() for part in line.split("=", 1))
        key = "server." + key
        if key in messages:
            raise ValueError(f"Duplicate translation {key} in {path}")
        if not value:
            raise ValueError(f"Empty translation {key} in {path}")
        messages[key] = value
    return messages


def check(root):
    directory = root / "src/main/resources/Server/Languages"
    base = parse(directory / "en-US/server.lang")
    locales = sorted(directory.glob("*/server.lang"))
    for path in locales:
        values = parse(path)
        if values.keys() != base.keys():
            raise ValueError(f"Locale {path.parent.name}: missing {sorted(base.keys() - values.keys())}; unknown {sorted(values.keys() - base.keys())}")
        for key, value in values.items():
            if set(re.findall(r"\{(\w+)\}", value)) != set(re.findall(r"\{(\w+)\}", base[key])):
                raise ValueError(f"Translation parameters differ: {path.parent.name}/{key}")
    for item in (root / "src/main/resources/Server/Item/Items").rglob("*.json"):
        for key in json.loads(item.read_text(encoding="utf-8")).get("TranslationProperties", {}).values():
            if key not in base:
                raise ValueError(f"Missing item translation {key} in {item.name}")
    def owned_references(value):
        if isinstance(value, str) and value.startswith("server.logicgates."):
            yield value
        elif isinstance(value, dict):
            for entry in value.values(): yield from owned_references(entry)
        elif isinstance(value, list):
            for entry in value: yield from owned_references(entry)
    for source in (root / "src/main/resources/Server/Item").rglob("*.json"):
        for key in owned_references(json.loads(source.read_text(encoding="utf-8"))):
            if key not in base: raise ValueError(f"Missing UI/category translation {key} in {source.name}")
    for source in (root / "src/main/java").rglob("*.java"):
        content = source.read_text(encoding="utf-8")
        keys = re.findall(r'"(server\.logicgates\.[\w.]+)"', content)
        keys += ["server.logicgates." + key for key in re.findall(r'\btext\("([\w.]+)"', content) if not key.endswith(".")]
        for key in keys:
            if key not in base:
                raise ValueError(f"Missing Java translation {key} in {source.name}")
    for source in (root / "src/main/resources/Common/UI/Custom").rglob("*.ui"):
        content = source.read_text(encoding="utf-8")
        # Comments are not references; quoted values may contain URL // sequences.
        tokens = re.finditer(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/|%(server\.logicgates\.[\w.]+)', content)
        for token in tokens:
            key = token.group(1)
            if key and key not in base:
                raise ValueError(f"Missing Custom UI translation {key} in {source.name}")
    print(f"Languages checked: {len(locales)} locales, {len(base)} keys each, matching parameters")
    return len(base)

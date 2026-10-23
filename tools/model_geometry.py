"""Physical model fingerprint, deliberately independent of texture UV dimensions."""
import hashlib
import json


def fingerprint(model):
    def vector(values):
        return {key: round(float(value), 9) for key, value in values.items()}

    def nodes(values):
        return [dict(id=node["id"], name=node["name"], position=vector(node["position"]),
                     orientation=vector(node["orientation"]), offset=vector(node["shape"]["offset"]),
                     dimensions={axis: round(float(node["shape"]["settings"]["size"][axis]
                                                   * node["shape"]["stretch"][axis]), 9) for axis in "xyz"},
                     children=nodes(node.get("children", []))) for node in values]
    content = json.dumps(nodes(model["nodes"]), sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(content.encode()).hexdigest()

"""Shared gate metadata; logic is implemented in Java and checked against this contract."""
import json
from project_config import ROOT

CATALOG = json.loads((ROOT / "src/main/resources/logicgates/gate-catalog.json").read_text(encoding="utf-8"))
if CATALOG["schemaVersion"] != 1:
    raise ValueError("Unsupported gate catalog schema")
GATES = {gate["kind"]: dict(gate["art"], folder=gate["folder"], allowed_mask=gate["allowedMask"], item_id=gate["itemId"])
         for gate in CATALOG["gates"]}
if len(GATES) != len(CATALOG["gates"]):
    raise ValueError("Duplicate gate kind")


def model_count():
    return sum(2 * sum(mask & ~gate["allowedMask"] == 0 for mask in range(16)) for gate in CATALOG["gates"])

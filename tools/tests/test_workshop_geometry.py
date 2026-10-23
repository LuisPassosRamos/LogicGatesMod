"""The art revision must retain the physical models approved in release 1.2.0."""
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from project_config import ROOT
from model_geometry import fingerprint


class ApprovedGeometryTest(unittest.TestCase):
    def test_all_264_approved_variants_keep_their_physical_geometry(self):
        expected = json.loads((Path(__file__).parent / "fixtures/approved_geometry.json").read_text())
        self.assertEqual(264, len(expected))
        for relative, approved in expected.items():
            with self.subTest(model=relative):
                model = json.loads((ROOT / "src/main/resources" / relative).read_text())
                self.assertEqual(approved, fingerprint(model), "The approved physical model changed")

    def test_new_lit_lamp_variants_only_change_materials(self):
        base = ROOT / "src/main/resources/Common/Items/LogicGates/Lamp"
        for mask in range(16):
            with self.subTest(mask=mask):
                before = json.loads((base / f"Logic_Lamp_Connections_{mask}.blockymodel").read_text())
                after = json.loads((base / f"Logic_Lamp_On_Connections_{mask}.blockymodel").read_text())
                self.assertEqual(fingerprint(before), fingerprint(after))


if __name__ == "__main__":
    unittest.main()

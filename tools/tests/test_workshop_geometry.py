"""The art revision must retain the physical models approved in release 1.2.0."""
import json
from itertools import combinations
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from project_config import ROOT
from model_geometry import fingerprint
from model_icons import texture


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

    def test_open_and_on_bench_has_no_overlapping_outward_coplanar_faces(self):
        """Coincident exposed faces compete in the depth buffer and flicker."""
        source = ROOT / "assets/models/Workbench/Logic_Workbench.blockymodel"
        model = json.loads(source.read_text(encoding="utf-8"))
        pieces = []
        for node in model["nodes"]:
            if not node["name"].startswith(("open_AND_", "removed_AND_")):
                continue
            shape = node["shape"]
            bounds = {}
            for axis in "xyz":
                center = node["position"][axis] + shape["offset"][axis]
                half = shape["settings"]["size"][axis] * shape["stretch"][axis] / 2
                bounds[axis] = (center - half, center + half)
            pieces.append((node["name"], bounds))
        self.assertGreater(len(pieces), 10)
        for (first, a), (second, b) in combinations(pieces, 2):
            for axis in "xyz":
                other = [c for c in "xyz" if c != axis]
                area = 1
                for c in other:
                    area *= max(0, min(a[c][1], b[c][1]) - max(a[c][0], b[c][0]))
                if area <= 1e-7:
                    continue
                for side in (0, 1):
                    self.assertGreater(abs(a[axis][side] - b[axis][side]), 1e-7,
                                       f"Flickering faces: {first}/{second}, {axis}/{side}")

    def test_all_cable_hubs_arms_and_component_wires_use_neutral_black_rubber(self):
        directory = ROOT / "src/main/resources/Common/Items/LogicGates"
        wire_names = {"junction", "north", "east", "south", "west", "output", "input_a", "input_b", "input_c"}
        patches = set()
        cable_variants = 0
        for path in directory.rglob("*.blockymodel"):
            if path.parent.name == "Workbench":
                continue
            model = json.loads(path.read_text(encoding="utf-8"))
            if path.parent.name == "Cable":
                cable_variants += 1
            atlas = next(path.parent.glob("*_Texture.png"))
            for node in model["nodes"]:
                if node["name"] not in wire_names:
                    continue
                shape = node["shape"]
                size = shape["settings"]["size"]
                for face, uv in shape["textureLayout"].items():
                    w = size["z" if face in ("left", "right") else "x"]
                    h = size["z" if face in ("top", "bottom") else "y"]
                    patches.add((atlas, uv["offset"]["x"], uv["offset"]["y"], w, h))
        self.assertEqual(16, cable_variants)
        self.assertGreaterEqual(len(patches), 20)
        for atlas, u, v, w, h in patches:
            width, height, pixels = texture(str(atlas))
            with self.subTest(atlas=atlas.name, uv=(u, v)):
                self.assertLessEqual(u + w, width)
                self.assertLessEqual(v + h, height)
                # A regular grid covers every area, including the small highlights.
                for y in range(v, v + h, 4):
                    for x in range(u, u + w, 4):
                        red, green, blue, alpha = pixels[(y * width + x) * 4:(y * width + x) * 4 + 4]
                        self.assertEqual(255, alpha)
                        self.assertLessEqual(max(red, green, blue), 80, "Cable must remain black")
                        self.assertLessEqual(max(red, green, blue) - min(red, green, blue), 12,
                                             "No copper or colored conductor on the cable")


if __name__ == "__main__":
    unittest.main()

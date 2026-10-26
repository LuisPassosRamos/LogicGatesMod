"""Portable icon encoding and mandatory registration for newly added components."""
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from model_icons import png, texture
import verify_workshop


class WorkshopToolsTest(unittest.TestCase):
    def test_renderer_png_round_trip_preserves_color_and_alpha_across_deflate_blocks(self):
        width, height = 300, 64
        pixels = bytes((i * 17) % 256 for i in range(width * height * 4))
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "icon.png"
            path.write_bytes(png(width, height, pixels))
            actual_w, actual_h, actual = texture(str(path))
            self.assertEqual((width, height), (actual_w, actual_h))
            self.assertEqual(pixels, actual)

    def test_future_component_without_catalog_registration_is_rejected(self):
        items = verify_workshop.definitions()
        items["Logic_Future_Component"] = {}
        with patch.object(verify_workshop, "definitions", return_value=items):
            with self.assertRaisesRegex(ValueError, "Uncatalogued"):
                verify_workshop.check()

    def test_missing_recipe_is_rejected_before_packaging(self):
        items = verify_workshop.definitions()
        items["Logic_Cable"].pop("Recipe")
        with patch.object(verify_workshop, "definitions", return_value=items):
            with self.assertRaisesRegex(ValueError, "Missing/stale crafting recipe"):
                verify_workshop.check()

    def test_distribution_without_workshop_items_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "missing-items.jar"
            with zipfile.ZipFile(path, "w") as output: output.writestr("manifest.json", "{}")
            with zipfile.ZipFile(path) as archive:
                with self.assertRaisesRegex(ValueError, "Item pack missing"):
                    verify_workshop.packaged(archive)

    def test_internal_selection_tool_cannot_acquire_crafting_or_editor_privileges(self):
        for key, value in (("Recipe", {}), ("Categories", ["LogicGates.Components"]), ("BuilderTool", {"Id": "Selection"})):
            with self.subTest(key=key):
                items = verify_workshop.definitions()
                items["Logic_Classroom_Select"][key] = value
                with patch.object(verify_workshop, "definitions", return_value=items):
                    with self.assertRaisesRegex(ValueError, "Invalid internal selection tool"):
                        verify_workshop.check()


if __name__ == "__main__":
    unittest.main()

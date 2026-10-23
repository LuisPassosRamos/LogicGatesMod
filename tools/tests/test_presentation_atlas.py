"""Asset-level checks for shared AND materials and readable, correctly placed labels."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from project_config import ROOT
from png_codec import png, texture
from presentation_atlas import crop
from model_icons import mesh, render

COMMON = ROOT / "src/main/resources/Common/Items/LogicGates"


def model(path):
    return json.loads(path.read_text(encoding="utf-8"))


def face_pixels(image, shape, face):
    width, _, pixels = image
    size = shape["settings"]["size"]
    uv = shape["textureLayout"][face]["offset"]
    return crop(pixels, width, uv["x"], uv["y"], size["z" if face in ("left", "right") else "x"],
                size["z" if face in ("top", "bottom") else "y"])


class PresentationAtlasTest(unittest.TestCase):
    def test_compiled_atlases_keep_every_original_material_pixel(self):
        for kind in ("Workbench", "Lever"):
            with self.subTest(kind=kind):
                width, height, original = texture(str(ROOT / f"assets/textures/workshop/Logic_{kind}_Atlas.png"))
                actual_w, actual_h, actual = texture(str(COMMON / kind / f"Logic_{kind}_Texture.png"))
                self.assertEqual(height, actual_h)
                self.assertEqual(width + 272, actual_w)
                self.assertEqual(original, crop(actual, actual_w, 0, 0, width, height))

    def test_open_and_and_removed_lid_use_the_actual_floor_and_pixels(self):
        floor = model(COMMON / "AndGate/Logic_AND_Gate_Off_Connections_0.blockymodel")
        body = next(n["shape"] for n in floor["nodes"] if n["name"] == "body_1")
        indicator = next(n["shape"] for n in floor["nodes"] if n["name"] == "indicator")
        floor_image = texture(str(COMMON / "AndGate/Logic_AND_Gate_Texture.png"))
        bench = model(COMMON / "Workbench/Logic_Workbench_Placed.blockymodel")
        bench_image = texture(str(COMMON / "Workbench/Logic_Workbench_Texture.png"))
        for name in ("open_AND_base", "open_AND_back_wall", "removed_AND_lid_base"):
            shape = next(n["shape"] for n in bench["nodes"] if n["name"] == name)
            for face in ("top", "front", "bottom"):
                with self.subTest(piece=name, face=face):
                    self.assertEqual(face_pixels(floor_image, body, face), face_pixels(bench_image, shape, face))
        lens = next(n["shape"] for n in bench["nodes"] if n["name"] == "loose_indicator_lens")
        self.assertEqual(face_pixels(floor_image, indicator, "top"), face_pixels(bench_image, lens, "top"))

    def test_lever_labels_are_legible_and_aligned_with_the_two_handle_positions(self):
        image = texture(str(COMMON / "Lever/Logic_Lever_Texture.png"))
        expected_words = {
            "OFF": ("11101110111", "10101000100", "10101100110", "10101000100", "11101000100"),
            "ON": ("11101001", "10101101", "10101011", "10101001", "11101001"),
        }
        for on in (False, True):
            for mask in range(16):
                m = model(COMMON / f"Lever/Logic_Lever_{'On' if on else 'Off'}_Connections_{mask}.blockymodel")
                base = m["nodes"][0]["shape"]
                # The handle's actual center determines the physical ON/OFF end.
                handle = m["nodes"][2]
                q, o = handle["orientation"], handle["shape"]["offset"]
                center_x = handle["position"]["x"] + (1 - 2 * q["z"] ** 2) * o["x"] - 2 * q["z"] * q["w"] * o["y"]
                self.assertGreater(center_x if on else -center_x, 0)
                for face, labels in (("front", (("OFF", 64), ("ON", 192))),
                                     ("back", (("ON", 64), ("OFF", 192)))):
                    pixels = face_pixels(image, base, face)
                    for word, center in labels:
                        glyph = expected_words[word]
                        x = center - len(glyph[0]) * 9 // 2
                        y = 128 - 5 * 9 // 2
                        for row, pattern in enumerate(glyph):
                            for column, bit in enumerate(pattern):
                                u, v = x + column * 9 + 4, y + row * 9 + 4
                                ink = pixels[(v * 256 + u) * 4:(v * 256 + u) * 4 + 3] == b"\x1c\x1d\x1f"
                                self.assertEqual(bit == "1", ink, f"{on}/{mask}/{face}/{word}/{row}/{column}")

    def test_top_face_uv_axes_match_the_blockbench_hytale_convention(self):
        # Four colored quadrants of an asymmetric top texture must appear at
        # the correct projected world corners, rather than rotated by 90 deg.
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary) / "corners.png"
            pixels = bytearray()
            for y in range(32):
                for x in range(32):
                    pixels.extend((255, 0, 0, 255) if x < 16 and y < 16 else
                                  (0, 255, 0, 255) if y < 16 else
                                  (0, 0, 255, 255) if x < 16 else (255, 255, 0, 255))
            source.write_bytes(png(32, 32, pixels))
            shape = {"type": "box", "visible": True, "offset": dict.fromkeys("xyz", 0),
                     "stretch": dict.fromkeys("xyz", 1), "settings": {"size": dict.fromkeys("xyz", 32)},
                     "shadingMode": "fullbright", "textureLayout": {f: {"offset": {"x": 0, "y": 0},
                         "mirror": {"x": False, "y": False}, "angle": 0} for f in ("front", "back", "right", "left", "top", "bottom")}}
            faces = mesh({"nodes": [{"position": dict.fromkeys("xyz", 0),
                          "orientation": {"x": 0, "y": 0, "z": 0, "w": 1}, "shape": shape}]}, source)
            # The emitted faces are front/right/top; keep only the top face.
            target = Path(temporary) / "render.png"
            target.write_bytes(render(faces[-1:], 64))
            width, height, actual = texture(str(target))
            for (x, y), expected in { (32, 23): (255, 0, 0), (45, 32): (0, 255, 0),
                                      (18, 32): (0, 0, 255), (32, 40): (255, 255, 0)}.items():
                self.assertEqual(bytes(expected), actual[(y * width + x) * 4:(y * width + x) * 4 + 3])


if __name__ == "__main__":
    unittest.main()

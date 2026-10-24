"""Lossless compression with a platform decoder and no platform encoder dependency."""
import hashlib
from pathlib import Path
import random
import sys
import tempfile
import unittest
from unittest.mock import patch
import zlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from portable_deflate import compress
from png_codec import png, texture


class PngCompressionTest(unittest.TestCase):
    def test_canonical_png_bytes_match_across_python_and_zlib_versions(self):
        self.assertEqual("be4c4c257e9d9f627b40ba8659a1cb8bb9595a5dd683f6e40bb8225051af710d",
                         hashlib.sha256(png(4, 1, bytes(range(16)))).hexdigest())

    def test_deflate_literals_matches_overlap_and_window_boundaries(self):
        noise = random.Random(1526).randbytes(70000)
        for data in (b"", b"a", bytes(range(256)), b"a" * 100000, noise,
                     noise[:32768] * 3, (b"RGBA\0" * 100000)):
            with self.subTest(size=len(data)):
                compressed = compress(data)
                self.assertEqual(data, zlib.decompress(compressed))
                self.assertEqual(compressed, compress(data))

    def test_png_compacts_large_flat_atlas_without_changing_rgba_or_using_zlib_encoder(self):
        width, height = 1526, 1254
        pixels = bytes((146, 170, 133, 255)) * width * height
        with patch("zlib.compress", side_effect=AssertionError("Platform encoder forbidden")), \
                patch("zlib.compressobj", side_effect=AssertionError("Platform encoder forbidden")):
            encoded = png(width, height, pixels)
        self.assertLess(len(encoded), len(pixels) // 50)
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "atlas.png"
            path.write_bytes(encoded)
            actual = texture(str(path))
            self.assertEqual((width, height), actual[:2])
            self.assertEqual(pixels, actual[2])

    def test_invalid_rgba_dimensions_rejected(self):
        for width, height, pixels in ((0, 1, b""), (1, -1, b""), (1, 1, b"rgb")):
            with self.assertRaises(ValueError):
                png(width, height, pixels)


if __name__ == "__main__":
    unittest.main()

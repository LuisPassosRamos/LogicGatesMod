"""Lossless PNG IO shared by model renders and deterministic UV atlas packing.

Source artwork is never repainted: packing copies its existing RGBA pixels.
Canonical fixed-Huffman DEFLATE makes PNGs compact and reproducible across zlib versions.
"""
from functools import lru_cache
from pathlib import Path
import struct
import zlib
from portable_deflate import compress


def chunk(kind, content):
    return struct.pack(">I", len(content)) + kind + content + struct.pack(">I", zlib.crc32(kind + content))


def png(width, height, pixels):
    if width <= 0 or height <= 0 or len(pixels) != width * height * 4:
        raise ValueError("Expected positive dimensions and exactly width * height RGBA pixels")
    # Opaque atlases need no alpha channel; the decoder restores exactly 255.
    opaque = bytes(pixels[3::4]).count(255) == width * height
    channels = 3 if opaque else 4
    if opaque:
        rgb = bytearray(width * height * 3)
        for channel in range(3):
            rgb[channel::3] = pixels[channel::4]
        pixels = rgb
    rows = bytearray()
    stride = width * channels
    previous = bytes(stride)
    for y in range(height):
        row = pixels[y * stride:(y + 1) * stride]
        rows.append(2)  # PNG Up filter: preserve repeated rows and flat materials.
        rows.extend((value - above) & 255 for value, above in zip(row, previous))
        previous = row
    compressed = compress(rows)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2 if opaque else 6, 0, 0, 0))
            + chunk(b"IDAT", bytes(compressed)) + chunk(b"IEND", b""))


@lru_cache(maxsize=None)
def texture(path):
    data = Path(path).read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"Not PNG: {path}")
    width, height, bits, color, _, _, interlace = struct.unpack(">IIBBBBB", data[16:29])
    if bits != 8 or color not in (2, 6) or interlace:
        raise ValueError(f"Expected non-interlaced RGB/RGBA8 atlas: {path}")
    channels = 4 if color == 6 else 3
    compressed, offset = bytearray(), 8
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        if data[offset + 4:offset + 8] == b"IDAT":
            compressed.extend(data[offset + 8:offset + 8 + length])
        offset += length + 12
    raw = zlib.decompress(compressed)
    stride, previous, pixels = width * channels, bytearray(width * channels), bytearray()
    for y in range(height):
        start = y * (stride + 1)
        filtering, row = raw[start], bytearray(raw[start + 1:start + stride + 1])
        if filtering not in range(5):
            raise ValueError(f"Unknown PNG filter: {filtering}")
        for i in range(stride):
            left, above = row[i - channels] if i >= channels else 0, previous[i]
            corner = previous[i - channels] if i >= channels else 0
            if filtering == 1: predictor = left
            elif filtering == 2: predictor = above
            elif filtering == 3: predictor = (left + above) // 2
            elif filtering == 4:
                estimate = left + above - corner
                distances = abs(estimate - left), abs(estimate - above), abs(estimate - corner)
                predictor = (left, above, corner)[distances.index(min(distances))]
            else: predictor = 0
            row[i] = (row[i] + predictor) & 255
        previous = row
        if channels == 4: pixels.extend(row)
        else:
            for i in range(0, stride, 3): pixels.extend((*row[i:i + 3], 255))
    return width, height, pixels


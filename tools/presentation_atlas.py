"""Compile existing material pixels and code-native lettering into single-texture models.

Hytale models share one atlas. Keep the complete original atlas on the left and
append only the required patches on the right; source PNGs remain untouched.
"""
from png_codec import png, texture
from workshop_material import atlas, apply_surface, PATCH_SIZE

GLYPHS = {
    "O": ("111", "101", "101", "101", "111"),
    "N": ("1001", "1101", "1011", "1001", "1001"),
    "F": ("111", "100", "110", "100", "100"),
}
GUTTER = 8  # Extruded borders prevent neighboring swatches bleeding at a distance.


def crop(pixels, width, u, v, w, h):
    return bytearray(b"".join(pixels[((v + y) * width + u) * 4:((v + y) * width + u + w) * 4]
                              for y in range(h)))


def paste(target, width, pixels, w, h, u, v):
    for y in range(h):
        start = ((v + y) * width + u) * 4
        target[start:start + w * 4] = pixels[y * w * 4:(y + 1) * w * 4]


def surface(item_id, role="paint"):
    shape = {"settings": {"size": dict.fromkeys("xyz", 1)}, "stretch": dict.fromkeys("xyz", 1)}
    apply_surface(shape, item_id, role)
    return shape


def patch(item_id, shape, face):
    w, h, pixels = texture(str(atlas(item_id)))
    uv = shape["textureLayout"][face]["offset"]
    size = shape["settings"]["size"]["x"]
    if not (0 <= uv["x"] <= w - size and 0 <= uv["y"] <= h - size):
        raise ValueError("Material patch exceeds its source atlas")
    return crop(pixels, w, uv["x"], uv["y"], size, size), size


def compile_atlas(item_id, patches):
    width, height, original = texture(str(atlas(item_id)))
    output_width = width + PATCH_SIZE + GUTTER * 2
    if sum(size + GUTTER * 2 for pixels, size in patches) > height:
        raise ValueError("Appended patches exceed atlas height")
    output = bytearray(b"\x00\x00\x00\xff" * (output_width * height))
    paste(output, output_width, original, width, height, 0, 0)
    y = 0
    for pixels, size in patches:
        rows = [bytes(pixels[row * size * 4:row * size * 4 + 4]) * GUTTER
                + pixels[row * size * 4:(row + 1) * size * 4]
                + bytes(pixels[(row + 1) * size * 4 - 4:(row + 1) * size * 4]) * GUTTER
                for row in range(size)]
        padded = rows[0] * GUTTER + b"".join(rows) + rows[-1] * GUTTER
        padded_size = size + GUTTER * 2
        paste(output, output_width, padded, padded_size, padded_size, width, y)
        y += padded_size
    return png(output_width, height, output)


def bench_patches():
    painted = surface("Logic_AND_Gate")
    lens = surface("Logic_AND_Gate", "indicator")
    return [patch("Logic_AND_Gate", painted, face) for face in ("top", "left", "bottom")] \
        + [patch("Logic_AND_Gate", lens, "top")]


def bench_texture():
    return compile_atlas("Logic_Workbench", bench_patches())


def apply_bench_and(shape, indicator=False):
    """Same physical box, referencing exact AND materials in the appended strip."""
    width, _, _ = texture(str(atlas("Logic_Workbench")))
    size = 64 if indicator else PATCH_SIZE
    for axis in "xyz":
        shape["stretch"][axis] *= shape["settings"]["size"][axis] / size
    shape["settings"]["size"] = dict.fromkeys("xyz", size)
    for face, mapping in shape["textureLayout"].items():
        row = 3 if indicator else 0 if face == "top" else 2 if face == "bottom" else 1
        mapping["offset"] = {"x": width + GUTTER, "y": (PATCH_SIZE + GUTTER * 2) * row + GUTTER}
        mapping["angle"] = 0
        mapping["mirror"] = {"x": False, "y": False}
    shape["shadingMode"] = "flat"


def word(pixels, text, center_x, center_y, scale, quarter_turns=0):
    """Rasterize the project's pixel glyphs as plain, non-emissive printed ink."""
    widths = [len(GLYPHS[letter][0]) for letter in text]
    x = center_x - (sum(widths) + len(text) - 1) * scale // 2
    y = center_y - 5 * scale // 2
    for letter, width in zip(text, widths):
        for row, pattern in enumerate(GLYPHS[letter]):
            for column, cell in enumerate(pattern):
                if cell != "1":
                    continue
                for dy in range(scale):
                    for dx in range(scale):
                        u, v = x + column * scale + dx, y + row * scale + dy
                        for _ in range(quarter_turns):
                            u, v = center_x - (v - center_y), center_y + (u - center_x)
                        if not (0 <= u < PATCH_SIZE and 0 <= v < PATCH_SIZE):
                            raise ValueError("Lever lettering exceeds face")
                        start = (v * PATCH_SIZE + u) * 4
                        pixels[start:start + 4] = b"\x1c\x1d\x1f\xff"
        x += (width + 1) * scale


def lever_patches():
    metal = surface("Logic_Lever", "iron")
    result = []
    # Front: negative X is OFF; positive X is ON. The back face reverses its
    # horizontal UV axis, so swap words there without mirroring their letters.
    for first, second in (("OFF", "ON"), ("ON", "OFF")):
        pixels, size = patch("Logic_Lever", metal, "front")
        word(pixels, first, 64, 128, 9)
        word(pixels, second, 192, 128, 9)
        result.append((pixels, size))
    pixels, size = patch("Logic_Lever", metal, "top")
    # Top UV: u follows +X and v follows +Z, as in the Hytale Blockbench codec.
    # Keep the rotated letters on the rim, outside the 6-by-6 central pedestal.
    word(pixels, "ON", 235, 128, 6, quarter_turns=1)
    word(pixels, "OFF", 21, 128, 6, quarter_turns=1)
    result.append((pixels, size))
    return result


def lever_texture():
    return compile_atlas("Logic_Lever", lever_patches())


def apply_lever_labels(shape):
    width, _, _ = texture(str(atlas("Logic_Lever")))
    for i, face in enumerate(("front", "back", "top")):
        shape["textureLayout"][face]["offset"] = {"x": width + GUTTER, "y": i * (PATCH_SIZE + GUTTER * 2) + GUTTER}

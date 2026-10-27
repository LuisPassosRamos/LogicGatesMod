"""Deterministic textured model renders, using only Python's standard library.

This renders the actual Hytale boxes/UVs; it never paints or recolors texture sources.
All items share an orthographic camera, soft directional light and transparent canvas.
"""
import argparse
import json
import math

from project_config import ROOT
from generate_terminal_assets import rotate
from asset_io import write_asset
from png_codec import png, texture

RESOURCE = ROOT / "src/main/resources"


FACES = {
    "front": ((0, 0, 1), ((-1, -1, 1), (1, -1, 1), (1, 1, 1), (-1, 1, 1))),
    "back": ((0, 0, -1), ((1, -1, -1), (-1, -1, -1), (-1, 1, -1), (1, 1, -1))),
    "right": ((1, 0, 0), ((1, -1, 1), (1, -1, -1), (1, 1, -1), (1, 1, 1))),
    "left": ((-1, 0, 0), ((-1, -1, -1), (-1, -1, 1), (-1, 1, 1), (-1, 1, -1))),
    "top": ((0, 1, 0), ((-1, 1, 1), (1, 1, 1), (1, 1, -1), (-1, 1, -1))),
    "bottom": ((0, -1, 0), ((-1, -1, 1), (-1, -1, -1), (1, -1, -1), (1, -1, 1)))
}
RIGHT = (2 ** -.5, 0, -(2 ** -.5))
UP = (-1 / math.sqrt(6), 2 / math.sqrt(6), -1 / math.sqrt(6))
CAMERA = (1 / math.sqrt(3),) * 3
LIGHT = (-.35, .85, .4)


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def mesh(model, texture_path, offset=(0, 0, 0), scale=1):
    result = []
    for node in model["nodes"]:
        shape = node["shape"]
        if shape["type"] != "box" or not shape.get("visible", True): continue
        if node.get("children"): raise ValueError("Render flattened runtime models")
        half = [shape["settings"]["size"][a] * shape["stretch"][a] / 2 for a in "xyz"]
        for face, (normal, signs) in FACES.items():
            normal = rotate(normal, node["orientation"])
            if dot(normal, CAMERA) <= 1e-6: continue
            vertices = []
            for sign in signs:
                point = [shape["offset"][a] + half[i] * sign[i] for i, a in enumerate("xyz")]
                point = rotate(point, node["orientation"])
                point = [(point[i] + node["position"][a]) * scale + offset[i] for i, a in enumerate("xyz")]
                vertices.append((dot(point, RIGHT), -dot(point, UP), dot(point, CAMERA)))
            illumination = 1 if shape["shadingMode"] == "fullbright" else .73 + .27 * max(0, dot(normal, LIGHT))
            uv = shape["textureLayout"][face]
            size = shape["settings"]["size"]
            w = size["z"] if face in ("left", "right") else size["x"]
            h = size["z"] if face in ("top", "bottom") else size["y"]
            result.append((vertices, texture_path, uv, w, h, illumination))
    return result


def render(faces, size=64):
    # Two samples per axis make readable transparent silhouettes at native icon size.
    canvas = size * 2
    xs = [v[0] for face in faces for v in face[0]]
    ys = [v[1] for face in faces for v in face[0]]
    factor = (canvas - 12) / max(max(xs) - min(xs), max(ys) - min(ys))
    center = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    pixels = bytearray(canvas * canvas * 4)
    depth = [-math.inf] * (canvas * canvas)
    for vertices, path, mapping, w, h, illumination in faces:
        points = [((v[0] - center[0]) * factor + canvas / 2,
                   (v[1] - center[1]) * factor + canvas / 2, v[2]) for v in vertices]
        image_w, image_h, image = texture(str(path))
        for ids in ((0, 1, 2), (0, 2, 3)):
            triangle = [points[i] for i in ids]
            uv_points = [(0, 1), (1, 1), (1, 0), (0, 0)]
            a, b, c = triangle
            denominator = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(denominator) < 1e-10: continue
            for y in range(max(0, math.floor(min(v[1] for v in triangle))), min(canvas, math.ceil(max(v[1] for v in triangle)))):
                for x in range(max(0, math.floor(min(v[0] for v in triangle))), min(canvas, math.ceil(max(v[0] for v in triangle)))):
                    p, q = x + .5, y + .5
                    weights = ((b[1] - c[1]) * (p - c[0]) + (c[0] - b[0]) * (q - c[1])) / denominator, \
                              ((c[1] - a[1]) * (p - c[0]) + (a[0] - c[0]) * (q - c[1])) / denominator
                    weights = (*weights, 1 - sum(weights))
                    if min(weights) < -1e-8: continue
                    z = sum(weights[i] * triangle[i][2] for i in range(3))
                    index = y * canvas + x
                    if z < depth[index] - 1e-8: continue
                    u, v = (sum(weights[i] * uv_points[ids[i]][axis] for i in range(3)) for axis in (0, 1))
                    angle = mapping["angle"] % 360
                    for _ in range(angle // 90): u, v = v, 1 - u
                    if mapping["mirror"]["x"]: u = -u
                    if mapping["mirror"]["y"]: v = -v
                    tx = max(0, min(image_w - 1, math.floor(mapping["offset"]["x"] + u * (w - 1))))
                    ty = max(0, min(image_h - 1, math.floor(mapping["offset"]["y"] + v * (h - 1))))
                    color = image[(ty * image_w + tx) * 4:(ty * image_w + tx) * 4 + 4]
                    if color[3] == 0: continue
                    depth[index] = z
                    pixels[index * 4:index * 4 + 4] = bytes((*[round(c * illumination) for c in color[:3]], color[3]))
    output = bytearray()
    for y in range(size):
        for x in range(size):
            samples = [pixels[((y * 2 + j) * canvas + x * 2 + i) * 4:((y * 2 + j) * canvas + x * 2 + i) * 4 + 4]
                       for j in range(2) for i in range(2)]
            alpha = sum(sample[3] for sample in samples)
            output.extend((*[round(sum(s[channel] * s[3] for s in samples) / alpha) if alpha else 0 for channel in range(3)], round(alpha / 4)))
    return png(size, size, output)


def item_mesh(item, mask=None, on=False, offset=(0, 0, 0), scale=1):
    block = item["BlockType"]
    if mask is not None:
        states = block["State"]["Definitions"]
        key = ("On_" if on else "Off_") + f"Connections_{mask}"
        block = {**block, **states.get(key, states.get(f"Connections_{mask}", {}))}
    model = json.loads((RESOURCE / "Common" / block["CustomModel"]).read_text(encoding="utf-8"))
    path = RESOURCE / "Common" / block["CustomModelTexture"][0]["Texture"]
    return mesh(model, path, offset, scale)


def outputs():
    from workshop_material import ITEMS
    result, items = {}, {}
    for item_id in ITEMS:
        matches = list((RESOURCE / "Server/Item/Items").rglob(item_id + ".json"))
        if len(matches) != 1: raise ValueError(f"Expected one item definition: {item_id}")
        item = items[item_id] = json.loads(matches[0].read_text(encoding="utf-8"))
        faces = item_mesh(item, 10 if item_id == "Logic_Cable" else 0 if item_id != "Logic_Workbench" else None)
        result[RESOURCE / "Common" / item["Icon"]] = render(faces)
    directory = RESOURCE / "Common/Icons/ItemCategories/LogicGates"
    scene = item_mesh(items["Logic_AND_Gate"], 2, True, offset=(-15, 0, 0))
    scene += item_mesh(items["Logic_Cable"], 10, offset=(9, 0, 0), scale=.5)
    scene += item_mesh(items["Logic_Lamp"], 8, True, offset=(26, 0, 0))
    result[directory / "Category.png"] = render(scene, 48)
    for category, item_id in (("Components", "Logic_Cable"), ("Gates", "Logic_AND_Gate"), ("Benches", "Logic_Workbench")):
        content = render(item_mesh(items[item_id], 10 if category == "Components" else 0 if category == "Gates" else None), 48)
        result[directory / (category + ".png")] = content
        if category != "Benches":
            result[RESOURCE / "Common/Icons/CraftingCategories/LogicGates" / (category + ".png")] = content
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = outputs()
    for path, content in files.items():
        if args.check:
            if not path.exists() or path.read_bytes() != content: raise SystemExit(f"Outdated model icon: {path}")
        else:
            write_asset(path, content)
    print(f"{len(files)} faithful model icons {'checked' if args.check else 'rendered'}")


if __name__ == "__main__":
    main()

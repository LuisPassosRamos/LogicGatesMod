"""Small geometric port labels, using opaque atlas tiles instead of introducing bitmap dependencies."""
from generate_cable_assets import box

GLYPHS = {
    "A": ("010", "101", "111", "101", "101"),
    "B": ("110", "101", "110", "101", "110"),
    "C": ("011", "100", "100", "100", "011")
}


def markings(kind, start, tile):
    nodes = []

    def piece(name, x, z, width, depth):
        node = box(start + len(nodes), name, x, z, width, depth)
        node["shape"]["offset"]["y"] = 4.06
        node["shape"]["stretch"]["y"] = .06  # physical thickness .12, below the indicator top
        for uv in node["shape"]["textureLayout"].values():
            uv["offset"] = dict(zip("xy", tile))
        nodes.append(node)

    labels = [("C", -3, 0, .5)]
    if kind != "NOT":
        labels = [("A", 0, -8, .6), ("B", 0, 8, .6)] + labels
    for label, x, z, pixel in labels:
        for row, pattern in enumerate(GLYPHS[label]):
            column = 0
            while column < len(pattern):
                if pattern[column] != "1":
                    column += 1
                    continue
                end = column + 1
                while end < len(pattern) and pattern[end] == "1": end += 1
                piece(f"port_label_{label}_{row}_{column}", x + ((column + end) / 2 - 1.5) * pixel,
                      z + (row - 2) * pixel, (end - column) * pixel, pixel)
                column = end
    # +X arrow, aligned with the single logical output for every rotation.
    piece("port_output_arrow_stem", 5.5, 0, 4, .6)
    piece("port_output_arrow_tip_top", 6.3, -.6, .6, .6)
    piece("port_output_arrow_tip_bottom", 6.3, .6, .6, .6)
    return nodes

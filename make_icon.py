"""生成 Fabric mod 图标 icon.png（128x128，Minecraft 风格像素画）。

不依赖第三方库：纯 Python 用 zlib + struct 手写 PNG。
逻辑分辨率 32x32，最近邻放大 4 倍到 128x128。
构图：天空渐变 + 云朵 + 悬浮空岛（草方块/泥土/石头）+ 岛上的奖励宝箱。
"""
import os
import struct
import zlib

LOGICAL = 32
SCALE = 4
OUT = os.path.join("src", "main", "resources", "assets", "random_island", "icon.png")

SKY_TOP = (0x5F, 0xAA, 0xE0)
SKY_BOTTOM = (0xBF, 0xE6, 0xFA)
CLOUD = (0xFF, 0xFF, 0xFF)
OUTLINE = (0x2E, 0x22, 0x1A)

GRASS_TOP = (0x86, 0xCC, 0x62)
GRASS_SIDE = (0x6A, 0xA8, 0x45)
DIRT = (0x8B, 0x5A, 0x2B)
DIRT_DARK = (0x71, 0x47, 0x21)
STONE = (0x84, 0x84, 0x8A)
STONE_DARK = (0x66, 0x66, 0x6E)

CHEST_LID = (0xC2, 0x8B, 0x3E)
CHEST_BODY = (0xA6, 0x6E, 0x31)
CHEST_BAND = (0x4A, 0x33, 0x1C)
CHEST_LATCH = (0xFF, 0xD7, 0x4D)

# 悬浮岛：每行 (y, x0, x1, 颜色)
ISLAND_ROWS = [
    (14, 9, 22, GRASS_TOP),
    (15, 9, 22, GRASS_SIDE),
    (16, 9, 22, DIRT),
    (17, 10, 21, DIRT),
    (18, 11, 20, DIRT_DARK),
    (19, 12, 19, STONE),
    (20, 12, 19, STONE),
    (21, 14, 17, STONE_DARK),
]

# 宝箱：坐在草方块顶面（y=14）之上
CHEST_X0, CHEST_X1 = 13, 18
CHEST_LID_Y = 11
CHEST_BAND_Y = 12
CHEST_BODY_Y = 13
CHEST_LATCH_X = (15, 16)
CHEST_LATCH_Y = CHEST_BODY_Y
CHEST_LATCH_COLOR = (0xFF, 0xD7, 0x4D)

CLOUDS = [
    (2, 5, 5, 1), (10, 3, 5, 1), (24, 4, 4, 1),
    (6, 9, 3, 1), (22, 10, 3, 1), (26, 8, 3, 1),
]


def build_mask():
    """返回 {(x, y): (r, g, b)}：先填主体，再补一圈深色描边。"""
    mask = {}

    for y, x0, x1, color in ISLAND_ROWS:
        for x in range(x0, x1 + 1):
            mask[(x, y)] = color

    for x in range(CHEST_X0, CHEST_X1 + 1):
        mask[(x, CHEST_LID_Y)] = CHEST_LID
        mask[(x, CHEST_BAND_Y)] = CHEST_BAND
        mask[(x, CHEST_BODY_Y)] = CHEST_BODY
    for x in CHEST_LATCH_X:
        mask[(x, CHEST_LATCH_Y)] = CHEST_LATCH_COLOR

    outlined = dict(mask)
    for (x, y) in mask:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in mask:
                outlined[(x + dx, y + dy)] = OUTLINE
    return outlined


def render():
    pixels = []
    for y in range(LOGICAL):
        t = y / (LOGICAL - 1)
        base = tuple(int(SKY_TOP[i] + (SKY_BOTTOM[i] - SKY_TOP[i]) * t) for i in range(3))
        pixels.append([(base[0], base[1], base[2], 255)] * LOGICAL)

    for cx, cy, cw, ch in CLOUDS:
        for y in range(cy, cy + ch):
            for x in range(cx, cx + cw):
                if 0 <= x < LOGICAL and 0 <= y < LOGICAL:
                    pixels[y][x] = CLOUD + (255,)

    for (x, y), color in build_mask().items():
        if 0 <= x < LOGICAL and 0 <= y < LOGICAL:
            pixels[y][x] = color + (255,)

    big = []
    for y in range(LOGICAL):
        row = []
        for x in range(LOGICAL):
            row.extend([pixels[y][x]] * SCALE)
        for _ in range(SCALE):
            big.append(list(row))
    return big


def write_png(path, pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for px in row:
            raw.extend(bytes(px))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", len(pixels[0]), len(pixels), 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", ihdr)
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


image = render()
write_png(OUT, image)
print("written:", OUT)
print("dimensions:", len(image[0]), "x", len(image))
print("size:", os.path.getsize(OUT), "bytes")

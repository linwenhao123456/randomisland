import struct, zlib, os

W = H = 128

def px(x, y):
    # 背景：天空蓝渐变
    sky = (135, 206, 235)
    if y < 82:
        # 顶部稍深的蓝，底部更亮的蓝
        t = y / 82.0
        return (int(sky[0] * (0.8 + 0.2 * t)), int(sky[1] * (0.8 + 0.2 * t)), int(sky[2] * (0.8 + 0.2 * t)), 255)
    # 空岛：中央 64x18 的方块
    cx0, cx1 = 32, 96
    cy0, cy1 = 82, 100
    if cx0 <= x < cx1 and cy0 <= y < cy1:
        if y < cy0 + 6:
            return (86, 184, 66, 255)      # 草顶 绿
        return (134, 96, 67, 255)          # 泥土 棕
    # 其余透明
    return (0, 0, 0, 0)

# 构造原始图像数据：每行前加 filter byte(0)
raw = bytearray()
for y in range(H):
    raw.append(0)
    for x in range(W):
        r, g, b, a = px(x, y)
        raw += bytes((r, g, b, a))

compressed = zlib.compress(bytes(raw), 9)

def chunk(tag, data):
    out = struct.pack(">I", len(data)) + tag + data
    crc = zlib.crc32(tag + data) & 0xffffffff
    return out + struct.pack(">I", crc)

png = b"\x89PNG\r\n\x1a\n"
png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))  # 8位 RGBA
png += chunk(b"IDAT", compressed)
png += chunk(b"IEND", b"")

out_path = r"e:\PY Project\随机空岛\src\main\resources\assets\random_island\icon.png"
os.makedirs(os.path.dirname(out_path), exist_ok=True)
with open(out_path, "wb") as f:
    f.write(png)
print("生成图标:", out_path, len(png), "字节")

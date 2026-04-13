#!/usr/bin/env python3
import struct, zlib, os

def make_png(path, sz, r=21, g=101, b=192):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    def ck(n, d):
        c = struct.pack(">I", len(d)) + n + d
        return c + struct.pack(">I", zlib.crc32(n + d) & 0xFFFFFFFF)
    row = b"\x00" + bytes([r, g, b]) * sz
    raw = row * sz
    with open(path, "wb") as f:
        f.write(
            b"\x89PNG\r\n\x1a\n"
            + ck(b"IHDR", struct.pack(">IIBBBBB", sz, sz, 8, 2, 0, 0, 0))
            + ck(b"IDAT", zlib.compress(raw))
            + ck(b"IEND", b"")
        )

base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/res")
icons = [("mipmap-mdpi",48),("mipmap-hdpi",72),("mipmap-xhdpi",96),("mipmap-xxhdpi",144),("mipmap-xxxhdpi",192)]
for d, sz in icons:
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        make_png(os.path.join(base, d, name), sz)
    print(f"OK {d} ({sz}px)")
print("Done.")

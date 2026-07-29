#!/usr/bin/env python3
"""AnyWebView launcher icon generator - final design (dark + neon cyan globe + 8-tooth gear).

Regenerates:
  - fastlane/metadata/android/en-US/images/icon.png (512)
  - app/src/main/res/mipmap-*/ic_launcher.png (legacy launcher, 48-192)

The adaptive-icon foreground (API 26+) is a hand-written VectorDrawable at
app/src/main/res/drawable/ic_launcher_foreground.xml - keep it in sync with this file.
"""
import math
import os
from PIL import Image, ImageDraw

S = 1024
CYAN = (103, 232, 249)   # #67E8F9
DARK_TOP = (16, 22, 34)  # #101622
DARK_BOT = (6, 8, 14)    # #06080E

def vgrad(size, top, bottom, radius_ratio=0.18):
    img = Image.new("RGB", (size, size), top)
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(size):
            px[x, y] = c
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * radius_ratio), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out

def globe(d, cx, cy, r, w, color=CYAN):
    # meridian + latitudes first; chord endpoints inset by half line width
    d.ellipse([cx - r * 0.52, cy - r, cx + r * 0.52, cy + r], outline=color, width=int(w * 0.85))
    for dy, lw in [(0, w), (-r * 0.47, int(w * 0.8)), (r * 0.47, int(w * 0.8))]:
        half = math.sqrt(max(r * r - dy * dy, 0)) - lw / 2
        d.line([cx - half, cy + dy, cx + half, cy + dy], fill=color, width=lw)
    # outer circle last: covers joints
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=w)

def gear_badge(base, bx, by, br, gear_color=CYAN, ring_color=DARK_TOP, teeth=8):
    d = ImageDraw.Draw(base)
    ring = int(br * 0.16)
    d.ellipse([bx - br - ring, by - br - ring, bx + br + ring, by + br + ring], fill=ring_color)
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hw = br * 0.20
    for i in range(teeth):
        tooth = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        td = ImageDraw.Draw(tooth)
        td.rounded_rectangle([bx - hw, by - br, bx + hw, by - br * 0.58],
                             radius=hw * 0.45, fill=gear_color)
        layer = Image.alpha_composite(layer, tooth.rotate(i * (360 / teeth), resample=Image.BICUBIC, center=(bx, by)))
    base = Image.alpha_composite(base, layer)
    d = ImageDraw.Draw(base)
    d.ellipse([bx - br * 0.70, by - br * 0.70, bx + br * 0.70, by + br * 0.70], fill=gear_color)
    d.ellipse([bx - br * 0.30, by - br * 0.30, bx + br * 0.30, by + br * 0.30], fill=ring_color)
    return base

def full_icon():
    img = vgrad(S, DARK_TOP, DARK_BOT)
    d = ImageDraw.Draw(img)
    globe(d, S * 0.5, S * 0.47, S * 0.30, int(S * 0.026))
    return gear_badge(img, S * 0.68, S * 0.67, S * 0.14)

def save(img, path, size):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(path)
    print("wrote", path, size)

if __name__ == "__main__":
    full = full_icon()
    save(full, "fastlane/metadata/android/en-US/images/icon.png", 512)
    for dpi, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        save(full, f"app/src/main/res/mipmap-{dpi}/ic_launcher.png", px)

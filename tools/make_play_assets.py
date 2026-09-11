"""Generate the Google Play store assets from the app's own console styling.

Outputs into play/:
  icon-512.png                  512x512, required by Play
  feature-graphic-1024x500.png  1024x500, required by Play
  screenshots/*.png             1080x1920, padded from whatever is in play/source

Play rejects phone screenshots outside a 16:9..9:16 aspect ratio, and a modern
phone capture is taller than that, so each source image is scaled to fit and
padded with the app's background colour rather than cropped.

Usage:  python tools/make_play_assets.py
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "play"
SOURCE = OUT / "source"

INK = (10, 13, 11)
PANEL = (18, 23, 19)
EDGE = (36, 48, 38)
AMBER = (232, 178, 58)
SIGNAL = (125, 219, 138)
DIM = (141, 154, 143)
BRIGHT = (233, 240, 233)

SCREENSHOT_SIZE = (1080, 1920)

# A device capture includes the status bar and the navigation bar, which carry
# the owner's clock, battery and notification icons and say nothing about the
# app. Trimmed off before padding. Set both to 0 for a capture that has none.
CROP_TOP = 100
CROP_BOTTOM = 120

FONT_CANDIDATES = [
    "C:/Windows/Fonts/consolab.ttf",
    "C:/Windows/Fonts/consola.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
]


def load_font(size: int, bold: bool = True) -> ImageFont.FreeTypeFont:
    for candidate in FONT_CANDIDATES:
        path = Path(candidate)
        if not path.exists():
            continue
        if bold and "b.ttf" not in path.name and "Bold" not in path.name:
            continue
        return ImageFont.truetype(str(path), size)
    for candidate in FONT_CANDIDATES:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    raise SystemExit("No monospace TrueType font found; edit FONT_CANDIDATES.")


def tracked_width(draw: ImageDraw.ImageDraw, text: str, font, tracking: float) -> float:
    if not text:
        return 0.0
    return sum(draw.textlength(c, font=font) for c in text) + tracking * (len(text) - 1)


def draw_tracked(draw, xy, text, font, fill, tracking=0.0) -> None:
    x, y = xy
    for char in text:
        draw.text((x, y), char, font=font, fill=fill)
        x += draw.textlength(char, font=font) + tracking


def draw_grid(draw: ImageDraw.ImageDraw, size, step: int, colour) -> None:
    width, height = size
    x = step
    while x < width:
        draw.line([(x, 0), (x, height)], fill=colour, width=2)
        x += step
    y = step
    while y < height:
        draw.line([(0, y), (width, y)], fill=colour, width=2)
        y += step


def draw_mark(draw: ImageDraw.ImageDraw, cx: float, cy: float, radius: float) -> None:
    """The crosshair and shell used by the launcher icon, at any size."""
    stroke = max(2, round(radius * 0.18))
    draw.ellipse(
        [cx - radius, cy - radius, cx + radius, cy + radius],
        outline=AMBER,
        width=stroke,
    )
    inner, outer = radius * 0.80, radius * 1.38
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        draw.line(
            [cx + dx * inner, cy + dy * inner, cx + dx * outer, cy + dy * outer],
            fill=AMBER,
            width=stroke,
        )
    shell = radius * 0.30
    draw.polygon(
        [
            (cx, cy - shell),
            (cx + shell * 0.64, cy + shell * 0.56),
            (cx - shell * 0.64, cy + shell * 0.56),
        ],
        fill=SIGNAL,
    )


def make_icon(path: Path) -> None:
    size = 512
    image = Image.new("RGB", (size, size), PANEL)
    draw = ImageDraw.Draw(image)
    draw_grid(draw, (size, size), size // 3, EDGE)
    draw_mark(draw, size / 2, size / 2, radius=size * 0.203)
    image.save(path)
    print(f"{path.relative_to(ROOT)}  {size}x{size}")


def make_feature_graphic(path: Path) -> None:
    width, height = 1024, 500
    image = Image.new("RGB", (width, height), INK)
    draw = ImageDraw.Draw(image)
    draw_grid(draw, (width, height), 64, (20, 26, 21))
    draw.rectangle([20, 20, width - 21, height - 21], outline=EDGE, width=2)

    draw_mark(draw, 205, height / 2, radius=108)

    left = 390
    title_font = load_font(62)
    sub_font = load_font(26)
    body_font = load_font(22, bold=False)

    draw_tracked(draw, (left, 150), "WARDOGS IDF", title_font, BRIGHT, tracking=4)
    draw_tracked(draw, (left, 236), "INDIRECT FIRE CALCULATOR", sub_font, AMBER, tracking=4)
    draw.text((left, 296), "Range, bearing and mortar elevation", font=body_font, fill=DIM)
    draw.text((left, 330), "from two map grid coordinates.", font=body_font, fill=DIM)
    draw.text((left, 382), "Offline. No ads. No tracking.", font=body_font, fill=SIGNAL)

    image.save(path)
    print(f"{path.relative_to(ROOT)}  {width}x{height}")


def fit_screenshot(source: Path, destination: Path) -> None:
    target_w, target_h = SCREENSHOT_SIZE
    canvas = Image.new("RGB", SCREENSHOT_SIZE, INK)
    with Image.open(source) as original:
        image = original.convert("RGB")
        if CROP_TOP or CROP_BOTTOM:
            image = image.crop((0, CROP_TOP, image.width, image.height - CROP_BOTTOM))
        scale = min(target_w / image.width, target_h / image.height)
        resized = image.resize(
            (round(image.width * scale), round(image.height * scale)),
            Image.LANCZOS,
        )
    canvas.paste(resized, ((target_w - resized.width) // 2, (target_h - resized.height) // 2))
    canvas.save(destination)
    print(f"{destination.relative_to(ROOT)}  {target_w}x{target_h}  from {source.name}")


def main() -> int:
    OUT.mkdir(exist_ok=True)
    shots_out = OUT / "screenshots"
    shots_out.mkdir(exist_ok=True)

    make_icon(OUT / "icon-512.png")
    make_feature_graphic(OUT / "feature-graphic-1024x500.png")

    if not SOURCE.is_dir():
        print(f"No {SOURCE.relative_to(ROOT)} directory; skipping screenshots.")
        return 0

    sources = sorted(SOURCE.glob("*.png"))
    if not sources:
        print(f"No PNGs in {SOURCE.relative_to(ROOT)}; skipping screenshots.")
        return 0
    for source in sources:
        fit_screenshot(source, shots_out / source.name)
    return 0


if __name__ == "__main__":
    sys.exit(main())

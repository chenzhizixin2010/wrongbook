"""把真机/模拟器截图拼成一张 UI 展示图。

用法：
    python scripts/make_ui_sheet.py
输入：docs/screens/{home,scan,chat,generate,settings}.png
输出：docs/ui-screens.png
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
SHOTS = ROOT / "docs" / "screens"
OUT = ROOT / "docs" / "ui-screens.png"

ITEMS = [
    ("splash", "开屏"),
    ("home", "题库"),
    ("scan", "扫描"),
    ("chat", "AI 对话"),
    ("generate", "生成"),
    ("settings", "设置"),
]

BG = (14, 15, 20)
FG = (236, 237, 244)
MUTED = (150, 152, 166)

SCALE = 0.52
GAP = 30
MARGIN = 52


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\msyh.ttc",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def rounded(im: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width, im.height), radius=radius, fill=255)
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out


def main() -> None:
    shots = []
    for key, _ in ITEMS:
        path = SHOTS / f"{key}.png"
        im = Image.open(path).convert("RGB")
        im = im.resize((int(im.width * SCALE), int(im.height * SCALE)), Image.LANCZOS)
        shots.append(rounded(im, 34))

    w, h = shots[0].size
    title_h = 128
    label_h = 64
    width = MARGIN * 2 + w * len(shots) + GAP * (len(shots) - 1)
    height = title_h + h + label_h + MARGIN

    sheet = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(sheet)
    draw.text((MARGIN, 34), "Errata · Liquid Glass", font=font(38, bold=True), fill=FG)
    draw.text(
        (MARGIN, 84),
        "实际运行截图 · 1080×1920 · 开屏 + 5 个主 Tab · 浅色模式",
        font=font(19),
        fill=MUTED,
    )

    x = MARGIN
    for im, (_, label) in zip(shots, ITEMS):
        # 柔和投影
        shadow = Image.new("RGBA", (w + 40, h + 40), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle(
            (20, 20, 20 + w, 20 + h), radius=38, fill=(0, 0, 0, 150)
        )
        sheet.paste(shadow, (x - 20, title_h - 20), shadow)
        sheet.paste(im, (x, title_h), im)
        tw = draw.textlength(label, font=font(21, bold=True))
        draw.text((x + (w - tw) / 2, title_h + h + 18), label, font=font(21, bold=True), fill=FG)
        x += w + GAP

    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT, optimize=True)
    print(f"written {OUT} ({width}x{height})")


if __name__ == "__main__":
    main()

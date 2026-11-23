from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

root = Path(r"D:\错题整理app")
shots = root / "docs" / "screens"

columns = [
    ("MiuiX / HyperOS", ["miuix-home-light.png", "miuix-settings-light.png", "miuix-generate-light.png"]),
    ("iOS / Cupertino", ["ios-home-dark.png", "ios-settings-dark.png", "ios-chat-dark.png"]),
    ("0.9.0 液态玻璃", ["v090-home.png", "v090-settings.png", "v090-scan.png"]),
    ("MD3 Expressive", ["md3e-home.png", "md3e-settings.png", "md3e-scan.png"]),
]

font_path = r"C:\Windows\Fonts\msyh.ttc"
title_font = ImageFont.truetype(font_path, 44)
sub_font = ImageFont.truetype(font_path, 22)
col_font = ImageFont.truetype(font_path, 26)
small_font = ImageFont.truetype(font_path, 19)

thumb_w = 300
thumb_h = int(thumb_w * 1920 / 1080)
gap = 24
margin = 40
header_h = 200
col_header_h = 46
width = margin * 2 + len(columns) * thumb_w + (len(columns) - 1) * gap
height = header_h + col_header_h + 3 * thumb_h + 2 * gap + margin

canvas = Image.new("RGB", (width, height), "#F2F3F5")
draw = ImageDraw.Draw(canvas)
draw.text((margin, 38), "Errata", font=title_font, fill="#1B1B1B")
draw.text((margin + 190, 52), "Four skins", font=sub_font, fill="#2F5F8F")
draw.text(
    (margin, 98),
    "澎湃 OS / iOS / 0.9.0 液态玻璃 / MD3 Expressive · 设置里实时切换",
    font=small_font,
    fill="#6B7280",
)

for col_idx, (name, files) in enumerate(columns):
    x = margin + col_idx * (thumb_w + gap)
    draw.text((x, header_h - 30), name, font=col_font, fill="#2A2A2A")
    for row_idx, file in enumerate(files):
        img = Image.open(shots / file).convert("RGB")
        img = img.resize((thumb_w, thumb_h), Image.LANCZOS)
        y = header_h + col_header_h + row_idx * (thumb_h + gap)
        draw.rectangle((x - 1, y - 1, x + thumb_w, y + thumb_h), outline="#D7DBE0", width=2)
        canvas.paste(img, (x, y))

out = root / "docs" / "ui-screens.png"
canvas.save(out, optimize=True)
print(out, canvas.size)

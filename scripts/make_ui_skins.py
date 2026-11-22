from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

root = Path(r"D:\错题整理app")
shots = root / "docs" / "screens"
items = [
    ("miuix-home-light.png", "Miuix · 题库"),
    ("miuix-settings-light.png", "Miuix · 设置"),
    ("miuix-generate-light.png", "Miuix · 生成"),
    ("ios-home-dark.png", "iOS · 题库"),
    ("ios-settings-dark.png", "iOS · 设置"),
    ("ios-chat-dark.png", "iOS · 对话"),
    ("v090-home.png", "0.9.0 · 题库"),
    ("v090-settings.png", "0.9.0 · 设置"),
    ("v090-scan.png", "0.9.0 · 扫描"),
]

font_path = r"C:\Windows\Fonts\msyh.ttc"
title_font = ImageFont.truetype(font_path, 44)
sub_font = ImageFont.truetype(font_path, 24)
label_font = ImageFont.truetype(font_path, 24)
small_font = ImageFont.truetype(font_path, 20)

thumb_w = 300
thumb_h = int(thumb_w * 1920 / 1080)
gap = 28
margin = 48
cols = 3
rows = 3
header_h = 152
label_h = 46
width = margin * 2 + cols * thumb_w + (cols - 1) * gap
height = header_h + rows * (thumb_h + label_h) + (rows - 1) * gap + margin

canvas = Image.new("RGB", (width, height), "#F2F3F5")
draw = ImageDraw.Draw(canvas)
draw.text((margin, 42), "Errata", font=title_font, fill="#1B1B1B")
draw.text((margin + 190, 58), "Three skins", font=sub_font, fill="#2F5F8F")
draw.text(
    (margin, 106),
    "澎湃 OS（Miuix） / iOS（Cupertino） / 0.9.0（液态玻璃） · 设置里实时切换",
    font=small_font,
    fill="#6B7280",
)

for i, (name, label) in enumerate(items):
    img = Image.open(shots / name).convert("RGB")
    img = img.resize((thumb_w, thumb_h), Image.LANCZOS)
    col = i % cols
    row = i // cols
    x = margin + col * (thumb_w + gap)
    y = header_h + row * (thumb_h + label_h + gap)
    draw.rectangle((x - 1, y - 1, x + thumb_w, y + thumb_h), outline="#D7DBE0", width=2)
    canvas.paste(img, (x, y))
    tw = draw.textlength(label, font=label_font)
    draw.text((x + (thumb_w - tw) / 2, y + thumb_h + 10), label, font=label_font, fill="#2A2A2A")

out = root / "docs" / "ui-screens.png"
canvas.save(out, optimize=True)
print(out, canvas.size)

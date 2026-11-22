from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

root = Path(r"D:\错题整理app")
shots = root / "docs" / "screens"
items = [
    ("home-light.png", "题库"),
    ("scan-light.png", "扫描"),
    ("generate-light.png", "生成"),
    ("chat-light.png", "AI 对话"),
    ("settings-light.png", "设置"),
    ("home-dark.png", "深色模式"),
]

font_path = r"C:\Windows\Fonts\msyh.ttc"
title_font = ImageFont.truetype(font_path, 44)
sub_font = ImageFont.truetype(font_path, 24)
label_font = ImageFont.truetype(font_path, 26)
small_font = ImageFont.truetype(font_path, 20)

thumb_w = 300
thumb_h = int(thumb_w * 1920 / 1080)
gap = 28
margin = 48
cols = 3
rows = 2
header_h = 150
label_h = 44
width = margin * 2 + cols * thumb_w + (cols - 1) * gap
height = header_h + rows * (thumb_h + label_h) + (rows - 1) * gap + margin

canvas = Image.new("RGB", (width, height), "#F2F3F5")
draw = ImageDraw.Draw(canvas)
draw.text((margin, 42), "Errata", font=title_font, fill="#1B1B1B")
draw.text((margin + 190, 58), "Miuix / HyperOS", font=sub_font, fill="#2F5F8F")
draw.text(
    (margin, 104),
    "纯色 · Monet 动态取色 · 贴底导航 · 内嵌列表",
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
    # 1px 描边，避免白底截图糊在画布上
    draw.rectangle((x - 1, y - 1, x + thumb_w, y + thumb_h), outline="#D7DBE0", width=2)
    canvas.paste(img, (x, y))
    tw = draw.textlength(label, font=label_font)
    draw.text((x + (thumb_w - tw) / 2, y + thumb_h + 10), label, font=label_font, fill="#2A2A2A")

out = root / "docs" / "ui-screens.png"
canvas.save(out, optimize=True)
print(out, canvas.size)

# App icon 512
icon = Image.new("RGB", (512, 512), "#3482FF")
d = ImageDraw.Draw(icon)
d.rounded_rectangle((0, 0, 512, 512), radius=112, fill="#3482FF")
for y, x2 in ((158, 372), (248, 300), (338, 372)):
    d.rounded_rectangle((142, y - 22, x2, y + 22), radius=22, fill="#FFFFFF")
icon_path = root / "docs" / "app-icon.png"
icon.save(icon_path, optimize=True)
print(icon_path)

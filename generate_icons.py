import os
from PIL import Image, ImageDraw

densities = {
    'mipmap-mdpi': {'size': 48, 'fg_size': 108},
    'mipmap-hdpi': {'size': 72, 'fg_size': 162},
    'mipmap-xhdpi': {'size': 96, 'fg_size': 216},
    'mipmap-xxhdpi': {'size': 144, 'fg_size': 324},
    'mipmap-xxxhdpi': {'size': 192, 'fg_size': 432}
}

res_dir = 'app/src/main/res'

def create_icon_image(size, is_round=False, is_foreground=False):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0) if is_foreground else (18, 67, 42, 255))
    draw = ImageDraw.Draw(img)

    if not is_foreground:
        draw.rectangle([0, 0, size, size], fill=(18, 67, 42, 255))
        border_width = max(2, int(size * 0.04))
        corner_radius = int(size * 0.22) if not is_round else int(size * 0.5)

        if is_round:
            draw.ellipse([0, 0, size - 1, size - 1], outline=(212, 175, 55, 255), width=border_width)
        else:
            draw.rounded_rectangle(
                [0, 0, size - 1, size - 1],
                radius=corner_radius,
                outline=(212, 175, 55, 255),
                width=border_width
            )

    # Full-bleed scale (scale factor 1.0 relative to 108x108 base design)
    def scale(val):
        return max(1, int(val * (size / 108.0)))

    def tx(val):
        return int(val * (size / 108.0))

    def ty(val):
        return int(val * (size / 108.0))

    # Back card shadow
    back_shadow_box = [tx(27), ty(30), tx(69), ty(94)]
    draw.rounded_rectangle(back_shadow_box, radius=scale(4), fill=(68, 0, 0, 68))

    # Back card
    back_box = [tx(24), ty(27), tx(66), ty(91)]
    draw.rounded_rectangle(
        back_box,
        radius=scale(4),
        fill=(255, 248, 231, 255), # #FFF8E7
        outline=(197, 160, 89, 255), # #C5A059
        width=max(1, scale(1.5))
    )

    # Front card shadow
    front_shadow_box = [tx(43), ty(19), tx(85), ty(83)]
    draw.rounded_rectangle(front_shadow_box, radius=scale(4), fill=(102, 0, 0, 102))

    # Front card
    front_box = [tx(40), ty(16), tx(82), ty(80)]
    draw.rounded_rectangle(
        front_box,
        radius=scale(4),
        fill=(255, 255, 255, 255),
        outline=(212, 175, 55, 255), # #D4AF37
        width=max(1, scale(2))
    )

    # Center Gold Spade Symbol
    spade_cx = tx(53)
    spade_cy = ty(48)
    spade_r = scale(10)

    draw.polygon([
        (spade_cx, spade_cy - spade_r),
        (spade_cx - spade_r, spade_cy + int(spade_r * 0.5)),
        (spade_cx + spade_r, spade_cy + int(spade_r * 0.5))
    ], fill=(212, 175, 55, 255))

    stem_w = max(2, scale(3))
    stem_h = scale(6)
    draw.rectangle([
        spade_cx - stem_w//2, spade_cy + int(spade_r * 0.4),
        spade_cx + stem_w//2, spade_cy + int(spade_r * 0.4) + stem_h
    ], fill=(212, 175, 55, 255))

    # Gold Accent Ribbon on Front Card
    ribbon_box = [tx(40), ty(72), tx(82), ty(76)]
    draw.rectangle(ribbon_box, fill=(218, 165, 32, 255)) # #DAA520

    if is_round:
        mask = Image.new('L', (size, size), 0)
        mask_draw = ImageDraw.Draw(mask)
        mask_draw.ellipse([0, 0, size, size], fill=255)
        img.putalpha(mask)

    return img

for density, info in densities.items():
    folder = os.path.join(res_dir, density)
    os.makedirs(folder, exist_ok=True)

    size = info['size']
    fg_size = info['fg_size']

    # Generate ic_launcher.png (square)
    ic_img = create_icon_image(size, is_round=False, is_foreground=False)
    ic_img.save(os.path.join(folder, 'ic_launcher.png'))

    # Generate ic_launcher_round.png (round)
    round_img = create_icon_image(size, is_round=True, is_foreground=False)
    round_img.save(os.path.join(folder, 'ic_launcher_round.png'))

    # Generate ic_launcher_foreground.png (foreground for adaptive icon)
    fg_img = create_icon_image(fg_size, is_round=False, is_foreground=True)
    fg_img.save(os.path.join(folder, 'ic_launcher_foreground.png'))

print("All full-bleed launcher icons generated successfully!")

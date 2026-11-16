# 로고(식판)로 안드로이드 적응형 아이콘(전경·단색)과 스토어용 512px 아이콘을,
# 캐릭터 그림으로 앱 화면용 마스코트를 만든다.
# 실행: python store/make_icon.py store/logo-original.webp store/mascot-original.webp . preview.png
import os, sys
from collections import deque
from PIL import Image, ImageDraw

logo_src, mascot_src, project, preview_path = sys.argv[1:5]
res = os.path.join(project, 'app', 'src', 'main', 'res')
store = os.path.join(project, 'store')


def clear_background(img, is_background):
    """네 귀퉁이에서 이어진 바탕 픽셀만 투명하게 한다. 그림 안쪽의 같은 색은 테두리에 막혀 남는다."""
    img = img.convert('RGBA')
    w, h = img.size
    px = img.load()
    seen = bytearray(w * h)
    q = deque([(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)])
    while q:
        x, y = q.popleft()
        i = y * w + x
        if seen[i]:
            continue
        seen[i] = 1
        if not is_background(px[x, y]):
            continue
        px[x, y] = (0, 0, 0, 0)
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not seen[ny * w + nx]:
                q.append((nx, ny))
    return img


# ---------- 1) 로고: 노란 바탕을 떼어 내고 식판만 남긴다 ----------
logo = Image.open(logo_src).convert('RGB')
lw, lh = logo.size
corners = [logo.getpixel(p) for p in ((2, 2), (lw - 3, 2), (2, lh - 3), (lw - 3, lh - 3))]
bg = tuple(sorted(c[i] for c in corners)[len(corners) // 2] for i in range(3))
tray = clear_background(logo, lambda c: all(abs(c[i] - bg[i]) <= 28 for i in range(3)))
tray = tray.crop(tray.getbbox())
tw, th = tray.size
print('background #%02X%02X%02X' % bg, 'tray', tw, 'x', th)

# ---------- 2) 적응형 아이콘 전경 ----------
# 108dp 캔버스에서 폰이 원으로 자르면 지름 72dp만 보인다. 식판의 대각선 절반이 반지름 31dp 안에 들게
# 크기를 정해서, 둥근 아이콘에서도 모서리가 잘리지 않게 한다.
half_diagonal = ((tw / 2) ** 2 + (th / 2) ** 2) ** 0.5
scale_dp = 31 / half_diagonal  # 원본 1px당 dp

# 단색(테마 아이콘)용: 식판의 하얀 부분만 남긴 모양
mono = Image.new('RGBA', tray.size, (0, 0, 0, 0))
tp, mp = tray.load(), mono.load()
for y in range(th):
    for x in range(tw):
        r, g, b, a = tp[x, y]
        if a and r > 235 and g > 235 and b > 235:
            mp[x, y] = (255, 255, 255, 255)

densities = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
for name, density in densities.items():
    canvas = round(108 * density)
    size = (round(tw * scale_dp * density), round(th * scale_dp * density))
    offset = ((canvas - size[0]) // 2, (canvas - size[1]) // 2)
    folder = os.path.join(res, 'mipmap-' + name)
    os.makedirs(folder, exist_ok=True)
    for img, fname in ((tray, 'ic_launcher_foreground.png'), (mono, 'ic_launcher_monochrome.png')):
        layer = Image.new('RGBA', (canvas, canvas), (0, 0, 0, 0))
        layer.alpha_composite(img.resize(size, Image.LANCZOS), offset)
        layer.save(os.path.join(folder, fname), optimize=True)

# ---------- 3) 플레이스토어용 512px (정사각형, 투명 없음) ----------
store_icon = Image.new('RGBA', (512, 512), bg + (255,))
store_scale = 512 * 0.62 / tw  # 식판이 가로 62% 정도를 차지하게
ssize = (round(tw * store_scale), round(th * store_scale))
store_icon.alpha_composite(tray.resize(ssize, Image.LANCZOS), ((512 - ssize[0]) // 2, (512 - ssize[1]) // 2))
store_icon.convert('RGB').save(os.path.join(store, 'icon-512.png'), optimize=True)

# ---------- 4) 앱 화면용 마스코트 (흰 바탕을 떼어 낸 캐릭터) ----------
mascot = clear_background(Image.open(mascot_src), lambda c: c[0] > 235 and c[1] > 235 and c[2] > 235)
mascot_dir = os.path.join(res, 'drawable-nodpi')
os.makedirs(mascot_dir, exist_ok=True)
mascot.resize((360, 360), Image.LANCZOS).save(os.path.join(mascot_dir, 'mascot.png'), optimize=True)

# ---------- 미리보기: 원형·둥근 네모로 잘린 런처 아이콘 ----------
fg = Image.open(os.path.join(res, 'mipmap-xxxhdpi', 'ic_launcher_foreground.png'))
full = Image.new('RGBA', (432, 432), bg + (255,))
full.alpha_composite(fg)
out = Image.new('RGBA', (2 * 312 + 40, 312), (255, 255, 255, 255))
for i, shape in enumerate(('circle', 'squircle')):
    mask = Image.new('L', (432, 432), 0)
    box = (72, 72, 360, 360)
    if shape == 'circle':
        ImageDraw.Draw(mask).ellipse(box, fill=255)
    else:
        ImageDraw.Draw(mask).rounded_rectangle(box, radius=80, fill=255)
    tile = Image.new('RGBA', (432, 432), (255, 255, 255, 255))
    tile.paste(full, (0, 0), mask)
    out.paste(tile.crop((60, 60, 372, 372)), (i * (312 + 40), 0))
out.save(preview_path)
print('ok')

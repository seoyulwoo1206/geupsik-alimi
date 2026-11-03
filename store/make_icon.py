# 로고 그림으로 안드로이드 적응형 아이콘(전경·단색)과 스토어용 512px 아이콘을 만든다
# 실행: python store/make_icon.py store/logo-original.webp . preview.png
import os, sys
from collections import deque
from PIL import Image, ImageDraw

src, project = sys.argv[1], sys.argv[2]
res = os.path.join(project, 'app', 'src', 'main', 'res')
store = os.path.join(project, 'store')
os.makedirs(store, exist_ok=True)

im = Image.open(src).convert('RGBA')
w, h = im.size
px = im.load()

# 1) 네 귀퉁이에서 이어진 흰 바탕만 투명하게 (캐릭터 안의 흰색은 검은 테두리에 막혀 그대로 남는다)
def whiteish(c):
    return c[0] > 235 and c[1] > 235 and c[2] > 235

seen = bytearray(w * h)
q = deque([(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)])
while q:
    x, y = q.popleft()
    i = y * w + x
    if seen[i]:
        continue
    seen[i] = 1
    if not whiteish(px[x, y]):
        continue
    px[x, y] = (0, 0, 0, 0)
    for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
        if 0 <= nx < w and 0 <= ny < h and not seen[ny * w + nx]:
            q.append((nx, ny))


YELLOW = (254, 206, 72, 255)

# 2) 적응형 아이콘 전경: 108dp 캔버스 가운데 72dp에 로고를 둔다 (원형으로 잘려도 로고 원이 딱 맞음)
densities = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

# 단색(테마 아이콘)용: 어두운 테두리 선만 남긴 그림
mono = Image.new('RGBA', (w, h), (0, 0, 0, 0))
mp = mono.load()
for y in range(h):
    for x in range(w):
        r, g, b, a = px[x, y]
        if a and (r * 299 + g * 587 + b * 114) / 1000 < 80:
            mp[x, y] = (255, 255, 255, 255)

for name, scale in densities.items():
    canvas = round(108 * scale)
    inner = round(72 * scale)
    off = (canvas - inner) // 2
    folder = os.path.join(res, 'mipmap-' + name)
    os.makedirs(folder, exist_ok=True)
    for img, fname in ((im, 'ic_launcher_foreground.png'), (mono, 'ic_launcher_monochrome.png')):
        layer = Image.new('RGBA', (canvas, canvas), (0, 0, 0, 0))
        layer.alpha_composite(img.resize((inner, inner), Image.LANCZOS), (off, off))
        layer.save(os.path.join(folder, fname), optimize=True)

# 3) 플레이스토어용 512px 아이콘 (정사각형, 투명 없음)
icon = Image.new('RGBA', (512, 512), YELLOW)
logo = im.resize((470, 470), Image.LANCZOS)
icon.alpha_composite(logo, (21, 21))
icon.convert('RGB').save(os.path.join(store, 'icon-512.png'), optimize=True)

# 미리보기: 원형으로 잘린 런처 아이콘 모습
prev = Image.new('RGBA', (432, 432), YELLOW)
fg = Image.open(os.path.join(res, 'mipmap-xxxhdpi', 'ic_launcher_foreground.png'))
prev.alpha_composite(fg)
mask = Image.new('L', (432, 432), 0)
ImageDraw.Draw(mask).ellipse((72, 72, 360, 360), fill=255)
out = Image.new('RGBA', (432, 432), (255, 255, 255, 255))
out.paste(prev, (0, 0), mask)
out.crop((60, 60, 372, 372)).save(sys.argv[3])
print('ok')

# Restores the (Object) casts javac strips from mixin self-casts, which Vineflower can't recover
import glob, os, re

total = 0
for p in glob.glob(os.path.join('src', 'main', 'java', '**', 'mixin', '**', '*.java'), recursive=True):
    s = open(p, encoding='utf-8').read()
    orig = s
    s = re.sub(r'\((\w+(?:<\?>)?)\)this\b', r'(\1)(Object)this', s)
    s = s.replace('(Object)(Object)this', '(Object)this')
    s = re.sub(r'(?<!\(Object\))\bthis == Minecraft\.getInstance\(\)\.player', '(Object)this == Minecraft.getInstance().player', s)
    if s != orig:
        open(p, 'w', encoding='utf-8', newline='').write(s)
        total += 1
        print(os.path.relpath(p, os.path.join('src', 'main', 'java')))
print('files fixed:', total)

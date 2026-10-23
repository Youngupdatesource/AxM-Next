#!/usr/bin/env python3
"""Generate a heavier static instance from OnePlusSans-Bold.ttf by dilating outlines.

Usage: python3 tools/embolden_font.py input.ttf output.ttf outset_units weight_class label
  outset_units = how many font units (UPM 1000) each outline edge moves outward.
  Bold stem is ~160 units; outset 8 -> ~176, outset 16 -> ~192.
Requires: pip install fonttools
"""
import math
import sys

from fontTools.pens.basePen import BasePen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

SRC = sys.argv[1] if len(sys.argv) > 1 else "manager/src/main/res/font/oneplus_sans_bold.ttf"
DST = sys.argv[2] if len(sys.argv) > 2 else "manager/src/main/res/font/oneplus_sans_heavy.ttf"
D = -abs(float(sys.argv[3]) if len(sys.argv) > 3 else 16.0)
WEIGHT_CLASS = int(sys.argv[4]) if len(sys.argv) > 4 else 800
LABEL = sys.argv[5] if len(sys.argv) > 5 else "Heavy"
STEPS = 16
SHIFT = D * 0.6

class FlattenPen(BasePen):
    def __init__(self, gs):
        super().__init__(gs)
        self.contours, self.cur = [], None

    def _moveTo(self, p):
        self.cur = [p]

    def _lineTo(self, p):
        self.cur.append(p)

    def _qCurveToOne(self, p1, p2):
        p0 = self._getCurrentPoint()
        for i in range(1, STEPS + 1):
            t = i / STEPS
            a, b, c = (1 - t) ** 2, 2 * (1 - t) * t, t * t
            self.cur.append((a * p0[0] + b * p1[0] + c * p2[0], a * p0[1] + b * p1[1] + c * p2[1]))

    def _curveToOne(self, p1, p2, p3):
        p0 = self._getCurrentPoint()
        for i in range(1, STEPS + 1):
            t = i / STEPS
            u = 1 - t
            self.cur.append((
                u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
                u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1]))

    def _closePath(self):
        pts = self.cur
        if len(pts) > 1 and pts[0] == pts[-1]:
            pts = pts[:-1]
        out = [p for i, p in enumerate(pts) if p != pts[i - 1]]
        if len(out) >= 3:
            self.contours.append(out)
        self.cur = None

    _endPath = _closePath

def erode(pts, d):
    """TrueType fills the RIGHT side of every contour; a negative d moves edges away from the ink."""
    n = len(pts)
    res = []
    for i in range(n):
        p0, p1, p2 = pts[i - 1], pts[i], pts[(i + 1) % n]
        e1 = (p1[0] - p0[0], p1[1] - p0[1])
        e2 = (p2[0] - p1[0], p2[1] - p1[1])
        l1, l2 = math.hypot(*e1), math.hypot(*e2)
        n1 = (e1[1] / l1, -e1[0] / l1)
        n2 = (e2[1] / l2, -e2[0] / l2)
        bx, by = n1[0] + n2[0], n1[1] + n2[1]
        bl = math.hypot(bx, by)
        if bl < 1e-6:
            bx, by, k = n1[0], n1[1], d
        else:
            bx, by = bx / bl, by / bl
            cos_half = max(bx * n1[0] + by * n1[1], 0.45)
            k = d / cos_half
        res.append((p1[0] + bx * k, p1[1] + by * k))
    return res

def area(pts):
    return 0.5 * sum(pts[i - 1][0] * pts[i][1] - pts[i][0] * pts[i - 1][1] for i in range(len(pts)))

font = TTFont(SRC)
gs = font.getGlyphSet()
glyf = font["glyf"]
new = {}
for name in font.getGlyphOrder():
    fp = FlattenPen(gs)
    gs[name].draw(fp)
    pen = TTGlyphPen(None)
    for c in fp.contours:
        a0 = area(c)
        if abs(a0) < 1:
            continue
        e = erode(c, D)
        if area(e) * a0 <= 0 or abs(area(e)) < 0.15 * abs(a0):
            e = c
        pen.moveTo((round(e[0][0] - SHIFT), round(e[0][1])))
        for p in e[1:]:
            pen.lineTo((round(p[0] - SHIFT), round(p[1])))
        pen.closePath()
    new[name] = pen.glyph()
for name, g in new.items():
    glyf[name] = g
    adv, lsb = font["hmtx"][name]
    if adv > 0:
        adv = max(round(adv - 2 * SHIFT), 0)
    g.recalcBounds(glyf)
    font["hmtx"][name] = (adv, getattr(g, "xMin", 0) if g.numberOfContours else 0)

for t in ("fpgm", "prep", "cvt ", "hdmx", "VDMX", "DSIG", "LTSH", "gasp"):
    if t in font:
        del font[t]
for g in glyf.glyphs.values():
    if hasattr(g, "program"):
        from fontTools.ttLib.tables import ttProgram
        g.program = ttProgram.Program()
        g.program.fromBytecode(b"")

font["maxp"].maxSizeOfInstructions = 0
font["OS/2"].usWeightClass = WEIGHT_CLASS
for rec in font["name"].names:
    if rec.nameID == 4:
        rec.string = "OnePlus Sans " + LABEL
    elif rec.nameID == 6:
        rec.string = "OnePlusSans-" + LABEL.replace(" ", "")
    elif rec.nameID == 17:
        rec.string = LABEL
font.save(DST)
print("saved", DST)

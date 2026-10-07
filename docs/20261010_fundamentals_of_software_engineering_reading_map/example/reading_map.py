"""章末の Additional Resources を読書マップ（drawio.svg）として描く。

依存ライブラリなし。`python3 reading_map.py` で ../imgs/reading_map.drawio.svg を生成する。
出力は draw.io の mxGraphModel を content 属性に埋め込んだ SVG で、
VS Code の draw.io 拡張で開けば図として編集できる。
"""

from __future__ import annotations

import pathlib
from dataclasses import dataclass, field
from xml.sax.saxutils import escape, quoteattr

OUT = pathlib.Path(__file__).resolve().parent.parent / "imgs" / "reading_map.drawio.svg"


@dataclass
class Book:
    title: str  # 表示タイトル（邦訳があれば邦題）
    chapters: str  # 本書で挙がる章
    translation: str = "yes"  # yes: 邦訳あり / old: 旧版のみ邦訳 / none: 邦訳なし（色分け）


@dataclass
class Row:
    name: str
    cells: list[list[Book]] = field(default_factory=list)  # 列ごとの本


COLUMNS = [
    "本書の直後に読む",
    "現場でその話題にあたったら読む",
    "視野を組織や歴史へ広げるときに読む",
]

ROWS = [
    Row(
        "コードを読む・書く・直す\n（2・3・5・6章）",
        [
            [Book("Clean Code", "5章"), Book("リファクタリング 第2版", "6章")],
            [Book("レガシーコード改善ガイド", "6章"), Book("CODE COMPLETE 第2版", "1章")],
            [Book("人月の神話", "1・3章"), Book("オブジェクト指向における再利用のためのデザインパターン", "1章")],
        ],
    ),
    Row(
        "図と設計\n（4・9章）",
        [
            [Book("UMLモデリングのエッセンス 第3版", "4・9章"), Book("Thinking Architecturally", "9・12・13章", "none")],
            [Book("ソフトウェアアーキテクチャの基礎", "9章"), Book("ユーザーストーリーマッピング", "4章")],
            [Book("進化的アーキテクチャ", "9章", "old"), Book("開発者とアーキテクトのためのコミュニケーションガイド", "4・9章")],
        ],
    ),
    Row(
        "データ\n（8章）",
        [
            [Book("7つのデータベース 7つの世界", "8章", "old")],
            [Book("データ指向アプリケーションデザイン", "8章", "old"), Book("データベース・リファクタリング", "8章")],
            [Book("データエンジニアリングの基礎", "8章"), Book("NoSQL Distilled", "8章", "none")],
        ],
    ),
    Row(
        "本番環境とデリバリ\n（10章）",
        [
            [Book("Head First Git", "10章", "none")],
            [Book("継続的デリバリー", "10章"), Book("Learning GitHub Actions", "10章", "none")],
            [Book("The DevOps 逆転だ！", "10章")],
        ],
    ),
    Row(
        "UI デザイン\n（7章）",
        [
            [Book("ノンデザイナーズ・デザインブック 第4版", "7章")],
            [Book("誰のためのデザイン？ 増補・改訂版", "7章"), Book("デザイニング・インターフェース", "7章", "old")],
            [Book("ABOUT FACE インタラクションデザインの本質", "7章")],
        ],
    ),
    Row(
        "学び方と働き方\n（11・12章）",
        [
            [Book("達人プログラマー 第2版", "1・12・14章"), Book("情熱プログラマー", "11・12・14章")],
            [Book("リファクタリング・ウェットウェア", "12章"), Book("プロダクティブ・プログラマ", "1・11章")],
            [Book("フロー体験 喜びの現象学", "11章"), Book("SECOND BRAIN", "11章")],
        ],
    ),
    Row(
        "キャリアと影響力\n（13・14章）",
        [
            [Book("人を動かす", "9・13章")],
            [Book("影響力の武器［新版］", "9・13章"), Book("Help Your Boss Help You", "14章", "none")],
            [Book("スタッフエンジニアの道", "1章"), Book("エンジニアのためのマネジメントキャリアパス", "14章")],
        ],
    ),
    Row(
        "AI\n（15章）",
        [
            [Book("バイブコーディングを超えて", "15章"), Book("これからのAI、正しい付き合い方と使い方", "15章")],
            [Book("LLMのプロンプトエンジニアリング", "15章"), Book("AIエンジニアリング", "15章")],
            [Book("AI新生", "15章"), Book("エージェントアプローチ人工知能", "15章", "old")],
        ],
    ),
]

# レイアウト定数（px）
ROW_HEADER_W = 190
COL_W = 340
BOX_W = COL_W - 24
BOX_H = 54
BOX_GAP = 8
HEADER_H = 44
CELL_PAD = 10
FONT = 13
SUB_FONT = 11
MARGIN = 16

# 色
C_TRANSLATED = ("#dae8fc", "#6c8ebf")
C_OLD = ("#fff2cc", "#d6b656")
C_ORIGINAL = ("#f5f5f5", "#999999")
COLORS = {"yes": C_TRANSLATED, "old": C_OLD, "none": C_ORIGINAL}
C_HEADER = ("#1f3b57", "#1f3b57")
C_ROW = ("#f9f9f9", "#cccccc")


def row_height(row: Row) -> int:
    n = max(len(c) for c in row.cells)
    return CELL_PAD * 2 + n * BOX_H + (n - 1) * BOX_GAP


def text_width(s: str, size: int) -> float:
    return sum(size * (0.55 if ord(ch) < 0x3000 else 1.0) for ch in s)


def wrap(s: str, size: int, width: float) -> list[str]:
    lines, cur = [], ""
    for ch in s:
        if text_width(cur + ch, size) > width and cur:
            lines.append(cur)
            cur = ch
        else:
            cur += ch
    if cur:
        lines.append(cur)
    return lines


class Canvas:
    def __init__(self) -> None:
        self.svg: list[str] = []
        self.cells: list[str] = []
        self.n = 1

    def _id(self) -> str:
        self.n += 1
        return f"c{self.n}"

    def rect(self, x: int, y: int, w: int, h: int, fill: str, stroke: str, label: str, *,
             font: int = FONT, bold: bool = False, color: str = "#000000", rounded: bool = True,
             sub: str | None = None) -> None:
        r = 6 if rounded else 0
        self.svg.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" ry="{r}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="1"/>'
        )
        lines: list[str] = []
        for part in label.split("\n"):
            lines.extend(wrap(part, font, w - 12))
        total = len(lines) * (font + 3) + ((SUB_FONT + 2) if sub else 0)
        if total > h - 4:
            raise SystemExit(f"text overflows box: {label!r} ({len(lines)} lines in {h}px)")
        ty = y + (h - total) / 2 + font
        weight = ' font-weight="bold"' if bold else ""
        for line in lines:
            self.svg.append(
                f'<text x="{x + w / 2}" y="{ty:.1f}" text-anchor="middle" font-family="sans-serif" '
                f'font-size="{font}" fill="{color}"{weight}>{escape(line)}</text>'
            )
            ty += font + 3
        if sub:
            self.svg.append(
                f'<text x="{x + w / 2}" y="{ty:.1f}" text-anchor="middle" font-family="sans-serif" '
                f'font-size="{SUB_FONT}" fill="#555555">{escape(sub)}</text>'
            )
        html = escape(label).replace("\n", "<br>")
        if sub:
            html += f'<br><font style="font-size: {SUB_FONT}px" color="#555555">{escape(sub)}</font>'
        style = (
            f"rounded={1 if rounded else 0};whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
            f"fontSize={font};fontColor={color};fontStyle={1 if bold else 0};"
        )
        self.cells.append(
            f'<mxCell id="{self._id()}" value={quoteattr(html)} style={quoteattr(style)} vertex="1" parent="1">'
            f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>'
        )


def build() -> str:
    cv = Canvas()
    x0, y0 = MARGIN, MARGIN
    total_w = ROW_HEADER_W + COL_W * len(COLUMNS)

    # 列見出し
    cv.rect(x0, y0, ROW_HEADER_W, HEADER_H, *C_HEADER, "章末の推薦書を\nいつ読むか", font=12, bold=True,
            color="#ffffff", rounded=False)
    for i, name in enumerate(COLUMNS):
        cv.rect(x0 + ROW_HEADER_W + i * COL_W, y0, COL_W, HEADER_H, *C_HEADER, name, bold=True,
                color="#ffffff", rounded=False)

    y = y0 + HEADER_H
    for row in ROWS:
        h = row_height(row)
        cv.rect(x0, y, ROW_HEADER_W, h, *C_ROW, row.name, bold=True, rounded=False)
        for i, books in enumerate(row.cells):
            cx = x0 + ROW_HEADER_W + i * COL_W
            cv.rect(cx, y, COL_W, h, "#ffffff", C_ROW[1], "", rounded=False)
            by = y + CELL_PAD
            for b in books:
                fill, stroke = COLORS[b.translation]
                cv.rect(cx + 12, by, BOX_W, BOX_H, fill, stroke, b.title, sub=f"本書 {b.chapters}")
                by += BOX_H + BOX_GAP
        y += h

    # 凡例
    ly = y + 12
    cv.rect(x0, ly, 150, 28, *C_TRANSLATED, "邦訳あり", font=12)
    cv.rect(x0 + 160, ly, 190, 28, *C_OLD, "邦訳は旧版のみ", font=12)
    cv.rect(x0 + 360, ly, 150, 28, *C_ORIGINAL, "邦訳なし（原書）", font=12)
    total_h = ly + 28 + MARGIN

    width, height = total_w + MARGIN * 2, total_h
    model = (
        '<mxfile host="reading_map.py" type="device">'
        '<diagram id="reading-map" name="reading-map">'
        f'<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" '
        f'fold="1" page="1" pageScale="1" pageWidth="{width}" pageHeight="{height}" math="0" shadow="0">'
        '<root><mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(cv.cells) +
        "</root></mxGraphModel></diagram></mxfile>"
    )
    svg = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'version="1.1" width="{width}px" height="{height}px" viewBox="0 0 {width} {height}" '
        f'content={quoteattr(model)}>\n'
        f'<rect x="0" y="0" width="{width}" height="{height}" fill="#ffffff"/>\n'
        + "\n".join(cv.svg)
        + "\n</svg>\n"
    )
    return svg


if __name__ == "__main__":
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(build(), encoding="utf-8")
    print(f"wrote {OUT}")

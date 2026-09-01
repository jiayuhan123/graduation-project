"""Generate the spacious, relationship-complete overall Chen ER diagram."""

from pathlib import Path
import sys

from reportlab.lib import colors
from reportlab.lib.pagesizes import A1, landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "er图.pdf"
PAGE_WIDTH, PAGE_HEIGHT = landscape(A1)

INK = colors.HexColor("#20344F")
LINE = colors.HexColor("#536A85")
PANEL_BORDER = colors.HexColor("#D7E2ED")
CONTENT_PANEL = colors.HexColor("#F6F9FC")
LOOP_PANEL = colors.HexColor("#F7FBF8")
ENTRY_PANEL = colors.HexColor("#FFFCF7")
ENTITY_FILL = colors.HexColor("#EAF3FF")
ENTITY_STROKE = colors.HexColor("#2D76B9")
RELATION_FILL = colors.HexColor("#FFF4E7")
RELATION_STROKE = colors.HexColor("#D97A00")
ATTRIBUTE_STROKE = colors.HexColor("#8EA2BC")
KEY_STROKE = colors.HexColor("#147A55")


def text(c: canvas.Canvas, x: float, y: float, value: str, size: float = 14) -> None:
    c.setFillColor(INK)
    c.setFont("STSong-Light", size)
    c.drawCentredString(x, y - size * 0.34, value)


def panel(c: canvas.Canvas, x: float, y: float, width: float, height: float,
          fill: colors.Color) -> None:
    c.setFillColor(fill)
    c.setStrokeColor(PANEL_BORDER)
    c.setLineWidth(1.2)
    c.roundRect(x, y, width, height, 20, fill=1, stroke=1)


def link(c: canvas.Canvas, points: list[tuple[float, float]]) -> None:
    c.setStrokeColor(LINE)
    c.setLineWidth(1.5)
    c.setLineJoin(1)
    path = c.beginPath()
    path.moveTo(*points[0])
    for point in points[1:]:
        path.lineTo(*point)
    c.drawPath(path, stroke=1, fill=0)


def bridge(c: canvas.Canvas, x: float, y: float,
           fill: colors.Color = colors.white) -> None:
    """Clear a small gap under the next connector at a line crossing."""
    c.setFillColor(fill)
    c.circle(x, y, 7, fill=1, stroke=0)


def entity(c: canvas.Canvas, x: float, y: float, label: str,
           width: float = 180, height: float = 58) -> None:
    c.setFillColor(ENTITY_FILL)
    c.setStrokeColor(ENTITY_STROKE)
    c.setLineWidth(2.15)
    c.rect(x - width / 2, y - height / 2, width, height, fill=1, stroke=1)
    text(c, x, y, label, 18)


def relation(c: canvas.Canvas, x: float, y: float, label: str,
             width: float = 110, height: float = 50) -> None:
    c.setFillColor(RELATION_FILL)
    c.setStrokeColor(RELATION_STROKE)
    c.setLineWidth(2)
    path = c.beginPath()
    path.moveTo(x, y + height / 2)
    path.lineTo(x + width / 2, y)
    path.lineTo(x, y - height / 2)
    path.lineTo(x - width / 2, y)
    path.close()
    c.drawPath(path, fill=1, stroke=1)
    text(c, x, y, label, 13 if len(label) <= 4 else 11.5)


def attribute(c: canvas.Canvas, x: float, y: float, label: str,
              key: bool = False, rx: float | None = None) -> None:
    radius_x = rx or max(56, len(label) * 10)
    radius_y = 20
    c.setFillColor(colors.white)
    c.setStrokeColor(KEY_STROKE if key else ATTRIBUTE_STROKE)
    c.setLineWidth(1.45 if key else 1.1)
    c.ellipse(x - radius_x, y - radius_y, x + radius_x, y + radius_y, fill=1, stroke=1)
    text(c, x, y, label, 13)
    if key:
        c.setStrokeColor(KEY_STROKE)
        c.setLineWidth(0.9)
        value_width = pdfmetrics.stringWidth(label, "STSong-Light", 13)
        c.line(x - value_width / 2, y - 10.5, x + value_width / 2, y - 10.5)


def cardinality(c: canvas.Canvas, x: float, y: float, value: str) -> None:
    width = 26 if len(value) <= 1 else 46
    c.setFillColor(colors.white)
    c.roundRect(x - width / 2, y - 13, width, 26, 6, fill=1, stroke=0)
    text(c, x, y, value, 12)


def draw(c: canvas.Canvas) -> None:
    # Visual grouping only: the page contains no headings, legends, or notes.
    panel(c, 70, 1280, PAGE_WIDTH - 140, 280, CONTENT_PANEL)
    panel(c, 70, 520, PAGE_WIDTH - 140, 680, LOOP_PANEL)
    panel(c, 70, 80, PAGE_WIDTH - 140, 350, ENTRY_PANEL)

    # Content foundation links.
    link(c, [(340, 1430), (415, 1430)])
    link(c, [(525, 1430), (630, 1430)])
    link(c, [(810, 1430), (905, 1430)])
    link(c, [(1015, 1430), (1110, 1430)])
    link(c, [(1290, 1430), (1425, 1430)])
    link(c, [(1535, 1430), (1670, 1430)])

    # Learning-loop links.
    link(c, [(335, 955), (425, 1020)])
    link(c, [(535, 1020), (630, 1060)])
    link(c, [(720, 1401), (720, 1225)])
    link(c, [(720, 1175), (720, 1089)])
    link(c, [(330, 920), (415, 790)])
    link(c, [(525, 790), (630, 780)])
    link(c, [(810, 780), (905, 780)])
    link(c, [(1015, 780), (1110, 780)])
    link(c, [(1200, 1401), (1200, 1225)])
    link(c, [(1200, 1175), (1200, 809)])
    link(c, [(1290, 780), (1395, 780)])
    link(c, [(1505, 780), (1610, 780)])
    link(c, [(810, 1060), (905, 1050)])
    link(c, [(1015, 1050), (1110, 810)])
    link(c, [(1610, 750), (1265, 600)])
    link(c, [(1135, 600), (790, 810)])

    # Business-entry links.
    link(c, [(220, 911), (220, 600), (365, 600)])
    link(c, [(475, 600), (630, 290)])
    link(c, [(720, 289), (720, 475)])
    link(c, [(720, 525), (720, 751)])
    link(c, [(1440, 289), (1515, 500)])
    link(c, [(1335, 500), (1280, 751)])
    link(c, [(2050, 971), (2050, 625)])
    link(c, [(2050, 575), (2050, 289)])

    # Student submits photo-marking tasks. The bridge keeps the connector
    # distinct from the game-to-practice line.
    bridge(c, 720, 607, LOOP_PANEL)
    link(c, [(300, 911), (340, 680), (915, 570)])
    link(c, [(1025, 570), (1455, 289)])

    # A teacher may review low-confidence photo-marking tasks.
    link(c, [(1960, 971), (1920, 760), (1905, 600)])
    link(c, [(1795, 600), (1545, 289)])

    # Students complete many teacher assignments and one assignment is
    # completed by many students. White bridges make crossings unambiguous.
    bridge(c, 720, 460)
    bridge(c, 1193, 460)
    link(c, [(200, 911), (150, 820), (150, 460), (1530, 460)])
    bridge(c, 1680, 455)
    link(c, [(1670, 460), (2000, 289)])

    # Content foundation.
    entity(c, 250, 1430, "内容模块")
    relation(c, 470, 1430, "包含")
    entity(c, 720, 1430, "知识点")
    relation(c, 960, 1430, "考查")
    entity(c, 1200, 1430, "题目")
    relation(c, 1480, 1430, "维护")
    entity(c, 1760, 1430, "管理员")

    cardinality(c, 375, 1468, "1")
    cardinality(c, 580, 1468, "N")
    cardinality(c, 855, 1468, "M")
    cardinality(c, 1065, 1468, "N")
    cardinality(c, 1350, 1468, "N")
    cardinality(c, 1605, 1468, "1")

    attribute(c, 250, 1530, "模块编号", key=True)
    link(c, [(250, 1510), (250, 1459)])
    attribute(c, 135, 1340, "年级/学期", rx=66)
    link(c, [(165, 1357), (215, 1401)])
    attribute(c, 370, 1340, "学习主题", rx=58)
    link(c, [(350, 1357), (295, 1401)])

    attribute(c, 720, 1530, "知识点编号", key=True, rx=70)
    link(c, [(720, 1510), (720, 1459)])
    attribute(c, 585, 1340, "名称")
    link(c, [(610, 1355), (680, 1401)])
    attribute(c, 855, 1340, "难度等级", rx=60)
    link(c, [(830, 1356), (760, 1401)])

    attribute(c, 1200, 1530, "题目编号", key=True)
    link(c, [(1200, 1510), (1200, 1459)])
    attribute(c, 1070, 1340, "题型")
    link(c, [(1090, 1356), (1160, 1401)])
    attribute(c, 1250, 1340, "难度")
    link(c, [(1240, 1356), (1215, 1401)])
    attribute(c, 1400, 1340, "标准答案", rx=62)
    link(c, [(1375, 1356), (1260, 1401)])

    attribute(c, 1760, 1530, "管理员编号", key=True, rx=72)
    link(c, [(1760, 1510), (1760, 1459)])

    # Learning loop.
    entity(c, 250, 940, "学生")
    relation(c, 480, 1020, "拥有")
    entity(c, 720, 1060, "掌握记录")
    relation(c, 720, 1200, "对应")
    relation(c, 470, 790, "完成")
    entity(c, 720, 780, "练习任务")
    relation(c, 960, 780, "产生")
    entity(c, 1200, 780, "答题记录")
    relation(c, 1200, 1200, "对应题目", 138)
    relation(c, 1450, 780, "诊断")
    entity(c, 1700, 780, "错因记录")
    relation(c, 1200, 600, "推荐生成", 140)
    relation(c, 960, 1050, "更新")
    entity(c, 2050, 1000, "教师")

    cardinality(c, 365, 985, "1")
    cardinality(c, 585, 1045, "N")
    cardinality(c, 755, 1280, "1")
    cardinality(c, 755, 1130, "N")
    cardinality(c, 360, 835, "1")
    cardinality(c, 575, 800, "N")
    cardinality(c, 850, 815, "1")
    cardinality(c, 1070, 815, "N")
    cardinality(c, 1240, 1280, "1")
    cardinality(c, 1240, 1125, "N")
    cardinality(c, 1345, 815, "1")
    cardinality(c, 1565, 815, "0-N")
    cardinality(c, 1055, 1015, "N")
    cardinality(c, 860, 1065, "1")
    cardinality(c, 1450, 650, "M")
    cardinality(c, 1050, 650, "N")

    attribute(c, 250, 1040, "学生编号", key=True)
    link(c, [(250, 1020), (250, 969)])
    attribute(c, 400, 1140, "年级/学期", rx=66)
    link(c, [(380, 1124), (315, 969)])

    attribute(c, 720, 1150, "掌握编号", key=True)
    link(c, [(720, 1130), (720, 1089)])
    attribute(c, 585, 955, "掌握程度", rx=60)
    link(c, [(615, 968), (665, 1031)])
    attribute(c, 845, 955, "最近练习时间", rx=74)
    link(c, [(820, 970), (775, 1031)])

    attribute(c, 720, 870, "练习编号", key=True)
    link(c, [(720, 850), (720, 809)])
    attribute(c, 600, 675, "来源")
    link(c, [(625, 689), (675, 751)])
    attribute(c, 840, 675, "状态")
    link(c, [(815, 689), (765, 751)])

    attribute(c, 1200, 870, "答题编号", key=True)
    link(c, [(1200, 850), (1200, 809)])
    attribute(c, 1075, 675, "学生答案", rx=62)
    link(c, [(1100, 690), (1160, 751)])
    attribute(c, 1325, 675, "正确性")
    link(c, [(1300, 690), (1240, 751)])

    attribute(c, 1700, 660, "错因编号", key=True)
    link(c, [(1700, 680), (1700, 751)])
    attribute(c, 1560, 680, "错误类型", rx=60)
    link(c, [(1590, 695), (1650, 751)])
    attribute(c, 1840, 680, "错误位置", rx=60)
    link(c, [(1810, 695), (1750, 751)])

    attribute(c, 2050, 1100, "教师编号", key=True)
    link(c, [(2050, 1080), (2050, 1029)])

    # Business entries.
    relation(c, 420, 600, "参与")
    entity(c, 720, 260, "闯关对局")
    relation(c, 720, 500, "生成练习", 136)
    entity(c, 1500, 260, "拍照批改任务", 190)
    relation(c, 1425, 500, "生成答题记录", 180)
    entity(c, 2050, 260, "教师作业")
    relation(c, 2050, 600, "创建")
    relation(c, 970, 570, "提交")
    relation(c, 1850, 600, "复核")
    relation(c, 1600, 460, "完成作业", 140)

    cardinality(c, 270, 700, "M")
    cardinality(c, 560, 420, "N")
    cardinality(c, 760, 390, "1")
    cardinality(c, 760, 650, "N")
    cardinality(c, 1490, 390, "1")
    cardinality(c, 1320, 650, "N")
    cardinality(c, 2090, 700, "1")
    cardinality(c, 2090, 410, "N")
    cardinality(c, 420, 665, "1")
    cardinality(c, 1350, 350, "N")
    cardinality(c, 1965, 820, "0-1")
    cardinality(c, 1640, 405, "N")
    cardinality(c, 175, 540, "M")
    cardinality(c, 1900, 350, "N")

    attribute(c, 720, 365, "闯关编号", key=True)
    link(c, [(720, 345), (720, 289)])
    attribute(c, 590, 160, "模式")
    link(c, [(620, 174), (675, 231)])
    attribute(c, 840, 160, "题量")
    link(c, [(815, 174), (765, 231)])
    attribute(c, 620, 95, "时限")
    link(c, [(640, 110), (690, 231)])
    attribute(c, 820, 95, "通过率")
    link(c, [(800, 110), (750, 231)])
    attribute(c, 1000, 95, "参与人数（1-4人）", rx=88)
    link(c, [(970, 110), (780, 231)])

    attribute(c, 1500, 365, "批改编号", key=True)
    link(c, [(1500, 345), (1500, 289)])
    attribute(c, 1380, 160, "图片")
    link(c, [(1410, 174), (1460, 231)])
    attribute(c, 1650, 160, "识别可信度", rx=68)
    link(c, [(1620, 174), (1540, 231)])
    attribute(c, 1500, 95, "处理状态", rx=60)
    link(c, [(1500, 115), (1500, 231)])

    attribute(c, 2050, 365, "作业编号", key=True)
    link(c, [(2050, 345), (2050, 289)])
    attribute(c, 1920, 160, "标题")
    link(c, [(1950, 174), (2010, 231)])
    attribute(c, 2190, 160, "截止时间", rx=60)
    link(c, [(2160, 174), (2090, 231)])
    attribute(c, 2050, 95, "分层规则", rx=60)
    link(c, [(2050, 115), (2050, 231)])


def main() -> None:
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else OUTPUT
    pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
    document = canvas.Canvas(str(output), pagesize=(PAGE_WIDTH, PAGE_HEIGHT))
    document.setTitle("小学数学个性化练习与游戏闯关平台 - 概念 ER 图")
    document.setAuthor("jiayuhan123")
    document.setSubject("整体概念 ER 图（Chen 记法）")
    draw(document)
    document.save()
    print(f"generated: {output}")


if __name__ == "__main__":
    main()

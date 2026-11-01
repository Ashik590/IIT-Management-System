#!/usr/bin/env python3
import argparse
import html
import os
import re
import textwrap
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Flowable,
    KeepTogether,
    LongTable,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


BLUE = colors.HexColor("#3973AC")
DARK_BLUE = colors.HexColor("#16324F")
MID_BLUE = colors.HexColor("#72B6E8")
LIGHT_BLUE = colors.HexColor("#DCEEFF")
PALE_BLUE = colors.HexColor("#F3F8FC")
TEXT = colors.HexColor("#1F2933")
MUTED = colors.HexColor("#5D6B78")
GRID = colors.HexColor("#B9CAD8")


def normalize_ascii(text):
    return (
        text.replace("\u2011", "-")
        .replace("\u2012", "-")
        .replace("\u2013", "-")
        .replace("\u2014", "-")
        .replace("\u2212", "-")
        .replace("\u2018", "'")
        .replace("\u2019", "'")
        .replace("\u201c", '"')
        .replace("\u201d", '"')
    )


def register_fonts():
    regular = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    bold = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
    mono = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
    if not all(os.path.exists(p) for p in (regular, bold, mono)):
        raise FileNotFoundError("DejaVu fonts are required for PDF generation")
    pdfmetrics.registerFont(TTFont("DejaVu", regular))
    pdfmetrics.registerFont(TTFont("DejaVu-Bold", bold))
    pdfmetrics.registerFont(TTFont("DejaVu-Mono", mono))


def inline_markup(raw):
    raw = normalize_ascii(raw.strip())
    raw = raw.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
    parts = re.split(r"(`[^`]*`)", raw)
    rendered = []
    for part in parts:
        if part.startswith("`") and part.endswith("`"):
            rendered.append(f'<font name="DejaVu-Mono">{html.escape(part[1:-1])}</font>')
        else:
            escaped = html.escape(part)
            escaped = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", escaped)
            escaped = re.sub(r"\*(.+?)\*", r"<i>\1</i>", escaped)
            rendered.append(escaped)
    return "".join(rendered).replace("\n", "<br/>")


def make_styles():
    styles = getSampleStyleSheet()
    base = ParagraphStyle(
        "Body",
        parent=styles["BodyText"],
        fontName="DejaVu",
        fontSize=9.2,
        leading=13.8,
        textColor=TEXT,
        spaceAfter=9,
    )
    return {
        "body": base,
        "cover_title": ParagraphStyle(
            "CoverTitle",
            parent=base,
            fontName="DejaVu-Bold",
            fontSize=27,
            leading=34,
            textColor=DARK_BLUE,
            alignment=TA_CENTER,
            spaceAfter=12,
        ),
        "cover_subtitle": ParagraphStyle(
            "CoverSubtitle",
            parent=base,
            fontName="DejaVu",
            fontSize=15,
            leading=21,
            textColor=BLUE,
            alignment=TA_CENTER,
            spaceAfter=10,
        ),
        "h1": ParagraphStyle(
            "HeadingOne",
            parent=base,
            fontName="DejaVu-Bold",
            fontSize=18,
            leading=23,
            textColor=DARK_BLUE,
            spaceBefore=8,
            spaceAfter=14,
            keepWithNext=True,
        ),
        "h2": ParagraphStyle(
            "HeadingTwo",
            parent=base,
            fontName="DejaVu-Bold",
            fontSize=14,
            leading=18,
            textColor=BLUE,
            spaceBefore=12,
            spaceAfter=9,
            keepWithNext=True,
        ),
        "h3": ParagraphStyle(
            "HeadingThree",
            parent=base,
            fontName="DejaVu-Bold",
            fontSize=11,
            leading=15,
            textColor=DARK_BLUE,
            backColor=LIGHT_BLUE,
            borderPadding=(4, 7, 4, 7),
            spaceBefore=9,
            spaceAfter=8,
            keepWithNext=True,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=base,
            leftIndent=15,
            firstLineIndent=-8,
            bulletIndent=3,
            spaceAfter=4,
        ),
        "numbered": ParagraphStyle(
            "Numbered",
            parent=base,
            leftIndent=18,
            firstLineIndent=-12,
            spaceAfter=5,
        ),
        "quote": ParagraphStyle(
            "Quote",
            parent=base,
            leftIndent=12,
            rightIndent=12,
            borderColor=BLUE,
            borderWidth=0,
            borderPadding=7,
            backColor=PALE_BLUE,
            textColor=MUTED,
            spaceBefore=4,
            spaceAfter=10,
        ),
        "table": ParagraphStyle(
            "TableText",
            parent=base,
            fontSize=7.2,
            leading=9.6,
            spaceAfter=0,
        ),
        "table_header": ParagraphStyle(
            "TableHeader",
            parent=base,
            fontName="DejaVu-Bold",
            fontSize=7.4,
            leading=9.7,
            textColor=colors.white,
            alignment=TA_LEFT,
            spaceAfter=0,
        ),
        "caption": ParagraphStyle(
            "Caption",
            parent=base,
            fontName="DejaVu-Bold",
            fontSize=9,
            textColor=MUTED,
            alignment=TA_CENTER,
            spaceBefore=5,
        ),
        "toc": ParagraphStyle(
            "TOC",
            parent=base,
            fontSize=11,
            leading=18,
            leftIndent=8,
            textColor=DARK_BLUE,
            spaceAfter=2,
        ),
    }


def table_widths(headers, available):
    normalized = [re.sub(r"[^a-z]", "", h.lower()) for h in headers]
    n = len(headers)
    if n == 2 and normalized[0] in ("no", "ref"):
        return [38, available - 38]
    if n == 3 and "class" in normalized[1]:
        return [28, 145, available - 173]
    if n == 4 and "associatednouns" in normalized:
        return [25, 130, 168, available - 323]
    if n == 5 and "acceptedgc" in normalized:
        return [27, 214, 104, 48, available - 393]
    if n == 6 and "acceptedsc" in normalized:
        return [25, 132, 76, 42, 45, available - 320]
    if n == 3:
        return [32, 132, available - 164]
    weights = [max(4, min(24, len(h))) for h in headers]
    total = sum(weights)
    return [available * w / total for w in weights]


def markdown_table(rows, styles, available):
    headers = [cell.strip() for cell in rows[0]]
    body = rows[2:] if len(rows) > 1 and all(re.fullmatch(r":?-{3,}:?", c.strip()) for c in rows[1]) else rows[1:]
    data = [[Paragraph(inline_markup(c), styles["table_header"]) for c in headers]]
    for row in body:
        padded = row + [""] * (len(headers) - len(row))
        data.append([Paragraph(inline_markup(c), styles["table"]) for c in padded[: len(headers)]])
    widths = table_widths(headers, available)
    table = LongTable(data, colWidths=widths, repeatRows=1, splitByRow=1, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), BLUE),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "DejaVu-Bold"),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, GRID),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    for idx in range(1, len(data)):
        if idx % 2 == 0:
            commands.append(("BACKGROUND", (0, idx), (-1, idx), PALE_BLUE))
    table.setStyle(TableStyle(commands))
    return table


def attributes_methods_table(attributes, methods, styles, available):
    header = [
        Paragraph("Attributes", styles["table_header"]),
        Paragraph("Methods", styles["table_header"]),
    ]
    rows = [header]
    for index in range(max(len(attributes), len(methods))):
        attribute = attributes[index] if index < len(attributes) else ""
        method = methods[index] if index < len(methods) else ""
        rows.append([
            Paragraph(inline_markup(attribute), styles["table"]),
            Paragraph(inline_markup(method), styles["table"]),
        ])
    table = LongTable(rows, colWidths=[available / 2, available / 2], repeatRows=1, splitByRow=1, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), BLUE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, GRID),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]
    for index in range(1, len(rows)):
        if index % 2 == 0:
            commands.append(("BACKGROUND", (0, index), (-1, index), PALE_BLUE))
    table.setStyle(TableStyle(commands))
    return table


def parse_table(lines, start):
    rows = []
    i = start
    while i < len(lines) and lines[i].lstrip().startswith("|"):
        raw = lines[i].strip().strip("|")
        rows.append([cell.strip() for cell in raw.split("|")])
        i += 1
    return rows, i


def parse_puml(path):
    text = normalize_ascii(path.read_text(encoding="utf-8"))
    title_match = re.search(r"title Ref: Table\s+(\d+)\\nName:\s*([^\n]+)", text)
    if not title_match:
        raise ValueError(f"Cannot parse title in {path}")
    ref = int(title_match.group(1))
    name = title_match.group(2).strip()
    nodes = {}
    for label, alias in re.findall(r'^rectangle\s+"([^"]+)"\s+as\s+(\w+)', text, re.M):
        nodes[alias] = label
    edges = []
    pattern = re.compile(r"^(\w+)\s+-(down|up|left|right)-\s+(\w+)\s*:\s*(.+)$", re.M)
    for source, direction, target, label in pattern.findall(text):
        edges.append((source, direction, target, label.replace("\\n", "\n")))
    return {"ref": ref, "name": name, "nodes": nodes, "edges": edges, "path": path}


class CRCDiagram(Flowable):
    def __init__(self, diagram, width, height=540):
        super().__init__()
        self.diagram = diagram
        self.width = width
        self.height = height

    def wrap(self, availWidth, availHeight):
        return min(self.width, availWidth), min(self.height, availHeight)

    def _direction_for_other(self, center, source, direction, target):
        if target == center:
            return {"down": "above", "up": "below", "right": "left", "left": "right"}[direction]
        if source == center:
            return {"down": "below", "up": "above", "right": "right", "left": "left"}[direction]
        return "below"

    def _spread(self, count, low, high):
        if count == 1:
            return [(low + high) / 2]
        step = (high - low) / (count - 1)
        return [low + i * step for i in range(count)]

    def draw(self):
        c = self.canv
        w, h = self.width, self.height
        name = self.diagram["name"]
        nodes = self.diagram["nodes"]
        center = next((a for a, label in nodes.items() if label == name), None)
        if center is None:
            center = next(iter(nodes))

        groups = {"above": [], "below": [], "left": [], "right": []}
        for source, direction, target, _ in self.diagram["edges"]:
            other = source if target == center else target
            if other == center or other not in nodes:
                continue
            side = self._direction_for_other(center, source, direction, target)
            if other not in groups[side]:
                groups[side].append(other)

        # Keep spokes readable when the source diagram places many peers on
        # one side. The PDF renderer has a fixed A4 portrait canvas, so spill
        # excess nodes into the least-populated side columns rather than
        # drawing overlapping boxes in a single row.
        for side in ("above", "below"):
            while len(groups[side]) > 3:
                alias = groups[side].pop()
                target_side = min(("left", "right"), key=lambda candidate: len(groups[candidate]))
                groups[target_side].append(alias)

        positions = {center: (w / 2, h / 2 + 15)}
        for alias, x in zip(groups["above"], self._spread(len(groups["above"]), 90, w - 90)):
            positions[alias] = (x, h - 65)
        for alias, x in zip(groups["below"], self._spread(len(groups["below"]), 90, w - 90)):
            positions[alias] = (x, 72)
        for alias, y in zip(groups["left"], self._spread(len(groups["left"]), 135, h - 130)):
            positions[alias] = (75, y)
        for alias, y in zip(groups["right"], self._spread(len(groups["right"]), 135, h - 130)):
            positions[alias] = (w - 75, y)

        # Event has nine collaborators and is the densest CRC card. Give its
        # collaborators a deliberate three-sided layout so none of the peer
        # boxes or their relationship labels collide on the portrait page.
        if name == "Event":
            event_layout = {
                "Activity": (w * 0.19, h - 65),
                "CulturalSecretary": (w * 0.50, h - 65),
                "Notification": (w * 0.81, h - 65),
                "Member": (w * 0.19, 72),
                "ExecutiveCommitteeMember": (w * 0.50, 72),
                "MediaPost": (w * 0.81, 72),
                "PublicitySecretary": (75, h * 0.68),
                "FinancialManagement": (w - 75, h * 0.68),
                "Treasurer": (w - 75, h * 0.30),
            }
            for alias, point in event_layout.items():
                if alias in nodes:
                    positions[alias] = point

        for alias in nodes:
            positions.setdefault(alias, (w / 2, 72))

        # Connections first so the boxes cover the endpoints.
        c.setStrokeColor(MUTED)
        c.setLineWidth(1.2)
        for source, _, target, label in self.diagram["edges"]:
            if source not in positions or target not in positions:
                continue
            x1, y1 = positions[source]
            x2, y2 = positions[target]
            c.line(x1, y1, x2, y2)
            other = source if target == center else target
            side = self._direction_for_other(center, source, _, target)
            crowded_spokes = side in ("above", "below") and len(groups[side]) >= 3
            wrap_width = 21 if crowded_spokes else 30
            label_lines = []
            for explicit in label.splitlines():
                label_lines.extend(textwrap.wrap(explicit, width=wrap_width) or [""])
            mx, my = (x1 + x2) / 2, (y1 + y2) / 2
            if name == "Event" and other in positions:
                center_x, center_y = positions[center]
                other_x, other_y = positions[other]
                mx = center_x + (other_x - center_x) * 0.72
                my = center_y + (other_y - center_y) * 0.72
            elif crowded_spokes and other in positions and other in groups[side]:
                center_x, center_y = positions[center]
                other_x, other_y = positions[other]
                mx = center_x + (other_x - center_x) * 0.65
                my = center_y + (other_y - center_y) * 0.57
                lane = groups[side].index(other) - (len(groups[side]) - 1) / 2
                my += lane * 32
            # Horizontal spokes have very little room between the central and
            # collaborator boxes. Lift their labels into the surrounding
            # whitespace so the text never sits on top of a class box.
            if name != "Event" and abs(x2 - x1) > abs(y2 - y1):
                my += 42
            font_size = 6.7 if crowded_spokes else 7.6
            leading = 8.1 if crowded_spokes else 9.2
            label_width = max((pdfmetrics.stringWidth(line, "DejaVu", font_size) for line in label_lines), default=30) + 10
            label_height = leading * len(label_lines) + 5
            c.setFillColor(colors.white)
            c.roundRect(mx - label_width / 2, my - label_height / 2, label_width, label_height, 4, fill=1, stroke=0)
            c.setFillColor(TEXT)
            c.setFont("DejaVu", font_size)
            ty = my + (len(label_lines) - 1) * leading / 2 - 2.5
            for line in label_lines:
                c.drawCentredString(mx, ty, line)
                ty -= leading

        # Nodes.
        for alias, label in nodes.items():
            x, y = positions[alias]
            node_w = min(170, max(105, pdfmetrics.stringWidth(label, "DejaVu-Bold", 9.2) + 22))
            node_h = 38
            c.setFillColor(MID_BLUE if alias == center else LIGHT_BLUE)
            c.setStrokeColor(BLUE)
            c.setLineWidth(1.2)
            c.roundRect(x - node_w / 2, y - node_h / 2, node_w, node_h, 8, fill=1, stroke=1)
            c.setFillColor(DARK_BLUE)
            c.setFont("DejaVu-Bold", 9.2)
            c.drawCentredString(x, y - 3.2, label)

        c.setFillColor(MUTED)
        c.setFont("DejaVu-Bold", 9)
        c.drawCentredString(w / 2, 12, f"Fig crc.{self.diagram['ref'] - 9}: {name}")


def build_story(md_path, puml_dir, styles, available_width):
    md = normalize_ascii(md_path.read_text(encoding="utf-8"))
    lines = md.splitlines()

    diagrams = [parse_puml(p) for p in puml_dir.glob("*.puml")]
    diagrams.sort(key=lambda d: d["ref"])

    story = []
    # Cover.
    story.extend([
        Spacer(1, 45 * mm),
        Paragraph("Class-Based Modelling", styles["cover_title"]),
        Paragraph("Ramna Yoga Society Management System", styles["cover_subtitle"]),
        Spacer(1, 9 * mm),
    ])
    summary = Table(
        [
            [Paragraph("Analysis stage", styles["table_header"]), Paragraph("Confirmed result", styles["table_header"])],
            [Paragraph("Total nouns", styles["body"]), Paragraph("250", styles["body"])],
            [Paragraph("Solution-space nouns", styles["body"]), Paragraph("154", styles["body"])],
            [Paragraph("GC-passed nouns", styles["body"]), Paragraph("50", styles["body"])],
            [Paragraph("SC-passed final classes", styles["body"]), Paragraph("18", styles["body"])],
            [Paragraph("Approved CRC diagrams", styles["body"]), Paragraph(str(len(diagrams)), styles["body"])],
        ],
        colWidths=[82 * mm, 48 * mm],
        hAlign="CENTER",
    )
    summary.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), BLUE),
        ("GRID", (0, 0), (-1, -1), 0.5, GRID),
        ("BACKGROUND", (0, 1), (-1, -1), PALE_BLUE),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    story.extend([summary, Spacer(1, 14 * mm), Paragraph("Prepared from the approved editable class-based modelling source and CRC diagram files.", styles["quote"]), PageBreak()])

    # Contents.
    story.append(Paragraph("Document Structure", styles["h1"]))
    toc_items = [
        "1. List of Total Nouns",
        "2. List of Nouns in Solution Space",
        "3. General Classifications (GC)",
        "4. Selection Criteria (SC)",
        "5. Associated Noun and Verb Identification",
        "6. Attribute Selection",
        "7. Method Identification",
        "8. Finalizing Classes",
        "9. Class Cards",
        "10. CRC Diagrams",
    ]
    for item in toc_items:
        story.append(Paragraph(item, styles["toc"]))
    story.append(PageBreak())

    start = next(i for i, line in enumerate(lines) if line.strip() == "### Modelling Rule")
    end = next(i for i, line in enumerate(lines) if line.startswith("## 10. CRC"))
    content = lines[start:end]
    i = 0
    seen_major = False
    class_card = False
    class_attributes = []
    class_methods = []
    in_responsibility_collaborator = False
    while i < len(content):
        line = content[i].rstrip()
        stripped = line.strip()
        if not stripped:
            i += 1
            continue
        if stripped == "---":
            i += 1
            continue
        if stripped.startswith("|"):
            rows, i = parse_table(content, i)
            if class_card and in_responsibility_collaborator and rows:
                rows[0] = ["Responsibilities", "Collaborative Class"]
            story.append(markdown_table(rows, styles, available_width))
            continue
        if stripped.startswith("## "):
            if seen_major:
                story.append(PageBreak())
            seen_major = True
            story.append(Paragraph(inline_markup(stripped[3:]), styles["h1"]))
            i += 1
            continue
        if stripped.startswith("### "):
            title = stripped[4:]
            class_card = bool(re.match(r"^9\.\d+\s+", title))
            class_attributes = []
            class_methods = []
            in_responsibility_collaborator = False
            if title.startswith("9."):
                story.append(PageBreak())
            story.append(Paragraph(inline_markup(title), styles["h2"]))
            i += 1
            continue
        if stripped.startswith("#### "):
            title = stripped[5:]
            if class_card and title in ("Attributes", "Methods"):
                target = class_attributes if title == "Attributes" else class_methods
                i += 1
                while i < len(content) and not content[i].strip():
                    i += 1
                while i < len(content) and content[i].strip().startswith("- "):
                    target.append(content[i].strip()[2:])
                    i += 1
                continue
            if class_card and title in ("Responsibility-Collaborator Mapping", "Responsibility-Collaborator"):
                story.append(attributes_methods_table(class_attributes, class_methods, styles, available_width))
                story.append(Spacer(1, 7))
                title = "Responsibility-Collaborator"
                in_responsibility_collaborator = True
            else:
                in_responsibility_collaborator = False
            story.append(Paragraph(inline_markup(title), styles["h3"]))
            i += 1
            continue
        if stripped.startswith(">"):
            story.append(Paragraph(inline_markup(stripped.lstrip("> ")), styles["quote"]))
            i += 1
            continue
        if stripped.startswith("- "):
            story.append(Paragraph("- " + inline_markup(stripped[2:]), styles["bullet"]))
            i += 1
            continue
        numbered = re.match(r"^(\d+)\.\s+(.*)$", stripped)
        if numbered:
            story.append(Paragraph(f"{numbered.group(1)}. {inline_markup(numbered.group(2))}", styles["numbered"]))
            i += 1
            continue
        if stripped.startswith("```"):
            i += 1
            code_lines = []
            while i < len(content) and not content[i].strip().startswith("```"):
                code_lines.append(content[i])
                i += 1
            i += 1
            code_text = "<br/>".join(html.escape(normalize_ascii(x)) for x in code_lines)
            code_style = ParagraphStyle("Code", parent=styles["body"], fontName="DejaVu-Mono", fontSize=7.5, leading=10, backColor=PALE_BLUE, borderPadding=6)
            story.append(Paragraph(code_text, code_style))
            continue

        paragraph_parts = [stripped]
        i += 1
        while i < len(content):
            nxt = content[i].strip()
            if not nxt or nxt.startswith(("#", "|", "- ", ">", "```")) or re.match(r"^\d+\.\s+", nxt):
                break
            paragraph_parts.append(nxt)
            i += 1
        story.append(Paragraph(inline_markup(" ".join(paragraph_parts)), styles["body"]))

    # Replace the Markdown CRC overview with the fixed PlantUML sources.
    story.append(PageBreak())
    story.append(Paragraph("10. CRC Diagrams", styles["h1"]))
    story.append(Paragraph("Each diagram is reproduced from the approved PlantUML source in the CRC diagrams folder. Its classes, connections, layout intent, and collaborators are preserved; only relationship wording is aligned with the finalized workflows.", styles["body"]))
    for idx, diagram in enumerate(diagrams):
        if idx:
            story.append(PageBreak())
        story.append(Paragraph(f"Ref: Table {diagram['ref']}", styles["h2"]))
        story.append(Paragraph(f"Name: {inline_markup(diagram['name'])}", styles["h3"]))
        story.append(Spacer(1, 5))
        story.append(CRCDiagram(diagram, available_width, height=545))

    return story, diagrams


def page_decorator(canvas, doc):
    canvas.saveState()
    page = canvas.getPageNumber()
    width, height = A4
    if page > 1:
        canvas.setStrokeColor(GRID)
        canvas.setLineWidth(0.5)
        canvas.line(18 * mm, height - 16 * mm, width - 18 * mm, height - 16 * mm)
        canvas.setFillColor(MUTED)
        canvas.setFont("DejaVu", 7.5)
        canvas.drawString(18 * mm, height - 12.5 * mm, "Class-Based Modelling - Ramna Yoga Society Management System")
    canvas.setStrokeColor(GRID)
    canvas.line(18 * mm, 14 * mm, width - 18 * mm, 14 * mm)
    canvas.setFillColor(MUTED)
    canvas.setFont("DejaVu", 7.5)
    canvas.drawString(18 * mm, 9.5 * mm, "Class-based analysis documentation")
    canvas.drawRightString(width - 18 * mm, 9.5 * mm, str(page))
    canvas.restoreState()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--markdown", required=True, type=Path)
    parser.add_argument("--puml-dir", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    register_fonts()
    styles = make_styles()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    usable_width = A4[0] - 36 * mm
    story, diagrams = build_story(args.markdown, args.puml_dir, styles, usable_width)
    doc = SimpleDocTemplate(
        str(args.output),
        pagesize=A4,
        rightMargin=18 * mm,
        leftMargin=18 * mm,
        topMargin=21 * mm,
        bottomMargin=18 * mm,
        title="Class-Based Modelling - Ramna Yoga Society Management System",
        author="YCMS Project Team",
        subject="Class-based analysis and CRC diagrams",
    )
    doc.build(story, onFirstPage=page_decorator, onLaterPages=page_decorator)
    print(f"Created {args.output}")
    print(f"CRC diagrams included: {len(diagrams)}")


if __name__ == "__main__":
    main()

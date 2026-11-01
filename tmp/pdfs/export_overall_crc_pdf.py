#!/usr/bin/env python3
"""Export the overall CRC graph and a legible collaboration index to A4 PDF."""

from html import unescape
from pathlib import Path
import re
import textwrap
from xml.etree import ElementTree as ET

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph, Table, TableStyle


INPUT = Path("/home/ashik/Documents/ChatGPT/Software Requirement Specification/YCMS/Our Work/CRC diagrams/Overall CRC Diagram.drawio")
OUTPUT = INPUT.with_name("Overall CRC Diagram.pdf")
PAGE_W, PAGE_H = A4
MODEL_W, MODEL_H = 794.0, 1123.0
SCALE = min(PAGE_W / MODEL_W, PAGE_H / MODEL_H)
BOX_W, BOX_H = 122.0, 50.0
BLUE = colors.HexColor("#3973AC")
DARK_BLUE = colors.HexColor("#16324F")
MUTED = colors.HexColor("#526D82")
GRID = colors.HexColor("#B9CAD8")
PALE_BLUE = colors.HexColor("#F3F8FC")


def decode_label(value):
    value = unescape(value).replace("\\", "")
    value = re.sub(r"<br\s*/?>", " ", value, flags=re.I)
    return " ".join(value.split())


def parse_drawio():
    xml = ET.parse(INPUT).getroot()
    model = xml.find("./diagram/mxGraphModel")
    nodes, edges = {}, []
    for item in model.find("./root").findall("mxCell"):
        if item.get("vertex") == "1" and item.get("id") != "overall-title":
            geometry = item.find("mxGeometry")
            fill = re.search(r"fillColor=(#[0-9A-Fa-f]{6})", item.get("style", ""))
            nodes[item.get("id")] = {
                "label": item.get("value", ""),
                "fill": fill.group(1) if fill else "#DCEEFF",
                "x": float(geometry.get("x")),
                "y": float(geometry.get("y")),
            }
        elif item.get("edge") == "1":
            edges.append((item.get("source"), item.get("target"), decode_label(item.get("value", ""))))
    if len(nodes) != 18 or len(edges) != 47:
        raise ValueError(f"Expected 18 classes and 47 collaborations; got {len(nodes)} and {len(edges)}")
    return nodes, edges


def class_lines(label):
    parts = re.findall(r"[A-Z]?[a-z]+|[A-Z]+(?=[A-Z][a-z]|\b)|[A-Z]", label)
    result, current = [], ""
    for part in parts:
        if current and len(current + part) > 14:
            result.append(current)
            current = part
        else:
            current += part
    if current:
        result.append(current)
    return result


def model_point(x, y):
    return x * SCALE, PAGE_H - y * SCALE


def draw_overview(pdf, nodes, edges):
    pdf.setFillColor(DARK_BLUE)
    pdf.setFont("DejaVu-Bold", 15)
    pdf.drawCentredString(PAGE_W / 2, PAGE_H - 17 * mm, "Overall CRC Collaboration Diagram")
    pdf.setFont("DejaVu", 8)
    pdf.setFillColor(MUTED)
    pdf.drawCentredString(PAGE_W / 2, PAGE_H - 23 * mm, "Ramna Yoga Society Management System")

    rows = {}
    for alias, node in nodes.items():
        rows.setdefault(round(node["y"] / 190), []).append(alias)
    y_by_row = {row: 90 + index * 205 for index, row in enumerate(sorted(rows))}
    for row, aliases in rows.items():
        ordered = sorted(aliases, key=lambda alias: nodes[alias]["x"])
        count = len(ordered)
        gap = (MODEL_W - 40 - count * BOX_W) / (count - 1) if count > 1 else 0
        start = (MODEL_W - (count * BOX_W + (count - 1) * gap)) / 2
        for index, alias in enumerate(ordered):
            nodes[alias]["x"] = start + index * (BOX_W + gap)
            nodes[alias]["y"] = y_by_row[row]

    centers = {}
    for alias, node in nodes.items():
        x, top = model_point(node["x"], node["y"])
        w, h = BOX_W * SCALE, BOX_H * SCALE
        centers[alias] = (x + w / 2, top - h / 2)

    # Connectors are intentionally unlabeled here; the following pages map each
    # collaboration line to its full responsibility wording.
    pdf.setStrokeColor(colors.HexColor("#A3B5C2"))
    pdf.setLineWidth(0.55)
    for source, target, _ in edges:
        x1, y1 = centers[source]
        x2, y2 = centers[target]
        pdf.line(x1, y1, x2, y2)

    for alias, node in nodes.items():
        x, top = model_point(node["x"], node["y"])
        w, h = BOX_W * SCALE, BOX_H * SCALE
        bottom = top - h
        pdf.setFillColor(colors.HexColor(node["fill"]))
        pdf.setStrokeColor(BLUE)
        pdf.setLineWidth(0.9)
        pdf.roundRect(x, bottom, w, h, 5, fill=1, stroke=1)
        lines = class_lines(node["label"])
        size, leading = 7.5, 8.5
        pdf.setFillColor(DARK_BLUE)
        pdf.setFont("DejaVu-Bold", size)
        baseline = bottom + h / 2 + (len(lines) - 1) * leading / 2 - 2.4
        for line in lines:
            pdf.drawCentredString(x + w / 2, baseline, line)
            baseline -= leading

    pdf.setFillColor(MUTED)
    pdf.setFont("DejaVu", 8)
    pdf.drawCentredString(PAGE_W / 2, 20 * mm, "18 classes  |  47 collaborations")
    pdf.setFont("DejaVu", 7)
    pdf.drawCentredString(PAGE_W / 2, 15 * mm, "Full responsibility labels are listed in the collaboration index on the following pages.")
    pdf.showPage()


def draw_relationship_tables(pdf, edges):
    style = ParagraphStyle("cell", fontName="DejaVu", fontSize=8.4, leading=11, alignment=TA_LEFT, textColor=colors.HexColor("#1F2933"))
    header_style = ParagraphStyle("header", parent=style, fontName="DejaVu-Bold", fontSize=8.5, leading=10, textColor=colors.white)
    indexed = sorted(edges, key=lambda edge: (edge[0].casefold(), edge[1].casefold(), edge[2].casefold()))
    chunk_size = 16
    for page_index, start in enumerate(range(0, len(indexed), chunk_size), 1):
        subset = indexed[start : start + chunk_size]
        pdf.setFillColor(DARK_BLUE)
        pdf.setFont("DejaVu-Bold", 16)
        pdf.drawString(18 * mm, PAGE_H - 20 * mm, "Collaboration Index")
        pdf.setFillColor(MUTED)
        pdf.setFont("DejaVu", 8)
        pdf.drawRightString(PAGE_W - 18 * mm, PAGE_H - 20 * mm, f"Part {page_index}")
        rows = [[
            Paragraph("Class", header_style),
            Paragraph("Collaborator", header_style),
            Paragraph("Responsibility / collaboration", header_style),
        ]]
        for source, target, label in subset:
            rows.append([
                Paragraph(source, style),
                Paragraph(target, style),
                Paragraph(label.replace("&", "&amp;"), style),
            ])
        table = Table(rows, colWidths=[45 * mm, 52 * mm, PAGE_W - 36 * mm - 97 * mm], repeatRows=1, hAlign="LEFT")
        commands = [
            ("BACKGROUND", (0, 0), (-1, 0), BLUE),
            ("GRID", (0, 0), (-1, -1), 0.45, GRID),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 6),
            ("RIGHTPADDING", (0, 0), (-1, -1), 6),
            ("TOPPADDING", (0, 0), (-1, -1), 6),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ]
        for row_number in range(1, len(rows)):
            if row_number % 2 == 0:
                commands.append(("BACKGROUND", (0, row_number), (-1, row_number), PALE_BLUE))
        table.setStyle(TableStyle(commands))
        _, table_h = table.wrapOn(pdf, PAGE_W - 36 * mm, PAGE_H)
        table.drawOn(pdf, 18 * mm, PAGE_H - 29 * mm - table_h)
        pdf.setFillColor(MUTED)
        pdf.setFont("DejaVu", 7)
        pdf.drawRightString(PAGE_W - 18 * mm, 12 * mm, f"{start + 1}-{start + len(subset)} of {len(indexed)}")
        pdf.showPage()


def main():
    pdfmetrics.registerFont(TTFont("DejaVu", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"))
    pdfmetrics.registerFont(TTFont("DejaVu-Bold", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"))
    nodes, edges = parse_drawio()
    pdf = canvas.Canvas(str(OUTPUT), pagesize=A4, pageCompression=1)
    pdf.setTitle("Overall CRC Collaboration Diagram")
    pdf.setAuthor("YCMS Project Team")
    pdf.setSubject("Overall CRC diagram and collaboration index")
    draw_overview(pdf, nodes, edges)
    draw_relationship_tables(pdf, edges)
    pdf.save()
    print(f"Created {OUTPUT}")
    print(f"Portrait A4 pages: {1 + (len(edges) + 15) // 16}; classes: {len(nodes)}; collaborations: {len(edges)}")


if __name__ == "__main__":
    main()

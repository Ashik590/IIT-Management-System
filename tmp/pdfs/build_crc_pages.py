from __future__ import annotations

import math
import os
import textwrap
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path("/home/ashik/Desktop/Sem-4/IIT-Management-System")
SOURCE = ROOT / "output/pdf/Class based modelling.pdf"
TMP = ROOT / "tmp/pdfs/crc-pages.pdf"
FINAL_TMP = ROOT / "tmp/pdfs/Class based modelling-updated.pdf"

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
pdfmetrics.registerFont(TTFont("DejaVu", FONT))
pdfmetrics.registerFont(TTFont("DejaVu-Bold", FONT_BOLD))

NAVY = HexColor("#18324A")
MUTED = HexColor("#5E6B75")
RULE = HexColor("#CBD4DC")
ORANGE = HexColor("#F4B867")
ORANGE_BORDER = HexColor("#C97813")
INK = HexColor("#15212A")
LABEL_BG = HexColor("#FFFDF8")


CRC = [
    ("UserAccount", [
        ("Member", "Authenticates and provides member access"),
        ("YogaInstructor", "Authenticates and provides instructor access"),
        ("ExecutiveCommitteeMember", "Authenticates and controls committee access"),
    ]),
    ("Member", [
        ("UserAccount", "Authenticates and accesses the member account"),
        ("ExecutiveCommitteeMember", "Submits membership data and receives verification"),
        ("Activity", "Views activities and contributes participation records"),
        ("Event", "Registers for events"),
        ("FinancialManagement", "Makes payments or donations and views history"),
        ("YogaContent", "Views approved yoga content"),
    ]),
    ("YogaInstructor", [
        ("UserAccount", "Authenticates and accesses instructor functions"),
        ("YogaSession", "Receives assignments and views assigned sessions"),
        ("YogaContent", "Creates, updates and submits yoga content"),
    ]),
    ("ExecutiveCommitteeMember", [
        ("UserAccount", "Authenticates and receives committee access"),
        ("Member", "Verifies member information or requests correction"),
        ("Activity", "Creates activities and records attendance"),
        ("FinancialManagement", "Records and reviews financial information"),
        ("MediaPost", "Publishes notices and activity media"),
        ("InventoryManagement", "Manages equipment and operational tasks"),
    ]),
    ("President", [
        ("ExecutiveCommitteeMember", "Provides common committee access and operations"),
        ("FinancialManagement", "Reviews financial reports"),
        ("Activity", "Reviews attendance and monitors activities"),
        ("Secretary", "Coordinates supervision and committee work"),
    ]),
    ("Secretary", [
        ("ExecutiveCommitteeMember", "Provides common committee access and operations"),
        ("President", "Coordinates supervision, records and reporting"),
        ("Activity", "Manages schedules and attendance summaries"),
    ]),
    ("Treasurer", [
        ("ExecutiveCommitteeMember", "Provides common committee access and operations"),
        ("Member", "Receives applications and maintains membership records"),
        ("FinancialManagement", "Records transactions and generates reports"),
        ("PaymentGateway", "Verifies external payment results"),
        ("President", "Submits financial reports for review"),
        ("Secretary", "Shares financial records and reports"),
    ]),
    ("PublicitySecretary", [
        ("ExecutiveCommitteeMember", "Provides common committee access and operations"),
        ("MediaPost", "Creates, publishes and archives posts"),
        ("Activity", "Publishes session and activity information"),
        ("Event", "Manages event publicity and media"),
    ]),
    ("CulturalSecretary", [
        ("ExecutiveCommitteeMember", "Provides common committee access and operations"),
        ("Event", "Creates and manages cultural events"),
        ("Member", "Manages event participants"),
        ("FinancialManagement", "Coordinates event-related financial matters"),
        ("MediaPost", "Coordinates event publicity"),
    ]),
    ("FieldEquipmentSecretary", [
        ("ExecutiveCommitteeMember", "Provides common committee access and operations"),
        ("InventoryManagement", "Maintains equipment and operational tasks"),
        ("Activity", "Assigns equipment and support to activities"),
    ]),
    ("Activity", [
        ("Member", "Records participation and attendance"),
        ("ExecutiveCommitteeMember", "Receives creation, scheduling and attendance updates"),
        ("MediaPost", "Supplies publishable activity information"),
        ("FinancialManagement", "Supplies event-related payment information"),
        ("InventoryManagement", "Requests equipment and task support"),
    ]),
    ("YogaSession", [
        ("Activity", "Provides common schedule, participant and attendance behavior"),
        ("YogaInstructor", "Assigns the instructor and exposes assigned sessions"),
        ("Member", "Allows members to view schedules and attend sessions"),
    ]),
    ("Event", [
        ("Activity", "Provides common schedule, participant and attendance behavior"),
        ("Member", "Registers participants"),
        ("CulturalSecretary", "Creates and manages the event"),
        ("MediaPost", "Publishes event publicity and media"),
        ("FinancialManagement", "Maintains event-related financial information"),
    ]),
    ("FinancialManagement", [
        ("Member", "Records payments or donations and provides history"),
        ("Treasurer", "Receives transaction entries and generates reports"),
        ("President", "Provides financial reports for review"),
        ("Secretary", "Provides financial records and summaries"),
        ("PaymentGateway", "Processes and verifies external payments"),
        ("Event", "Maintains event-related financial information"),
    ]),
    ("PaymentGateway", [
        ("FinancialManagement", "Returns payment processing and verification results"),
        ("Treasurer", "Provides payment results for verification"),
        ("Member", "Accepts payment requests and returns confirmation"),
    ]),
    ("MediaPost", [
        ("PublicitySecretary", "Receives post creation, publication and archiving actions"),
        ("Activity", "Presents activity-linked information"),
        ("Event", "Presents event publicity and media"),
        ("Member", "Provides audience-visible posts"),
    ]),
    ("YogaContent", [
        ("YogaInstructor", "Receives content creation, updates and approval"),
        ("VajraAssistant", "Supplies approved content for search and answers"),
        ("Member", "Provides approved content for viewing and search"),
    ]),
    ("VajraAssistant", [
        ("YogaContent", "Searches approved yoga content"),
        ("Member", "Answers questions and suggests relevant material"),
        ("YogaInstructor", "Uses instructor-approved safety guidance"),
    ]),
    ("InventoryManagement", [
        ("FieldEquipmentSecretary", "Receives inventory and task maintenance actions"),
        ("Activity", "Provides equipment and task support"),
        ("FinancialManagement", "Receives expense and helping-hand payment information"),
    ]),
]


W, H = A4


def header_footer(c: canvas.Canvas, page_no: int) -> None:
    c.setFont("DejaVu", 7.6)
    c.setFillColor(MUTED)
    c.drawString(42, H - 28, "Class Based Modelling - Ramna Yoga Society Management System")
    c.setStrokeColor(RULE)
    c.setLineWidth(0.6)
    c.line(42, H - 35, W - 42, H - 35)
    c.line(42, 35, W - 42, 35)
    c.drawString(42, 22, "Class-based analysis documentation")
    c.drawRightString(W - 42, 22, str(page_no))


def class_box(c: canvas.Canvas, x: float, y: float, w: float, h: float, name: str, central: bool = False) -> None:
    c.setFillColor(ORANGE if central else HexColor("#FFD79F"))
    c.setStrokeColor(ORANGE_BORDER)
    c.setLineWidth(1.4 if central else 1.0)
    c.roundRect(x - w / 2, y - h / 2, w, h, 5, fill=1, stroke=1)
    size = 9.3 if central else 8.2
    if pdfmetrics.stringWidth(name, "DejaVu-Bold", size) > w - 12:
        size = 7.2
    c.setFont("DejaVu-Bold", size)
    c.setFillColor(INK)
    c.drawCentredString(x, y - size * 0.34, name)


def border_point(cx: float, cy: float, bw: float, bh: float, tx: float, ty: float) -> tuple[float, float]:
    dx, dy = tx - cx, ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    sx = (bw / 2) / abs(dx) if dx else 1e9
    sy = (bh / 2) / abs(dy) if dy else 1e9
    scale = min(sx, sy)
    return cx + dx * scale, cy + dy * scale


def wrapped_lines(text: str, max_chars: int = 31) -> list[str]:
    lines = textwrap.wrap(text, width=max_chars, break_long_words=False, break_on_hyphens=False)
    if len(lines) <= 3:
        return lines
    return lines[:2] + [" ".join(lines[2:])]


def relation_label(c: canvas.Canvas, x: float, y: float, text: str, angle: float) -> None:
    lines = wrapped_lines(text)
    font_size = 6.5
    leading = 8.0
    max_w = max(pdfmetrics.stringWidth(line, "DejaVu", font_size) for line in lines)
    box_w = min(max_w + 9, 142)
    box_h = len(lines) * leading + 5
    # Move horizontal labels away from the line; diagonal labels need less displacement.
    if abs(math.sin(angle)) < 0.25:
        # Side collaborators leave only a short horizontal connector. Place its
        # responsibility above the line so it never obscures the central class.
        y += 38
    elif math.sin(angle) > 0:
        y -= 2
    else:
        y += 2
    c.setFillColor(LABEL_BG)
    c.setStrokeColor(HexColor("#E7D5BC"))
    c.setLineWidth(0.35)
    c.roundRect(x - box_w / 2, y - box_h / 2, box_w, box_h, 3, fill=1, stroke=1)
    c.setFont("DejaVu", font_size)
    c.setFillColor(INK)
    start_y = y + ((len(lines) - 1) * leading) / 2 - 2.2
    for i, line in enumerate(lines):
        c.drawCentredString(x, start_y - i * leading, line)


def outer_positions(count: int, center_y: float) -> list[tuple[float, float]]:
    if count == 3:
        return [(W / 2, 635), (115, 270), (W - 115, 270)]
    if count == 4:
        return [(W / 2, 640), (105, center_y), (W - 105, center_y), (W / 2, 230)]
    if count == 5:
        return [(135, 635), (W - 135, 635), (98, 360), (W - 98, 360), (W / 2, 205)]
    if count == 6:
        return [(135, 645), (W - 135, 645), (95, 420), (W - 95, 420), (140, 205), (W - 140, 205)]
    raise ValueError(f"Unsupported collaborator count: {count}")


def draw_crc_page(c: canvas.Canvas, idx: int, class_name: str, collaborators: list[tuple[str, str]]) -> None:
    page_no = 39 + idx
    header_footer(c, page_no)
    if idx == 0:
        c.setFont("DejaVu-Bold", 18)
        c.setFillColor(NAVY)
        c.drawString(42, H - 70, "9. CRC Diagram")
        c.setFont("DejaVu", 8.2)
        c.setFillColor(MUTED)
        c.drawString(42, H - 87, "Each figure is centered on one class and shows every collaborator identified in its class card.")
        ref_y = H - 117
    else:
        ref_y = H - 70

    c.setFont("DejaVu-Bold", 8.5)
    c.setFillColor(INK)
    c.drawString(42, ref_y, f"Ref: Table {10 + idx}")
    c.drawString(42, ref_y - 18, f"Name: {class_name}")

    center_y = 425 if idx == 0 else 445
    center_x = W / 2
    cw, ch = 176, 44
    ow, oh = 156, 38
    positions = outer_positions(len(collaborators), center_y)

    # Connectors are drawn first so class boxes and responsibility labels stay legible.
    for (other, responsibility), (ox, oy) in zip(collaborators, positions):
        start = border_point(center_x, center_y, cw, ch, ox, oy)
        end = border_point(ox, oy, ow, oh, center_x, center_y)
        c.setStrokeColor(HexColor("#56646E"))
        c.setLineWidth(0.85)
        c.line(start[0], start[1], end[0], end[1])

    class_box(c, center_x, center_y, cw, ch, class_name, central=True)
    for (other, _), (ox, oy) in zip(collaborators, positions):
        class_box(c, ox, oy, ow, oh, other)

    for (_, responsibility), (ox, oy) in zip(collaborators, positions):
        start = border_point(center_x, center_y, cw, ch, ox, oy)
        end = border_point(ox, oy, ow, oh, center_x, center_y)
        # Favor the outer end on diagonal connectors so labels do not crowd the
        # central class. Horizontal labels are lifted separately above the line.
        horizontal = abs(end[1] - start[1]) < 8
        t = 0.50 if horizontal else 0.62
        lx = start[0] + (end[0] - start[0]) * t
        ly = start[1] + (end[1] - start[1]) * t
        relation_label(c, lx, ly, responsibility, math.atan2(end[1] - start[1], end[0] - start[0]))

    c.setFont("DejaVu-Bold", 8)
    c.setFillColor(MUTED)
    c.drawCentredString(W / 2, 72, f"Fig crc.{idx + 1}: {class_name}")
    c.showPage()


def main() -> None:
    TMP.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(TMP), pagesize=A4)
    c.setTitle("CRC Diagrams - Ramna Yoga Society Management System")
    for idx, (class_name, collaborators) in enumerate(CRC):
        draw_crc_page(c, idx, class_name, collaborators)
    c.save()

    original = PdfReader(str(SOURCE))
    crc_pdf = PdfReader(str(TMP))
    if len(original.pages) < 39:
        raise RuntimeError(f"Expected at least 39 source pages, found {len(original.pages)}")
    if len(crc_pdf.pages) != 19:
        raise RuntimeError(f"Expected 19 CRC pages, found {len(crc_pdf.pages)}")

    writer = PdfWriter()
    for page in original.pages[:38]:
        writer.add_page(page)
    for page in crc_pdf.pages:
        writer.add_page(page)
    metadata = {k: str(v) for k, v in (original.metadata or {}).items() if v is not None}
    metadata["/Title"] = "Class Based Modelling - Ramna Yoga Society Management System"
    writer.add_metadata(metadata)
    with FINAL_TMP.open("wb") as f:
        writer.write(f)
    os.replace(FINAL_TMP, SOURCE)
    print(f"Updated {SOURCE} with {len(writer.pages)} pages")


if __name__ == "__main__":
    main()

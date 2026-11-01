#!/usr/bin/env python3
"""Generate an editable diagrams.net XML overall CRC chart in portrait A4."""

from pathlib import Path
from xml.etree.ElementTree import Element, SubElement, tostring
from xml.dom import minidom
import re


SOURCE = Path(
    "/home/ashik/Documents/ChatGPT/Software Requirement Specification/YCMS/Our Work/CRC diagrams/Overall CRC Diagram.puml"
)
OUTPUT = Path(
    "/home/ashik/Documents/ChatGPT/Software Requirement Specification/YCMS/Our Work/CRC diagrams/Overall CRC Diagram.drawio"
)

# Portrait A4 at 96 CSS pixels per inch.
PAGE_W, PAGE_H = 794, 1123
NODE_W, NODE_H = 122, 42

TIERS = [
    ["UserAccount", "Member", "YogaInstructor", "YogaContent", "VajraAssistant"],
    ["President", "Secretary", "ExecutiveCommitteeMember", "Treasurer"],
    ["PublicitySecretary", "CulturalSecretary", "FieldEquipmentSecretary"],
    ["Activity", "YogaSession", "Event", "FinancialManagement", "PaymentGateway"],
    ["MediaPost"],
]


def parse_source():
    text = SOURCE.read_text(encoding="utf-8")
    nodes = re.findall(r'^rectangle "([^"]+)" as (\w+)(?: (#[0-9A-Fa-f]{6}))?$', text, re.M)
    edges = re.findall(r'^(\w+) -- (\w+) : (.+)$', text, re.M)
    aliases = {alias for _, alias, _ in nodes}
    if len(nodes) != 18 or len(edges) != 47:
        raise ValueError(f"Expected 18 classes and 47 collaborations; got {len(nodes)} and {len(edges)}")
    if any(a not in aliases or b not in aliases for a, b, _ in edges):
        raise ValueError("A relationship refers to a missing class")
    return nodes, edges


def cell(root, cell_id, value="", style="", vertex=False, edge=False, parent="1"):
    attrs = {"id": cell_id, "value": value, "style": style, "parent": parent}
    if vertex:
        attrs["vertex"] = "1"
    if edge:
        attrs["edge"] = "1"
    return SubElement(root, "mxCell", attrs)


def wrap_label(label):
    label = label.replace("\\n", "\n")
    return "<br>".join(label.splitlines())


def main():
    nodes, edges = parse_source()
    mxfile = Element(
        "mxfile",
        {
            "host": "app.diagrams.net",
            "modified": "2026-10-06T00:00:00.000Z",
            "agent": "Codex",
            "version": "24.7.17",
            "type": "device",
        },
    )
    diagram = SubElement(mxfile, "diagram", {"id": "overall-crc", "name": "Overall CRC"})
    model = SubElement(
        diagram,
        "mxGraphModel",
        {
            "dx": "794",
            "dy": "1123",
            "grid": "1",
            "gridSize": "10",
            "guides": "1",
            "tooltips": "1",
            "connect": "1",
            "arrows": "1",
            "fold": "1",
            "page": "1",
            "pageScale": "1",
            "pageWidth": str(PAGE_W),
            "pageHeight": str(PAGE_H),
            "math": "0",
            "shadow": "0",
        },
    )
    root = SubElement(model, "root")
    cell(root, "0")
    cell(root, "1", parent="0")

    bounds = {a: fill for label, a, fill in nodes}
    positions = {}
    y_start, y_step = 80, 190
    for tier_index, tier in enumerate(TIERS):
        count = len(tier)
        gap = min(18, (PAGE_W - 36 - count * NODE_W) / max(1, count - 1)) if count > 1 else 0
        row_width = count * NODE_W + max(0, count - 1) * gap
        x_start = (PAGE_W - row_width) / 2
        y = y_start + tier_index * y_step
        for idx, alias in enumerate(tier):
            positions[alias] = (x_start + idx * (NODE_W + gap), y)

    for label, alias, fill in nodes:
        x, y = positions[alias]
        color = fill or "#DCEEFF"
        style = (
            "rounded=1;whiteSpace=wrap;html=1;arcSize=16;"
            f"fillColor={color};strokeColor=#3973AC;fontColor=#16324F;"
            "fontStyle=1;fontSize=12;align=center;verticalAlign=middle;"
            "spacing=5;"
        )
        node = cell(root, alias, label, style, vertex=True)
        SubElement(node, "mxGeometry", {"x": str(positions[alias][0]), "y": str(y), "width": str(NODE_W), "height": str(NODE_H), "as": "geometry"})

    for index, (source, target, label) in enumerate(edges, 1):
        edge = cell(
            root,
            f"e{index}",
            wrap_label(label),
            "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;"
            "html=1;strokeColor=#526D82;strokeWidth=1.2;startArrow=none;endArrow=none;"
            "fontColor=#263746;fontSize=8;labelBackgroundColor=#FFFFFF;align=center;"
            "verticalAlign=middle;spacing=2;",
            edge=True,
        )
        edge.set("source", source)
        edge.set("target", target)
        SubElement(edge, "mxGeometry", {"relative": "1", "as": "geometry"})

    # Title is a separate editable text object within the A4 page.
    title = cell(root, "overall-title", "Overall CRC Collaboration Diagram", "text;html=1;align=center;verticalAlign=middle;fontStyle=1;fontSize=18;fontColor=#16324F;", vertex=True)
    SubElement(title, "mxGeometry", {"x": "80", "y": "24", "width": str(PAGE_W - 160), "height": "34", "as": "geometry"})

    pretty = minidom.parseString(tostring(mxfile, encoding="utf-8")).toprettyxml(indent="  ", encoding="utf-8")
    OUTPUT.write_bytes(pretty)
    print(f"Created {OUTPUT}")
    print(f"Page: portrait A4 ({PAGE_W} x {PAGE_H})")
    print(f"Classes: {len(nodes)}; collaborations: {len(edges)}")


if __name__ == "__main__":
    main()

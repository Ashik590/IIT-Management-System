from copy import deepcopy
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


source = Path(sys.argv[1])
output = Path(sys.argv[2])
tree = ET.parse(source)
root = tree.getroot()
diagram = next((item for item in root.findall("diagram") if item.get("name") == "Page-41"), None)
if diagram is None:
    raise SystemExit("Could not find the combined lifeline diagram (Page-41).")
graph_root = diagram.find("mxGraphModel/root")
if graph_root is None:
    raise SystemExit("Page-41 has no mxGraphModel/root.")

cells = {cell.get("id"): cell for cell in graph_root.findall("mxCell")}
if any(cell.get("id") == "notification-lifeline" for cell in cells.values()):
    raise SystemExit("Page-41 already contains the Notification lane; refusing to duplicate it.")


def add_cell(cell_id, *, value="", style="", vertex=None, edge=None, source_id=None, target_id=None):
    attrs = {"id": cell_id, "parent": "1"}
    if vertex:
        attrs["vertex"] = "1"
    if edge:
        attrs["edge"] = "1"
    if source_id:
        attrs["source"] = source_id
    if target_id:
        attrs["target"] = target_id
    if value:
        attrs["value"] = value
    if style:
        attrs["style"] = style
    cell = ET.SubElement(graph_root, "mxCell", attrs)
    return cell


def add_vertex(cell_id, label, x, y, width, height, *, font=5, style="html=1;whiteSpace=wrap;"):
    value = f'<span style="font-size: {font}px;">{label}</span>'
    cell = add_cell(cell_id, value=value, style=style, vertex=True)
    ET.SubElement(cell, "mxGeometry", {
        "height": str(height), "width": str(width), "x": str(x), "y": str(y), "as": "geometry"
    })
    return cell


def add_edge(cell_id, source_id, target_id, label, *, horizontal=False, target_y=None):
    if horizontal:
        style = (
            "edgeStyle=none;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;"
            "exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;"
        )
    elif target_y is not None:
        entry_y = target_y / 5032
        style = (
            "edgeStyle=none;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;"
            f"exitX=0;exitY=0.5;exitDx=0;exitDy=0;entryX=0.5;entryY={entry_y:.4f};"
            "entryDx=0;entryDy=0;entryPerimeter=0;"
        )
    else:
        style = (
            "edgeStyle=none;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;"
            "exitX=0.5;exitY=1;exitDx=0;exitDy=0;entryX=0.5;entryY=0;entryDx=0;entryDy=0;"
        )
    cell = add_cell(cell_id, style=style, edge=True, source_id=source_id, target_id=target_id)
    ET.SubElement(cell, "mxGeometry", {"relative": "1", "as": "geometry"})
    label_cell = ET.SubElement(graph_root, "mxCell", {
        "id": f"{cell_id}-label", "parent": cell_id,
        "style": "edgeLabel;html=1;align=center;verticalAlign=middle;resizable=0;points=[];",
        "value": f'<font style="font-size: 6px;">{label}</font>', "vertex": "1", "connectable": "0"
    })
    ET.SubElement(label_cell, "mxGeometry", {
        "relative": "1", "x": "-0.4", "y": "-1", "as": "geometry"
    })
    return cell


# Add the nineteenth class lifeline while preserving the existing class lanes.
lifeline = add_cell(
    "notification-lifeline", style=cells["4nu8Ytd-eLPOHQkFgWr6-5"].get("style", ""), vertex=True
)
ET.SubElement(lifeline, "mxGeometry", {
    "height": "5032", "width": "20", "x": "940", "y": "48", "as": "geometry"
})
add_vertex(
    "notification-header", '<font style="font-size: 10px;">Notification</font>',
    930, 48, 40, 42, font=10
)

# Three representative notification cycles: event announcement, registration
# confirmation, and attendance/milestone notification. Each uses only the
# agreed Notification operations and delivers directly to Member.
flows = [
    {
        "prefix": "notification-event",
        "start": 1260,
        "trigger_source": "4nu8Ytd-eLPOHQkFgWr6-86",  # Cultural Secretary managing events
        "trigger": "event created",
        "delivery": "event announcement to all members",
    },
    {
        "prefix": "notification-registration",
        "start": 1510,
        "trigger_source": "4nu8Ytd-eLPOHQkFgWr6-90",  # Event registration completed
        "trigger": "registration completed",
        "delivery": "registration confirmation to the member",
    },
    {
        "prefix": "notification-attendance",
        "start": 2610,
        "trigger_source": None,
        "trigger": "check-in recorded or attendance milestone reached",
        "delivery": "attendance/milestone message to checked-in member",
    },
]

for flow in flows:
    prefix = flow["prefix"]
    y0 = flow["start"]
    request_id = f"{prefix}-requested"
    mode_id = f"{prefix}-mode"
    compose_id = f"{prefix}-compose"
    send_id = f"{prefix}-send"
    for suffix, label, y in (
        ("requested", "Notification<br>Requested", y0),
        ("mode", "Selecting<br>Delivery Mode", y0 + 58),
        ("compose", "Composing<br>Message", y0 + 116),
        ("send", "Sending<br>Message", y0 + 174),
    ):
        add_vertex(f"{prefix}-{suffix}", label, 935, y, 30, 50, font=5)

    if flow["trigger_source"]:
        add_edge(f"{prefix}-trigger", flow["trigger_source"], request_id, flow["trigger"], horizontal=True)
    else:
        # Attendance may be recorded by Activity, YogaSession, or Event.
        for suffix, source_id, label in (
            ("activity-trigger", "4nu8Ytd-eLPOHQkFgWr6-115", "check-in or milestone recorded"),
            ("session-trigger", "4nu8Ytd-eLPOHQkFgWr6-123", "session check-in recorded"),
            ("event-trigger", "4nu8Ytd-eLPOHQkFgWr6-98", "event check-in recorded"),
        ):
            add_edge(f"{prefix}-{suffix}", source_id, request_id, label, horizontal=True)

    add_edge(f"{prefix}-select", request_id, mode_id, "selectMode()")
    add_edge(f"{prefix}-compose-call", mode_id, compose_id, "composeMessage()")
    add_edge(f"{prefix}-send-call", compose_id, send_id, "sendMessage()")
    add_edge(
        f"{prefix}-to-member", send_id, "4nu8Ytd-eLPOHQkFgWr6-6",
        flow["delivery"], target_y=y0 + 174
    )

output.parent.mkdir(parents=True, exist_ok=True)
ET.indent(tree, space="  ")
tree.write(output, encoding="utf-8", xml_declaration=True)
print(f"Wrote updated editable Draw.io file: {output}")

#!/usr/bin/env python3
import re
import sys
from pathlib import Path


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: split_state_diagrams.py COMBINED_FILE OUTPUT_DIRECTORY")

    source = Path(sys.argv[1])
    output_directory = Path(sys.argv[2])
    text = source.read_text(encoding="utf-8")
    blocks = re.findall(r"@startuml\b.*?@enduml", text, flags=re.DOTALL)

    created = []
    for block in blocks:
        match = re.search(r"^mainframe\s+([^\r\n]+)", block, flags=re.MULTILINE)
        if not match:
            raise ValueError("A PlantUML block does not contain a mainframe name")
        diagram_name = match.group(1).strip()
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", diagram_name):
            raise ValueError(f"Unsafe diagram name: {diagram_name!r}")
        output_path = output_directory / f"{diagram_name}.puml"
        output_path.write_text(block.strip() + "\n", encoding="utf-8")
        created.append(output_path.name)

    if len(created) != 18:
        raise ValueError(f"Expected 18 diagrams, found {len(created)}")
    print("\n".join(created))


if __name__ == "__main__":
    main()

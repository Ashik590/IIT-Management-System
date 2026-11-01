#!/usr/bin/env python3
import argparse
from pathlib import Path

from PIL import Image, ImageDraw


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("render_dir", type=Path)
    args = parser.parse_args()

    pages = sorted(args.render_dir.glob("page-*.png"))
    thumb_w, thumb_h = 238, 337
    cols, rows = 4, 4
    batches = 0
    for batches, start in enumerate(range(0, len(pages), cols * rows), 1):
        subset = pages[start : start + cols * rows]
        sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 24)), "white")
        draw = ImageDraw.Draw(sheet)
        for index, path in enumerate(subset):
            image = Image.open(path).convert("RGB")
            image.thumbnail((thumb_w - 10, thumb_h - 10))
            column, row = index % cols, index // cols
            x = column * thumb_w + (thumb_w - image.width) // 2
            y = row * (thumb_h + 24) + 5
            sheet.paste(image, (x, y))
            draw.text(
                (column * thumb_w + 8, row * (thumb_h + 24) + thumb_h + 4),
                f"Page {start + index + 1}",
                fill="black",
            )
        sheet.save(args.render_dir / f"contact-{batches:02d}.png")
    print(f"{len(pages)} pages rendered; {batches} contact sheets")


if __name__ == "__main__":
    main()

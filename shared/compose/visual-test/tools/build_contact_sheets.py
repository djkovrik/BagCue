from __future__ import annotations

import hashlib
import json
import math
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[4]
SNAPSHOTS = ROOT / "shared/compose/visual-test/src/test/snapshots/images"
OUTPUT = ROOT / "docs/design/visual-review/screens"
INDEX = ROOT / "docs/design/visual-review/index.json"
SCREEN_PATTERN = re.compile(r"screen_(\d{3})", re.IGNORECASE)
COLS = 4
CELL_WIDTH = 300
CELL_HEIGHT = 500
THUMB_WIDTH = 276
THUMB_HEIGHT = 430


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def screen_for(path: Path) -> str:
    match = SCREEN_PATTERN.search(path.name)
    if not match:
        raise ValueError(f"Snapshot has no SCREEN identity: {path.name}")
    return f"SCREEN-{match.group(1)}"


def main() -> None:
    snapshots = sorted(SNAPSHOTS.glob("*.png"), key=lambda item: item.name.casefold())
    if len(snapshots) != 118:
        raise ValueError(f"Expected 118 snapshots, found {len(snapshots)}")
    grouped: dict[str, list[Path]] = {}
    for snapshot in snapshots:
        grouped.setdefault(screen_for(snapshot), []).append(snapshot)
    if sorted(grouped) != [f"SCREEN-{number:03d}" for number in range(1, 12)]:
        raise ValueError(f"Unexpected screen inventory: {sorted(grouped)}")

    OUTPUT.mkdir(parents=True, exist_ok=True)
    font = ImageFont.load_default()
    index_entries: list[dict[str, object]] = []
    sheets: list[dict[str, object]] = []
    for screen, images in sorted(grouped.items()):
        rows = math.ceil(len(images) / COLS)
        sheet = Image.new("RGB", (COLS * CELL_WIDTH, rows * CELL_HEIGHT), "#ECEFF1")
        draw = ImageDraw.Draw(sheet)
        for offset, source_path in enumerate(images):
            row, column = divmod(offset, COLS)
            x = column * CELL_WIDTH
            y = row * CELL_HEIGHT
            with Image.open(source_path) as original:
                preview = original.convert("RGB")
                preview.thumbnail((THUMB_WIDTH, THUMB_HEIGHT), Image.Resampling.LANCZOS)
            px = x + (CELL_WIDTH - preview.width) // 2
            py = y + 34 + (THUMB_HEIGHT - preview.height) // 2
            sheet.paste(preview, (px, py))
            cell = offset + 1
            label = f"{cell:02d}  {source_path.name.removeprefix('Paparazzi_Preview_Test_')[:42]}"
            draw.rectangle((x, y, x + CELL_WIDTH - 1, y + CELL_HEIGHT - 1), outline="#90A4AE", width=1)
            draw.text((x + 8, y + 10), label, fill="#102027", font=font)
            index_entries.append(
                {
                    "screen": screen,
                    "sheet": f"screens/{screen}-contact-sheet.png",
                    "cell": cell,
                    "row": row + 1,
                    "column": column + 1,
                    "golden": source_path.relative_to(ROOT).as_posix(),
                    "sha256": sha256(source_path),
                    "pixelWidth": preview.width,
                    "pixelHeight": preview.height,
                }
            )
        sheet_path = OUTPUT / f"{screen}-contact-sheet.png"
        sheet.save(sheet_path, optimize=True)
        sheets.append(
            {
                "screen": screen,
                "path": sheet_path.relative_to(ROOT).as_posix(),
                "cellCount": len(images),
                "sha256": sha256(sheet_path),
            }
        )

    payload = {
        "schemaVersion": "1.0",
        "goldenCount": len(snapshots),
        "screenCount": len(grouped),
        "ordering": "screen ascending, golden filename case-insensitive ascending, row-major cells",
        "sheets": sheets,
        "entries": index_entries,
    }
    INDEX.parent.mkdir(parents=True, exist_ok=True)
    INDEX.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

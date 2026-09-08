"""Validate BagCue's documented composeApp -> shared/compose resource path adaptation."""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

REPOSITORY = Path(__file__).resolve().parents[2]
SKILL_SCRIPTS = Path(r"C:\Users\Sergey\.codex\skills\vibe-developer\scripts")
sys.path.insert(0, str(SKILL_SCRIPTS))

from asset_contract import validate_delivery  # noqa: E402


def main() -> int:
    spec = json.loads((REPOSITORY / "spec/app-spec/app-spec.json").read_text(encoding="utf-8-sig"))
    for item in spec["assetRequirements"]["items"]:
        for variant in item["variants"]:
            variant["path"] = variant["path"].replace(
                "composeApp/src/commonMain/composeResources/",
                "shared/compose/src/commonMain/composeResources/",
            )
    errors, warnings = validate_delivery(spec, REPOSITORY)
    density_sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, size in density_sizes.items():
        base = REPOSITORY / f"androidApp/src/main/res/mipmap-{density}"
        for name in ("ic_launcher.png", "ic_launcher_background.png", "ic_launcher_foreground.png", "ic_launcher_monochrome.png"):
            with Image.open(base / name) as image:
                image.load()
                if image.size != (size, size):
                    errors.append(f"{name} {density}: expected {size}x{size}, got {image.size}")
                alpha = image.convert("RGBA").getchannel("A").getextrema()
                if name in ("ic_launcher_foreground.png", "ic_launcher_monochrome.png") and alpha != (0, 255):
                    errors.append(f"{name} {density}: expected transparent and visible pixels, got alpha {alpha}")
                if name in ("ic_launcher.png", "ic_launcher_background.png") and alpha != (255, 255):
                    errors.append(f"{name} {density}: expected an opaque platform background, got alpha {alpha}")

    ios_dir = REPOSITORY / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
    contents_text = (ios_dir / "Contents.json").read_text(encoding="utf-8-sig")
    import re
    contents = json.loads(re.sub(r",\s*([}\]])", r"\1", contents_text))
    for item in contents["images"]:
        if "filename" not in item:
            continue
        expected = round(float(item["size"].split("x")[0]) * int(item["scale"].rstrip("x")))
        with Image.open(ios_dir / item["filename"]) as image:
            image.load()
            if image.size != (expected, expected):
                errors.append(f"{item['filename']}: expected {expected}x{expected}, got {image.size}")
            if image.convert("RGBA").getchannel("A").getextrema() != (255, 255):
                errors.append(f"{item['filename']}: iOS launcher icons must be opaque")
    print(json.dumps({"valid": not errors, "errors": errors, "warnings": warnings}, indent=2))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())

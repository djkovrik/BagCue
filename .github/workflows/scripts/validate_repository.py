#!/usr/bin/env python3
"""Deterministic repository contracts used by local and CI quality gates."""
from pathlib import Path
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def resource_keys(path: Path) -> set[str]:
    return {node.attrib["name"] for node in ET.parse(path).getroot() if "name" in node.attrib}


def main() -> int:
    android_build = (ROOT / "androidApp/build.gradle.kts").read_text(encoding="utf-8")
    manifest = (ROOT / "androidApp/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    podfile = (ROOT / "iosApp/Podfile").read_text(encoding="utf-8")
    project = (ROOT / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
    ios_controller = (ROOT / "shared/platform/src/iosMain/kotlin/com/sedsoftware/bagcue/platform/apa/IosApaPlatform.kt").read_text(encoding="utf-8")
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")

    require('applicationId = "com.sedsoftware.bagcue"' in android_build, "Android application ID drift")
    require('PRODUCT_BUNDLE_IDENTIFIER = com.sedsoftware.bagcue.iosApp;' in project, "iOS bundle ID drift")
    require("bagcue.productionYandexAdUnitId=R-M-19857241-1" in properties, "production Yandex ID drift")
    require(android_build.count("R-M-19857241-1") <= 1, "production ad ID must come from the documented property")
    require('buildConfigField("String", "YANDEX_AD_UNIT_ID", "\\"\\"")' in android_build, "debug/default ad ID is not empty")
    require('tools:node="remove"' in manifest and "com.google.android.gms.permission.AD_ID" in manifest, "AD_ID removal missing")
    require("pod 'FirebaseAnalytics', '12.18.0'" in podfile, "FirebaseAnalytics pod drift")
    require("pod 'YandexMobileAds', '8.4.0'" in podfile, "YandexMobileAds pod drift")
    require("R-M-" not in podfile and "demo" not in podfile.lower(), "iOS Podfile contains an ad unit")
    require("adUnitId = null" in ios_controller, "iOS advertising configuration is not nullable")

    base = ROOT / "shared/compose/src/commonMain/composeResources/values/strings.xml"
    ru = ROOT / "shared/compose/src/commonMain/composeResources/values-ru/strings.xml"
    require(resource_keys(base) == resource_keys(ru), "EN/RU Compose string key sets differ")

    asset_manifest = json.loads((ROOT / "docs/assets/asset-manifest.json").read_text(encoding="utf-8"))
    for asset in asset_manifest.get("assets", []):
        for output in asset.get("outputs", []):
            path = ROOT / output["path"]
            require(path.is_file(), f"missing asset output: {output['path']}")
            expected_hash = output.get("sha256")
            if expected_hash:
                require(hashlib.sha256(path.read_bytes()).hexdigest() == expected_hash, f"asset hash drift: {output['path']}")

    workflow_dir = ROOT / ".github/workflows"
    expected = {
        "AnalysisAndTest.yml", "MeasureTestCoverage.yml", "CodeCoverageBadge.yml",
        "CreateAndroidRelease.yml", "PublishAndroidRelease.yml",
    }
    require(expected.issubset({p.name for p in workflow_dir.glob("*.yml")}), "required workflow missing")
    for path in workflow_dir.glob("*.yml"):
        text = path.read_text(encoding="utf-8")
        require("TODO" not in text and "<package" not in text, f"placeholder in {path.name}")
        for match in re.finditer(r"uses:\s*([^\s]+)", text):
            value = match.group(1)
            if value.startswith("./"):
                continue
            require(re.search(r"@[0-9a-f]{40}(?:\s|$)", value), f"action is not SHA-pinned in {path.name}: {value}")
    print("repository contracts: OK")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, ET.ParseError) as error:
        print(f"repository contract failure: {error}", file=sys.stderr)
        raise SystemExit(1)

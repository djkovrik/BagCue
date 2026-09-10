#!/usr/bin/env python3
"""Deterministic repository contracts used by local and CI quality gates."""
from pathlib import Path
import hashlib
import json
import plistlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
POLICY_URL = "https://sedsoftware.com/apps/bagcue/policy.html"
POLICY_SHA256 = "3bace33ed5b83c21c0901d20f6fca23c622fb9d7f5402f498209b5e119566e7a"


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
    android_firebase_path = ROOT / "androidApp/google-services.json"
    ios_firebase_path = ROOT / "iosApp/iosApp/GoogleService-Info.plist"
    ios_controller = (ROOT / "shared/platform/src/iosMain/kotlin/com/sedsoftware/bagcue/platform/apa/IosApaPlatform.kt").read_text(encoding="utf-8")
    android_entrypoint = (ROOT / "androidApp/src/main/kotlin/com/sedsoftware/bagcue/AppActivity.kt").read_text(encoding="utf-8")
    ios_entrypoint = (ROOT / "shared/compose/src/iosMain/kotlin/main.kt").read_text(encoding="utf-8")
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")

    require('applicationId = "com.sedsoftware.bagcue"' in android_build, "Android application ID drift")
    require('PRODUCT_BUNDLE_IDENTIFIER = com.sedsoftware.bagcue.iosApp;' in project, "iOS bundle ID drift")
    require(android_firebase_path.is_file(), "Android Firebase configuration missing")
    android_firebase = json.loads(android_firebase_path.read_text(encoding="utf-8"))
    android_clients = android_firebase.get("client", [])
    require(
        any(client.get("client_info", {}).get("android_client_info", {}).get("package_name") == "com.sedsoftware.bagcue" for client in android_clients),
        "Android Firebase package mismatch",
    )
    require(ios_firebase_path.is_file(), "iOS Firebase configuration missing")
    with ios_firebase_path.open("rb") as stream:
        ios_values = plistlib.load(stream)
    require(ios_values.get("BUNDLE_ID") == "com.sedsoftware.bagcue.iosApp", "iOS Firebase bundle ID mismatch")
    require(project.count("GoogleService-Info.plist in Resources") == 2, "iOS Firebase configuration is not in the app Resources phase")
    require("bagcue.productionYandexAdUnitId=R-M-19857241-1" in properties, "production Yandex ID drift")
    require(android_build.count("R-M-19857241-1") <= 1, "production ad ID must come from the documented property")
    require('buildConfigField("String", "YANDEX_AD_UNIT_ID", "\\"\\"")' in android_build, "debug/default ad ID is not empty")
    require('tools:node="remove"' in manifest and "com.google.android.gms.permission.AD_ID" in manifest, "AD_ID removal missing")
    require("pod 'FirebaseAnalytics', '12.18.0'" in podfile, "FirebaseAnalytics pod drift")
    require("pod 'YandexMobileAds', '8.4.0'" in podfile, "YandexMobileAds pod drift")
    require("R-M-" not in podfile and "demo" not in podfile.lower(), "iOS Podfile contains an ad unit")
    require("adUnitId = null" in ios_controller, "iOS advertising configuration is not nullable")
    require(POLICY_URL in android_entrypoint and "Intent.ACTION_VIEW" in android_entrypoint, "Android privacy policy URL/opening drift")
    require(POLICY_URL in ios_entrypoint and "UIApplication.sharedApplication.openURL" in ios_entrypoint, "iOS privacy policy URL/opening drift")
    require(hashlib.sha256((ROOT / "docs/bagcue-policy.html").read_bytes()).hexdigest() == POLICY_SHA256, "reviewed privacy policy hash drift")

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
    publish_workflow = (workflow_dir / "PublishAndroidRelease.yml").read_text(encoding="utf-8")
    badge_workflow = (workflow_dir / "CodeCoverageBadge.yml").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    require("FIREBASE_GOOGLE_SERVICES_JSON_BASE64" not in publish_workflow, "Firebase config must not use a GitHub secret")
    require(
        "test -s androidApp/google-services.json" in publish_workflow,
        "Android release must validate the committed Firebase config",
    )
    require("validate_privacy_policy.py" in publish_workflow, "Android publication must verify the hosted privacy policy")
    require("bagcue-coverage-badge.json" in badge_workflow, "coverage workflow badge filename drift")
    require("/raw/bagcue-coverage-badge.json" in readme, "README coverage endpoint drift")
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

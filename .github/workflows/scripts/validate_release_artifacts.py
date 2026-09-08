#!/usr/bin/env python3
from pathlib import Path
import sys
import zipfile


def main() -> int:
    apk, aab, mapping = map(Path, sys.argv[1:4])
    for path in (apk, aab, mapping):
        if not path.is_file() or path.stat().st_size == 0:
            raise SystemExit(f"missing or empty release artifact: {path}")
    if not zipfile.is_zipfile(apk) or not zipfile.is_zipfile(aab):
        raise SystemExit("APK/AAB is not a valid ZIP container")
    with zipfile.ZipFile(aab) as bundle:
        required = {"BundleConfig.pb", "base/manifest/AndroidManifest.xml"}
        if not required.issubset(bundle.namelist()):
            raise SystemExit("AAB structure is incomplete")
    print("release artifacts: OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

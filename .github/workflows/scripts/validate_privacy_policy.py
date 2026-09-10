#!/usr/bin/env python3
"""Verify that the canonical hosted BagCue privacy policy matches the reviewed source."""
from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[3]
POLICY_PATH = ROOT / "docs/bagcue-policy.html"
POLICY_URL = "https://sedsoftware.com/apps/bagcue/policy.html"
EXPECTED_SHA256 = "3bace33ed5b83c21c0901d20f6fca23c622fb9d7f5402f498209b5e119566e7a"


def main() -> int:
    local = POLICY_PATH.read_bytes()
    local_hash = sha256(local).hexdigest()
    if local_hash != EXPECTED_SHA256:
        raise RuntimeError(f"reviewed local policy hash drift: {local_hash}")

    request = Request(POLICY_URL, headers={"User-Agent": "BagCue-release-policy-check/1"})
    with urlopen(request, timeout=20) as response:
        final_url = response.geturl()
        content_type = response.headers.get_content_type()
        remote = response.read(len(local) + 1)
    if final_url != POLICY_URL:
        raise RuntimeError(f"canonical policy URL redirected to: {final_url}")
    if content_type != "text/html":
        raise RuntimeError(f"unexpected policy Content-Type: {content_type}")
    if remote != local:
        raise RuntimeError(f"hosted policy drift: {sha256(remote).hexdigest()}")

    print(f"privacy policy: OK ({local_hash})")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (HTTPError, URLError, OSError, RuntimeError) as error:
        print(f"privacy policy validation failed: {error}", file=sys.stderr)
        raise SystemExit(1)

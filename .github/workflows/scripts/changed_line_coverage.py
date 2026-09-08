#!/usr/bin/env python3
"""Fail when executable Kotlin lines added by a PR fall below the configured coverage."""
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def added_lines(base: str) -> dict[str, set[int]]:
    diff = subprocess.check_output(
        ["git", "diff", "--unified=0", f"{base}...HEAD", "--", "*.kt"], text=True
    )
    result: dict[str, set[int]] = {}
    current = ""
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
        elif current and line.startswith("@@"):
            match = re.search(r"\+(\d+)(?:,(\d+))?", line)
            if match:
                start, count = int(match.group(1)), int(match.group(2) or "1")
                result.setdefault(current, set()).update(range(start, start + count))
    return result


def main() -> int:
    xml_path, base, minimum = Path(sys.argv[1]), sys.argv[2], float(sys.argv[3])
    additions = added_lines(base)
    executable = covered = 0
    tree = ET.parse(xml_path)
    for package in tree.getroot().iter("package"):
        package_path = package.attrib.get("name", "")
        for source in package.findall("sourcefile"):
            suffix = f"/{package_path}/{source.attrib['name']}"
            candidates = [path for path in additions if path.endswith(suffix)]
            if not candidates:
                continue
            changed = additions[candidates[0]]
            for line in source.findall("line"):
                number = int(line.attrib["nr"])
                if number in changed:
                    executable += 1
                    covered += int(line.attrib.get("ci", "0")) > 0
    percent = 100.0 if executable == 0 else covered * 100.0 / executable
    print(f"changed executable lines: {covered}/{executable} ({percent:.2f}%)")
    return 0 if percent >= minimum else 1


if __name__ == "__main__":
    raise SystemExit(main())

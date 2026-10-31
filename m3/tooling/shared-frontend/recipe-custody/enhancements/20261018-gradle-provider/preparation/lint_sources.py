#!/usr/bin/env python3
"""Read-only source hygiene and ownership gate for the isolated provider candidate."""
import ast
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
candidate = Path(sys.argv[1]).resolve()
prefix = "m3/tooling/module-packs/build-support/gradle-provider/"
files = sorted(path for path in candidate.rglob("*") if path.is_file())
if not files:
    raise SystemExit("No actual candidate files")
inventory = []
for path in files:
    relative = path.relative_to(candidate).as_posix()
    if not relative.startswith(prefix) or path.is_symlink():
        raise SystemExit("Source ownership mismatch: " + relative)
    data = path.read_bytes()
    text = data.decode("utf-8")
    if not text.endswith("\n") or "\0" in text or "\r" in text or data.startswith(b"\xef\xbb\xbf"):
        raise SystemExit("Invalid source text: " + relative)
    for number, line in enumerate(text.splitlines(), 1):
        if line.rstrip(" \t") != line:
            raise SystemExit(f"Trailing whitespace: {relative}:{number}")
    if path.suffix == ".xml":
        ET.fromstring(data)
    inventory.append({"path": relative, "sha256": hashlib.sha256(data).hexdigest(), "bytes": len(data)})
for script in [root / "run_gate.py", root / "lint_sources.py"]:
    ast.parse(script.read_text(), filename=str(script))
for script in (root / "canonical").rglob("*.py"):
    ast.parse(script.read_text(), filename=str(script))
print(json.dumps({"status": "PASS", "files": inventory}, indent=2))

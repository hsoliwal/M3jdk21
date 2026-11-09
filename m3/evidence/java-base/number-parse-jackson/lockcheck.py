#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""lockcheck.py <repo> : compare every donor of the jackson pack SOURCE_LOCK.json with build.py's own tree_digest.
Run it on POSIX, where the pack builds: build.py orders paths by the platform's Path sort."""
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
repo = Path(sys.argv[1])
pack = repo / "m3/tooling/module-packs/jackson"
spec = importlib.util.spec_from_file_location("m3_jackson_pack_build", pack / "build.py")
build = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build)
lock = json.loads((pack / "SOURCE_LOCK.json").read_bytes())
for name, entry in sorted(lock["donors"].items()):
    root = repo / "m3/vendor/third_party" / name
    if not root.is_dir():
        print(f"ABSENT\t{name}")
        continue
    digest, count = build.tree_digest(root)
    same = digest == entry["tree_sha256"] and count == entry["file_count"]
    print(f"{'MATCH' if same else 'DIFF'}\t{name}\tfiles={count}\tlock_files={entry['file_count']}")

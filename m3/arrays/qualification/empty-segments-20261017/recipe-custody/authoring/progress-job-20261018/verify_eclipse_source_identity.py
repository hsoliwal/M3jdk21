#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compare reviewed Maven release source bytes with their pinned GitHub SCM files."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--donor-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    source = args.donor_root / "receipt.json"
    records = []
    notices = []
    for artifact in json.loads(source.read_text())["files"]:
        path = args.donor_root / artifact["file"]
        if sha(path.read_bytes()) != artifact["sha256"]:
            raise ValueError("artifact drift")
        if path.name.endswith(".jar"):
            with zipfile.ZipFile(path) as archive:
                data = archive.read("about.html")
                output = args.output / "notices" / (path.name + "-about.html.txt")
                output.parent.mkdir(exist_ok=True)
                output.write_bytes(data)
                notices.append({"artifact": path.name, "entry": "about.html", "path": str(output.relative_to(args.output)),
                                "sha256": sha(data), "bytes": len(data)})
        for entry in artifact.get("reviewed_sources", []):
            url = (artifact["scm"]["url"].replace("https://github.com/", "https://raw.githubusercontent.com/")
                   .replace("/tree/", "/") + "/src/" + entry["entry"])
            data = subprocess.check_output(["curl", "--fail", "--silent", "--show-error", "--location", "--max-time", "45", url], timeout=50)
            if sha(data) != entry["sha256"]:
                raise ValueError("GitHub SCM source differs from reviewed release source: " + url)
            records.append({"url": url, "revision": artifact["scm"]["tag"], "sha256": sha(data),
                            "git_blob": entry["git_blob"], "bytes": len(data), "result": "EXACT_RELEASE_SOURCE_MATCH"})
    if len(records) != 4:
        raise ValueError("expected four reviewed GitHub owners")
    receipt = {"schema": "M3_PROGRESS_JOB_ECLIPSE_GITHUB_CROSSCHECK_V1", "result": "PASS",
               "release_receipt_sha256": sha(source.read_bytes()), "source_files": records,
               "preserved_distribution_notices": notices, "source_copied_into_java_base": False}
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS four pinned GitHub/Eclipse release source identities and original distribution notices")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fetch exact Eclipse release inputs for source review and a real status witness."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import zipfile


COORDINATES = (
    ("org.eclipse.core.runtime", "3.35.0", ("pom",)),
    ("org.eclipse.equinox.common", "3.21.0", ("pom", "jar", "sources.jar")),
    ("org.eclipse.core.jobs", "3.15.900", ("pom", "sources.jar")),
    ("org.eclipse.osgi", "3.24.300", ("pom", "jar")),
)
REVIEW_ENTRIES = {
    "org.eclipse.equinox.common": (
        "org/eclipse/core/runtime/IStatus.java",
        "org/eclipse/core/runtime/Status.java",
        "org/eclipse/core/runtime/MultiStatus.java",
    ),
    "org.eclipse.core.jobs": ("org/eclipse/core/runtime/jobs/Job.java",),
}
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def digest(data, algorithm="sha256"):
    return hashlib.new(algorithm, data).hexdigest()


def download(url):
    return subprocess.check_output(
        ["curl", "--fail", "--silent", "--show-error", "--location", "--max-time", "45", url],
        timeout=50,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    entries = []
    for artifact, version, kinds in COORDINATES:
        base = "https://repo.maven.apache.org/maven2/org/eclipse/platform/" + artifact + "/" + version + "/"
        scm = None
        for kind in kinds:
            name = artifact + "-" + version + ("-sources.jar" if kind == "sources.jar" else "." + kind)
            url = base + name
            data = download(url)
            checksum = download(url + ".sha1").decode("ascii").strip().split()[0]
            if digest(data, "sha1") != checksum:
                raise ValueError("published Maven checksum mismatch: " + name)
            path = args.output / name
            path.write_bytes(data)
            (args.output / (name + ".sha1")).write_text(checksum + "\n")
            record = {"coordinate": "org.eclipse.platform:" + artifact + ":" + version,
                      "file": name, "url": url, "sha256": digest(data), "sha1": checksum,
                      "bytes": len(data)}
            if kind == "pom":
                pom = ET.fromstring(data)
                scm = {key: pom.findtext("m:scm/m:" + key, namespaces=NS) for key in ("tag", "url")}
                record["scm"] = scm
                record["declared_licenses"] = [e.text for e in pom.findall("m:licenses/m:license/m:name", NS)]
            elif kind == "sources.jar":
                reviewed = []
                with zipfile.ZipFile(path) as archive:
                    for entry in REVIEW_ENTRIES[artifact]:
                        source = archive.read(entry)
                        target = args.output / "review" / artifact / (entry + ".txt")
                        target.parent.mkdir(parents=True, exist_ok=True)
                        target.write_bytes(source)
                        reviewed.append({"entry": entry, "file": str(target.relative_to(args.output)),
                                         "sha256": digest(source), "bytes": len(source),
                                         "git_blob": digest(b"blob " + str(len(source)).encode("ascii") + b"\0" + source, "sha1")})
                record["reviewed_sources"] = reviewed
                record["scm"] = scm
            entries.append(record)
    receipt = {"schema": "M3_PROGRESS_JOB_ECLIPSE_SOURCE_INPUTS_V1", "result": "PASS",
               "purpose": "Actual source review and real Eclipse status identity witness; no Eclipse source copied into java.base.",
               "repository_owned_version": "m3/eclipse/core-runtime/pom.xml already selects org.eclipse.core.runtime:3.35.0",
               "files": entries}
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS", len(entries), "published artifacts; checksum and source identity retained")


if __name__ == "__main__":
    main()

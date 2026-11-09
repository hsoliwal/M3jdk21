#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Differential oracle for the ChangeMap lower-bound replay.

Compiles the whole jdk.m3.file package plus M3FileChangeMapBinarySearchProof in up to three lanes
and runs the proof in each: (receiver) the live owner in this checkout; (donor) the donor postimage
template under the package it declares, with every other class of the package renamed to that
package by its declaration line only; (preimage) the pinned preimage template. Same seed, same
50,001 maps, same 9,341,740 checks: the printed digests must be identical. Writes
<out>/DIFFERENTIAL.tsv (lane, package, source_sha256, maps, checks, digest, status).
"""
import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
PACKAGE_LINE = re.compile(rb"^package ([A-Za-z0-9_.]+);$", re.MULTILINE)
RESULT = re.compile(r"^PASS M3_FILE_CHANGEMAP_LOWER_BOUND_V1 maps=(\d+) checks=(\d+) digest=([0-9a-f]{64})$",
                    re.MULTILINE)
OWNER = "M3FileEditBatch.java"
PROOF = "M3FileChangeMapBinarySearchProof.java"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def package_of(text: bytes) -> str:
    found = PACKAGE_LINE.findall(text)
    if len(found) != 1:
        raise SystemExit("REFUSED: expected one package declaration")
    return found[0].decode("ascii")


def repackaged(text: bytes, package: str) -> bytes:
    return PACKAGE_LINE.sub(("package " + package + ";").encode("ascii"), text, count=1)


def tool(java_home: Path, name: str) -> str:
    return str(java_home / "bin" / (name + (".exe" if os.name == "nt" else "")))


def run_checked(label: str, command: list) -> subprocess.CompletedProcess:
    result = subprocess.run(command, text=True, capture_output=True, check=False)
    if result.returncode:
        raise SystemExit("REFUSED: %s exit %d\n%s%s" % (label, result.returncode, result.stdout, result.stderr))
    return result


def lane(name: str, owner: bytes, root: Path, out: Path, java_home: Path):
    package = package_of(owner)
    src, classes = out / name / "src", out / name / "classes"
    for directory in (src, classes):
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir(parents=True)
    main = root / "m3/file/src/main/java/jdk/m3/file"
    tree = src.joinpath(*package.split("."))
    tree.mkdir(parents=True)
    for source in sorted(main.glob("*.java")):
        data = owner if source.name == OWNER else repackaged(source.read_bytes(), package)
        (tree / source.name).write_bytes(data)
    proof = root / "m3/file/src/test/java/jdk/m3/file" / PROOF
    (tree / PROOF).write_bytes(repackaged(proof.read_bytes(), package))
    # Only the owner closure is compiled (-sourcepath pulls M3FileSource and what it needs); the rest
    # of the package is never touched, so an unrelated defect elsewhere cannot block this oracle.
    run_checked(name + " javac", [tool(java_home, "javac"), "--release", "21", "-Xlint:all", "-Werror",
                                  "-sourcepath", str(src), "-d", str(classes), str(tree / PROOF), str(tree / OWNER)])
    run = run_checked(name + " java", [tool(java_home, "java"), "-cp", str(classes),
                                       package + ".M3FileChangeMapBinarySearchProof"])
    match = RESULT.search(run.stdout)
    if match is None:
        raise SystemExit("REFUSED: proof did not print its PASS line in lane " + name + ":\n" + run.stdout)
    return (name, package, sha256(owner), match.group(1), match.group(2), match.group(3))


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--root", type=Path, default=HERE.parents[3])
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--donor-postimage", type=Path, help="donor template under its own package")
    parser.add_argument("--preimage", type=Path, help="pinned preimage template (linear scan)")
    args = parser.parse_args(argv)
    root, out = args.root.resolve(), args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    receiver = (root / "m3/file/src/main/java/jdk/m3/file" / OWNER).read_bytes()
    lanes = [lane("receiver", receiver, root, out, args.java_home)]
    if args.donor_postimage:
        lanes.append(lane("donor", args.donor_postimage.read_bytes(), root, out, args.java_home))
    if args.preimage:
        lanes.append(lane("preimage", args.preimage.read_bytes(), root, out, args.java_home))
    reference = lanes[0][3:]
    rows = ["lane\tpackage\tsource_sha256\tmaps\tchecks\tdigest\tstatus"]
    status = 0
    for row in lanes:
        verdict = "EQUAL" if row[3:] == reference else "DIFFERENT"
        status = status or (verdict == "DIFFERENT")
        rows.append("\t".join((*row, verdict)))
    (out / "DIFFERENTIAL.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8", newline="")
    sys.stdout.write("\n".join(rows) + "\n")
    return 1 if status else 0


if __name__ == "__main__":
    raise SystemExit(main())

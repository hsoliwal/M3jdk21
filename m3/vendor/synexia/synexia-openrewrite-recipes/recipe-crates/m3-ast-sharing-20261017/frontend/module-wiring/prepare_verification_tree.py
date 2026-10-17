#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Assemble isolated proof inputs from actual sealed SDK outputs; never edit owner sources."""
from pathlib import Path
from hashlib import sha256
import argparse
import json
import shutil

HERE = Path(__file__).resolve().parent


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", type=Path, required=True)
    args = parser.parse_args()
    output = HERE / "target/receiver-module"
    output.mkdir(parents=True, exist_ok=True)
    for module in ("compiletime", "compiletime-openrewrite"):
        relative = "m3/ports/" + module
        shutil.copytree(args.target / relative, output / relative, dirs_exist_ok=True,
                        ignore=shutil.ignore_patterns("target", "__pycache__"))
    frontend = HERE.parent
    manifest = frontend / "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-ast-frontend-receiver-20261017/manifest.tsv"
    frontend_records = []
    for line in manifest.read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        path, before, after, template = line.split("\t")
        source = frontend / "target/candidate/receiver" / path
        if digest(source) != after:
            raise ValueError("frontend SDK candidate drift: " + path)
        destination = output / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        frontend_records.append({"path": path, "sha256": after, "origin": "actual frontend SDK candidate"})
    closure = json.loads(HERE.joinpath("VENDOR_CLOSURE.json").read_text())
    for relative in closure["vendor_sources"]:
        path = "m3/vendor/synexia/" + relative
        if not (output / path).exists():
            destination = output / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(args.target / path, destination)
    receipt = {"schema": "M3_AST_STAGED_RECEIVER_MODULE_V1", "output": str(output),
               "ordinary_source_origin": str(args.target), "frontend_candidate_manifest_sha256": digest(manifest),
               "frontend_records": frontend_records,
               "policy": "isolated normal Maven module proof; root source materialization is separately verified"}
    HERE.joinpath("target/STAGED_RECEIVER.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"output": str(output), "frontend_candidate_files": len(frontend_records)}))


if __name__ == "__main__":
    main()

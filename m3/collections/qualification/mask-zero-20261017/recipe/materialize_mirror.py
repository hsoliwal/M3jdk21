#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Copy sealed canonical proof custody after an immutable canonical commit exists."""
from pathlib import Path
from hashlib import sha256
import argparse
import json
import re

HERE = Path(__file__).resolve().parent
CANONICAL = "synexia-openrewrite-recipes/recipe-crates/collections-superset-counts/m3jdk21-mask-family-receiver-20261016/mask-zero-20261017"
RECEIVING = "m3/collections/qualification/mask-zero-20261017"


def digest(data):
    return sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--canonical-commit", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", args.canonical_commit) or set(args.canonical_commit) == {"0"}:
        parser.error("canonical-commit must identify an immutable nonzero Git commit")
    target = args.target.resolve()
    pins = json.loads((HERE / "SOURCE_PINS.json").read_text())
    for item in pins["sources"]:
        path = target / item["path"]
        if not path.is_file() or digest(path.read_bytes()) != item["after_sha256"]:
            raise ValueError("Receiver is not the sealed postimage: " + item["path"])

    payload = {}
    for line in (HERE / "PUBLICATION_FILES.tsv").read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        canonical_path, expected = line.split("\t")
        relative = Path(canonical_path).relative_to(CANONICAL)
        if ".." in relative.parts or relative.is_absolute():
            raise ValueError("Unsafe canonical path: " + canonical_path)
        data = (HERE / relative).read_bytes()
        if digest(data) != expected:
            raise ValueError("Canonical publication content drift: " + canonical_path)
        payload[Path("recipe") / relative] = data
    payload[Path("recipe/PUBLICATION_FILES.tsv")] = (HERE / "PUBLICATION_FILES.tsv").read_bytes()
    receipt = "# recipe-relative-path\tsha256\n" + "".join(
        f"{path.relative_to('recipe').as_posix()}\t{digest(data)}\n"
        for path, data in sorted(payload.items()))
    payload[Path("MIRROR_RECEIPT.tsv")] = receipt.encode()
    handoff = {
        "schema": "M3_MASK_ZERO_HANDOFF_V1",
        "canonical_repository": "hsoliwal/com.synexia",
        "canonical_commit": args.canonical_commit,
        "canonical_path": CANONICAL,
        "target_preimage_revision": pins["target_revision"],
        "canonical_publication_inventory_sha256": digest((HERE / "PUBLICATION_FILES.tsv").read_bytes()),
        "mirror_receipt_sha256": digest(payload[Path("MIRROR_RECEIPT.tsv")]),
        "target_postimages": [{"path": row["path"], "sha256": row["after_sha256"]} for row in pins["sources"]],
        "status": "EXACT_CANONICAL_PROOF_CUSTODY"
    }
    payload[Path("handoff.json")] = (json.dumps(handoff, indent=2) + "\n").encode()
    home = target / RECEIVING
    for relative, data in payload.items():
        destination = home / relative
        if destination.exists() and destination.read_bytes() != data:
            raise ValueError("Refusing different existing custody: " + str(destination))
    for relative, data in payload.items():
        destination = home / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        if destination.read_bytes() != data:
            raise IOError("Custody readback mismatch: " + str(destination))
    print(json.dumps({"status": "PASSED", "canonical_commit": args.canonical_commit,
                      "receiver_custody_files": len(payload), "home": str(home)}))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Seal a reviewable receiving handoff from an actual canonical commit and qualified receipts."""
import argparse
import hashlib
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
ROOT = "m3/compiler/ast-sharing-20261017/"
CRATE = "m3-ast-integration-20261017"
LEVELS = [
    {"level": 0, "scope": "PER_FILE_FACTS_AND_ATOMS"},
    {"level": 1, "scope": "FILE_SUMMARY"},
    {"level": 2, "scope": "PACKAGE"},
    {"level": 3, "scope": "MODULE"},
    {"level": 4, "scope": "PROJECT"},
    {"level": 5, "scope": "REPOSITORY"},
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--canonical-revision", required=True)
    parser.add_argument("--canonical-tree", required=True)
    parser.add_argument("--qualification", type=Path, required=True)
    args = parser.parse_args()
    if any(re.fullmatch(r"[0-9a-f]{40}", value) is None
           for value in (args.canonical_revision, args.canonical_tree)):
        parser.error("the handoff requires actual full canonical commit and tree identities")
    data = json.loads(args.qualification.read_text())
    if data["levels"] != LEVELS:
        parser.error("qualification mapping differs from the user-defined L0–L5 contract")
    for key in ("plugin_sha256", "patch_sha256"):
        if re.fullmatch(r"[0-9a-f]{64}", data[key]) is None:
            parser.error("missing qualified artifact identity: " + key)
    table = ["| Area | Result | Evidence |", "|---|---|---|"]
    for row in data["qualifications"]:
        if row["status"] not in ("PASS", "NOT_QUALIFIED", "NOT_RUN"):
            parser.error("unresolved qualification: " + row["area"])
        table.append("| " + " | ".join(row[key].replace("|", "\\|")
                     for key in ("area", "result", "evidence")) + " |")
    replacements = {
        "__SOURCE_REVISION__": args.canonical_revision,
        "__HIERARCHY_DESCRIPTION__": data["hierarchy_description"],
        "__QUALIFICATION_TABLE__": "\n".join(table),
        "__PLUGIN_SHA__": data["plugin_sha256"],
        "__PATCH_SHA__": data["patch_sha256"],
    }
    readme = (HERE / "README.template.md").read_text()
    for token, value in replacements.items():
        readme = readme.replace(token, value)
    if re.search(r"__[A-Z_]+__", readme):
        parser.error("unresolved receiving documentation token")
    handoff = dict(data)
    handoff.update(schema="M3_AST_SHARING_HANDOFF_V1",
                   canonical_implementation_revision=args.canonical_revision,
                   canonical_implementation_tree=args.canonical_tree,
                   receiving_baseline="4e29dc9d4141261e8763fbe9ffd6a20e017b1aff",
                   original_discovery_baseline="e336c083c90bd882030e9a370949136e191386f2",
                   recipe_owner_revision="3aa8e45d93f5fed721fdb64579b655f4a432a308",
                   receiving_metadata_recipe=CRATE)
    files = {
        ROOT + "README.md": readme,
        ROOT + "handoff.json": json.dumps(handoff, indent=2) + "\n",
        ROOT + "REPRODUCE.md": data["reproduction_markdown"],
        ROOT + "CATALOGUE_INDEX.tsv": data["catalogue_index_tsv"],
        ".github/workflows/m3-compiletime-precompute.yml":
            (HERE / "compiletime-workflow.template.yml").read_text(),
    }
    resource = HERE / "src/main/resources/com/synexia/rewrite/hash-pinned-text" / CRATE
    resource.mkdir(parents=True, exist_ok=True)
    rows = []
    workflow_source = json.loads((HERE / "WORKFLOW_SOURCE.json").read_text())
    for number, (path, text) in enumerate(sorted(files.items())):
        if not text.endswith("\n"):
            text += "\n"
        raw = text.encode("utf-8")
        template = f"{number:03d}-{Path(path).name}.txt"
        (resource / template).write_bytes(raw)
        before = "ABSENT"
        if path == workflow_source["path"]:
            before = hashlib.sha256((HERE / "preimages" / path).read_bytes()).hexdigest()
            if before != workflow_source["preimage_sha256"]:
                parser.error("existing workflow preimage custody drift")
        rows.append("\t".join((path, before, hashlib.sha256(raw).hexdigest(), template)))
    (resource / "manifest.tsv").write_text("\n".join(rows) + "\n")
    print("Sealed four receiving metadata candidates and the existing CI workflow extension")


if __name__ == "__main__":
    main()

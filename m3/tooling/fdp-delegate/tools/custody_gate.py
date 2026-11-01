#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""fdp-delegate gate (d): custody of the FastDoubleParser 1.0.90 overlays.

  python custody_gate.py --out <scratch dir>

1. the donor's verify_source.py passes (pins, production inventory, overlays re-derived through the recipe);
2. the recipe is a fixed point (fdp_delegate_overlay.py --check);
3. the jackson module pack lock: build.py's own tree digest, file count and provenance hash equal SOURCE_LOCK.json
   for every donor this leaf changes, with every PROVENANCE pin inside its donor tree (build.py check_donors);
   unchanged donors are reported as found (master's entry for jackson-core 2.19.2 is already stale);
4. the central vendored-tree verifier's per-module rule (.m3/task-crates/dependency-source-inline-20261010/
   verify_crate.py: every PROVENANCE pin byte-identical, pom carries the -m3-source-1 version) for those donors;
5. tamper probes on a scratch copy: each mutation must make verify_source.py refuse with its specific reason.
The record also carries the repository HEAD and whether the worktree's donor, pack and recipe equal it
(GIT_NO_LAZY_FETCH=1). Writes nothing into the repository; prints one JSON record and exits 1 unless every check
passes.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

sys.dont_write_bytecode = True

REPO = Path(__file__).resolve().parents[4]
TOOL_REL = "m3/tooling/fdp-delegate/tools/fdp_delegate_overlay.py"
DONOR_REL = "m3/vendor/third_party/fastdoubleparser-1.0.90"
PACK_REL = "m3/tooling/module-packs/jackson"
LOCKED_DONORS = ("fastdoubleparser-1.0.90", "jackson-core-2.19.2")
BASE_REVISION = "52fb81c483c74448a12eecb63aa9465e42cdb82c"
OVERLAY = "src/main/java/ch/randelshofer/fastdoubleparser/JavaDoubleParser.java"
RETAINED = "upstream-excluded/ch/randelshofer/fastdoubleparser/JavaDoubleParser.java"
STRAY = "src/main/java/ch/randelshofer/fastdoubleparser/M3Stray.java"
POM_VERSION = "<version>{}</version>"
TAMPER_NOTE = b"\n// tamper probe\n"
GIT_ENV = {"GIT_NO_LAZY_FETCH": "1"}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(*args: str) -> tuple[int, str]:
    result = subprocess.run(["git", "-C", str(REPO), *args], capture_output=True, text=True, check=False,
                            env={**os.environ, **GIT_ENV})
    return result.returncode, result.stdout.strip()


def run(args: list[object]) -> tuple[int, str]:
    result = subprocess.run([str(a) for a in args], capture_output=True, text=True, check=False)
    return result.returncode, (result.stdout + result.stderr).strip()


def lock_check() -> list[dict]:
    spec = importlib.util.spec_from_file_location("m3_jackson_pack_build", REPO / PACK_REL / "build.py")
    build = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(build)
    lock = json.loads((REPO / PACK_REL / "SOURCE_LOCK.json").read_text(encoding="utf-8"))
    rows = []
    for name in LOCKED_DONORS:
        root, expected = REPO / "m3/vendor/third_party" / name, lock["donors"][name]
        tree, count = build.tree_digest(root)
        provenance = json.loads((root / "PROVENANCE.json").read_text(encoding="utf-8"))
        pins = all((REPO / item["repository"]).is_relative_to(root) and (REPO / item["repository"]).is_file()
                   and build.digest(REPO / item["repository"]) == item["sha256"] for item in provenance["files"])
        matches = (tree == expected["tree_sha256"] and count == expected["file_count"]
                   and sha256(root / "PROVENANCE.json") == expected["provenance_sha256"])
        changed = subprocess.run(["git", "-C", str(REPO), "diff", "--quiet", BASE_REVISION, "HEAD", "--",
                                  str(root.relative_to(REPO).as_posix())], check=False).returncode != 0
        # A donor this leaf changes must match its relocked entry; an unchanged donor's entry is reported as found.
        rows.append({"donor": name, "tree_sha256": tree, "file_count": count, "pins_inside_tree": pins,
                     "lock_matches": matches, "changed_by_leaf": changed, "ok": pins and (matches or not changed)})
    return rows


def crate_rule() -> list[dict]:
    rows = []
    for name in LOCKED_DONORS:
        root = REPO / "m3/vendor/third_party" / name
        provenance = json.loads((root / "PROVENANCE.json").read_text(encoding="utf-8"))
        drift = [item["repository"] for item in provenance["files"]
                 if not (REPO / item["repository"]).is_file() or sha256(REPO / item["repository"]) != item["sha256"]]
        version = provenance["coordinate"].rsplit(":", 1)[-1]
        pom = POM_VERSION.format(version) in (root / "pom.xml").read_text(encoding="utf-8")
        rows.append({"donor": name, "pins": len(provenance["files"]), "drift": drift, "pom_version": pom,
                     "ok": not drift and pom})
    return rows


def rewrite_json(path: Path, change) -> None:
    value = json.loads(path.read_text(encoding="utf-8"))
    change(value)
    path.write_bytes((json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8"))


def repin(donor: Path, relative: str) -> None:
    """Re-pin one donor file in PROVENANCE.json (and OVERLAY_SOURCES.json), so only the deeper check can refuse."""
    target, digest = DONOR_REL + "/" + relative, sha256(donor / relative)

    def overlays(value):
        for item in value.get("m3_overlays", []):
            if item["repository"] == target:
                item["sha256"] = digest
    rewrite_json(donor / "OVERLAY_SOURCES.json", overlays)
    record, record_digest = DONOR_REL + "/OVERLAY_SOURCES.json", sha256(donor / "OVERLAY_SOURCES.json")

    def provenance(value):
        for item in value["files"]:
            if item["repository"] == target:
                item["sha256"] = digest
            if item["repository"] == record:
                item["sha256"] = record_digest
    rewrite_json(donor / "PROVENANCE.json", provenance)


def drop_overlay_record(donor: Path) -> None:
    rewrite_json(donor / "OVERLAY_SOURCES.json", lambda value: value.pop("m3_overlays"))
    repin(donor, "OVERLAY_SOURCES.json")


def tamper(scratch: Path) -> list[dict]:
    probes = [
        ("pristine copy", None, None),
        ("overlay byte changed", lambda d, t: (d / OVERLAY).write_bytes((d / OVERLAY).read_bytes() + TAMPER_NOTE),
         "Donor bytes changed"),
        ("retained original changed", lambda d, t: (d / RETAINED).write_bytes((d / RETAINED).read_bytes() + TAMPER_NOTE),
         "Donor bytes changed"),
        ("recipe changed", lambda d, t: t.write_bytes(t.read_bytes() + TAMPER_NOTE.replace(b"//", b"#")),
         "M3 overlay recipe changed"),
        ("overlay record dropped, re-pinned", lambda d, t: drop_overlay_record(d), "M3 overlays differ"),
        ("overlay replaced by original, re-pinned",
         lambda d, t: ((d / OVERLAY).write_bytes((d / RETAINED).read_bytes()), repin(d, OVERLAY)),
         "is not its recipe's output"),
        ("unpinned production file", lambda d, t: (d / STRAY).write_text("class M3Stray {}\n", encoding="utf-8"),
         "Production donor inventory changed"),
    ]
    rows = []
    for label, mutate, expected in probes:
        root = scratch / "tamper"
        shutil.rmtree(root, ignore_errors=True)
        donor, tool = root / DONOR_REL, root / TOOL_REL
        shutil.copytree(REPO / DONOR_REL, donor)
        tool.parent.mkdir(parents=True)
        shutil.copyfile(REPO / TOOL_REL, tool)
        if mutate:
            mutate(donor, tool)
        code, text = run([sys.executable, "-I", donor / "verify_source.py"])
        ok = code == 0 if expected is None else code != 0 and expected in text
        rows.append({"probe": label, "exit": code, "refusal": text.splitlines()[-1] if text else "", "ok": ok})
    shutil.rmtree(scratch / "tamper", ignore_errors=True)
    return rows


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    args.out.mkdir(parents=True, exist_ok=True)
    verify_code, verify = run([sys.executable, "-I", REPO / DONOR_REL / "verify_source.py"])
    check_code, check = run([sys.executable, "-I", REPO / TOOL_REL, "--check"])
    head = git("rev-parse", "HEAD")[1]
    matches_head = git("diff", "--quiet", "HEAD", "--", DONOR_REL, PACK_REL, TOOL_REL)[0] == 0
    record = {"gate": "d_custody", "verify_source": {"exit": verify_code, "receipt": verify},
              "recipe_fixed_point": {"exit": check_code, "result": check},
              "jackson_pack_lock": lock_check(), "central_crate_rule": crate_rule(), "tamper_probes": tamper(args.out),
              "repository_head": head, "worktree_matches_head": matches_head}
    passed = (verify_code == 0 and check_code == 0
              and all(row["ok"] for key in ("jackson_pack_lock", "central_crate_rule", "tamper_probes")
                      for row in record[key]))
    record["status"] = "PASS" if passed else "FAIL"
    (args.out / "custody.json").write_text(json.dumps(record, indent=1, sort_keys=True) + "\n", encoding="utf-8",
                                          newline="\n")
    print(json.dumps(record, sort_keys=True))
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())

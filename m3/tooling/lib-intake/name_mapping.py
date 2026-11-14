#!/usr/bin/env python3
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""LIB_INTAKE name-mapping record (template-first): one m3/docs/name-mapping.d/lib-intake-<lib>.json per library whose
intake run passed every stage, in the shape of the existing name-mapping.d records (schema, product, intake mode, target
module and path, recipe owner, source, runtime packages, mappings, receipts, legal notice). Values come only from the run
ledgers (RUN_CONTEXT.tsv, RELOCATION_MAP.tsv, STAGES.tsv) and the vendored UPSTREAM_LICENSE.json / PROVENANCE.json;
nothing is typed by hand. Usage (repository root): name_mapping.py [lib ...]   (default: every runs/<lib> with S8 PASS)"""
import csv, json, os, sys

RUNS = "m3/tooling/lib-intake/runs"
OUT = "m3/docs/name-mapping.d"


def tsv(path):
    with open(path, encoding="utf-8") as f:
        return list(csv.reader(f, delimiter="\t"))


def record(lib):
    run = RUNS + "/" + lib
    stages = tsv(os.path.join(run, "STAGES.tsv"))
    if not any(r[:2] == ["S8_LAYER", "PASS"] for r in stages):
        return None
    ctx = {r[0]: r[1] for r in tsv(os.path.join(run, "RUN_CONTEXT.tsv"))[1:] if len(r) > 1}
    moves = [r for r in tsv(os.path.join(run, "RELOCATION_MAP.tsv"))[1:] if len(r) > 2 and r[2] == "MOVE"]
    src = ctx["source"]
    lic = {}
    for name in ("UPSTREAM_LICENSE.json", "PROVENANCE.json"):
        p = os.path.join(src, name)
        if os.path.isfile(p):
            with open(p, encoding="utf-8") as f:
                lic[name] = json.load(f)
    up = lic.get("UPSTREAM_LICENSE.json", {})
    prov = lic.get("PROVENANCE.json", {})
    roots = [m.split("=", 1) for m in ctx["mappings"].split(";")]
    return {
        "schema": "M3_LIB_INTAKE_NAME_MAPPING_V1",
        "copyright": "Copyright 2026 Hitesh Soliwal and contributors (M3 modifications)",
        "license": (up.get("pom_license") or "upstream LICENSE in legal_notice (" + up.get("origin", "vendored source") + ")") + "; original notices retained",
        "product": "M3JDK21",
        "intake_mode": "LIB_INTAKE_TO_JMOD_V2",
        "target_module": ctx["module"],
        "target_path": ctx["layer"],
        "canonical_recipe": "m3/tooling/lib-intake/lib_intake.sh (pattern LIB_INTAKE_TO_JMOD v2)",
        "canonical_recipe_owners": ["org.openrewrite.java.ChangePackage (recursive=false, longest first; boundary-checked form of com.synexia M3BoundaryCheckedPackageRenameRecipe)"],
        "source_path": src,
        "source_coordinate": prov.get("coordinate", ""),
        "source_version": ctx["version"],
        "source_license_origin": up.get("origin", ""),
        "source_packages": [o for o, _ in roots],
        "runtime_packages": [t for _, t in roots],
        "intake_dependencies": ctx.get("intake_deps", "-"),
        "qualification_receipt": f"{run}/STAGES.tsv",
        "relocation_map": f"{run}/RELOCATION_MAP.tsv",
        "legal_notice": ctx["layer"],
        "mappings": [{"source": o + " and subpackages", "target": t + " and subpackages",
                      "role": "relocated donor API in the optional " + ctx["module"] + " JMOD; type names retained"} for o, t in roots]
                    + [{"source": m[0], "target": m[1], "role": "package (ChangePackage)"} for m in moves],
    }


def main():
    libs = sys.argv[1:] or sorted(os.listdir(RUNS))
    os.makedirs(OUT, exist_ok=True)
    n = 0
    for lib in libs:
        if not os.path.isfile(os.path.join(RUNS, lib, "STAGES.tsv")):
            continue
        rec = record(lib)
        if rec is None:
            print(f"SKIP {lib}: not every stage passed")
            continue
        with open(os.path.join(OUT, f"lib-intake-{lib}.json"), "w", encoding="utf-8", newline="\n") as w:
            json.dump(rec, w, indent=2, ensure_ascii=False)
            w.write("\n")
        n += 1
    print(f"NAME_MAPPING records={n}")


if __name__ == "__main__":
    main()

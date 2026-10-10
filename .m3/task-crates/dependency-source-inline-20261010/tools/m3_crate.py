#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""M3jdk21 receiver crate for DEPENDENCY-SOURCE-INLINE: `.m3/task-crates/dependency-source-inline-20261010/`
in the M3jdk21 tree (README, evidence.json, THIRD_PARTY_MODULES.tsv, BUILD_GATE.tsv, FOSS_REUSE_DECISION.tsv,
THIRD_PARTY_NOTICES.md, DOWNSTREAM_DEPENDENCY_LEDGER.tsv, verify_crate.py) plus the m3/vendor/third_party
README. The downstream ledger lists every remaining binary dependency of the M3jdk21 tooling poms (user rule:
"jdk cannot have downstream dependency") with its vendored-source status and removal path. CPU only."""
from __future__ import annotations
import argparse, csv, hashlib, io, json, re, shutil, sys, time
from pathlib import Path

CRATE = ".m3/task-crates/dependency-source-inline-20261010"
SUFFIX = "-m3-source-1"
DEP_RE = re.compile(r"<dependency>(.*?)</dependency>", re.S)


def tsv(p: Path):
    return list(csv.DictReader(io.open(p, encoding="utf-8", newline=""), delimiter="\t"))


def sha256(p: Path) -> str:
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", type=Path, required=True, help="M3jdk21 worktree")
    ap.add_argument("--c6", type=Path, required=True)
    ap.add_argument("--synexia-crate", type=Path, required=True)
    ap.add_argument("--synexia-pr", default="(pending)")
    ap.add_argument("--baseline", required=True)
    ap.add_argument("--consumers", type=Path, required=True, help="file listing the consumer pom paths (repo-relative)")
    args = ap.parse_args()
    crate = args.repo / CRATE
    if crate.exists():
        shutil.rmtree(crate)
    crate.mkdir(parents=True)
    mods = tsv(args.c6 / "M3_THIRD_PARTY_MODULES.tsv")
    gate = tsv(args.c6 / "M3_BUILD_GATE.tsv") if (args.c6 / "M3_BUILD_GATE.tsv").exists() else []
    switch = tsv(args.c6 / "M3_CONSUMER_SWITCH.tsv") if (args.c6 / "M3_CONSUMER_SWITCH.tsv").exists() else []
    for n in ("M3_THIRD_PARTY_MODULES.tsv", "M3_BUILD_GATE.tsv", "M3_CONSUMER_SWITCH.tsv", "candidates_t7.tsv", "DEPENDENCY_INVENTORY_T7.tsv", "UNRESOLVED_IMPORTS.tsv", "BUILD_INPUTS.tsv"):
        if (args.c6 / n).exists():
            shutil.copyfile(args.c6 / n, crate / n.replace("M3_", ""))
    (crate / "tools").mkdir()
    for n in ("gen_third_party.py", "build_modules.py", "switch_consumers.py", "m3_crate.py", "ModuleCompile.java", "make_lock2.py"):
        shutil.copyfile(args.c6 / n, crate / "tools" / n)
    # synexia-side evidence pointers (not copied: the crate in the Synexia PR is canonical)
    syn_ev = json.loads((args.synexia_crate / "evidence.json").read_text(encoding="utf-8"))
    # downstream dependency ledger: every non-plugin third-party dependency of the consumer poms
    by_coord = {}
    for m in mods:
        v = m["version"][:-len(SUFFIX)] if m["version"].endswith(SUFFIX) else m["version"]
        by_coord[(m["group"], m["artifact"], v)] = m
    rows = []
    for rel in [l.strip() for l in args.consumers.read_text(encoding="utf-8").splitlines() if l.strip()]:
        pom = args.repo / rel
        if not pom.exists():
            rows.append((rel, "", "", "", "POM_NOT_IN_WORKTREE", "")); continue
        text = pom.read_text(encoding="utf-8")
        props = {"rewrite.version": "8.17.1", "openrewrite.version": "8.17.1"}  # parent-declared in m3/tooling (fallback when the child pom does not restate it)
        props.update(dict(re.findall(r"<([A-Za-z0-9._-]+\.version)>([^<]*)</\1>", text)))
        plugin_spans = [(m.start(), m.end()) for m in re.finditer(r"<plugin>.*?</plugin>", text, re.S)]
        for m in DEP_RE.finditer(text):
            if any(a <= m.start() < b for a, b in plugin_spans):
                continue
            g = re.search(r"<groupId>([^<]*)</groupId>", m.group(1)); a = re.search(r"<artifactId>([^<]*)</artifactId>", m.group(1)); v = re.search(r"<version>([^<]*)</version>", m.group(1))
            if not (g and a):
                continue
            gv, av = g.group(1).strip(), a.group(1).strip()
            if gv.startswith("com.m3") or gv.startswith("com.synexia") or "junit" in gv or "opentest4j" in gv or gv == "org.assertj" or gv == "org.hamcrest":
                continue
            raw = v.group(1).strip() if v else ""
            mm = re.fullmatch(r"\$\{([^}]+)\}", raw)
            eff = props.get(mm.group(1), raw) if mm else raw
            eff_plain = eff[:-len(SUFFIX)] if eff.endswith(SUFFIX) else eff
            mod = by_coord.get((gv, av, eff_plain))
            if mod is None:
                status, path = "NOT_VENDORED", ""
            elif mod["kind"] == "MODULE":
                status, path = ("VENDORED_SOURCE_BUILD" + ("+SWITCHED" if eff.endswith(SUFFIX) else "+BINARY_STILL_RESOLVED")), mod["path"]
            else:
                status, path = "VENDORED_IMAGE_" + mod["status"] + " (" + mod["reason"] + ")", mod["path"]
            removal = ""
            if av.startswith("rewrite-"):
                removal = "next leaf: OpenRewrite source tree (Lombok build tool + micrometer, classgraph, java-object-diff, jackson smile/paramnames, kotlin stdlib, groovy, antlr4; see UNRESOLVED_IMPORTS.tsv)"
            elif status == "NOT_VENDORED":
                removal = "inventory + vendor in a follow-up pass"
            rows.append((rel, gv, av, eff, status, path, removal))
    (crate / "DOWNSTREAM_DEPENDENCY_LEDGER.tsv").write_text("\n".join(["pom\tgroup\tartifact\teffective_version\tstatus\tvendored_path\tremoval_path"] + ["\t".join(r + ("",) * (7 - len(r))) for r in rows]) + "\n", encoding="utf-8", newline="\n")
    # FOSS ledger
    frows = []
    for m in mods:
        v = m["version"][:-len(SUFFIX)] if m["version"].endswith(SUFFIX) else m["version"]
        central = "https://repo.maven.apache.org/maven2/" + m["group"].replace(".", "/") + "/" + m["artifact"] + "/" + v + "/"
        lic = ""
        pj = args.repo / m["path"] / "PROVENANCE.json"
        if pj.exists():
            lic = json.loads(pj.read_text(encoding="utf-8")).get("license", "")
        decision = "VENDOR_BYTE_FOR_BYTE+SOURCE_BUILD" if m["kind"] == "MODULE" else "VENDOR_BYTE_FOR_BYTE_IMAGE"
        fit = "PROVEN_COMPILE_FROM_SOURCE (donor-superset %s; standalone Maven gate %s)" % (m["status"], next((g["result"] for g in gate if g["module"] == m["path"]), "-")) if m["kind"] == "MODULE" else "UNPROVEN (%s)" % m["status"]
        frows.append(("m3-tooling.dependency-source-inline." + m["artifact"] + "." + v, central + m["artifact"] + "-" + v + "-sources.jar", "sources-jar (SHA-1 in PROVENANCE.json)", m["path"] + "/**", lic + ":CODE_REUSE_ALLOWED_WITH_ATTRIBUTION", fit, decision, m["reason"], m["path"] + "/PROVENANCE.json", m["path"] + "/THIRD_PARTY_NOTICES.md"))
    (crate / "FOSS_REUSE_DECISION.tsv").write_text("\n".join(["capability\tcandidate_repo\tcommit\tsource_paths\tlicense\tcontract_fit\tdecision\trejected_reason\tadapted_hashes\tnotice_path"] + ["\t".join(r) for r in frows]) + "\n", encoding="utf-8", newline="\n")
    notices = ["# Third-party notices: vendored tooling dependencies (m3/vendor/third_party, 2026-10-10)", "",
               "Every directory below retains the unchanged published sources of the named artifact with its upstream license and a", "PROVENANCE.json pinning each file by SHA-256. The Maven descriptors are M3jdk21 composition (Apache-2.0) and do not relicense",
               "any donor. Nothing here enters src/** (GPLv2+CPE); these are build-time tooling inputs only.", "", "| donor | license | status | path |", "|---|---|---|---|"]
    for m in mods:
        pj = args.repo / m["path"] / "PROVENANCE.json"
        lic = json.loads(pj.read_text(encoding="utf-8")).get("license", "") if pj.exists() else ""
        notices.append("| %s:%s:%s | %s | %s | %s |" % (m["group"], m["artifact"], m["version"], lic, m["status"], m["path"]))
    (crate / "THIRD_PARTY_NOTICES.md").write_text("\n".join(notices) + "\n", encoding="utf-8", newline="\n")
    counts = {"modules": sum(1 for m in mods if m["kind"] == "MODULE"), "images": sum(1 for m in mods if m["kind"] == "IMAGE"), "gate_pass": sum(1 for g in gate if g["result"] == "PASS"), "gate": len(gate),
              "switched": sum(1 for s in switch if s["action"] == "SWITCHED"), "downstream_rows": len(rows), "downstream_not_vendored": sum(1 for r in rows if r[4] == "NOT_VENDORED"), "downstream_binary_still_resolved": sum(1 for r in rows if "BINARY_STILL_RESOLVED" in r[4] or r[4].startswith("VENDORED_IMAGE"))}
    evidence = {"schema": "m3.dependency-source-inline.receiver.v1", "generated": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "baseline": args.baseline, "synexia_pr": args.synexia_pr,
                "synexia_crate": {"path": ".m3/task-crates/dependency-source-inline-20261010 (hsoliwal/com.synexia)", "evidence_sha256": sha256(args.synexia_crate / "evidence.json"), "counts": syn_ev["counts"]},
                "user_directives": ["pull all thriepart code and inline ands refactor it", "inline and refactor all third party code ... jdk cannot have downstream dependency"], "counts": counts}
    (crate / "evidence.json").write_text(json.dumps(evidence, indent=2, sort_keys=True) + "\n", encoding="utf-8", newline="\n")
    verify = '''#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the vendored third-party tree: every file pinned by a PROVENANCE.json under m3/vendor/third_party*
is byte-identical to its pin and every module pom carries the -m3-source-1 version."""
import csv, hashlib, io, json, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]


def sha256(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    bad = 0
    rows = list(csv.DictReader(io.open(HERE / "THIRD_PARTY_MODULES.tsv", encoding="utf-8", newline=""), delimiter="\\t"))
    for r in rows:
        d = ROOT / r["path"]
        prov = json.loads((d / "PROVENANCE.json").read_text(encoding="utf-8"))
        for f in prov["files"]:
            p = ROOT / f["repository"]
            if not p.is_file() or sha256(p) != f["sha256"]:
                print("DRIFT", f["repository"]); bad += 1
        if r["kind"] == "MODULE" and "<version>%s</version>" % r["version"] not in (d / "pom.xml").read_text(encoding="utf-8"):
            print("POM_VERSION", r["path"]); bad += 1
    print("checked", len(rows), "entries", "drift", bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
'''
    (crate / "verify_crate.py").write_text(verify, encoding="utf-8", newline="\n")
    readme = f"""# dependency-source-inline-20261010 (M3jdk21 receiver)

User directives (2026-10-10): "pull all thriepart code and inline ands refactor it", then "inline and refactor all
third party code ... jdk cannot have downstream dependency".

The Synexia crate (`.m3/task-crates/dependency-source-inline-20261010` in hsoliwal/com.synexia, PR {args.synexia_pr})
is canonical: inventories, locks, donor-superset receipts, FOSS ledger, the CPU atomize/patternize projection.
This receiver vendors the third-party artifacts the M3jdk21 tooling pins directly into `m3/vendor/third_party/`
({counts["modules"]} source-built modules, `<version>{SUFFIX}`, standalone Maven gate {counts["gate_pass"]}/{counts["gate"]}) and
`m3/vendor/third_party-images/` ({counts["images"]} byte-for-byte images that cannot compile without their platform:
OpenRewrite needs Lombok at source level plus a dependency tree that is not yet inlined).

`DOWNSTREAM_DEPENDENCY_LEDGER.tsv` lists every remaining third-party dependency of the tooling poms with its vendored
status and removal path ({counts["downstream_rows"]} rows; {counts["downstream_binary_still_resolved"]} still resolve a published binary,
{counts["downstream_not_vendored"]} not yet vendored). Nothing here enters `src/**` (GPLv2+CPE); the JDK runtime has no
third-party dependency. The remaining downstream dependency of the *tooling* is the OpenRewrite 8.17.1 engine and its
tree; its source inline is the next leaf.

Verify: `python -I {CRATE}/verify_crate.py`; build: `mvn -B -ntp -f m3/vendor/third_party/pom.xml install` (aggregator).
"""
    (crate / "README.md").write_text(readme, encoding="utf-8", newline="\n")
    # aggregator pom for the vendored modules
    vendor = args.repo / "m3" / "vendor" / "third_party"
    modules = sorted(m["path"].split("/")[-1] for m in mods if m["kind"] == "MODULE")
    agg = """<!-- SPDX-License-Identifier: Apache-2.0; aggregator only, every module keeps its upstream license. -->
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.m3.vendor</groupId><artifactId>third-party-sources</artifactId><version>1.0.0-SNAPSHOT</version><packaging>pom</packaging>
  <name>M3jdk21 vendored third-party sources (build-time tooling inputs)</name>
  <modules>
""" + "".join("    <module>%s</module>\n" % m for m in modules) + """  </modules>
</project>
"""
    (vendor / "pom.xml").write_text(agg, encoding="utf-8", newline="\n")
    (vendor / "README.md").write_text(f"""# m3/vendor/third_party

Unchanged published sources of the third-party artifacts the M3jdk21 build-time tooling pins, vendored so the JDK
repository carries the code itself ("jdk cannot have downstream dependency"). Each module keeps its upstream license,
PROVENANCE.json and notices; the Maven descriptors are M3jdk21 composition (Apache-2.0). Coordinates:
`<group>:<artifact>:<version>{SUFFIX}`. Build all: `mvn -B -ntp -f m3/vendor/third_party/pom.xml install`.
Images that cannot compile from their sources jar live in `m3/vendor/third_party-images/`. Ledger and verifier:
`{CRATE}/`. Nothing here enters src/**.
""", encoding="utf-8", newline="\n")
    print("m3 crate written", crate, json.dumps(counts, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())

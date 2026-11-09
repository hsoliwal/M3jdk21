#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""fdp-delegate gates: (a) compile, (b) FDP differential, (c) Jackson differential, optionally on M3 java.base.

  python run_gates.py --jdk <jdk21> --out <dir> [--gates a,b,c] [--count N] [--json-count N] [--m3-src <tree>]

From the repository checkout only, it builds
  fdp-upstream.jar  FastDoubleParser 1.0.90 with the retained upstream JavaDoubleParser/JavaFloatParser,
  fdp-overlay.jar   FastDoubleParser 1.0.90 as committed (the M3 overlays),
as multi-release jars (base release 8 plus versions 11/17/21, the donor pom's layout), then jackson-core 2.19.2
against each (javac over its complete vendored closure: FastDoubleParser is its only dependency).
--m3-src <M3JDK tree> compiles jdk/internal/math (FloatingDecimal + M3*) with --patch-module java.base and repeats
(b) and (c) on that java.base, where Double.parseDouble/Float.parseFloat run the absorbed FastDoubleParser.
Each gate appends its JSON record to <out>/RESULT.jsonl; nothing is written into the repository.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

REPO = Path(__file__).resolve().parents[4]
TOOLS = Path(__file__).resolve().parent
DONOR = REPO / "m3/vendor/third_party/fastdoubleparser-1.0.90"
JACKSON = REPO / "m3/vendor/third_party/jackson-core-2.19.2"
PACKAGE = "ch/randelshofer/fastdoubleparser"
OVERLAID = ("JavaDoubleParser.java", "JavaFloatParser.java")
BASE_RELEASE = "8"
VERSION_RELEASES = ("11", "17", "21")
MULTI_RELEASE_ROOT = "META-INF/versions"
MULTI_RELEASE_MANIFEST = "Manifest-Version: 1.0\nMulti-Release: true\n"
HARNESS_RELEASE = "21"
VARIANTS = ("upstream", "overlay")
M3_MATH = "src/java.base/share/classes/jdk/internal/math"
M3_SOURCES = ("FloatingDecimal.java", "M3*.java")
CORPUS = ("M3FdpParseCorpus.java",)
FDP_HARNESS = "M3FdpDelegateDifferential"
JACKSON_HARNESS = "M3JacksonFastDoubleDifferential"
REFEREE = "verify_rounding.py"
DEFAULT_COUNT = 250_000
DEFAULT_JSON_COUNT = 200_000
MIN_FREE_BYTES = 600 * 1024 * 1024
JAVAC_RECORD = re.compile(r"^(?P<file>.+?):(?P<line>\d+): (?P<severity>error|warning): (?P<message>.*)$")
JAVAC_TOTAL = re.compile(r"^(?P<count>\d+) (?P<severity>error|warning)s?$")
RESULT_LINE = re.compile(r"^RESULT\t(?P<status>PASS|FAIL)\b.*$", re.MULTILINE)
DIGEST_LINE = re.compile(r"^DIGEST\t(?P<digests>.*)$", re.MULTILINE)


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(args: list[object], log: Path) -> tuple[int, str]:
    rendered = [str(item) for item in args]
    result = subprocess.run(rendered, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    text = result.stdout.decode("utf-8", "replace")
    log.write_text(" ".join(rendered) + "\n" + text, encoding="utf-8", newline="\n")
    return result.returncode, text


def bllse(text: str) -> dict:
    """javac output -> structured records (file, line, severity, message) and totals; never prose."""
    records = [match.groupdict() for match in map(JAVAC_RECORD.match, text.splitlines()) if match]
    totals = {match["severity"]: int(match["count"]) for match in map(JAVAC_TOTAL.match, text.splitlines()) if match}
    return {"errors": totals.get("error", 0), "warnings": totals.get("warning", 0), "records": records[:20]}


def javac(jdk: Path, release: str, out: Path, sources: list[Path], log: Path, classpath: str | None = None,
          extra: list[str] | None = None) -> dict:
    out.mkdir(parents=True, exist_ok=True)
    argfile = log.with_suffix(".args")
    argfile.write_text("\n".join('"' + str(p).replace("\\", "/") + '"' for p in sources) + "\n",
                       encoding="utf-8", newline="\n")
    args = [jdk / "bin/javac", "--release", release, "-encoding", "UTF-8", "-Xlint:none", "-d", out]
    args += (["-cp", classpath] if classpath else []) + (extra or []) + ["@" + str(argfile)]
    code, text = run(args, log)
    record = bllse(text)
    record.update({"exit": code, "sources": len(sources), "classes": sum(1 for _ in out.rglob("*.class"))})
    if code:
        raise RuntimeError(f"javac failed ({code}); see {log}")
    return record


def fdp_sources(variant: str) -> list[Path]:
    sources = sorted((DONOR / "src/main/java").rglob("*.java"))
    if variant == "overlay":
        return sources
    retained = DONOR / "upstream-excluded" / PACKAGE
    return [retained / p.name if p.name in OVERLAID and p.parent.name == PACKAGE.rsplit("/", 1)[-1] else p
            for p in sources]


def build_fdp(jdk: Path, out: Path, variant: str, logs: Path) -> dict:
    root = out / ("fdp-" + variant)
    base = root / "base"
    record = {"base": javac(jdk, BASE_RELEASE, base, fdp_sources(variant), logs / f"javac-fdp-{variant}.log")}
    for release in VERSION_RELEASES:
        sources = sorted((DONOR / "src/main/versions" / release).rglob("*.java"))
        record["v" + release] = javac(jdk, release, base / MULTI_RELEASE_ROOT / release, sources,
                                      logs / f"javac-fdp-{variant}-{release}.log", classpath=str(base))
    # The donor pom (maven-jar-plugin) packs versions/N classes as-is; jar --release would refuse FastDoubleSwar,
    # whose versioned classes differ in package-private API, so the manifest attribute is set directly.
    manifest = root / "MANIFEST.MF"
    manifest.write_text(MULTI_RELEASE_MANIFEST, encoding="utf-8", newline="\n")
    jar = out / f"fdp-{variant}.jar"
    code, _ = run([jdk / "bin/jar", "--create", "--file", jar, "--manifest", manifest, "-C", base, "."],
                  logs / f"jar-{variant}.log")
    if code:
        raise RuntimeError("jar failed for " + variant)
    record.update({"jar": jar.name, "jar_sha256": sha256_file(jar)})
    return record


def gate_compile(jdk: Path, out: Path, logs: Path) -> dict:
    record = {"gate": "a_compile", "fdp": {}, "jackson": {}}
    sources = sorted((JACKSON / "src/main/java").rglob("*.java"))
    for variant in VARIANTS:
        record["fdp"][variant] = build_fdp(jdk, out, variant, logs)
        record["jackson"][variant] = javac(jdk, BASE_RELEASE, out / ("jackson-" + variant), sources,
                                           logs / f"javac-jackson-{variant}.log", classpath=str(out / f"fdp-{variant}.jar"))
    harness = [TOOLS / name for name in CORPUS]
    record["harness_fdp"] = javac(jdk, HARNESS_RELEASE, out / "harness-fdp", harness + [TOOLS / (FDP_HARNESS + ".java")],
                                  logs / "javac-harness-fdp.log")
    record["harness_jackson"] = javac(jdk, HARNESS_RELEASE, out / "harness-jackson",
                                      harness + [TOOLS / (JACKSON_HARNESS + ".java")], logs / "javac-harness-jackson.log",
                                      classpath=str(out / "jackson-overlay"))
    record["status"] = "PASS"
    return record


def m3_patch(jdk: Path, out: Path, source_tree: Path, logs: Path) -> tuple[Path, dict]:
    stage = out / "m3-src/jdk/internal/math"
    stage.mkdir(parents=True, exist_ok=True)
    files = sorted({p for pattern in M3_SOURCES for p in (source_tree / M3_MATH).glob(pattern)})
    pins = {}
    for path in files:
        shutil.copyfile(path, stage / path.name)
        pins[path.name] = sha256_file(path)
    patch = out / "m3-patch"
    record = javac(jdk, HARNESS_RELEASE, patch, sorted(stage.glob("*.java")), logs / "javac-m3-patch.log",
                   extra=["--patch-module", "java.base=" + str(out / "m3-src")])
    record.pop("exit")
    return patch, {"files": pins, "javac": record}


def vm(jdk: Path, patch: Path | None) -> list[object]:
    return [jdk / "bin/java"] + (["--patch-module", "java.base=" + str(patch)] if patch else [])


def gate_fdp(jdk: Path, out: Path, logs: Path, count: int, patch: Path | None, label: str) -> dict:
    report = out / f"fdp-differential-{label}.tsv"
    bits = out / f"fdp-differential-{label}.bits.tsv"
    code, text = run(vm(jdk, patch) + ["-cp", out / "harness-fdp", FDP_HARNESS, out / "fdp-upstream.jar",
                                       out / "fdp-overlay.jar", count, report, bits],
                     logs / f"fdp-differential-{label}.log")
    status = RESULT_LINE.search(text)
    record = {"gate": "b_fdp_differential_" + label, "exit": code, "status": status["status"] if status else "FAIL",
              "report": report.name, "report_sha256": sha256_file(report) if report.is_file() else None,
              "summary": [line for line in text.splitlines() if line and not line.startswith("#")]}
    if bits.is_file() and bits.stat().st_size:
        # Raw-bits differences go to the exact-arithmetic referee: which side rounds each literal correctly?
        referee_code, referee = run([sys.executable, "-I", TOOLS / REFEREE, bits], logs / f"referee-{label}.log")
        record.update({"bits_differences": bits.name, "referee_exit": referee_code,
                       "referee": json.loads(referee[referee.index("["):])})
    return record


def gate_jackson(jdk: Path, out: Path, logs: Path, count: int, patch: Path | None, label: str) -> dict:
    record = {"gate": "c_jackson_differential_" + label, "builds": {}}
    for variant in VARIANTS:
        report = out / f"jackson-differential-{label}-{variant}.tsv"
        classpath = os.pathsep.join(str(p) for p in (out / "harness-jackson", out / ("jackson-" + variant),
                                                    out / f"fdp-{variant}.jar"))
        code, text = run(vm(jdk, patch) + ["-cp", classpath, JACKSON_HARNESS, count, report],
                         logs / f"jackson-differential-{label}-{variant}.log")
        status, digest = RESULT_LINE.search(text), DIGEST_LINE.search(text)
        record["builds"][variant] = {"exit": code, "status": status["status"] if status else "FAIL",
                                     "digests": digest["digests"] if digest else None, "report": report.name}
    builds = record["builds"].values()
    same = len({b["digests"] for b in builds}) == 1
    record.update({"digests_equal_across_builds": same,
                   "status": "PASS" if same and all(b["status"] == "PASS" for b in builds) else "FAIL"})
    return record


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--gates", default="a,b,c")
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT)
    parser.add_argument("--json-count", type=int, default=DEFAULT_JSON_COUNT)
    parser.add_argument("--m3-src", type=Path, help="M3JDK source tree whose java.base number parser to patch in")
    args = parser.parse_args(argv)
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if shutil.disk_usage(out).free < MIN_FREE_BYTES:
        raise SystemExit("GATES_REFUSED: less than 600 MB free on the output drive")
    logs = out / "logs"
    logs.mkdir(exist_ok=True)
    gates = args.gates.split(",")
    records = []
    if "a" in gates:
        records.append(gate_compile(args.jdk, out, logs))
    patch = None
    if args.m3_src:
        patch, provenance = m3_patch(args.jdk, out, args.m3_src.resolve(), logs)
        records.append({"gate": "m3_patch_compile", "status": "PASS", **provenance})
    labels = [("stock", None)] + ([("m3", patch)] if patch else [])
    for label, vm_patch in labels:
        if "b" in gates:
            records.append(gate_fdp(args.jdk, out, logs, args.count, vm_patch, label))
        if "c" in gates:
            records.append(gate_jackson(args.jdk, out, logs, args.json_count, vm_patch, label))
    with (out / "RESULT.jsonl").open("a", encoding="utf-8", newline="\n") as result:
        for record in records:
            record["repository_head"] = subprocess.run(["git", "-C", str(REPO), "rev-parse", "HEAD"],
                                                       capture_output=True, text=True).stdout.strip()
            result.write(json.dumps(record, sort_keys=True) + "\n")
            print(json.dumps({k: record[k] for k in ("gate", "status")}, sort_keys=True))
    return 0 if all(record["status"] == "PASS" for record in records) else 1


if __name__ == "__main__":
    raise SystemExit(main())

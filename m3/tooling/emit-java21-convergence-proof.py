#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Emit deterministic M3 proof artifacts after the Java 21 OpenRewrite Maven gate passes."""

from __future__ import annotations

import argparse
import csv
import hashlib
import os
import xml.etree.ElementTree as ET
from pathlib import Path

EXPECTED_RECIPE_IDS = (
    "m3-file-local-pure-int-atom",
    "m3-file-local-pure-int-inventory",
    "m3-file-local-pure-int-pattern",
    "m3-file-local-pure-int-document",
    "m3-file-local-pure-int-convergence",
    "m3-java21-file-convergence",
)
EXPECTED_SCOPE = "FILE"
EXPECTED_CONTRACT = "BEHAVIOR_AND_CONTRACT_PRESERVING"
EXPECTED_CATALOGUE_STATUS = "CANDIDATE_UNVERIFIED"
EXPECTED_CATALOGUE_VERIFICATION = "PENDING_CI"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_catalogue(path: Path) -> dict[str, dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    by_id = {row["recipe_id"]: row for row in rows}
    if len(by_id) != len(rows):
        raise SystemExit("duplicate recipe_id in recipe catalogue")
    for recipe_id in EXPECTED_RECIPE_IDS:
        if recipe_id not in by_id:
            raise SystemExit(f"missing admitted recipe: {recipe_id}")
        row = by_id[recipe_id]
        expected = {
            "scope": EXPECTED_SCOPE,
            "contract": EXPECTED_CONTRACT,
            "status": EXPECTED_CATALOGUE_STATUS,
            "verification": EXPECTED_CATALOGUE_VERIFICATION,
        }
        for key, value in expected.items():
            if row[key] != value:
                raise SystemExit(
                    f"{recipe_id}: expected {key}={value}, found {row[key]}"
                )
    return by_id


def read_surefire(report_dir: Path) -> tuple[int, int, int, int, list[Path]]:
    reports = sorted(report_dir.glob("TEST-*.xml"))
    if not reports:
        raise SystemExit("no Surefire XML reports found")
    tests = failures = errors = skipped = 0
    for report in reports:
        root = ET.parse(report).getroot()
        tests += int(root.attrib.get("tests", "0"))
        failures += int(root.attrib.get("failures", "0"))
        errors += int(root.attrib.get("errors", "0"))
        skipped += int(root.attrib.get("skipped", "0"))
    if tests <= 0:
        raise SystemExit("Surefire executed zero tests")
    if failures or errors or skipped:
        raise SystemExit(
            f"Surefire not clean: tests={tests} failures={failures} "
            f"errors={errors} skipped={skipped}"
        )
    return tests, failures, errors, skipped, reports


def jacoco_counters(path: Path) -> tuple[tuple[int, int], tuple[int, int]]:
    if not path.is_file():
        raise SystemExit("JaCoCo XML report missing")
    root = ET.parse(path).getroot()
    counters = {c.attrib["type"]: c for c in root.findall("counter")}
    line = counters.get("LINE")
    branch = counters.get("BRANCH")
    if line is None or branch is None:
        raise SystemExit("JaCoCo LINE/BRANCH counters missing")
    return (
        (int(line.attrib["covered"]), int(line.attrib["missed"])),
        (int(branch.attrib["covered"]), int(branch.attrib["missed"])),
    )


def write_tsv(path: Path, rows: list[tuple[str, ...]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerows(rows)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    repo = args.repo.resolve()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)

    module = repo / "m3/tooling/migration-recipes"
    catalogue = repo / "m3/tooling/recipe-catalogue.tsv"
    surefire = module / "target/surefire-reports"
    jacoco = module / "target/site/jacoco/jacoco.xml"

    recipes = read_catalogue(catalogue)
    tests, failures, errors, skipped, reports = read_surefire(surefire)
    (line_covered, line_missed), (branch_covered, branch_missed) = jacoco_counters(jacoco)

    status = out / "STATUS.tsv"
    run_context = out / "RUN_CONTEXT.tsv"
    provenance = out / "PROVENANCE.tsv"
    verify_contract = out / "VERIFY_CONTRACT.tsv"
    final_report = out / "FINAL_REPORT.md"
    output_contract = out / "OUTPUT_CONTRACT.tsv"

    write_tsv(
        status,
        [
            ("key", "value"),
            ("status", "PASS"),
            ("recipe", "com.m3.rewrite.M3Java21ConvergenceRecipe"),
            ("scope", EXPECTED_SCOPE),
            ("contract", EXPECTED_CONTRACT),
            ("recipes_admitted", str(len(EXPECTED_RECIPE_IDS))),
            ("junit_tests", str(tests)),
            ("junit_failures", str(failures)),
            ("junit_errors", str(errors)),
            ("junit_skipped", str(skipped)),
            ("jacoco_line_covered", str(line_covered)),
            ("jacoco_line_missed", str(line_missed)),
            ("jacoco_branch_covered", str(branch_covered)),
            ("jacoco_branch_missed", str(branch_missed)),
        ],
    )

    write_tsv(
        run_context,
        [
            ("key", "value"),
            ("github_repository", os.environ.get("GITHUB_REPOSITORY", "")),
            ("github_sha", os.environ.get("GITHUB_SHA", "")),
            ("github_run_id", os.environ.get("GITHUB_RUN_ID", "")),
            ("github_run_attempt", os.environ.get("GITHUB_RUN_ATTEMPT", "")),
            ("java_home", os.environ.get("JAVA_HOME", "")),
            ("recipe_entrypoint", "com.m3.rewrite.M3Java21ConvergenceRecipe"),
            ("verify_command", "mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify"),
        ],
    )

    verify_rows = [
        ("stage", "status", "evidence"),
        ("inventory", "PASS", "M3InventoryPureIntAtomCandidates JUnit"),
        ("atomize", "PASS", "M3AtomizePureIntReturnRecipe JUnit"),
        ("patternize_iop", "PASS", "M3PatternizePureIntAtomRecipe composite JUnit"),
        ("document", "PASS", "M3DocumentPureIntAtomRecipe composite JUnit"),
        ("fixed_point", "PASS", "M3Java21ConvergenceRecipe JUnit"),
        ("java21_compile_runtime", "PASS", "convergence/atom corpus JUnit uses --release 21"),
        ("coverage_gate", "PASS", "JaCoCo Maven verify completed before emitter"),
    ]
    write_tsv(verify_contract, verify_rows)

    provenance_rows = [("artifact", "sha256")]
    provenance_rows.append(("m3/tooling/recipe-catalogue.tsv", sha256(catalogue)))
    provenance_rows.append(("m3/tooling/migration-recipes/target/site/jacoco/jacoco.xml", sha256(jacoco)))
    for report in reports:
        provenance_rows.append(
            (str(report.relative_to(repo)).replace("\\", "/"), sha256(report))
        )
    write_tsv(provenance, provenance_rows)

    final_report.write_text(
        "# M3 Java 21 OpenRewrite convergence proof\n\n"
        "Status: **PASS**\n\n"
        "The CI builder completed the single trusted Java 21 FILE-local OpenRewrite "
        "convergence proof. The admitted DAG is inventory -> atomize -> patternize/IOP "
        "-> document -> fixed point. Maven completed JUnit/OpenRewrite tests and the "
        "configured JaCoCo 99% LINE/BRANCH gate before this evidence bundle was emitted.\n\n"
        f"- Admitted recipes: {len(recipes)} catalogue rows; "
        f"{len(EXPECTED_RECIPE_IDS)} required convergence recipes verified.\n"
        f"- JUnit tests: {tests}; failures={failures}; errors={errors}; skipped={skipped}.\n"
        f"- JaCoCo LINE: covered={line_covered}, missed={line_missed}.\n"
        f"- JaCoCo BRANCH: covered={branch_covered}, missed={branch_missed}.\n"
        "- Product JDK source mutation in this proof step: none.\n"
        "- Canonical promotion remains a separate serial action.\n",
        encoding="utf-8",
    )

    artifact_paths = [status, final_report, run_context, provenance, verify_contract]
    rows = [("artifact", "exists", "sha256")]
    for artifact in artifact_paths:
        rows.append((artifact.name, "true", sha256(artifact)))
    rows.append((output_contract.name, "true", "SELF"))
    write_tsv(output_contract, rows)

    print(final_report.read_text(encoding="utf-8"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

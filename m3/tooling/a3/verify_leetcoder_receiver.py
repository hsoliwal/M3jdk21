#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Fail-closed receiver inventory; never claims Synexia/JDK code has been qualified."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[3]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
RECIPE = "com.synexia.rewrite.M3LeetCoderShapeDispatchRecipe"
PROFILE = "m3-leetcoder-shape"
ALGORITHM = "ALG-LEETCODER-POPCOUNT"
CATALOGUE = "m3-leetcoder-shape-dispatch"
DOCUMENT = "m3/docs/leetcoder-shape-receiver-20261019.md"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError("LEETCODER_RECEIVER_REFUSED:" + message)


def text_at(element: ET.Element, xpath: str) -> str:
    value = element.findtext(xpath, namespaces=NS)
    return "" if value is None else value.strip()


def validate_profile(pom: Path) -> None:
    root = ET.parse(pom).getroot()
    profiles = [
        profile for profile in root.findall("m:profiles/m:profile", NS)
        if text_at(profile, "m:id") == PROFILE
    ]
    require(len(profiles) == 1, "EXACTLY_ONE_PROFILE")
    plugin = profiles[0].find(
        "m:build/m:plugins/m:plugin", NS
    )
    require(plugin is not None, "MISSING_PLUGIN")
    require(text_at(plugin, "m:groupId") == "org.openrewrite.maven",
            "NOT_OPENREWRITE")
    require(text_at(plugin, "m:artifactId") == "rewrite-maven-plugin",
            "WRONG_PLUGIN")
    recipes = plugin.findall(
        "m:configuration/m:activeRecipes/m:recipe", NS
    )
    require(len(recipes) == 1 and recipes[0].text == RECIPE,
            "RECIPE_IDENTITY_DRIFT")
    require(text_at(plugin, "m:configuration/m:failOnDryRunResults") == "false",
            "DRYRUN_MUST_RETAIN_CANDIDATES")
    require(text_at(plugin, "m:configuration/m:runPerSubmodule") == "false",
            "MULTI_SUBMODULE_MUTATION_REFUSED")
    executions = plugin.findall("m:executions/m:execution", NS)
    require(len(executions) == 1, "EXECUTION_COUNT")
    require(text_at(executions[0], "m:phase") == "verify",
            "NON_VERIFY_EXECUTION")
    goals = executions[0].findall("m:goals/m:goal", NS)
    require(len(goals) == 1 and goals[0].text == "dryRun",
            "MUST_DRYRUN")
    dependencies = plugin.findall("m:dependencies/m:dependency", NS)
    require(len(dependencies) == 1, "TOOL_DEPENDENCY_COUNT")
    dependency = dependencies[0]
    require(
        (
            text_at(dependency, "m:groupId"),
            text_at(dependency, "m:artifactId"),
            text_at(dependency, "m:version"),
        ) == (
            "com.synexia",
            "synexia-openrewrite-recipes",
            "1.0.0-SNAPSHOT",
        ),
        "MUST_USE_SYNEXIA_TOOL_ARTIFACT",
    )


def catalogue_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = [
            row for row in csv.DictReader(
                (line for line in handle if not line.startswith("#")),
                delimiter="\t",
            )
        ]
    require(bool(rows), "EMPTY_CATALOGUE")
    return rows


def validate_catalogues() -> None:
    recipes = catalogue_rows(ROOT / "m3/tooling/recipe-catalogue.tsv")
    matches = [row for row in recipes if row["recipe_id"] == CATALOGUE]
    require(len(matches) == 1, "RECIPE_CATALOGUE_ROW")
    row = matches[0]
    require(row["class"] == RECIPE, "RECIPE_CLASS")
    require(row["scope"] == "FILE", "NOT_FILE_LOCAL")
    require(row["status"] == "CANDIDATE_UNVERIFIED", "FALSE_SUCCESS_STATUS")
    require("PENDING" in row["verification"], "MISSING_PROOF_RESIDUE")

    algorithms = catalogue_rows(ROOT / "m3/backports/ALGORITHM_CATALOGUE.tsv")
    matches = [row for row in algorithms if row["atom_id"] == ALGORITHM]
    require(len(matches) == 1, "ALGORITHM_CATALOGUE_ROW")
    row = matches[0]
    require(
        (row["category"], row["disposition"], row["scope"],
         row["target_owner"], row["reuse_policy"])
        == ("BIT_OPERATIONS", "hold-contract", "FILE", "NONE",
            "EXTERNAL_RECIPE_TOOL"),
        "ALGORITHM_MUST_NOT_BE_PRODUCT_PROMOTED",
    )
    require(
        all(row[field].startswith("https://") for field in
            ("leetcode_ref", "hackerrank_ref", "geeksforgeeks_ref",
             "github_ref")),
        "MISSING_DONOR_EVIDENCE",
    )

    policy = (ROOT / "m3/docs/m3jdk-synexia-porting-policy.tsv").read_text(
        encoding="utf-8"
    )
    require("challenge_solution_copy\tfalse" in policy,
            "CHALLENGE_COPY_FORBIDDEN")


def verify(root: Path = ROOT) -> str:
    # The path is deliberately a fixed repository root, not an arbitrary
    # target root to rewrite; this verifier reads and hashes evidence only.
    require(root == ROOT, "UNSUPPORTED_ROOT")
    validate_profile(ROOT / "m3/pom.xml")
    validate_catalogues()
    document = ROOT / DOCUMENT
    require(document.is_file(), "MISSING_RECEIVER_POLICY")
    content = document.read_text(encoding="utf-8")
    for required in (
        "EXTERNAL_RECIPE_TOOL",
        "APPLY_CANDIDATE",
        "PENDING_JDK_BUILD_JTREG",
        "M3LeetCoderShapeDispatchRecipe",
    ):
        require(required in content, "MISSING_POLICY:" + required)
    digest = hashlib.sha256()
    for name in (
        "m3/pom.xml",
        "m3/tooling/recipe-catalogue.tsv",
        "m3/backports/ALGORITHM_CATALOGUE.tsv",
        DOCUMENT,
    ):
        data = (ROOT / name).read_bytes()
        encoded = name.encode("utf-8")
        digest.update(len(encoded).to_bytes(4, "big"))
        digest.update(encoded)
        digest.update(len(data).to_bytes(8, "big"))
        digest.update(data)
    return digest.hexdigest()


if __name__ == "__main__":
    try:
        receipt = verify()
    except (ValueError, ET.ParseError, OSError, KeyError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
    print("M3_LEETCODER_RECEIVER_INVENTORY_PASS", receipt)

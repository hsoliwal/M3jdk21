#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Freeze and qualify the bounded Sc0 task with retained Maven/OpenRewrite owners."""
from __future__ import annotations

import argparse
import ast
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

SOURCE = "d1ecbd43dbadaf218deec0d98a2ddc23f3a3f45c"
TARGET = "c0a14387009aefc7d62bd3268055d526e04f9074"
TARGET_TREE = "491bbd80a51483d0710fc12c1bad3a037b385ba0"
SUITE = "com.synexia.rewrite.Sc0RecipeTest"
QUALIFICATION_SHA256 = "c5474bd26c5eb60316c35b548dcf7e5111cd9d614670797cd8f4cdf53244777f"
MAIN = ("com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java",)
OWNER_SHA256 = "8015207ce502d22955f7f77697177e5939fa1e25be7cd74be89c639335ae48e6"
BUDGETS = {"static": 180, "compile": 600, "tests": 7200, "runtime": 7200}
EXPECTED_PROJECT = 20
EXPECTED_EXPORTS = 11
METHODS = ['closedFamiliesAndResourcePins',
 'declaredOrderedCompositions',
 'initialRefusals',
 'namedRecipesAndFixedPoint',
 'postScanDrift',
 'pureAdditionInputs',
 'spcMixedStates']
FAMILY_IDS = ('Spc',)
FAMILIES = {'com/m3/rewrite/backport/jdk21-hash-pinned-text/sc0-spc': ('m3/runtime-integration/fix-root-ascii-case/manifest.json',
                                                            'm3/tooling/image-gates/current-c0.json',
                                                            'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-history-convergence/03-M3StringPrecomputeSearchTest.java.txt',
                                                            'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-history-convergence/manifest.tsv',
                                                            'test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java')}
REPLACEMENTS = ('m3/runtime-integration/fix-root-ascii-case/manifest.json',
 'm3/tooling/image-gates/current-c0.json',
 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-history-convergence/03-M3StringPrecomputeSearchTest.java.txt',
 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-string-history-convergence/manifest.tsv',
 'test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java')
COUNTS = {'additions': 0,
 'families': 1,
 'initialRefusals': 27,
 'mixedStates': 32,
 'orderedCompositions': 2,
 'postScanRefusals': 10,
 'pureAdditionInputs': 0,
 'replacements': 5,
 'targets': 5}
TARGETS = tuple(path for targets in FAMILIES.values() for path in targets)
ADDITIONS = tuple(path for path in TARGETS if path not in REPLACEMENTS)
FLAGS = {"crossFamilyAtomicity": False, "canonicalProductionApplied": False,
         "strictCanonicalAdmission": False, "consumerQualified": False,
         "crossFamilyCartesianProductEnumerated": False,
         "unlistedOrderedCompositionsQualified": False}
OVERRIDES = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "MAVEN_OPTS", "MAVEN_ARGS",
             "MAVEN_DEBUG_OPTS", "MAVEN_PROJECTBASEDIR", "CLASSPATH", "PYTHONPATH", "PYTHONHOME")
SETTINGS = b'<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><offline>true</offline></settings>\n'


class Refusal(RuntimeError):
    """A declared identity, scope or ordered gate did not hold."""


def require(condition, message):
    if not condition:
        raise Refusal(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def sha(path):
    return digest(path.read_bytes())


def canonical(value):
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode("utf-8")


def load(path):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            require(key not in value, "duplicate JSON key: " + key)
            value[key] = item
        return value
    return json.loads(path.read_bytes(), object_pairs_hook=unique)


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("xb") as stream:
        stream.write(canonical(value))


def regular(path):
    require(path.is_file() and not path.is_symlink(), "regular file required: " + str(path))


def identity(path):
    regular(path)
    data = path.read_bytes()
    return {"path": str(path), "sha256": digest(data), "bytes": len(data)}


def relative(value):
    require(isinstance(value, str) and value and "\\" not in value
            and all(ord(char) >= 32 and ord(char) != 127 for char in value), "invalid relative path")
    name = PurePosixPath(value)
    require(not name.is_absolute() and all(part not in ("", ".", "..") and ":" not in part
            for part in value.split("/")), "unsafe path: " + value)
    return value


def files(directory):
    require(directory.is_dir() and not directory.is_symlink(), "directory required: " + str(directory))
    result = []
    for path in sorted(directory.rglob("*")):
        require(not path.is_symlink(), "symlink refused: " + str(path))
        if path.is_file():
            result.append(path)
        else:
            require(path.is_dir(), "special file refused: " + str(path))
    return result


def expected_paths():
    paths = {"pom.xml", "proof.py", "README.md", "src/test/java/com/synexia/rewrite/Sc0RecipeTest.java"}
    paths.update("dependency-source/" + name for name in MAIN)
    paths.update("src/test/resources/before/" + name for name in REPLACEMENTS)
    for family, targets in FAMILIES.items():
        paths.add("src/main/resources/" + family + "/manifest.tsv")
        paths.update("src/main/resources/" + family + "/target-" + str(i) + ".txt"
                     for i in range(1, len(targets) + 1))
        paths.add("src/main/resources/META-INF/rewrite/" + family.rsplit("/", 1)[1] + ".yml")
    paths.update("src/main/resources/" + name for name in
                 ("qualification.json", "composition-orders.json", "META-INF/rewrite/sc0-orders.yml"))
    require(len(paths) == EXPECTED_PROJECT, "closed project count drift")
    return paths


def check_environment():
    for name in OVERRIDES:
        require(not os.environ.get(name), "unsealed execution override: " + name)


def copy(source, target):
    regular(source)
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("xb") as stream:
        stream.write(source.read_bytes())


def input_row(row):
    require(set(row).issuperset({"file", "sha256", "bytes"}), "input identity fields missing")
    path = Path(row["file"])
    regular(path)
    path = path.resolve()
    observed = identity(path)
    require(observed["sha256"] == row["sha256"] and observed["bytes"] == row["bytes"],
            "declared input drift: " + str(path))
    return path


def prepare(args):
    check_environment()
    spec_path = args.spec.resolve()
    spec = load(spec_path)
    require(spec["schema"] == "synexia.sc0.proof-spec/1", "unknown proof spec")
    require(spec["sourceCommit"] == SOURCE and spec["targetCommit"] == TARGET, "baseline drift")
    require(sorted(spec["junitMethods"]) == METHODS, "test inventory differs from admission")
    rows = spec["inputs"]
    names = [relative(row["path"]) for row in rows]
    require(names == sorted(set(names)) and set(names) == expected_paths(), "closed project inventory required")
    sources = [(row, input_row(row)) for row in rows]
    require(len(spec.get("extraInputs", [])) >= 3, "sealed admission/provenance inputs required")
    extra = [input_row(row) for row in spec["extraInputs"]]
    require(next(path for row, path in sources if row["path"] == "proof.py").read_bytes()
            == Path(__file__).read_bytes(), "prepare must use the admitted runner bytes")
    jdk, maven, m2 = args.jdk.resolve(), args.maven.resolve(), args.m2.resolve()
    for path in (jdk / "bin/java", jdk / "bin/javac", jdk / "release", jdk / "lib/modules", maven / "bin/mvn"):
        regular(path)
    python = Path(sys.executable).resolve()
    declared_tools = spec["toolchain"]
    supplied_tools = {"python": python, "java": jdk / "bin/java", "javac": jdk / "bin/javac",
                      "jdkRelease": jdk / "release", "jdkModules": jdk / "lib/modules",
                      "maven": maven / "bin/mvn"}
    require(set(declared_tools) == set(supplied_tools), "exact admitted toolchain role pins required")
    for role, path in supplied_tools.items():
        pin = declared_tools[role]
        observed = identity(path)
        require(set(pin) == {"sha256", "bytes"} and re.fullmatch(r"[0-9a-f]{64}", pin["sha256"])
                and type(pin["bytes"]) is int and pin["bytes"] > 0
                and observed["sha256"] == pin["sha256"] and observed["bytes"] == pin["bytes"],
                "supplied tool differs from exact admitted toolchain: " + role)
    require(spec["stageTimeoutSeconds"] == BUDGETS
            and all(type(n) is int for n in spec["stageTimeoutSeconds"].values()), "stage budget drift")
    classpath_path = args.classpath.resolve()
    regular(classpath_path)
    jars = [Path(value).resolve() for value in classpath_path.read_text().strip().split(os.pathsep)]
    require(jars and len(jars) == len(set(jars)), "nonempty unique runtime classpath required")
    for path in jars:
        regular(path)
        require(path.suffix == ".jar" and path.is_relative_to(m2), "classpath must use the sealed local cache")
    proof = args.output.resolve()
    require(not proof.exists(), "fresh proof directory required")
    proof.mkdir(parents=True)
    project = proof / "project"
    for row, source in sources:
        copy(source, project / row["path"])
    copy(spec_path, proof / "spec.json")
    copy(classpath_path, proof / "runtime-classpath.txt")
    with (proof / "settings.xml").open("xb") as stream:
        stream.write(SETTINGS)
    staging = [{"path": row["path"], "source": str(source), "sha256": row["sha256"], "bytes": row["bytes"]}
               for row, source in sources]
    write(proof / "staging.json", staging)
    python = Path(sys.executable).resolve()
    classpath = os.pathsep.join([str(project / "target/test-classes"), str(project / "target/classes")]
                               + [str(path) for path in jars])
    mvn = [str(maven / "bin/mvn"), "-o", "-B", "-ntp", "-s", str(proof / "settings.xml"),
           "-gs", str(proof / "settings.xml"), "-Dmaven.repo.local=" + str(m2),
           "-Dsc0.python=" + str(python), "-f", str(project / "pom.xml")]
    commands = {
        "static": [str(python), str(project / "proof.py"), "_static", "--proof", str(proof)],
        "compile": mvn + ["test-compile"],
        "tests": mvn + ["org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test"],
        "runtime": [str(jdk / "bin/java"), "-ea", "-Dsc0.output=" + str(proof / "generated-candidate"),
                    "-Dsc0.python=" + str(python), "-cp", classpath, SUITE],
    }
    environment = {"JAVA_HOME": str(jdk), "PATH": str(jdk / "bin") + os.pathsep + str(maven / "bin")
                   + os.pathsep + os.defpath, "MAVEN_SKIP_RC": "true", "LC_ALL": "C", "TZ": "UTC"}
    config = {"sourceCommit": SOURCE, "targetCommit": TARGET, "python": str(python), "jdk": str(jdk),
              "maven": str(maven), "m2": str(m2), "environment": environment, "commands": commands,
              "junitSuite": SUITE, "junitMethods": METHODS, "stageTimeoutSeconds": BUDGETS, "toolBoundary":
              "Executable/JDK/Maven/cache file bytes sealed; OS, dynamic libraries, shell utilities and Python stdlib are not a complete image seal."}
    write(proof / "commands.json", config)
    write(proof / "tooling.json", {"python": identity(python), "pythonVersion": sys.version,
          "java": identity(jdk / "bin/java"), "javac": identity(jdk / "bin/javac"),
          "jdkRelease": identity(jdk / "release"), "maven": identity(maven / "bin/mvn"),
          "runtimeJars": [identity(path) for path in jars], "canonicalOfflineSourceClosure": False})
    sealed = set(files(project)) | {proof / name for name in ("spec.json", "runtime-classpath.txt",
             "settings.xml", "staging.json", "commands.json", "tooling.json")}
    sealed.update(extra)
    sealed.update((source for _, source in sources))
    sealed.update((spec_path, classpath_path, python))
    sealed.update(path for path in files(m2) if path.suffix in (".jar", ".pom"))
    # Retain exact lexical tool paths and symlink targets, including JDK legal-file links.
    tools = []
    for directory in (jdk, maven):
        for path in sorted(directory.rglob("*")):
            if path.is_symlink():
                require(path.is_file() and path.resolve().is_relative_to(directory), "external tool symlink")
                tools.append({"path": str(path), "target": os.readlink(path), "resolved": str(path.resolve()),
                              "sha256": sha(path), "bytes": path.stat().st_size})
                sealed.add(path.resolve())
            elif path.is_file():
                sealed.add(path)
            else:
                require(path.is_dir(), "special tool input")
    write(proof / "tool-links.json", tools)
    sealed.add(proof / "tool-links.json")
    write(proof / "inputs.json", [identity(path) for path in sorted(sealed, key=str)])
    write(proof / "PATCH.json", {"schema": "synexia.sc0.prepared/1", "status": "PREPARED_NOT_EXECUTED",
          "sourceCommit": SOURCE, "targetCommit": TARGET, "inputManifestSHA256": sha(proof / "inputs.json"),
          "specSHA256": sha(proof / "spec.json"), "commandsSHA256": sha(proof / "commands.json"),
          "stagingSHA256": sha(proof / "staging.json"), "projectFiles": len(staging),
          "expectedJUnitMethods": METHODS, "strictCanonicalAdmission": False, "canonicalProductionApplied": False})
    print(json.dumps({"proof": str(proof), "patchSHA256": sha(proof / "PATCH.json"),
                      "inputManifestSHA256": sha(proof / "inputs.json"), "gateExecuted": False}))


def verify_inputs(proof, seal):
    require(sha(proof / "PATCH.json") == seal, "external PATCH seal mismatch")
    patch = load(proof / "PATCH.json")
    require(patch["sourceCommit"] == SOURCE and patch["targetCommit"] == TARGET, "proof baseline drift")
    require(sha(proof / "inputs.json") == patch["inputManifestSHA256"], "input manifest drift")
    rows = load(proof / "inputs.json")
    paths = [row["path"] for row in rows]
    require(paths == sorted(set(paths)), "sealed input inventory is not unique/sorted")
    for row in rows:
        require(identity(Path(row["path"])) == row, "sealed input drift: " + row["path"])
    for row in load(proof / "tool-links.json"):
        path = Path(row["path"])
        require(path.is_symlink() and os.readlink(path) == row["target"]
                and str(path.resolve()) == row["resolved"] and sha(path) == row["sha256"]
                and path.stat().st_size == row["bytes"], "tool link drift")
    project = proof / "project"
    actual = {path.relative_to(project).as_posix() for path in files(project)
              if not path.is_relative_to(project / "target")}
    require(actual == expected_paths(), "project file inventory drift")
    config = load(proof / "commands.json")
    require(Path(sys.executable).resolve() == Path(config["python"]), "Python executable drift")
    require(Path(__file__).resolve() == project / "proof.py", "execute the frozen copied runner")
    return config


def qualification(project):
    resource = project / "src/main/resources"
    path = resource / "qualification.json"
    require(sha(path) == QUALIFICATION_SHA256, "admitted qualification resource drift")
    value = load(path)
    require(set(value) == {"schema", "sourceCommit", "targetCommit", "targetTree", "ownerSHA256",
            "counts", "families", "orders", "compositionOrdersSHA256", "compositionYamlSHA256"},
            "qualification fields differ from admission")
    require(value["schema"] == "synexia.sc0.qualification/1" and value["sourceCommit"] == SOURCE
            and value["targetCommit"] == TARGET and value["targetTree"] == TARGET_TREE,
            "qualification baseline drift")
    require(value["ownerSHA256"] == OWNER_SHA256
            and sha(project / "dependency-source" / MAIN[0]) == OWNER_SHA256, "retained text owner drift")
    require(value["counts"] == COUNTS and all(type(n) is int for n in value["counts"].values()),
            "qualification counts differ from admission")
    families = value["families"]
    require(tuple(row["id"] for row in families) == FAMILY_IDS, "family identity/order drift")
    require(len(families) == COUNTS["families"], "family count drift")
    orders = load(resource / "composition-orders.json")
    require(set(orders) == {"schema", "sourceCommit", "targetCommit", "orders"}
            and orders["schema"] == "synexia.sc0.composition-orders/1"
            and orders["sourceCommit"] == SOURCE and orders["targetCommit"] == TARGET,
            "composition order protocol drift")
    expected_orders = [{"id": "Forward", "recipe": "com.synexia.m3.Sc0Forward", "families": list(FAMILY_IDS)},
                       {"id": "Reverse", "recipe": "com.synexia.m3.Sc0Reverse", "families": list(reversed(FAMILY_IDS))}]
    require(orders["orders"] == value["orders"] == expected_orders, "only both declared complete orders admitted")
    require(sha(resource / "composition-orders.json") == value["compositionOrdersSHA256"]
            and sha(resource / "META-INF/rewrite/sc0-orders.yml") == value["compositionYamlSHA256"],
            "composition resources drift")
    computed = {"families": len(families), "targets": 0, "replacements": 0, "additions": 0,
                "mixedStates": 0, "initialRefusals": 0, "postScanRefusals": 0,
                "pureAdditionInputs": 0, "orderedCompositions": 2}
    for family, (folder, targets) in zip(families, FAMILIES.items()):
        require(set(family) == {"id", "crate", "namedRecipe", "manifestPath", "manifestSHA256",
                "yamlPath", "yamlSHA256", "targets"}, "family declaration fields drift")
        require(family["crate"] == "sc0-" + family["id"].lower()
                and family["namedRecipe"] == "com.synexia.m3.Sc0" + family["id"]
                and family["manifestPath"] == folder + "/manifest.tsv"
                and family["yamlPath"] == "META-INF/rewrite/" + family["crate"] + ".yml",
                "family path/name relationship drift")
        require(tuple(row["path"] for row in family["targets"]) == targets and 1 <= len(targets) <= 7,
                "bounded exact family targets required")
        require(sha(resource / family["manifestPath"]) == family["manifestSHA256"]
                and sha(resource / family["yamlPath"]) == family["yamlSHA256"], "family resource drift")
        size = len(targets)
        replacements = sum(row["operation"] == "REPLACE" for row in family["targets"])
        additions = size - replacements
        computed["targets"] += size
        computed["replacements"] += replacements
        computed["additions"] += additions
        computed["mixedStates"] += 2 ** size
        computed["initialRefusals"] += 3 * size + 2 * replacements + (2 if replacements else 0)
        computed["postScanRefusals"] += 2 * size
        if replacements == 0:
            computed["pureAdditionInputs"] += 2
    require(computed == COUNTS, "independent finite control-count derivation differs")
    return value


def manifest_outputs(project):
    value = qualification(project)
    expected = {}
    for family in value["families"]:
        folder = project / "src/main/resources" / Path(family["manifestPath"]).parent
        lines = [line for line in (folder / "manifest.tsv").read_text().splitlines()
                 if line and not line.startswith("#")]
        require(len(lines) == len(family["targets"]), "family manifest count drift")
        for index, (line, declared) in enumerate(zip(lines, family["targets"]), 1):
            require(set(declared) == {"path", "operation", "beforeSHA256", "beforeBytes", "afterSHA256", "afterBytes"},
                    "target identity fields drift")
            cells = line.split("\t")
            require(len(cells) == 4, "four manifest fields required")
            path, before, after, resource = cells
            relative(path)
            relative(resource)
            require(resource == "target-" + str(index) + ".txt", "numbered resource order drift")
            require((before == "ABSENT" or re.fullmatch(r"[0-9a-f]{64}", before))
                    and re.fullmatch(r"[0-9a-f]{64}", after), "manifest hashes or explicit ABSENT required")
            require(path == declared["path"] and before == declared["beforeSHA256"]
                    and after == declared["afterSHA256"], "manifest/qualification disagreement")
            require((path in ADDITIONS) == (before == "ABSENT"), "declared replacement/addition classification drift")
            before_path = project / "src/test/resources/before" / path
            if before == "ABSENT":
                before_bytes = None
                require(not before_path.exists() and declared["beforeBytes"] is None
                        and declared["operation"] == "ADD", "ABSENT target must not have a fabricated preimage")
            else:
                regular(before_path)
                before_bytes = before_path.read_bytes()
                require(path in REPLACEMENTS and digest(before_bytes) == before
                        and len(before_bytes) == declared["beforeBytes"] and declared["operation"] == "REPLACE",
                        "required replacement preimage drift")
            regular(folder / resource)
            after_bytes = (folder / resource).read_bytes()
            require(digest(after_bytes) == after and len(after_bytes) == declared["afterBytes"], "manifest postimage drift")
            require(path not in expected, "overlapping output families")
            expected[path] = {"path": path, "sha256": after, "bytes": len(after_bytes),
                              "operation": "ADD" if before == "ABSENT" else "REPLACE",
                              "beforeSHA256": before,
                              "beforeBytes": None if before_bytes is None else len(before_bytes)}
    require(set(expected) == set(TARGETS) and len(expected) == COUNTS["targets"], "closed target inventory drift")
    return expected


def static_gate(proof):
    project = proof / "project"
    ast.parse((project / "proof.py").read_bytes())
    ET.parse(project / "pom.xml")
    ET.parse(proof / "settings.xml")
    require({path.relative_to(project).as_posix() for path in files(project)} == expected_paths(), "unexpected static project file")
    value = qualification(project)
    outputs = manifest_outputs(project)
    fixture = (project / "src/test/java/com/synexia/rewrite/Sc0RecipeTest.java").read_text()
    observed = re.findall(r"@Test\s+(?:(?:public|final)\s+)*void\s+(\w+)\s*\(", fixture)
    require(sorted(observed) == METHODS, "actual source test method inventory drift")
    for family in value["families"]:
        folder = project / "src/main/resources" / Path(family["manifestPath"]).parent
        for index, row in enumerate(family["targets"], 1):
            if not row["path"].endswith(".py"):
                continue
            after = (folder / ("target-" + str(index) + ".txt")).read_bytes()
            ast.parse(after, filename=row["path"])
            if row["operation"] == "REPLACE":
                before = (project / "src/test/resources/before" / row["path"]).read_bytes()
                ast.parse(before, filename=row["path"])
    print(json.dumps({"status": "STATIC_SCOPE_PASS", "mainSources": len(MAIN), "testSources": 1,
                      "testMethods": METHODS, "outputs": len(outputs),
                      "typedJavaProof": False, **FLAGS}))


def artifacts(proof):
    target = proof / "project/target"
    result = []
    for folder in ("classes", "test-classes"):
        for path in files(target / folder):
            row = identity(path)
            row["path"] = path.relative_to(target).as_posix()
            result.append(row)
    return result


def check_artifacts(proof):
    require(artifacts(proof) == load(proof / "build-artifacts.json"), "compiled artifacts changed")


def junit(proof):
    reports = sorted((proof / "project/target/surefire-reports").glob("TEST-*.xml"))
    require(len(reports) == 1, "exactly one declared JUnit suite required")
    suite = ET.parse(reports[0]).getroot()
    require(suite.get("name") == SUITE, "unexpected JUnit suite")
    counts = {key: int(suite.get(key, "0")) for key in ("tests", "errors", "failures", "skipped")}
    require(counts == {"tests": len(METHODS), "errors": 0, "failures": 0, "skipped": 0}, "JUnit result differs from exact inventory")
    methods = []
    for case in suite.findall("testcase"):
        require(case.get("classname") == SUITE and not any(case.find(tag) is not None
                for tag in ("error", "failure", "skipped")), "JUnit case not accepted")
        methods.append(case.attrib["name"].removesuffix("()"))
    require(sorted(methods) == METHODS, "JUnit method inventory mismatch")
    result = {"suite": SUITE, "methods": sorted(methods), "counts": counts, "report": identity(reports[0])}
    write(proof / "junit.json", result)
    return result


def state_seal(rows):
    framed = "SYNEXIA-SC0-STATE/1\n" + "".join(
        row["path"] + "\t" + row["sha256"] + "\t"
        + ("ABSENT" if row["bytes"] is None else str(row["bytes"])) + "\n" for row in rows)
    return digest(framed.encode("utf-8"))


def output_report(proof, stdout):
    output = proof / "generated-candidate"
    project = proof / "project"
    expected = manifest_outputs(project)
    value = qualification(project)
    composition_files = set()
    for order in value["orders"]:
        for ordinal, family in enumerate(order["families"], 1):
            stem = "composition/" + order["id"] + "/" + str(ordinal).zfill(2) + "-" + family
            composition_files.update((stem + ".patch", stem + "-replay.patch"))
    expected_files = set(expected) | {"OUTPUT.json", "candidate.patch"} | composition_files
    require(len(expected_files) == EXPECTED_EXPORTS and
            {path.relative_to(output).as_posix() for path in files(output)} == expected_files,
            "generated output file inventory mismatch")
    payload = load(output / "OUTPUT.json")
    keys = {"schema", "sourceCommit", "targetCommit", "outputs", "outputSeal", "qualificationSHA256",
            "compositionOrdersSHA256", "orderedCompositions"} | set(COUNTS) | set(FLAGS)
    require(set(payload) == keys, "unknown/missing OUTPUT fields")
    require(payload["schema"] == "synexia.sc0.output/1" and payload["sourceCommit"] == SOURCE
            and payload["targetCommit"] == TARGET, "OUTPUT protocol/baseline drift")
    require(payload["qualificationSHA256"] == QUALIFICATION_SHA256
            and payload["compositionOrdersSHA256"] == value["compositionOrdersSHA256"],
            "OUTPUT qualification/order seal drift")
    for key, count in COUNTS.items():
        if key == "orderedCompositions":
            continue  # OUTPUT carries the detailed rows; qualification carries their exact count.
        require(type(payload[key]) is int and payload[key] == count, "OUTPUT count drift: " + key)
    for key, flag in FLAGS.items():
        require(payload[key] is flag, "OUTPUT authority drift: " + key)
    rows = [expected[path] for path in sorted(expected)]
    require(payload["outputs"] == rows, "OUTPUT path/hash/size declarations mismatch")
    for row in rows:
        actual = identity(output / row["path"])
        require(actual["sha256"] == row["sha256"] and actual["bytes"] == row["bytes"], "actual generated output drift")
    framed = "SYNEXIA-SC0-OUTPUT/1\n" + "".join(
        row["path"] + "\t" + row["sha256"] + "\t" + str(row["bytes"]) + "\n" for row in rows)
    seal = digest(framed.encode("utf-8"))
    require(payload["outputSeal"] == seal, "OUTPUT seal mismatch")
    families = {family["id"]: family for family in value["families"]}
    initial = {row["path"]: {"path": row["path"], "sha256": row["beforeSHA256"], "bytes": row["beforeBytes"]}
               for row in rows}
    final = [{"path": row["path"], "sha256": row["sha256"], "bytes": row["bytes"]} for row in rows]
    compositions = payload["orderedCompositions"]
    require(len(compositions) == COUNTS["orderedCompositions"], "ordered composition count drift")
    patch_rows = []
    forward_patches = []
    for declared, actual_order in zip(value["orders"], compositions):
        require(set(actual_order) == {"id", "recipe", "steps"}
                and actual_order["id"] == declared["id"] and actual_order["recipe"] == declared["recipe"],
                "ordered composition identity drift")
        steps = actual_order["steps"]
        require(len(steps) == len(FAMILY_IDS), "complete ordered composition steps required")
        state = dict(initial)
        for ordinal, (family_id, step) in enumerate(zip(declared["families"], steps), 1):
            require(set(step) == {"ordinal", "family", "inputRows", "outputRows", "inputSeal", "outputSeal",
                    "changedPaths", "patchPath", "patchSHA256", "replayInputSeal", "replayOutputSeal",
                    "replayChangedPaths", "replayPatchPath", "replayPatchSHA256"}, "composition step fields drift")
            require(type(step["ordinal"]) is int and step["ordinal"] == ordinal
                    and step["family"] == family_id, "composition ordinal/family drift")
            before = [state[path] for path in sorted(state)]
            require(step["inputRows"] == before and step["inputSeal"] == state_seal(before),
                    "composition input is not previous exact output")
            paths = sorted(row["path"] for row in families[family_id]["targets"])
            for path in paths:
                row = expected[path]
                state[path] = {"path": path, "sha256": row["sha256"], "bytes": row["bytes"]}
            after = [state[path] for path in sorted(state)]
            after_seal = state_seal(after)
            require(step["outputRows"] == after and step["outputSeal"] == after_seal
                    and step["changedPaths"] == paths, "composition family transition drift")
            stem = "composition/" + declared["id"] + "/" + str(ordinal).zfill(2) + "-" + family_id
            require(step["patchPath"] == stem + ".patch"
                    and step["replayPatchPath"] == stem + "-replay.patch", "composition patch path drift")
            patch = output / step["patchPath"]
            replay = output / step["replayPatchPath"]
            require(sha(patch) == step["patchSHA256"] and patch.stat().st_size > 0,
                    "actual composition patch missing or changed")
            require(sha(replay) == step["replayPatchSHA256"] and replay.read_bytes() == b"",
                    "fixed point replay patch must be retained and empty")
            require(step["replayInputSeal"] == after_seal and step["replayOutputSeal"] == after_seal
                    and step["replayChangedPaths"] == [], "composition fixed point drift")
            patch_rows.extend((identity(patch), identity(replay)))
            if declared["id"] == "Forward":
                forward_patches.append(patch.read_bytes())
        require([state[path] for path in sorted(state)] == final, "ordered composition did not converge to all postimages")
    require((output / "candidate.patch").read_bytes() == b"".join(forward_patches),
            "candidate patch must concatenate actual Forward scheduler patches")
    marker = ("SC0_VERIFIED targets=5 mixedStates=32 initialRefusals=27 "
              "postScanRefusals=10 outputSeal=" + seal)
    markers = [line for line in stdout.read_text().splitlines() if line.startswith("SC0_VERIFIED")]
    require(markers == [marker], "runtime completion marker mismatch")
    result = {"output": identity(output / "OUTPUT.json"), "patch": identity(output / "candidate.patch"),
              "files": rows, "outputSeal": seal, "compositionPatches": patch_rows,
              "qualificationSHA256": QUALIFICATION_SHA256,
              "compositionOrdersSHA256": value["compositionOrdersSHA256"],
              "marker": marker, **FLAGS}
    write(proof / "runtime.json", result)
    return result


def verify_command_evidence(receipts, seals):
    require([identity(path) for path in receipts] == seals, "command receipt drift")
    for receipt in receipts:
        row = load(receipt)
        for channel in ("stdout", "stderr"):
            expected = row[channel]
            require(identity(Path(expected["path"])) == expected,
                    "command " + channel + " evidence drift: " + str(receipt))


def verify_junit_evidence(evidence):
    if evidence:
        require(len(evidence) == 2, "exact JUnit JSON and raw report evidence required")
        report = Path(evidence[1]["path"])
        require(sorted(report.parent.glob("TEST-*.xml")) == [report], "retained JUnit report inventory drift")
    for expected in evidence:
        require(identity(Path(expected["path"])) == expected, "retained JUnit evidence drift")


def run(args):
    check_environment()
    proof = args.proof.resolve()
    config = verify_inputs(proof, args.seal)
    require(not any((proof / name).exists() for name in ("chain", "RESULT.json", "generated-candidate", "project/target")), "fresh unexecuted proof required")
    chain = proof / "chain"
    chain.mkdir()
    environment = os.environ.copy()
    environment.update(config["environment"])
    receipts = []
    success = False
    failure = None
    current = None
    compiled_seal = None
    prior_seals = []
    junit_evidence = []
    junit_result = None
    runtime_result = None
    try:
        for index, stage in enumerate(("static", "compile", "tests", "runtime"), 1):
            current = stage
            verify_inputs(proof, args.seal)
            verify_command_evidence(receipts, prior_seals)
            verify_junit_evidence(junit_evidence)
            if compiled_seal is not None:
                require(sha(proof / "build-artifacts.json") == compiled_seal, "compiled manifest seal drift")
            if stage in ("tests", "runtime"):
                check_artifacts(proof)
            if stage != "runtime":
                require(not (proof / "generated-candidate").exists(), "premature output export")
            stem = f"{index:02d}-{stage}"
            stdout, stderr = chain / (stem + ".stdout.log"), chain / (stem + ".stderr.log")
            command = config["commands"][stage]
            start = datetime.now(timezone.utc).isoformat()
            timed_out = False
            with stdout.open("xb") as out, stderr.open("xb") as err:
                try:
                    process = subprocess.run(command, cwd=proof / "project", env=environment,
                                             stdout=out, stderr=err, check=False, timeout=BUDGETS[stage])
                    exit_code = process.returncode
                except subprocess.TimeoutExpired:
                    timed_out = True
                    exit_code = 124
            end = datetime.now(timezone.utc).isoformat()
            receipt = {"stage": stage, "command": command, "cwd": str(proof / "project"),
                       "environment": config["environment"], "startedAt": start, "endedAt": end,
                       "exitCode": exit_code, "timedOut": timed_out, "timeoutSeconds": BUDGETS[stage],
                       "stdout": identity(stdout), "stderr": identity(stderr),
                       "inputManifestSHA256": sha(proof / "inputs.json"),
                       "priorReceipts": [identity(path) for path in receipts]}
            receipt_path = chain / (stem + ".json")
            write(receipt_path, receipt)
            receipts.append(receipt_path)
            prior_seals.append(identity(receipt_path))
            verify_command_evidence(receipts, prior_seals)
            verify_junit_evidence(junit_evidence)
            require(not timed_out and exit_code == 0, stage + " command failed with exit " + str(exit_code))
            verify_inputs(proof, args.seal)
            if stage == "compile":
                compiled = artifacts(proof)
                required = {"classes/" + name.removesuffix(".java") + ".class" for name in MAIN}
                required.add("test-classes/com/synexia/rewrite/Sc0RecipeTest.class")
                require(required.issubset({row["path"] for row in compiled}), "required compiled classes missing")
                write(proof / "build-artifacts.json", compiled)
                compiled_seal = sha(proof / "build-artifacts.json")
            elif stage == "tests":
                check_artifacts(proof)
                junit_result = junit(proof)
                junit_evidence = [identity(proof / "junit.json"), junit_result["report"]]
            elif stage == "runtime":
                check_artifacts(proof)
                runtime_result = output_report(proof, stdout)
            verify_command_evidence(receipts, prior_seals)
            verify_junit_evidence(junit_evidence)
            print(json.dumps({"stage": stage, "status": "PASS", "receiptSHA256": sha(receipt_path)}), flush=True)
        verify_command_evidence(receipts, prior_seals)
        require(len(junit_evidence) == 2 and junit_result is not None, "accepted JUnit evidence required")
        verify_junit_evidence(junit_evidence)
        require(sha(proof / "build-artifacts.json") == compiled_seal, "final compiled manifest drift")
        success = True
    except (Exception, KeyboardInterrupt) as error:
        failure = {"stage": current, "type": type(error).__name__, "message": str(error)}
    finally:
        result = {"schema": "synexia.sc0.proof-result/1", "status": "PASS_FOCUSED_PRODUCER" if success else "STOP",
                  "sourceCommit": SOURCE, "targetCommit": TARGET, "patchSHA256": args.seal,
                  "inputManifestSHA256": sha(proof / "inputs.json"), "receipts": prior_seals,
                  "failure": failure, "junit": junit_result, "junitEvidence": junit_evidence,
                  "runtime": runtime_result,
                  "compiledArtifacts": identity(proof / "build-artifacts.json") if (proof / "build-artifacts.json").exists() else None,
                  "consumerQualified": False, "canonicalOfflineSourceClosure": False, **FLAGS}
        write(proof / "RESULT.json", result)
    print(json.dumps({"status": result["status"], "resultSHA256": sha(proof / "RESULT.json"), "failure": failure}), flush=True)
    return 0 if success else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="operation", required=True)
    prepare_parser = sub.add_parser("prepare")
    for name in ("spec", "output", "jdk", "maven", "m2", "classpath"):
        prepare_parser.add_argument("--" + name, type=Path, required=True)
    run_parser = sub.add_parser("run")
    run_parser.add_argument("--proof", type=Path, required=True)
    run_parser.add_argument("--seal", required=True)
    static_parser = sub.add_parser("_static")
    static_parser.add_argument("--proof", type=Path, required=True)
    args = parser.parse_args()
    if args.operation == "prepare":
        prepare(args)
        return 0
    if args.operation == "_static":
        static_gate(args.proof.resolve())
        return 0
    return run(args)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (Refusal, OSError, ValueError, KeyError) as error:
        print(type(error).__name__ + ": " + str(error), file=sys.stderr)
        sys.exit(2)

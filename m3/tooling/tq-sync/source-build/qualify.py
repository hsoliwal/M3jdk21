# SPDX-License-Identifier: Apache-2.0
"""Qualify existing source producers and the TQ sync fixture recipe; no resolver."""
import argparse
import json
import os
from pathlib import Path, PurePosixPath
import re
import runpy
import subprocess
import xml.etree.ElementTree as ET
import zipfile

HERE = Path(__file__).resolve().parent
RECEIVER = HERE.parents[3]
ROOT = Path(os.environ["M3_PROOF_ROOT"]).resolve()
INPUTS = json.loads((HERE / "build-inputs.json").read_bytes())
MODEL = runpy.run_path(str(RECEIVER / "m3/tooling/tq-sync/source-reactor/test_reactor.py"),
                       run_name="m3_model_custody_reuse")
blob = MODEL["blob"]
gav = MODEL["gav"]
HELPER = INPUTS["existingSha256Helper"]
if blob((RECEIVER / HELPER["path"]).read_bytes()) != HELPER["gitBlob"]:
    raise RuntimeError("source hash helper drift")
sha256 = runpy.run_path(str(RECEIVER / HELPER["path"]),
                       run_name="m3_source_hash_reuse")["sha256"]


def git(*args, binary=False):
    return subprocess.check_output(["git", *args], cwd=RECEIVER, timeout=60,
                                   text=not binary).strip()


def safe_relative(value):
    path = PurePosixPath(value)
    if not value or path.is_absolute() or any(p in ("", ".", "..") for p in value.split("/")):
        raise RuntimeError("unsafe relative path")
    if "\\" in value or "\x00" in value or ":" in value:
        raise RuntimeError("unsafe path character")
    return path


def regular(path):
    if not path.is_file() or any(p.is_symlink() for p in [path, *path.parents]):
        raise RuntimeError("missing or redirected input: " + str(path))
    return path


def write(name, data):
    (ROOT / name).write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def binding():
    if git("rev-parse", "HEAD") != os.environ["EXPECTED_SOURCE_SHA"]:
        raise RuntimeError("checkout is not exact candidate")
    record = json.loads((HERE / "source-binding.json").read_bytes())
    if record["runtimeApplied"] is not False or record["phaseAdvanced"] is not False:
        raise RuntimeError("source binding scope drift")
    if not re.fullmatch(r"[0-9a-f]{40}", record["sourceCommit"]):
        raise RuntimeError("missing canonical source commit")
    for row in record["files"]:
        target = regular(RECEIVER / safe_relative(row["targetPath"]))
        if blob(target.read_bytes()) != row["gitBlob"]:
            raise RuntimeError("authoring input drift: " + row["targetPath"])
    if len(INPUTS["modules"]) != 57 or len(set(INPUTS["expectedProducerGavs"])) != 57:
        raise RuntimeError("exact 57 source producer inventory required")
    if set(row["gav"] for row in INPUTS["modules"]) != set(INPUTS["expectedProducerGavs"]):
        raise RuntimeError("producer inventory differs from source oracle")
    return record


def expand():
    binding()
    # Native Git fetch/checkout is a child of the outer monitored session.
    environment = os.environ.copy()
    environment["GIT_NO_LAZY_FETCH"] = "0"
    subprocess.run(["git", "sparse-checkout", "set", "--no-cone", "--stdin"],
                   cwd=RECEIVER, env=environment,
                   input="\n".join(INPUTS["expandedSparsePatterns"]) + "\n",
                   text=True, check=True, timeout=150)


def sources(require_fresh=False):
    record = binding()
    count = metadata_count = source_bytes = 0
    kinds = {}
    owner_paths = []
    for module in INPUTS["modules"]:
        prefix = "m3/vendor/third_party/" + module["module"]
        base = RECEIVER / safe_relative(prefix)
        if git("rev-parse", "HEAD:" + prefix) != module["gitTree"]:
            raise RuntimeError("module Git tree drift: " + prefix)
        if require_fresh and (base / "target").exists():
            raise RuntimeError("producer target must start absent: " + prefix)
        if gav(regular(base / "pom.xml")) != module["gav"]:
            raise RuntimeError("producer GAV drift")
        covered = set()
        for row in module["metadata"]:
            relative = prefix + "/" + str(safe_relative(row["path"]))
            path = regular(RECEIVER / relative)
            if blob(path.read_bytes()) != row["gitBlob"]:
                raise RuntimeError("module metadata drift: " + relative)
            covered.add(relative)
            metadata_count += 1
        provenance = json.loads((base / "PROVENANCE.json").read_bytes())
        for row in provenance["files"]:
            relative = str(safe_relative(row["repository"]))
            if not relative.startswith(prefix + "/") or relative in covered:
                raise RuntimeError("provenance path outside owner or duplicated")
            path = regular(RECEIVER / relative)
            if sha256(path) != row["sha256"]:
                raise RuntimeError("source provenance drift: " + relative)
            covered.add(relative)
            count += 1
            source_bytes += path.stat().st_size
            kind = row.get("kind", "UNSPECIFIED")
            kinds[kind] = kinds.get(kind, 0) + 1
        tracked = set(git("ls-files", "-z", "--", prefix, binary=True).decode("utf-8").split("\x00"))
        tracked.discard("")
        if covered != tracked or len(tracked) != module["files"]:
            raise RuntimeError("module tracked inventory differs from provenance plus metadata")
        owner_paths.append(prefix)
    aggregator = regular(RECEIVER / "m3/vendor/third_party/pom.xml")
    if blob(aggregator.read_bytes()) != MODEL["ORACLE"]["vendorAggregator"]["pomBlob"]:
        raise RuntimeError("vendor aggregator model drift")
    owner_paths.append("m3/vendor/third_party/pom.xml")
    # Preserve every collected model, including the unselected 64 producers.
    for row in MODEL["ORACLE"]["modelInputs"]:
        path = regular(RECEIVER / ("m3/vendor/third_party/" + row["module"] + "/pom.xml"))
        if blob(path.read_bytes()) != row["pomBlob"]:
            raise RuntimeError("collected model drift")
    tq = RECEIVER / "m3/tooling/tq-sync"
    if require_fresh:
        if (tq / "target").exists() or any((ROOT / "m2").rglob("*.jar")):
            raise RuntimeError("TQ targets and Maven repository must start fresh")
    if git("rev-parse", "HEAD:m3/tooling/tq-sync/src") != INPUTS["tqInputs"]["srcGitTree"]:
        raise RuntimeError("TQ fixture source tree drift")
    for row in INPUTS["tqInputs"]["engines"]:
        if blob(regular(RECEIVER / row["path"]).read_bytes()) != row["gitBlob"]:
            raise RuntimeError("shared recipe engine drift")
        owner_paths.append(row["path"])
    tq_pins = {
        "m3/tooling/tq-sync/pom.xml": INPUTS["tqInputs"]["pomBlob"],
        "m3/tooling/tq-sync/src/test/java/com/m3/tq/TQSyncTest.java": INPUTS["tqInputs"]["testBlob"],
        "m3/tooling/tq-sync/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-sync/manifest.tsv":
            INPUTS["tqInputs"]["manifestBlob"],
    }
    for relative, expected in tq_pins.items():
        if blob(regular(RECEIVER / relative).read_bytes()) != expected:
            raise RuntimeError("TQ source owner drift: " + relative)
    owner_paths += ["m3/tooling/tq-sync/src", "m3/tooling/tq-sync/pom.xml",
                    "m3/tooling/tq-sync/source-reactor", "m3/tooling/tq-sync/source-build",
                    HELPER["path"], ".github/workflows/m3-source-qualified-tq-model.yml",
                    ".github/workflows/m3-source-qualified-tq-build.yml"]
    subprocess.run(["git", "diff", "--exit-code", "--no-ext-diff", "--", *owner_paths],
                   cwd=RECEIVER, check=True, timeout=60)
    if count != 8513 or metadata_count != 342:
        raise RuntimeError("exact selected source/provenance custody count required")
    return {"schema": 1, "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
            "canonicalSourceCommit": record["sourceCommit"], "modules": 57,
            "provenanceFiles": count, "metadataFiles": metadata_count,
            "hashedSourceBytes": source_bytes, "allKindsIncluded": kinds,
            "full131RowVerifierExecuted": False, "sourceInputsUnchanged": True}


def logged_gavs(path):
    # Reuses the qualified model suite's actual Maven header/version format.
    selected = []
    current_ga = None
    for line in regular(path).read_text(encoding="utf-8").splitlines():
        header = re.fullmatch(r"\[INFO\] -+< ([^\s<>]+) >-+\s*", line)
        if header:
            current_ga = header.group(1)
        building = re.fullmatch(r"\[INFO\] Building (.+?)(?: \[\d+/\d+\])?\s*", line)
        if building and current_ga is not None:
            selected.append(current_ga + ":" + building.group(1).split()[-1])
            current_ga = None
    return selected


def installed():
    selected = logged_gavs(ROOT / "producer-install.log")
    if len(selected) != 57 or set(selected) != set(INPUTS["expectedProducerGavs"]):
        raise RuntimeError("actual producer Maven selection is not exact 57 GAVs")
    rows = []
    expected_paths = set()
    for module in INPUTS["modules"]:
        group, artifact, version = module["gav"].split(":")
        target = regular(RECEIVER / ("m3/vendor/third_party/" + module["module"]) /
                         "target" / (artifact + "-" + version + ".jar"))
        directory = ROOT / "m2" / group.replace(".", "/") / artifact / version
        jar = regular(directory / (artifact + "-" + version + ".jar"))
        pom = regular(directory / (artifact + "-" + version + ".pom"))
        target_hash = sha256(target)
        if jar.stat().st_size == 0 or sha256(jar) != target_hash or gav(pom) != module["gav"]:
            raise RuntimeError("source target/installed artifact mismatch: " + module["gav"])
        with zipfile.ZipFile(jar) as archive:
            classes = sum(name.endswith(".class") for name in archive.namelist())
        if module["module"] != "junit-jupiter-5.10.2" and classes == 0:
            raise RuntimeError("unexpected empty source producer JAR")
        expected_paths.add(jar)
        rows.append({"module": module["module"], "gav": module["gav"], "bytes": jar.stat().st_size,
                     "sha256": target_hash, "targetEqualsInstalled": True,
                     "installedPomSha256": sha256(pom), "classes": classes,
                     "freshUpstreamApiComparisonQualified": False})
    actual_paths = set((ROOT / "m2").rglob("*-m3-source-1.jar"))
    if expected_paths != actual_paths:
        raise RuntimeError("unexpected or missing source-qualified installed JAR")
    return {"schema": 1, "producers": rows, "count": len(rows),
            "launcherGav": "org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1",
            "upstreamDonorTestsQualified": False, "nativeLibrariesRebuilt": False}


def report():
    custody = sources()
    artifacts = installed()
    if artifacts != json.loads((ROOT / "source-artifacts.json").read_bytes()):
        raise RuntimeError("producer artifact changed during consumer verification")
    consumer = logged_gavs(ROOT / "tq-verify.log")
    if consumer != ["com.m3:tq-sync:1.0-SNAPSHOT"]:
        raise RuntimeError("unexpected consumer reactor selection")
    tq = RECEIVER / "m3/tooling/tq-sync"
    reports = sorted((tq / "target/surefire-reports").glob("TEST-*.xml"))
    if len(reports) != 1:
        raise RuntimeError("one fresh actual TQ sync suite required")
    xml = ET.parse(regular(reports[0])).getroot()
    expected = {"exactBytesAndFixedPoint", "refusesDriftMissingAndDuplicate", "retainsUnrelatedFiles"}
    cases = xml.findall("testcase")
    if xml.get("name") != "com.m3.tq.TQSyncTest" or len(cases) != 3:
        raise RuntimeError("wrong TQ sync suite")
    if any(xml.get(key) != str(value) for key, value in
           {"tests": 3, "failures": 0, "errors": 0, "skipped": 0}.items()):
        raise RuntimeError("TQ sync test counters not exact nonzero clean suite")
    if {case.get("name") for case in cases} != expected or any(
            case.get("classname") != "com.m3.tq.TQSyncTest" or
            any(case.find(tag) is not None for tag in ("failure", "error", "skipped")) for case in cases):
        raise RuntimeError("missing, skipped or failed required TQ sync method")
    res = tq / "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-sync"
    generated = tq / "target/generated"
    postimages = []
    for line in (res / "manifest.tsv").read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        row = line.split("\t")
        if len(row) != 4:
            raise RuntimeError("TQ snapshot manifest shape")
        path = str(safe_relative(row[0]))
        source = regular(res / safe_relative(row[3]))
        target = regular(generated / path)
        if source.read_bytes() != target.read_bytes() or sha256(target) != row[2]:
            raise RuntimeError("generated fixture postimage differs: " + path)
        postimages.append({"path": path, "bytes": target.stat().st_size, "sha256": row[2]})
    actual = {str(p.relative_to(generated).as_posix()) for p in generated.rglob("*") if p.is_file()}
    if len(postimages) != 24 or len(actual) != 24 or actual != {r["path"] for r in postimages}:
        raise RuntimeError("exact 24 generated fixture postimages required")
    consumer_jar = regular(tq / "target/tq-sync-1.0-SNAPSHOT.jar")
    receipt = {"schema": "M3_TQ_SOURCE_BUILD_QUALIFICATION_V1",
               "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "sourceCustody": custody,
               "modelReceiptSha256": sha256(ROOT / "model-receipt.json"),
               "sourceArtifactReceiptSha256": sha256(ROOT / "source-artifacts.json"),
               "surefireXmlSha256": sha256(reports[0]), "tqSyncFixtureJUnitMethods": sorted(expected),
               "tqSyncFixtureJUnitQualified": True, "producerBuildQualified": True,
               "consumerJarBytes": consumer_jar.stat().st_size, "consumerJarSha256": sha256(consumer_jar),
               "fixturePostimages": postimages, "postimagesVerifiedInJob": True,
               "compiledJarsAndPostimagesExported": False, "currentProductCustodyQualified": False,
               "runtimeTqJniQualified": False, "originalPrivateModelQualified": False,
               "canonicalOverflowQualified": False, "matchedJdkQualified": False,
               "performanceRetentionQualified": False, "phaseAdvanced": False}
    write("build-receipt.json", receipt)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "expand", "before", "installed", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        binding()
    elif operation == "expand":
        expand()
    elif operation == "before":
        write("source-custody-before.json", sources(require_fresh=True))
    elif operation == "installed":
        write("source-artifacts.json", installed())
    else:
        report()

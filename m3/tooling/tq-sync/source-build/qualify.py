# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify existing source producers and the TQ sync fixture recipe; no resolver."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import runpy
import shutil
import subprocess
import sys
import tempfile
import time
import unittest
import xml.etree.ElementTree as ET
import zipfile

HERE = Path(__file__).resolve().parent
RECEIVER = HERE.parents[3]
ROOT = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ARTIFACTS = ROOT / "artifacts"
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
    if not ARTIFACTS.is_dir() or ARTIFACTS.is_symlink():
        raise RuntimeError("workflow must create its owned artifacts directory first")
    if Path(name).name != name or not name.endswith(".json"):
        raise RuntimeError("unsafe evidence name")
    temporary = ARTIFACTS / (name + ".tmp")
    if temporary.is_symlink() or (ARTIFACTS / name).is_symlink():
        raise RuntimeError("redirected evidence path")
    temporary.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, ARTIFACTS / name)


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


PRODUCER_SELECTORS = ("rewrite-java-21", "rewrite-test", "slf4j-api-2.0.9",
                      "junit-jupiter-5.10.2", "junit-platform-launcher-1.10.2")
REDUCED_POMS = frozenset(("jackson-core-2.16.1", "junit-jupiter-params-5.10.2", "rewrite-core"))
CLEAN_GOAL = "org.apache.maven.plugins:maven-clean-plugin:3.5.0:clean"
SERIAL_TEST_METHODS = frozenset((
    "test_copies_of_real_artifacts_capture",
    "test_jar_mutation_before_capture_refuses",
    "test_pom_mutation_before_capture_refuses",
    "test_jar_and_pom_mutations_after_capture_refuse",
    "test_missing_duplicate_wrong_version_native_order_refuse",
    "test_failed_install_clean_and_incomplete_witnesses_refuse",
    "test_retained_target_and_incomplete_clean_refuse",
    "test_unsafe_output_and_redirected_target_refuse"))
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
LOG_LIMIT = 8 * 1024 ** 2


def parsed_gavs(text):
    # Reuses the qualified model suite's actual Maven header/version format.
    selected = []
    current_ga = None
    for line in text.splitlines():
        header = re.fullmatch(r"\[INFO\] -+< ([^\s<>]+) >-+\s*", line)
        if header:
            current_ga = header.group(1)
        building = re.fullmatch(r"\[INFO\] Building (.+?)(?: \[\d+/\d+\])?\s*", line)
        if building and current_ga is not None:
            selected.append(current_ga + ":" + building.group(1).split()[-1])
            current_ga = None
    return selected


def logged_gavs(path):
    return parsed_gavs(regular(path).read_text(encoding="utf-8"))


def exact_order(selected, expected):
    if len(selected) != 57 or len(set(selected)) != 57 or set(selected) != set(expected):
        raise RuntimeError("native producer order is not exactly the 57 pinned full GAVs")
    return list(selected)


def native_order():
    if tuple(INPUTS["producerSelectors"]) != PRODUCER_SELECTORS:
        raise RuntimeError("native producer selector drift")
    path = regular(ARTIFACTS / "producer-order.log")
    if path.stat().st_size > LOG_LIMIT:
        raise RuntimeError("native order log exceeds bounded evidence")
    text = path.read_text(encoding="utf-8")
    if text.count("[INFO] BUILD SUCCESS") != 1 or "[INFO] BUILD FAILURE" in text:
        raise RuntimeError("native validate success required before using its order")
    return exact_order(parsed_gavs(text), INPUTS["expectedProducerGavs"])


def module_base(module, receiver=RECEIVER):
    name = str(safe_relative(module["module"]))
    if len(PurePosixPath(name).parts) != 1:
        raise RuntimeError("producer must be one pinned module directory")
    base = receiver / "m3/vendor/third_party" / name
    regular(base / "pom.xml")
    if not base.resolve().is_relative_to(receiver.resolve()):
        raise RuntimeError("producer outside receiver")
    return base


def safe_producer_pom(module, receiver=RECEIVER):
    base = module_base(module, receiver)
    pom = regular(base / "pom.xml")
    if blob(pom.read_bytes()) != module["pomBlob"] or gav(pom) != module["gav"]:
        raise RuntimeError("producer source POM drift")
    project = ET.parse(pom).getroot()
    for element in ("parent", "profiles", "modules", "reporting"):
        if project.find("m:" + element, NS) is not None:
            raise RuntimeError("unadmitted inherited or additional producer output")
    build = project.find("m:build", NS)
    if build is not None:
        for element in ("directory", "outputDirectory", "testOutputDirectory", "reportDirectory"):
            if build.find("m:" + element, NS) is not None:
                raise RuntimeError("unadmitted producer output directory")
        for plugin in build.findall("m:plugins/m:plugin", NS):
            if plugin.findtext("m:artifactId", namespaces=NS) == "maven-clean-plugin":
                raise RuntimeError("unadmitted producer clean configuration")
    if project.find(".//m:filesets", NS) is not None:
        raise RuntimeError("unadmitted producer clean filesets")
    return pom


def artifact_paths(module, receiver=RECEIVER, repository=None):
    repository = ROOT / "m2" if repository is None else repository
    parts = module["gav"].split(":")
    if len(parts) != 3 or any(not re.fullmatch(r"[A-Za-z0-9_.-]+", part)
                              or part in (".", "..") for part in parts):
        raise RuntimeError("unsafe producer coordinate")
    group, artifact, version = parts
    base = module_base(module, receiver)
    directory = repository / group.replace(".", "/") / artifact / version
    return (base, base / "target" / (artifact + "-" + version + ".jar"),
            directory / (artifact + "-" + version + ".jar"),
            directory / (artifact + "-" + version + ".pom"))


def capture(module, receiver=RECEIVER, repository=None):
    source_pom = safe_producer_pom(module, receiver)
    base, target, jar, pom = artifact_paths(module, receiver, repository)
    regular(target)
    regular(jar)
    regular(pom)
    emitted = (base / "dependency-reduced-pom.xml"
               if module["module"] in REDUCED_POMS else source_pom)
    regular(emitted)
    if gav(emitted) != module["gav"] or gav(pom) != module["gav"]:
        raise RuntimeError("emitted or installed producer POM coordinate mismatch")
    if emitted.read_bytes() != pom.read_bytes():
        raise RuntimeError("native emitted/installed POM bytes differ")
    target_hash = sha256(target)
    if (jar.stat().st_size == 0 or target.stat().st_size != jar.stat().st_size
            or sha256(jar) != target_hash or target.read_bytes() != jar.read_bytes()):
        raise RuntimeError("source target/installed JAR mismatch")
    with zipfile.ZipFile(jar) as archive:
        classes = sum(name.endswith(".class") for name in archive.namelist())
    if module["module"] != "junit-jupiter-5.10.2" and classes == 0:
        raise RuntimeError("unexpected empty source producer JAR")
    return {"module": module["module"], "gav": module["gav"], "bytes": jar.stat().st_size,
            "sha256": target_hash, "classes": classes,
            "targetEqualsInstalledAtInstallTime": True,
            "targetEqualityScope": "AT_INSTALL_TIME_BEFORE_NATIVE_CLEAN",
            "sourcePomGitBlob": module["pomBlob"],
            "emittedPomKind": ("NATIVE_SHADE_DEPENDENCY_REDUCED_POM"
                               if module["module"] in REDUCED_POMS else "UNCHANGED_SOURCE_POM"),
            "emittedPomRelativePath": str(emitted.relative_to(receiver).as_posix()),
            "emittedPomSha256": sha256(emitted),
            "installedPomSha256": sha256(pom), "installedPomBytes": pom.stat().st_size,
            "freshUpstreamApiComparisonQualified": False}



def preserve_native_pom(module, row):
    if module["module"] not in REDUCED_POMS:
        return
    name = "native-pom-" + module["module"] + ".xml"
    emitted = regular(module_base(module) / "dependency-reduced-pom.xml")
    data = emitted.read_bytes()
    if len(data) > 128 * 1024 or hashlib.sha256(data).hexdigest() != row["emittedPomSha256"]:
        raise RuntimeError("native reduced POM evidence exceeds bound or changed")
    target = ARTIFACTS / name
    temporary = ARTIFACTS / (name + ".tmp")
    if target.exists() or target.is_symlink() or temporary.exists() or temporary.is_symlink():
        raise RuntimeError("native emitted POM evidence must start fresh")
    temporary.write_bytes(data)
    os.replace(temporary, target)
    row["emittedPomEvidence"] = {"path": name, "sha256": row["emittedPomSha256"], "bytes": len(data)}


def require_cleaned(module, receiver=RECEIVER):
    target = module_base(module, receiver) / "target"
    if os.path.lexists(target):
        raise RuntimeError("native clean did not remove the owned producer target")


def retained(module, row, receiver=RECEIVER, repository=None, cleaned=True):
    if row.get("module") != module["module"] or row.get("gav") != module["gav"]:
        raise RuntimeError("producer witness identity drift")
    if row.get("targetEqualsInstalledAtInstallTime") is not True or row.get(
            "targetEqualityScope") != "AT_INSTALL_TIME_BEFORE_NATIVE_CLEAN":
        raise RuntimeError("missing scoped at-install target equality witness")
    _, _, jar, pom = artifact_paths(module, receiver, repository)
    regular(jar)
    regular(pom)
    if jar.stat().st_size != row["bytes"] or sha256(jar) != row["sha256"]:
        raise RuntimeError("retained installed JAR drift")
    with zipfile.ZipFile(jar) as archive:
        classes = sum(name.endswith(".class") for name in archive.namelist())
    if classes != row["classes"] or (module["module"] != "junit-jupiter-5.10.2" and classes == 0):
        raise RuntimeError("retained installed JAR class metadata drift")
    if pom.stat().st_size != row["installedPomBytes"] or sha256(pom) != row["installedPomSha256"]:
        raise RuntimeError("retained installed POM drift")
    if gav(pom) != module["gav"] or row["emittedPomSha256"] != row["installedPomSha256"]:
        raise RuntimeError("retained producer POM identity drift")
    if cleaned:
        require_cleaned(module, receiver)
    return jar, pom


def command_ok(phase, code, text, expected):
    if code != 0 or parsed_gavs(text) != [expected]:
        raise RuntimeError("native " + phase + " failed or selected a different producer")
    if text.count("[INFO] BUILD SUCCESS") != 1 or "[INFO] BUILD FAILURE" in text:
        raise RuntimeError("native " + phase + " has no single successful build")


def log_window(path, start, end):
    if start < 0 or end < start or end - start > LOG_LIMIT:
        raise RuntimeError("command log window exceeds admitted bounded evidence")
    with regular(path).open("rb") as stream:
        stream.seek(start)
        data = stream.read(end - start)
    if len(data) != end - start:
        raise RuntimeError("command log window truncated")
    return data


def run_native(command, phase, expected, path, deadline, inherited=False):
    start = regular(path).stat().st_size
    remaining = deadline - time.monotonic()
    if remaining <= 0:
        raise RuntimeError("serial producer deadline exhausted")
    if inherited:
        sys.stdout.flush()
        inherited_info = os.fstat(sys.stdout.fileno())
        actual_info = path.stat()
        if (inherited_info.st_dev, inherited_info.st_ino) != (actual_info.st_dev, actual_info.st_ino):
            raise RuntimeError("install stdout must inherit the owned producer-install.log")
        done = subprocess.run(command, cwd=ROOT, stderr=subprocess.STDOUT, timeout=remaining, check=False)
    else:
        with path.open("ab", buffering=0) as stream:
            done = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT,
                                  timeout=remaining, check=False)
    end = path.stat().st_size
    data = log_window(path, start, end)
    command_ok(phase, done.returncode, data.decode("utf-8"), expected)
    return {"command": command, "exitCode": done.returncode, "log": path.name,
            "start": start, "end": end, "bytes": len(data),
            "sha256": hashlib.sha256(data).hexdigest(), "selectedGav": expected}


def validate_progress(progress, order):
    if progress.get("state") != "COMPLETE" or progress.get("activePhase") != "COMPLETE":
        raise RuntimeError("serial producer witnesses are incomplete")
    if progress.get("nativeOrder") != order:
        raise RuntimeError("serial producer order changed")
    rows = progress.get("producers", [])
    if len(rows) != 57 or [row.get("gav") for row in rows] != order:
        raise RuntimeError("exact ordered 57 completed producer witnesses required")
    for row in rows:
        for phase in ("install", "clean"):
            event = row.get(phase, {})
            if (event.get("exitCode") != 0 or event.get("selectedGav") != row["gav"]
                    or event.get("log") != "producer-" + phase + ".log"):
                raise RuntimeError("failed native producer lifecycle witness")
        if row.get("targetAbsentAfterNativeClean") is not True or row.get(
                "installedHashesUnchangedAfterNativeClean") is not True:
            raise RuntimeError("native cleanup or retained-artifact witness incomplete")
    return rows


def produce():
    binding()
    order = native_order()
    if (ARTIFACTS / "producer-witnesses.json").exists():
        raise RuntimeError("serial producer admission must start fresh")
    before = json.loads(regular(ARTIFACTS / "source-custody-before.json").read_bytes())
    if before.get("checkoutCommit") != os.environ["EXPECTED_SOURCE_SHA"] or before.get("modules") != 57:
        raise RuntimeError("exact source custody must precede native builds")
    modules = {row["gav"]: row for row in INPUTS["modules"]}
    for module in modules.values():
        safe_producer_pom(module)
        require_cleaned(module)
    clean_log = ARTIFACTS / "producer-clean.log"
    if clean_log.exists() or clean_log.is_symlink():
        raise RuntimeError("native clean log must start fresh")
    clean_log.touch(exist_ok=False)
    settings = ROOT / "home/.m2"
    regular(settings / "settings.xml")
    regular(settings / "global-settings.xml")
    maven = ["mvn", "-B", "-ntp", "-s", str(settings / "settings.xml"),
             "-gs", str(settings / "global-settings.xml"),
             "-Dmaven.repo.local=" + str(ROOT / "m2"), "-Dstyle.color=never", "-N"]
    progress = {"schema": "M3_SERIAL_SOURCE_PRODUCER_WITNESSES_V1", "state": "RUNNING",
                "nativeOrder": order, "orderLogSha256": sha256(ARTIFACTS / "producer-order.log"),
                "activePhase": "READY", "activeModule": None, "producers": [],
                "pendingProducer": None, "upstreamDonorTestsQualified": False}
    deadline = time.monotonic() + 870
    write("producer-witnesses.json", progress)
    try:
        for coordinate in order:
            module = modules[coordinate]
            pom = safe_producer_pom(module)
            progress.update(activePhase="INSTALL", activeModule=module["module"])
            write("producer-witnesses.json", progress)
            event = run_native(maven + ["-f", str(pom), "install"], "install", coordinate,
                               ARTIFACTS / "producer-install.log", deadline, inherited=True)
            row = capture(module)
            preserve_native_pom(module, row)
            row["install"] = event
            progress.update(pendingProducer=row, activePhase="CLEAN")
            write("producer-witnesses.json", progress)
            clean = run_native(maven + ["-f", str(pom), CLEAN_GOAL,
                                      "-Dmaven.clean.fast=false", "-Dmaven.clean.followSymLinks=false",
                                      "-Dmaven.clean.failOnError=true", "-Dmaven.clean.skip=false"],
                               "clean", coordinate, clean_log, deadline)
            retained(module, row)
            row.update(clean=clean, targetAbsentAfterNativeClean=True,
                       installedHashesUnchangedAfterNativeClean=True)
            progress["producers"].append(row)
            progress.update(pendingProducer=None, activePhase="CLEAN_VERIFIED", activeModule=None)
            write("producer-witnesses.json", progress)
        progress.update(state="COMPLETE", activePhase="COMPLETE")
        validate_progress(progress, order)
        write("producer-witnesses.json", progress)
    except BaseException as error:
        progress.update(state="FAILED", activePhase="FAILED", failure=str(error)[:1000])
        write("producer-witnesses.json", progress)
        raise


def installed():
    order = native_order()
    progress = json.loads(regular(ARTIFACTS / "producer-witnesses.json").read_bytes())
    rows = validate_progress(progress, order)
    if logged_gavs(ARTIFACTS / "producer-install.log") != order:
        raise RuntimeError("actual independent install sessions differ from native 57 GAV order")
    if logged_gavs(ARTIFACTS / "producer-clean.log") != order:
        raise RuntimeError("native clean sessions differ from producer order")
    if progress["orderLogSha256"] != sha256(ARTIFACTS / "producer-order.log"):
        raise RuntimeError("native order log changed after producer admission")
    modules = {row["gav"]: row for row in INPUTS["modules"]}
    expected_jars, expected_poms = set(), set()
    for row in rows:
        module = modules[row["gav"]]
        safe_producer_pom(module)
        jar, pom = retained(module, row)
        expected_jars.add(jar)
        expected_poms.add(pom)
        if module["module"] in REDUCED_POMS:
            evidence = row.get("emittedPomEvidence", {})
            name = "native-pom-" + module["module"] + ".xml"
            if evidence.get("path") != name:
                raise RuntimeError("missing native reduced POM evidence")
            emitted = regular(ARTIFACTS / name)
            if (emitted.stat().st_size != evidence.get("bytes")
                    or sha256(emitted) != row["installedPomSha256"]
                    or evidence.get("sha256") != row["installedPomSha256"]
                    or emitted.read_bytes() != pom.read_bytes() or gav(emitted) != row["gav"]):
                raise RuntimeError("native reduced POM evidence differs from retained emitted model")
        for phase in ("install", "clean"):
            event = row[phase]
            data = log_window(ARTIFACTS / event["log"], event["start"], event["end"])
            if len(data) != event["bytes"] or hashlib.sha256(data).hexdigest() != event["sha256"]:
                raise RuntimeError("native producer command log receipt drift")
            command_ok(phase, event["exitCode"], data.decode("utf-8"), row["gav"])
    if expected_jars != set((ROOT / "m2").rglob("*-m3-source-1.jar")):
        raise RuntimeError("unexpected or missing retained source-qualified JAR")
    if expected_poms != set((ROOT / "m2").rglob("*-m3-source-1.pom")):
        raise RuntimeError("unexpected or missing retained source-qualified POM")
    return {"schema": 2, "producers": rows, "count": len(rows), "nativeOrder": order,
            "witnessReceiptSha256": sha256(ARTIFACTS / "producer-witnesses.json"),
            "targetEqualityScope": "AT_INSTALL_TIME_BEFORE_NATIVE_CLEAN",
            "producerTargetsAbsentNow": True, "retainedInstalledArtifactsRehashed": True,
            "launcherGav": "org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1",
            "upstreamDonorTestsQualified": False, "nativeLibrariesRebuilt": False}


def selftest():
    # These fixtures are copies of actual retained source-built artifacts. They never enter ROOT/m2.
    before = installed()
    candidates = [row for row in before["producers"]
                  if row["module"] not in REDUCED_POMS and row["classes"] > 0]
    seed = min(candidates, key=lambda row: row["bytes"])
    if seed["bytes"] > 128 * 1024 or seed["installedPomBytes"] > 32 * 1024:
        raise RuntimeError("real artifact contract fixture exceeds bounded retained sandbox plan")
    module = next(row for row in INPUTS["modules"] if row["gav"] == seed["gav"])
    _, _, original_jar, original_pom = artifact_paths(module)
    order = before["nativeOrder"]
    completed = json.loads(regular(ARTIFACTS / "producer-witnesses.json").read_bytes())
    temporary_root = ROOT / "tmp"
    if not temporary_root.is_dir() or temporary_root.is_symlink():
        raise RuntimeError("explicit owned test temp directory required")

    class SerialArtifactContracts(unittest.TestCase):
        def setUp(self):
            # Retain tiny owned fixtures until hosted job teardown: no concurrent directory deletion.
            self.root = Path(tempfile.mkdtemp(prefix="NOT_PRODUCER-", dir=temporary_root))
            self.receiver, self.repository = self.root / "receiver", self.root / "m2"
            self.module = dict(module)
            base = self.receiver / "m3/vendor/third_party" / module["module"]
            base.mkdir(parents=True)
            shutil.copyfile(module_base(module) / "pom.xml", base / "pom.xml")
            _, self.target, self.jar, self.pom = artifact_paths(self.module, self.receiver, self.repository)
            self.target.parent.mkdir()
            self.jar.parent.mkdir(parents=True)
            shutil.copyfile(original_jar, self.target)
            shutil.copyfile(original_jar, self.jar)
            shutil.copyfile(original_pom, self.pom)

        def copied_capture(self):
            return capture(self.module, self.receiver, self.repository)

        def test_copies_of_real_artifacts_capture(self):
            row = self.copied_capture()
            self.assertEqual(seed["sha256"], row["sha256"])
            self.assertEqual(seed["installedPomSha256"], row["installedPomSha256"])
            self.assertTrue(row["targetEqualsInstalledAtInstallTime"])

        def test_jar_mutation_before_capture_refuses(self):
            with self.jar.open("ab") as stream:
                stream.write(b"NOT_PRODUCER_MUTATION")
            with self.assertRaisesRegex(RuntimeError, "JAR mismatch"):
                self.copied_capture()
            shutil.copyfile(original_jar, self.jar)
            self.target.unlink()
            with self.assertRaisesRegex(RuntimeError, "missing or redirected"):
                self.copied_capture()

        def test_pom_mutation_before_capture_refuses(self):
            with self.pom.open("ab") as stream:
                stream.write(b"\n")
            with self.assertRaisesRegex(RuntimeError, "POM bytes differ"):
                self.copied_capture()

        def test_jar_and_pom_mutations_after_capture_refuse(self):
            row = self.copied_capture()
            mutated_metadata = dict(row, classes=row["classes"] + 1)
            with self.assertRaisesRegex(RuntimeError, "class metadata drift"):
                retained(self.module, mutated_metadata, self.receiver, self.repository, cleaned=False)
            with self.jar.open("ab") as stream:
                stream.write(b"NOT_PRODUCER_MUTATION")
            with self.assertRaisesRegex(RuntimeError, "JAR drift"):
                retained(self.module, row, self.receiver, self.repository, cleaned=False)
            shutil.copyfile(original_jar, self.jar)
            with self.pom.open("ab") as stream:
                stream.write(b"\n")
            with self.assertRaisesRegex(RuntimeError, "POM drift"):
                retained(self.module, row, self.receiver, self.repository, cleaned=False)

        def test_missing_duplicate_wrong_version_native_order_refuse(self):
            self.assertEqual(order, exact_order(order, INPUTS["expectedProducerGavs"]))
            mutations = (order[:-1], order[:-1] + [order[0]],
                         [order[0] + "-WRONG_VERSION", *order[1:]])
            for mutant in mutations:
                with self.subTest(mutant=mutant[-1]):
                    with self.assertRaises(RuntimeError):
                        exact_order(mutant, INPUTS["expectedProducerGavs"])

        def test_failed_install_clean_and_incomplete_witnesses_refuse(self):
            for phase in ("install", "clean"):
                mutant = json.loads(json.dumps(completed))
                mutant["producers"][0][phase]["exitCode"] = 9
                with self.subTest(phase=phase):
                    with self.assertRaisesRegex(RuntimeError, "lifecycle witness"):
                        validate_progress(mutant, order)
                with self.assertRaisesRegex(RuntimeError, "failed"):
                    command_ok(phase, 9, "", order[0])
            mutant = json.loads(json.dumps(completed))
            mutant["producers"].pop()
            with self.assertRaisesRegex(RuntimeError, "57 completed"):
                validate_progress(mutant, order)
            mutant = json.loads(json.dumps(completed))
            mutant["producers"].reverse()
            with self.assertRaisesRegex(RuntimeError, "ordered 57"):
                validate_progress(mutant, order)

        def test_retained_target_and_incomplete_clean_refuse(self):
            with self.assertRaisesRegex(RuntimeError, "did not remove"):
                require_cleaned(self.module, self.receiver)
            mutant = json.loads(json.dumps(completed))
            mutant["producers"][0]["targetAbsentAfterNativeClean"] = False
            with self.assertRaisesRegex(RuntimeError, "cleanup"):
                validate_progress(mutant, order)

        def test_unsafe_output_and_redirected_target_refuse(self):
            base = module_base(self.module, self.receiver)
            source = (base / "pom.xml").read_bytes()
            changed = source.replace(b"<build>", b"<build><directory>../outside</directory>", 1)
            self.assertNotEqual(source, changed)
            (base / "pom.xml").write_bytes(changed)
            self.module["pomBlob"] = blob(changed)
            with self.assertRaisesRegex(RuntimeError, "output directory"):
                safe_producer_pom(self.module, self.receiver)
            (base / "pom.xml").write_bytes(source)
            self.module["pomBlob"] = module["pomBlob"]
            redirected_receiver = self.root / "redirected-receiver"
            redirected_base = redirected_receiver / "m3/vendor/third_party" / self.module["module"]
            redirected_base.mkdir(parents=True)
            shutil.copyfile(module_base(module) / "pom.xml", redirected_base / "pom.xml")
            outside = self.root / "outside"
            outside.mkdir()
            shutil.copyfile(original_jar, outside / self.target.name)
            (redirected_base / "target").symlink_to(outside, target_is_directory=True)
            with self.assertRaises(RuntimeError):
                capture(self.module, redirected_receiver, self.repository)

    suite = unittest.defaultTestLoader.loadTestsFromTestCase(SerialArtifactContracts)
    names = sorted(unittest.defaultTestLoader.getTestCaseNames(SerialArtifactContracts))
    if set(names) != SERIAL_TEST_METHODS:
        raise RuntimeError("serial artifact contract method inventory drift")
    result = unittest.TextTestRunner(stream=sys.stdout, verbosity=2).run(suite)
    after = installed()
    if before != after:
        raise RuntimeError("contract tests changed actual retained producer artifacts")
    receipt = {"schema": "M3_SERIAL_PRODUCER_ARTIFACT_CONTRACT_V1",
               "tests": result.testsRun, "errors": len(result.errors),
               "failures": len(result.failures), "skips": len(result.skipped),
               "successful": result.wasSuccessful(), "methods": names,
               "fixtureGav": seed["gav"], "fixtureJarSha256": seed["sha256"],
               "fixturePomSha256": seed["installedPomSha256"],
               "fixturesAreCopiesOfActualSourceBuiltArtifacts": True,
               "fixtureSandboxPrefix": "ROOT/tmp/NOT_PRODUCER-",
               "fixtureDirectoriesRetainedUntilHostedJobTeardown": True,
               "fixtureJarCopyLimitBytes": 128 * 1024,
               "fixturePomCopyLimitBytes": 32 * 1024,
               "fixtureCopiesLogicalUpperBoundBytes": 17 * (128 + 32) * 1024 + 1024,
               "fixtureArtifactsAdmittedAsProducers": False,
               "actualRetainedArtifactsUnchanged": True,
               "storageCapAndReserveOwnedByOuterSupervisor": True,
               "upstreamDonorTestsQualified": False}
    write("serial-contract-receipt.json", receipt)
    if result.testsRun != 8 or not result.wasSuccessful() or result.skipped:
        raise RuntimeError("all eight actual serial artifact contract tests must execute cleanly")


def report():
    custody = sources()
    artifacts = installed()
    if artifacts != json.loads(regular(ARTIFACTS / "source-artifacts.json").read_bytes()):
        raise RuntimeError("producer artifact changed during consumer verification")
    contracts = json.loads(regular(ARTIFACTS / "serial-contract-receipt.json").read_bytes())
    if (contracts.get("tests") != 8 or contracts.get("successful") is not True
            or any(contracts.get(key) != 0 for key in ("errors", "failures", "skips"))
            or contracts.get("actualRetainedArtifactsUnchanged") is not True
            or contracts.get("fixtureArtifactsAdmittedAsProducers") is not False
            or set(contracts.get("methods", [])) != SERIAL_TEST_METHODS):
        raise RuntimeError("actual serial artifact contract suite is not complete clean nonzero execution")
    consumer = logged_gavs(ARTIFACTS / "tq-verify.log")
    if consumer != ["com.m3:tq-sync:1.0-SNAPSHOT"]:
        raise RuntimeError("unexpected consumer reactor selection")
    tq = RECEIVER / "m3/tooling/tq-sync"
    reports = sorted((tq / "target/surefire-reports").glob("TEST-*.xml"))
    expected_suites = {
        "com.m3.tq.TQSyncTest": {
            "exactBytesAndFixedPoint", "refusesDriftMissingAndDuplicate", "retainsUnrelatedFiles"},
        "com.m3.tq.TQFixtureManifestConsistencyTest": {
            "namedForwardAndInverseReplay", "rejectsUnknownMissingAndDuplicate",
            "keepsUnrelatedAndMatchesCurrentManifest"},
    }
    if len(reports) != 2:
        raise RuntimeError("exactly two fresh actual TQ fixture JUnit suites required")
    suite_receipts = []
    seen = set()
    for path in reports:
        xml = ET.parse(regular(path)).getroot()
        name = xml.get("name")
        if name not in expected_suites or name in seen:
            raise RuntimeError("wrong or duplicate TQ fixture suite")
        expected = expected_suites[name]
        cases = xml.findall("testcase")
        if len(cases) != 3 or any(xml.get(key) != str(value) for key, value in
                                 {"tests": 3, "failures": 0, "errors": 0, "skipped": 0}.items()):
            raise RuntimeError("TQ fixture suite counters not exact nonzero clean execution")
        if {case.get("name") for case in cases} != expected or any(
                case.get("classname") != name or
                any(case.find(tag) is not None for tag in ("failure", "error", "skipped")) for case in cases):
            raise RuntimeError("missing, skipped or failed required TQ fixture method")
        seen.add(name)
        suite_receipts.append({"class": name, "methods": sorted(expected), "tests": 3,
                               "errors": 0, "failures": 0, "skips": 0,
                               "xmlSha256": sha256(path), "xmlBytes": path.stat().st_size})
    if seen != set(expected_suites):
        raise RuntimeError("required TQ fixture JUnit suite missing")
    res = tq / "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-sync"
    generated = tq / "target/generated"
    postimages = []
    for line in regular(res / "manifest.tsv").read_text(encoding="utf-8").splitlines():
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
    live_path = ("m3/tooling/tq-sync/src/main/resources/com/m3/rewrite/backport/"
                 "jdk21-hash-pinned-text/m3-tq-sync/manifest.tsv")
    manifest_root = tq / "target/generated-recipe"
    manifest_output = regular(manifest_root / live_path)
    live_manifest = regular(RECEIVER / live_path)
    if manifest_output.read_bytes() != live_manifest.read_bytes():
        raise RuntimeError("named fixture recipe output is not the exact current committed manifest")
    manifest_files = {str(p.relative_to(manifest_root).as_posix())
                      for p in manifest_root.rglob("*") if p.is_file()}
    if manifest_files != {live_path}:
        raise RuntimeError("named fixture recipe generated an unexpected output set")
    consumer_jar = regular(tq / "target/tq-sync-1.0-SNAPSHOT.jar")
    old_suite = next(row for row in suite_receipts if row["class"] == "com.m3.tq.TQSyncTest")
    receipt = {"schema": "M3_TQ_SOURCE_BUILD_QUALIFICATION_V2",
               "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "sourceCustody": custody,
               "modelReceiptSha256": sha256(regular(ROOT / "model-receipt.json")),
               "sourceArtifactReceiptSha256": sha256(ARTIFACTS / "source-artifacts.json"),
               "serialContractReceiptSha256": sha256(ARTIFACTS / "serial-contract-receipt.json"),
               "serialArtifactContractQualified": True,
               "producerWitnessReceiptSha256": sha256(ARTIFACTS / "producer-witnesses.json"),
               "surefireXmlSha256": old_suite["xmlSha256"], "surefireSuites": suite_receipts,
               "tqSyncFixtureJUnitMethods": old_suite["methods"],
               "tqSyncFixtureJUnitQualified": True, "fixtureManifestJUnitQualified": True,
               "producerBuildQualified": True,
               "producerTargetEqualityScope": "AT_INSTALL_TIME_BEFORE_NATIVE_CLEAN",
               "installedProducerArtifactsRevalidatedAfterTq": True,
               "consumerJarBytes": consumer_jar.stat().st_size, "consumerJarSha256": sha256(consumer_jar),
               "fixturePostimages": postimages, "postimagesVerifiedInJob": True,
               "fixtureManifestOutput": {"path": live_path, "sha256": sha256(manifest_output),
                                         "bytes": manifest_output.stat().st_size,
                                         "equalsCommittedCurrentManifest": True},
               "compiledJarsAndPostimagesExported": False, "currentProductCustodyQualified": False,
               "runtimeTqJniQualified": False, "originalPrivateModelQualified": False,
               "canonicalOverflowQualified": False, "matchedJdkQualified": False,
               "performanceRetentionQualified": False, "phaseAdvanced": False}
    write("build-receipt.json", receipt)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "expand", "before", "produce",
                                               "selftest", "installed", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        binding()
    elif operation == "expand":
        expand()
    elif operation == "before":
        write("source-custody-before.json", sources(require_fresh=True))
    elif operation == "produce":
        produce()
    elif operation == "selftest":
        selftest()
    elif operation == "installed":
        write("source-artifacts.json", installed())
    else:
        report()

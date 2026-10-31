#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Receive sealed current-fork JARs through the existing M3 JMOD owners.

Original Gradle/Maven builds remain external, explicitly receipted inputs. Each
phase is separate so descriptor/probe compilation precedes tests and jlink.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from opennlp_pack import _source_inventory, prepare
from zlibsvm_pack import prepare as prepare_zlibsvm


HERE = Path(__file__).resolve().parent
TARGET = HERE.parents[4]
TOOLS = TARGET / "m3/tooling/jmod-recipes/tools"
BRIDGE = HERE.parent / "m3-bridge"
CAPABILITY = TARGET / "m3/ports/capability-runtime/src/main/java"
DATE = "2026-10-18T00:00:00Z"
SCHEMA = "m3-nlp-search-current-forks-v1"
LUCENE_HEAD = "d1b7235f1ba1c1eea5b04a5b7e6554595a90719e"
RECIPE_MAIN = "com.synexia.rewrite.M3CurrentForkMaterializer"
PROTECTED = ("aggregate", "external-aggregate", "base-pack", "bridge-pack", "compiler-pack", "modular",
             "jmods", "probe-classes", "spi-classes", "compiler-probe-classes",
             "packer-fixtures", "layer", "generated", "capability-classes",
             "bridge-classes", "compiler-classes", "dependency-probe-classes",
             "spatial-core-classes", "spatial-features-classes")


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        value = hashlib.sha256()
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
        return value.hexdigest()


def encoded(value: object) -> bytes:
    return (json.dumps(value, indent=2, sort_keys=True) + "\n").encode()


def load_owner(name: str):
    spec = importlib.util.spec_from_file_location(name, TOOLS / (name + ".py"))
    if spec is None or spec.loader is None:
        raise ValueError("missing existing tooling owner: " + name)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def regular(path: str | Path) -> Path:
    value = Path(path)
    if not value.is_absolute() or value.is_symlink() or not value.is_file():
        raise ValueError("absolute regular input file required: " + str(value))
    if value.resolve() != value:
        raise ValueError("symlink input path refused: " + str(value))
    return value


def jar_records(lock: dict, key: str) -> list[Path]:
    result = []
    for row in lock[key]:
        path = regular(row["path"])
        if digest(path) != row["sha256"] or path in result:
            raise ValueError("duplicate or drifted input: " + str(path))
        result.append(path)
    return result


def input_seal(lock_path: Path, lock: dict) -> dict:
    if lock["schema"] != SCHEMA or lock["lucene"].get("base_commit", lock["lucene"]["commit"]) != LUCENE_HEAD:
        raise ValueError("unadmitted schema or Lucene successor")
    sources = {}
    for name in ("lucene", "opennlp"):
        row = lock[name]
        files = _source_inventory(Path(row["root"]), row["commit"])
        if name == "lucene":
            subprocess.run(["git", "-C", row["root"], "merge-base", "--is-ancestor",
                            LUCENE_HEAD, row["commit"]], check=True, capture_output=True)
        sources[name] = {"commit": row["commit"], "files": len(files),
                         "tree_sha256": hashlib.sha256(encoded(files)).hexdigest()}
    files = {str(lock_path): digest(lock_path)}
    for key in ("opennlp_jars", "lucene_jars", "dependency_jars", "build_receipts"):
        if key != "dependency_jars" and not lock[key]:
            raise ValueError("nonempty input records required: " + key)
        for path in jar_records(lock, key):
            files[str(path)] = digest(path)
    provenance = lock["zlibsvm_provenance"]
    if set(provenance) != {"parent_pom", "license_file", "notice_file"}:
        raise ValueError("exact zlibsvm provenance records required")
    for row in provenance.values():
        path = regular(row["path"])
        if digest(path) != row["sha256"]:
            raise ValueError("dependency provenance drift: " + str(path))
        files[str(path)] = row["sha256"]
    if "spatial4j_pom" in lock:
        row = lock["spatial4j_pom"]
        path = regular(row["path"])
        if digest(path) != row["sha256"]:
            raise ValueError("Spatial4j POM provenance drift")
        files[str(path)] = row["sha256"]
    roots = [HERE, BRIDGE, CAPABILITY]
    for entry in lock["recipe_classpath"]:
        path = Path(entry)
        if path.is_dir() and path.resolve() == path:
            roots.append(path)
        else:
            files[str(regular(path))] = digest(path)
    for root in roots:
        for path in sorted(root.rglob("*")):
            if path.is_file() and "__pycache__" not in path.parts:
                files[str(regular(path))] = digest(path)
    for name in ("jmod_pack.py", "jmod_image.py", "descriptor_gen.py", "split_packages.py"):
        files[str(TOOLS / name)] = digest(TOOLS / name)
    for key in ("jdk_home", "jdk21_home"):
        jdk = Path(lock[key])
        if not (jdk / "lib/modules").is_file():
            raise ValueError("complete JDK runtime image required")
        for path in sorted(jdk.rglob("*")):
            if path.is_symlink():
                if not path.resolve().is_relative_to(jdk.resolve()):
                    raise ValueError("external toolchain symlink requires separate admission")
                files[str(path)] = "symlink:" + os.readlink(path)
            elif path.is_file():
                files[str(regular(path))] = digest(path)
    return {"sources": sources, "files": files}


def run(out: Path, label: str, *command, cwd: Path | None = None) -> str:
    logs = out / "logs"
    logs.mkdir(exist_ok=True)
    number = len(list(logs.glob("*.json"))) + 1
    prefix = logs / (f"{number:03d}-" + label)
    argv = [str(value) for value in command]
    result = subprocess.run(argv, cwd=cwd, capture_output=True, text=True,
                            encoding="utf-8", errors="replace")
    text = result.stdout + result.stderr
    prefix.with_suffix(".log").write_text(text, encoding="utf-8")
    prefix.with_suffix(".json").write_bytes(encoded({"command": argv,
        "cwd": str(cwd or Path.cwd()), "exit_code": result.returncode,
        "log_sha256": digest(prefix.with_suffix(".log"))}))
    print(label, "PASS" if result.returncode == 0 else "FAILED", flush=True)
    if result.returncode:
        raise RuntimeError("command failed; see " + str(prefix.with_suffix(".log")))
    return result.stdout


def tsv(path: Path, header: tuple[str, ...], rows: list[tuple]) -> None:
    with path.open("x", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)


def describe(out: Path, jdk: Path, jar: Path) -> tuple[str, bool]:
    text = run(out, "describe-" + jar.stem, jdk / "bin/jar", "--describe-module",
               "--file", jar, "--release", "25")
    named = next((line for line in text.splitlines() if "module-info.class" in line), None)
    if named is not None:
        return named.split()[0].split("@")[0], True
    automatic = next((line for line in text.splitlines() if line.endswith(" automatic")), None)
    if automatic is None:
        raise ValueError("no exact module identity for " + str(jar))
    return automatic.split()[0].split("@")[0], False


def recipe(out: Path, lock: dict, manifest: Path, layer: Path) -> None:
    layer.mkdir(exist_ok=True)
    run(out, "descriptor-recipe", Path(lock["jdk_home"]) / "bin/java", "-cp",
        os.pathsep.join(lock["recipe_classpath"]), RECIPE_MAIN, "descriptor", manifest,
        layer, out / (manifest.stem + "-recipe.tsv"), "--apply")


def jar(out: Path, jdk: Path, name: str, classes: Path) -> Path:
    destination = out / "modular" / (name + ".jar")
    run(out, "jar-" + name, jdk / "bin/jar", "--create", "--file", destination,
        "--date", DATE, "--no-compress", "-C", classes, ".")
    return destination


def pack(out: Path, lock: dict, name: str, named: list[tuple], manifest: Path,
         layer: Path) -> Path:
    image = out / (name + "-image.tsv")
    tsv(image, ("module", "jar", "disposition"), named)
    destination = out / name
    run(out, name, sys.executable, TOOLS / "jmod_pack.py", "--image-tsv", image,
        "--manifest", manifest, "--layer-root", layer, "--out", destination,
        "--jdk-home", lock["jdk_home"], "--release", "25", "--date", DATE,
        "--jmods-only")
    for path in sorted((destination / "jmods").glob("*.jmod")):
        target = out / "jmods" / path.name
        if target.exists():
            raise ValueError("duplicate final module: " + path.stem)
        shutil.copyfile(path, target)
    return destination


def protected(out: Path) -> dict[str, str]:
    return {str(path.relative_to(out)): digest(path)
            for name in PROTECTED for path in sorted((out / name).rglob("*"))
            if path.is_file()}


def spatial_optional_facts(lock: dict, identities: dict) -> dict | None:
    if "spatial4j" not in identities:
        return None
    row = lock["spatial4j_pom"]
    path = regular(row["path"])
    if digest(path) != row["sha256"]:
        raise ValueError("Spatial4j POM provenance drift")
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    pom = ET.parse(path).getroot()
    gav = tuple(pom.findtext("m:" + key, namespaces=ns)
                for key in ("groupId", "artifactId", "version"))
    if gav != ("org.locationtech.spatial4j", "spatial4j", "0.8"):
        raise ValueError("unadmitted Spatial4j optional dependency owner")
    declared = [tuple(dep.findtext("m:" + key, namespaces=ns)
                      for key in ("groupId", "artifactId", "version"))
                for dep in pom.findall("m:dependencies/m:dependency", ns)
                if dep.findtext("m:optional", namespaces=ns) == "true"]
    expected = {("org.noggit", "noggit", "0.8"),
                ("com.fasterxml.jackson.core", "jackson-databind", "2.9.10.5"),
                ("org.locationtech.jts", "jts-core", "1.17.0")}
    if len(declared) != len(expected) or set(declared) != expected:
        raise ValueError("Spatial4j original optional dependency contract drift")
    modules = ["com.fasterxml.jackson.core", "com.fasterxml.jackson.databind",
               "noggit", "org.locationtech.jts"]
    if any(name not in identities for name in modules):
        raise ValueError("real Spatial4j optional suppliers required for descriptor compilation")
    return {"source_pom": row, "original_optional_coordinates": sorted(declared),
            "public_signature_suppliers": modules,
            "clause_policy": "requires static transitive; retain complete original payload"}


def compile_pack(out: Path, lock_path: Path, lock: dict) -> None:
    before = input_seal(lock_path, lock)
    jdk = Path(lock["jdk_home"])
    version = re.search(r'(?m)^JAVA_VERSION="(\d+)', (jdk / "release").read_text())
    if version is None or int(version[1]) != 25:
        raise ValueError("this pinned fork lane requires a Java25 toolchain")
    if out.exists() or out.is_symlink() or not out.parent.is_dir() or out.parent.resolve() != out.parent:
        raise ValueError("fresh output under an existing non-symlink parent required")
    for source in (TARGET, Path(lock["lucene"]["root"]), Path(lock["opennlp"]["root"]),
                   jdk, Path(lock["jdk21_home"]), *map(Path, lock["recipe_classpath"])):
        if out.is_relative_to(source.resolve()):
            raise ValueError("output must be outside source/toolchain inputs")
    for source_set in ("main", "compiler"):
        if not (BRIDGE / "src" / source_set / "java/module-info.java").is_file():
            raise ValueError("materialize the canonical bridge descriptors before compilation")
    out.mkdir()
    (out / "modular").mkdir()
    (out / "jmods").mkdir()
    aggregate = prepare(Path(lock["opennlp"]["root"]), jar_records(lock, "opennlp_jars"),
                        out / "aggregate", expected_source_head=lock["opennlp"]["commit"])
    dependencies = jar_records(lock, "dependency_jars")
    pair_paths = [path for path in dependencies
                  if path.name in ("zlibsvm-core-3.0.0.jar", "zlibsvm-api-3.0.0.jar")]
    if len(pair_paths) != 2 or len({path.name for path in pair_paths}) != 2:
        raise ValueError("the full toolkit requires both pinned zlibsvm3.0.0 inputs")
    pair = {path.name.removesuffix("-3.0.0.jar"): path for path in pair_paths}
    provenance = lock["zlibsvm_provenance"]
    input_hashes = {row["path"]: row["sha256"] for row in lock["dependency_jars"]}
    zlibsvm = prepare_zlibsvm(list(pair.values()), out / "external-aggregate",
        parent_pom=Path(provenance["parent_pom"]["path"]),
        license_file=Path(provenance["license_file"]["path"]),
        notice_file=Path(provenance["notice_file"]["path"]),
        expected_sha256={**{name: input_hashes[str(path)] for name, path in pair.items()},
            "zlibsvm-parent.pom": provenance["parent_pom"]["sha256"],
            "LICENSE": provenance["license_file"]["sha256"],
            "NOTICE": provenance["notice_file"]["sha256"]})
    input_jars = [*jar_records(lock, "lucene_jars"),
                  *(path for path in dependencies if path not in pair.values()),
                  zlibsvm["merged_jar"], aggregate["merged_jar"]]
    packer = load_owner("jmod_pack")
    identities = {}
    for path in input_jars:
        packer.validate_jar(path, 25)
        name, named = describe(out, jdk, path)
        if name in identities:
            raise ValueError("duplicate module input: " + name)
        identities[name] = (path, named)
    if "org.apache.lucene.core" not in identities or "org.apache.opennlp.tools" not in identities:
        raise ValueError("both actual fork owners are required")
    descriptor_owner = load_owner("descriptor_gen")
    all_classes = set().union(*(descriptor_owner.class_names(path, release=25) for path in input_jars))
    rows = []
    facts = {"org.apache.opennlp.tools": json.loads(aggregate["descriptor_facts"].read_text()),
             "zlibsvm.core": json.loads(zlibsvm["descriptor_facts"].read_text())}
    spatial = spatial_optional_facts(lock, identities)
    for name, (path, named) in sorted(identities.items()):
        if named:
            continue
        generated = out / "generated" / name
        if name == "morfologik.ukrainian.search" and not descriptor_owner.class_names(path, release=25):
            generated.mkdir(parents=True)
            source = generated / "module-info.java"
            # Preserve automatic-module access to the dictionary's real resources.
            source.write_text("open module " + name + " {\n}\n", encoding="utf-8")
            (generated / "RESOURCE_ONLY.json").write_bytes(encoded({"module": name,
                "input_sha256": digest(path), "effective_classes": 0,
                "resource_access": "OPEN_MODULE",
                "jdeps": "NOT_APPLICABLE_NO_BYTECODE", "identity": "ACTUAL_JAR_DESCRIPTOR"}))
        else:
            run(out, "jdeps-" + name, jdk / "bin/jdeps", "--multi-release", "25",
                "--module-path", os.pathsep.join(str(p) for p in input_jars),
                "--generate-module-info", generated, path)
            candidates = list(generated.rglob("module-info.java"))
            if len(candidates) != 1:
                raise ValueError("expected one real generated descriptor: " + name)
            source = candidates[0]
        text = source.read_text()
        if name in facts:
            text = re.sub(r"(?m)^module ", "open module ", text, count=1)
            uses = {match[1] for match in re.finditer(r"\buses\s+([\w.$]+)\s*;", text)}
            additions = "".join("    uses " + service + ";\n" for service in facts[name]["uses"] if service not in uses)
            text = text[:text.rfind("}")] + additions + "}\n"
        if name == "zlibsvm.core":
            exports = set(re.findall(r"\bexports\s+([\w.]+)\s*;", text))
            if exports != set(facts[name]["exports"]):
                raise ValueError("zlibsvm generated exports differ from actual package inventory")
            for requirement in facts[name]["requires"]:
                clause = "requires " + " ".join([*requirement["modifiers"], requirement["module"]]) + ";"
                if clause not in text:
                    raise ValueError("zlibsvm generated public dependency differs from inspected signatures")
        if name == "spatial4j":
            for supplier in spatial["public_signature_suppliers"]:
                pattern = r"(?m)^([ \t]*)requires transitive " + re.escape(supplier) + r";[ \t]*$"
                text, count = re.subn(pattern, lambda match: match[1]
                    + "requires static transitive " + supplier + ";", text)
                if count != 1:
                    raise ValueError("Spatial4j generated optional API clause differs: " + supplier)
            (generated / "OPTIONAL_REQUIRES.json").write_bytes(encoded(spatial))
        text, dropped = descriptor_owner.normalize(text, path, all_classes, release=25)
        if any("package-info only" not in clause for clause in dropped):
            raise ValueError("descriptor would drop a service contract: " + repr(dropped))
        source.write_text(text, encoding="utf-8", newline="\n")
        (generated / "NORMALIZATION.json").write_bytes(encoded(dropped))
        rows.append((name, name, str(path), "descriptors/" + name + "/module-info.java",
                     digest(source), str(source)))
    manifest = out / "DESCRIPTOR_MANIFEST.tsv"
    tsv(manifest, ("module", "artifacts", "unit_jars", "target", "sha256", "generated"), rows)
    layer = out / "layer"
    recipe(out, lock, manifest, layer)
    capability_classes = out / "capability-classes"
    run(out, "compile-capability", jdk / "bin/javac", "--release", "21", "-Xlint:all,-module",
        "-Werror", "-d", capability_classes, *sorted(CAPABILITY.rglob("*.java")))
    capability_jar = jar(out, jdk, "com.m3.runtime.capability", capability_classes)
    named_rows = [(name, str(path), "MODULAR_JAR") for name, (path, named) in sorted(identities.items()) if named]
    named_rows.append(("com.m3.runtime.capability", str(capability_jar), "MODULAR_JAR"))
    base = pack(out, lock, "base-pack", named_rows, manifest, layer)
    for name, (path, named) in sorted(identities.items()):
        classes = base / ("flat" if named else "classes") / name
        jar(out, jdk, name, classes)
    empty = out / "EMPTY_MANIFEST.tsv"
    tsv(empty, ("module", "artifacts", "unit_jars", "target", "sha256", "generated"), [])
    module_path = str(out / "modular")
    run(out, "compile-bridge", jdk / "bin/javac", "--release", "25", "-Xlint:all,-module",
        "-Werror", "--module-path", module_path, "-d", out / "bridge-classes",
        *sorted((BRIDGE / "src/main/java").rglob("*.java")))
    bridge = jar(out, jdk, "com.m3.nlpsearch", out / "bridge-classes")
    pack(out, lock, "bridge-pack", [("com.m3.nlpsearch", str(bridge), "MODULAR_JAR")], empty, layer)
    run(out, "compile-probes", jdk / "bin/javac", "--release", "25", "-Xlint:all,-module",
        "-Werror", "--module-path", module_path, "--patch-module",
        "com.m3.nlpsearch=" + str(BRIDGE / "src/test/java"), "-d", out / "probe-classes",
        *sorted((BRIDGE / "src/test/java").rglob("*.java")))
    run(out, "compile-custom-spi", jdk / "bin/javac", "--release", "25", "-Xlint:all",
        "-Werror", "--module-path", module_path, "--module-source-path",
        HERE / "fixtures/custom-spi/*/src/main/java", "-d", out / "spi-classes", "-m",
        "opennlp.fixture.spi,opennlp.fixture.provider,opennlp.fixture.probe")
    run(out, "compile-dependency-probes", jdk / "bin/javac", "--release", "25", "-Xlint:all,-module",
        "-Werror", "--module-path", module_path, "-d", out / "dependency-probe-classes",
        *sorted((HERE / "fixtures/dependency-probe/src/main/java").rglob("*.java")))
    if spatial is not None:
        for fixture in ("spatial-core", "spatial-features"):
            run(out, "compile-" + fixture, jdk / "bin/javac", "--release", "25", "-Xlint:all,-module",
                "-Werror", "--module-path", module_path, "-d", out / (fixture + "-classes"),
                *sorted((HERE / "fixtures" / fixture / "src/main/java").rglob("*.java")))
    compiler_listing = run(out, "inspect-m3-compiler", jdk / "bin/jimage", "list", "--include",
                           "glob:**/M3AST.class", jdk / "lib/modules")
    compiler = "PENDING_MATCHED_M3_COMPILER_AND_M3STRING_RUNTIME"
    if "com/sun/source/util/M3AST.class" in compiler_listing:
        run(out, "compile-compiler-bridge", jdk / "bin/javac", "-source", "25", "-target", "25",
            "-Xlint:all,-module", "-Werror", "--module-path", module_path, "-d", out / "compiler-classes",
            *sorted((BRIDGE / "src/compiler/java").rglob("*.java")))
        compiler_jar = jar(out, jdk, "com.m3.nlpsearch.compiler", out / "compiler-classes")
        pack(out, lock, "compiler-pack", [("com.m3.nlpsearch.compiler", str(compiler_jar), "MODULAR_JAR")], empty, layer)
        run(out, "compile-m3-compiler-probe", jdk / "bin/javac", "-source", "25", "-target", "25",
            "-Xlint:all,-module", "-Werror", "--module-path", module_path, "--patch-module",
            "com.m3.nlpsearch.compiler=" + str(BRIDGE / "src/compiler-test/java"),
            "-d", out / "compiler-probe-classes", *sorted((BRIDGE / "src/compiler-test/java").rglob("*.java")))
        compiler = "COMPILED_PENDING_CALLBACK_TEST"
    run(out, "compile-packer-fixtures", sys.executable, HERE / "test_jmod_pack.py", "--phase", "prepare",
        "--jdk21-home", lock["jdk21_home"], "--jdk25-home", jdk, "--out", out / "packer-fixtures")
    if input_seal(lock_path, lock) != before:
        raise ValueError("input drift during compile boundary")
    receipt = {"schema": SCHEMA, "stage": "COMPILE", "status": "PASS", "inputs": before,
               "outputs": protected(out), "modules": sorted(p.stem for p in (out / "jmods").glob("*.jmod")),
               "m3_compiler": compiler, "upstream_build_authority": "SUPPLIED_SEALED_RECEIPTS"}
    (out / "COMPILE.json").write_bytes(encoded(receipt))


def verify_compile(out: Path, lock_path: Path, lock: dict) -> dict:
    receipt = json.loads((out / "COMPILE.json").read_text())
    if receipt["status"] != "PASS" or receipt["inputs"] != input_seal(lock_path, lock):
        raise ValueError("compile source/input seal drift")
    if receipt["outputs"] != protected(out):
        raise ValueError("compile output seal drift")
    return receipt


def probes(out: Path, java: Path, phase: str, *, image: bool, compiler: bool,
           lucene_root: Path) -> None:
    module_path = os.pathsep.join(str(out / name) for name in ("spi-classes", "dependency-probe-classes"))
    if not image:
        module_path += os.pathsep + str(out / "modular")
    for main in ("M3SourceInputStreamChecks", "M3NlpSearchChecks"):
        arguments = [out / (phase + "-index")] if main == "M3NlpSearchChecks" else []
        run(out, phase + "-" + main, java, "--module-path", module_path,
            "--patch-module", "com.m3.nlpsearch=" + str(out / "probe-classes"),
            "-m", "com.m3.nlpsearch/com.m3.nlpsearch." + main, *arguments)
    run(out, phase + "-custom-spi", java, "--module-path", module_path, "-m",
        "opennlp.fixture.probe/fixture.probe.CustomSpiProbe")
    run(out, phase + "-dependency-probes", java, "--module-path", module_path, "-m",
        "com.m3.nlpsearch.packchecks/com.m3.nlpsearch.packchecks.PackDependencyChecks", lucene_root)
    for fixture, owner, main in (("spatial-core", "core", "Spatial4jCoreChecks"),
                                 ("spatial-features", "features", "Spatial4jFeatureChecks")):
        classes = out / (fixture + "-classes")
        if classes.is_dir():
            module = "com.m3.nlpsearch.spatialchecks." + owner
            run(out, phase + "-" + fixture, java, "--module-path",
                str(classes) + os.pathsep + module_path, "-m", module + "/" + module + "." + main)
    if compiler:
        run(out, phase + "-m3-compiler", java, "--module-path", module_path, "--patch-module",
            "com.m3.nlpsearch.compiler=" + str(out / "compiler-probe-classes"), "-m",
            "com.m3.nlpsearch.compiler/com.m3.nlpsearch.compiler.M3AstNlpSearchChecks")


def test_pack(out: Path, lock_path: Path, lock: dict) -> None:
    receipt = verify_compile(out, lock_path, lock)
    if (out / "TEST.json").exists():
        raise FileExistsError("test receipt already exists; use a fresh candidate")
    run(out, "test-archive-admission", sys.executable, "-B", "-m", "unittest", "-v",
        "test_opennlp_pack", cwd=HERE)
    pair = {path.name.removesuffix("-3.0.0.jar"): path for path in jar_records(lock, "dependency_jars")
            if path.name in ("zlibsvm-core-3.0.0.jar", "zlibsvm-api-3.0.0.jar")}
    provenance = lock["zlibsvm_provenance"]
    run(out, "test-zlibsvm-admission", sys.executable, "-B", HERE / "test_zlibsvm_pack.py",
        "--core-jar", pair["zlibsvm-core"], "--api-jar", pair["zlibsvm-api"],
        "--parent-pom", provenance["parent_pom"]["path"],
        "--license-file", provenance["license_file"]["path"],
        "--notice-file", provenance["notice_file"]["path"],
        "--out", out / "packer-fixtures/zlibsvm-tests")
    run(out, "test-packer", sys.executable, HERE / "test_jmod_pack.py", "--phase", "test",
        "--jdk21-home", lock["jdk21_home"], "--jdk25-home", lock["jdk_home"],
        "--out", out / "packer-fixtures")
    probes(out, Path(lock["jdk_home"]) / "bin/java", "test", image=False,
           compiler="com.m3.nlpsearch.compiler" in receipt["modules"],
           lucene_root=Path(lock["lucene"]["root"]))
    # The fixture test owner may write its own test result files, not its compiled inputs.
    after = protected(out)
    for path, value in receipt["outputs"].items():
        if after.get(path) != value:
            raise ValueError("compiled input changed during tests: " + path)
    additions = set(after) - set(receipt["outputs"])
    if any(not path.startswith("packer-fixtures/") for path in additions):
        raise ValueError("unexpected compiled artifact additions during tests")
    if receipt["inputs"] != input_seal(lock_path, lock):
        raise ValueError("source input changed during tests")
    (out / "TEST.json").write_bytes(encoded({"schema": SCHEMA, "stage": "TEST", "status": "PASS",
        "compile_sha256": digest(out / "COMPILE.json"), "outputs": after,
        "coverage": "ARCHIVE_ADMISSION_PACKER_CURSOR_COMPOSITE_TEXT_MODEL_DISK_INDEX_CUSTOM_SPI_OPENNLP3_DICTIONARY_LIBSVM",
        "spatial4j": "CORE_AND_FEATURE_PROBES_PASS" if (out / "spatial-core-classes").is_dir() else "NOT_SELECTED",
        "upstream_full_suites": "SEPARATE_RECEIPTS_NOT_IMPLIED"}))


def runtime_pack(out: Path, lock_path: Path, lock: dict) -> None:
    compiled = json.loads((out / "COMPILE.json").read_text())
    tested = json.loads((out / "TEST.json").read_text())
    if (out / "RUNTIME.json").exists() or (out / "image").exists():
        raise FileExistsError("runtime already attempted; use a fresh candidate")
    if (tested["status"] != "PASS" or tested["compile_sha256"] != digest(out / "COMPILE.json")
            or tested["outputs"] != protected(out) or compiled["inputs"] != input_seal(lock_path, lock)):
        raise ValueError("unchanged compile and test receipts required")
    jdk = Path(lock["jdk_home"])
    run(out, "jlink", jdk / "bin/jlink", "--module-path",
        os.pathsep.join(map(str, (jdk / "jmods", out / "jmods"))), "--add-modules",
        ",".join(compiled["modules"]), "--output", out / "image", "--no-header-files", "--no-man-pages")
    java = out / "image/bin/java"
    run(out, "validate-modules", java, "--validate-modules")
    listed = run(out, "list-modules", java, "--list-modules")
    modules = {line.split()[0].split("@")[0] for line in listed.splitlines()}
    if not set(compiled["modules"]) <= modules:
        raise ValueError("linked module closure missing admitted inputs")
    probes(out, java, "runtime", image=True, compiler="com.m3.nlpsearch.compiler" in modules,
           lucene_root=Path(lock["lucene"]["root"]))
    if compiled["inputs"] != input_seal(lock_path, lock) or tested["outputs"] != protected(out):
        raise ValueError("input or compiled artifact drift during runtime")
    (out / "RUNTIME.json").write_bytes(encoded({"schema": SCHEMA, "stage": "RUNTIME", "status": "PASS",
        "test_sha256": digest(out / "TEST.json"), "modules": sorted(modules),
        "m3_compiler": "CALLBACK_PROBE_PASS" if "com.m3.nlpsearch.compiler" in modules else compiled["m3_compiler"],
        "full_m3_family_parity": "NOT_CLAIMED", "m3string_activation": "SEPARATE_MATCHED_RUNTIME_GATE"}))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stage", choices=("compile", "test", "runtime"))
    parser.add_argument("--lock", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    lock_path = regular(args.lock)
    output = args.out.absolute()
    lock = json.loads(lock_path.read_text())
    {"compile": compile_pack, "test": test_pack, "runtime": runtime_pack}[args.stage](output, lock_path, lock)
    print("M3_NLP_SEARCH_CURRENT_FORKS", args.stage.upper(), "PASS")


if __name__ == "__main__":
    main()

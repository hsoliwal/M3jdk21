#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Build all 29 current-fork Lucene production modules on Java 21 preview.

Only the canonical Synexia OpenRewrite SDK transforms staged source or emits
module descriptors. This driver verifies sealed inputs, invokes that owner,
compiles the real library sources, and reuses M3's existing JMOD packager.
The original donor Gradle/Java25 lane and every public source contract remain.
"""
import argparse
import csv
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

HERE = Path(__file__).resolve().parent
DATE = "2000-01-01T00:00:00Z"
DESCRIPTOR_HEADER = ["module", "artifacts", "unit_jars", "target", "sha256", "generated"]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log, env=None):
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open("w", encoding="utf-8") as stream:
        result = subprocess.run(list(map(str, command)), stdout=stream,
                                stderr=subprocess.STDOUT, env=env, check=False)
    if result.returncode:
        raise RuntimeError("command failed (%d); see %s" % (result.returncode, log))


def capture(command):
    return subprocess.check_output(list(map(str, command)), text=True, encoding="utf-8",
                                   stderr=subprocess.PIPE)


def put_file(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)


def safe_relative(text):
    path = Path(text)
    if path.is_absolute() or ".." in path.parts or "\\" in text:
        raise ValueError("unsafe sealed path: " + text)
    return path


def describe(jdk, jar):
    output = capture([jdk / "bin/jar", "--describe-module", "--file", jar, "--release", "21"])
    lines = output.splitlines()
    head = next((line for line in lines if "module-info.class" in line), None)
    if head is None:
        raise ValueError("explicit named module required: " + str(jar))
    name = head.split()[0].split("@")[0]
    requires = []
    for line in lines:
        if line.startswith("requires "):
            parts = line.split()
            requires.append({"module": parts[1], "static": "static" in parts[2:]})
    return name, requires


def release_payload(jar):
    """Compare actual release21 classes/resources, independent of wrapper metadata."""
    with zipfile.ZipFile(jar) as archive:
        view = {}
        for item in archive.infolist():
            if item.is_dir():
                continue
            path = safe_relative(item.filename)
            if not item.filename.startswith("META-INF/versions/"):
                view[str(path)] = archive.read(item)
        versions = sorted({int(x.split("/")[2]) for x in archive.namelist()
                           if x.startswith("META-INF/versions/") and x.split("/")[2].isdigit()
                           and int(x.split("/")[2]) <= 21})
        for version in versions:
            prefix = "META-INF/versions/%d/" % version
            for item in archive.infolist():
                if not item.is_dir() and item.filename.startswith(prefix):
                    view[item.filename[len(prefix):]] = archive.read(item)
    view.pop("META-INF/MANIFEST.MF", None)
    view.pop("module-info.class", None)
    return {key: hashlib.sha256(value).hexdigest() for key, value in sorted(view.items())}


def apply_recipe(args, kind, crate, resources, source, output, label):
    run([sys.executable, args.recipe_runner, "--kind", kind, "--crate", crate,
         "--resources", resources, "--input-root", source,
         "--output-root", output, "--jdk", args.jdk_home],
        args.out / "logs" / (label + ".log"))
    receipt = json.loads((output / "SDK_RECEIPT.json").read_text())
    if receipt["fixed_point_changes"] != 0:
        raise ValueError("canonical recipe did not reach a fixed point")
    refusal_checks = ["drift_refusals", "duplicate_refusals"]
    if kind != "descriptor":
        refusal_checks.append("missing_refusals")
    for check in refusal_checks:
        if receipt.get(check, 0) < 1:
            raise ValueError("canonical recipe refusal gate missing: " + check)
    return receipt


def stage_sources(args):
    lock = json.loads((HERE / "source-lock.json").read_text())
    expected = {row["path"]: row for row in lock["files"]}
    if len(expected) != len(lock["files"]):
        raise ValueError("duplicate source lock paths")
    covered = {"LICENSE.txt", "NOTICE.txt"}
    for module in lock["production_modules"]:
        for root in module["source_roots"]:
            for path in (args.source / root).rglob("*"):
                if path.is_file():
                    if path.is_symlink():
                        raise ValueError("symlink source refused: " + str(path))
                    covered.add(path.relative_to(args.source).as_posix())
    covered.update(str(p.relative_to(args.source)) for p in (args.source / "lucene/licenses").glob("*.txt"))
    if covered != set(expected):
        raise ValueError("source coverage drift: missing=%r extra=%r" %
                         (sorted(set(expected) - covered), sorted(covered - set(expected))))
    stage = args.out / "source"
    for relative, row in sorted(expected.items()):
        source = args.source / safe_relative(relative)
        if not source.is_file() or digest(source) != row["before"]:
            raise ValueError("source hash drift: " + relative)
        put_file(source, stage / relative)
    resources = args.recipe_resources or HERE / "source-overlay/resources"
    manifest = resources / "com/synexia/rewrite/hash-pinned-text" / lock["recipe"] / "manifest.tsv"
    if digest(manifest) != lock["manifest_sha256"]:
        raise ValueError("source recipe manifest hash drift")
    overlay = args.out / "source-overlay"
    receipt = apply_recipe(args, "text", lock["recipe"], resources, stage, overlay, "source-recipe")
    changed = {relative for relative, row in expected.items() if row["before"] != row["after"]}
    if receipt["targets"] != len(changed) or receipt["first_application_changes"] != len(changed):
        raise ValueError("source recipe coverage mismatch")
    actual = {str(p.relative_to(overlay)) for p in overlay.rglob("*.java")}
    if actual != changed:
        raise ValueError("source overlay target mismatch")
    for relative in sorted(changed):
        emitted = overlay / relative
        if digest(emitted) != expected[relative]["after"]:
            raise ValueError("SDK postimage hash mismatch: " + relative)
        put_file(emitted, stage / relative)
    for relative, row in expected.items():
        if digest(stage / relative) != row["after"]:
            raise ValueError("staged source postimage drift: " + relative)
    return stage, lock, receipt


def dependencies(args):
    lock = json.loads((HERE / "dependency-lock.json").read_text())
    directory = args.dependency_dir or args.out / "dependencies"
    if args.dependency_dir is None:
        command = [args.maven, "-B", "-ntp", "-f", HERE / "dependencies.xml"]
        if args.maven_settings:
            command += ["-s", args.maven_settings]
        command += ["org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies",
                    "-DincludeScope=runtime", "-DoutputDirectory=" + str(directory)]
        run(command, args.out / "logs/dependencies.log",
            dict(os.environ, JAVA_HOME=str(args.jdk_home)))
    jars = {}
    for filename, row in lock["artifacts"].items():
        path = directory / filename
        if not path.is_file() or digest(path) != row["sha256"]:
            raise ValueError("dependency missing or hash drift: " + str(path))
        if row["module"] in jars:
            raise ValueError("duplicate locked dependency module")
        jars[row["module"]] = path
        if "descriptor" in row and digest(HERE / row["descriptor"]) != row["descriptor_sha256"]:
            raise ValueError("dependency descriptor template drift: " + filename)
    return jars, lock


def make_jar(args, classes, output, name, version=None):
    command = [args.jdk_home / "bin/jar", "--create", "--file", output, "--date", DATE]
    if version:
        command += ["--module-version", version]
    command += ["-C", classes, "."]
    run(command, args.out / "logs" / (name + "-jar.log"))


def named_dependencies(args, original, lock, flatten_release):
    supplied = {}
    for path in args.opennlp_module:
        name, _ = describe(args.jdk_home, path)
        if name in supplied:
            raise ValueError("duplicate supplied OpenNLP module: " + name)
        supplied[name] = path
    if "org.apache.opennlp.tools" not in supplied:
        raise ValueError("current source-built org.apache.opennlp.tools required")
    for name in set(original) & set(supplied):
        if release_payload(original[name]) != release_payload(supplied[name]):
            raise ValueError("shared module has two class/resource implementations: " + name)
    manifest = args.out / "DEPENDENCY_DESCRIPTOR_MANIFEST.tsv"
    descriptors = {}
    rows_by_module = {row["module"]: row for row in lock["artifacts"].values()}
    with manifest.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(DESCRIPTOR_HEADER)
        for name, row in sorted(rows_by_module.items()):
            if name in supplied or "descriptor" not in row:
                continue
            target = "m3-jmod-descriptors/" + name + "/module-info.java"
            writer.writerow([name, row["coordinate"], "-", target,
                             row["descriptor_sha256"], str(HERE / row["descriptor"])])
            descriptors[name] = target
    empty = args.out / "descriptor-input"
    empty.mkdir()
    descriptor_root = args.out / "dependency-descriptors"
    receipt = apply_recipe(args, "descriptor", manifest, HERE, empty,
                           descriptor_root, "descriptor-recipe")
    jars = args.out / "jars"
    jars.mkdir()
    ready = {}
    for name, path in sorted(supplied.items()):
        target = jars / (name + ".jar")
        put_file(path, target)
        ready[name] = target
    for name, path in sorted(original.items()):
        if name in supplied:
            continue
        row = rows_by_module[name]
        target = jars / (name + ".jar")
        if name not in descriptors:
            actual, _ = describe(args.jdk_home, path)
            if actual != name:
                raise ValueError("locked explicit module identity mismatch: " + name)
            put_file(path, target)
        else:
            descriptor = descriptor_root / descriptors[name]
            if digest(descriptor) != row["descriptor_sha256"]:
                raise ValueError("SDK descriptor postimage mismatch: " + name)
            classes = args.out / "dependency-classes" / name
            flatten_release(path, classes, 21)
            modulepath = list(supplied.values()) + [p for n, p in original.items()
                                                   if n != name and n not in supplied]
            run([args.jdk_home / "bin/javac", "--release", "21", "-proc:none",
                 "--module-path", os.pathsep.join(map(str, modulepath)),
                 "--patch-module", name + "=" + str(path),
                 "-d", classes, descriptor],
                args.out / "logs" / (name + "-descriptor.log"))
            make_jar(args, classes, target, name, row["version"])
            if release_payload(path) != release_payload(target):
                raise ValueError("dependency payload changed during descriptor packaging: " + name)
        ready[name] = target
    return ready, receipt


def compile_lucene(args, stage, lock, ready):
    pending = {module["name"]: module for module in lock["production_modules"]}
    module_names = set(pending)
    report = []
    while pending:
        available = [name for name, row in pending.items()
                     if not (set(row["requires"]) & set(pending))]
        if not available:
            raise ValueError("Lucene module dependency cycle")
        for name in sorted(available):
            row = pending.pop(name)
            source_root = stage / row["directory"] / "src/java"
            sources = sorted(source_root.rglob("*.java"))
            classes = args.out / "classes" / name
            classes.mkdir(parents=True)
            argfile = args.out / "logs" / (name + ".sources")
            argfile.write_text("\n".join('"' + str(p).replace('\\', '\\\\').replace('"', '\\"') + '"'
                                         for p in sources) + "\n")
            run([args.jdk_home / "bin/javac", "--enable-preview", "--release", "21",
                 "-encoding", "UTF-8", "-proc:none", "--module-path", args.out / "jars",
                 "-d", classes, "@" + str(argfile)],
                args.out / "logs" / (name + "-compile.log"))
            resources = 0
            for root_name in row["source_roots"]:
                root = stage / root_name
                for path in root.rglob("*"):
                    if not path.is_file() or (root == source_root and path.suffix == ".java"):
                        continue
                    relative = path.relative_to(root)
                    destination = classes / relative
                    if destination.exists() and destination.read_bytes() != path.read_bytes():
                        raise ValueError("resource collision: " + name + "/" + str(relative))
                    put_file(path, destination)
                    resources += 1
            put_file(stage / "LICENSE.txt", classes / "META-INF/LICENSE.txt")
            put_file(stage / "NOTICE.txt", classes / "META-INF/NOTICE.txt")
            target = args.out / "jars" / (name + ".jar")
            make_jar(args, classes, target, name, "11.0.0-m3-current-" + lock["revision"][:12])
            actual, requires = describe(args.jdk_home, target)
            if actual != name:
                raise ValueError("compiled module identity mismatch")
            ready[name] = target
            report.append({"module": name, "java_sources": len(sources), "resources": resources,
                           "jar_sha256": digest(target), "requires": requires})
            print("COMPILED", name, len(sources), "sources", flush=True)
    if {row["module"] for row in report} != module_names or len(report) != 29:
        raise ValueError("production module closure incomplete")
    return report


def package(args, ready, stage):
    image_tsv = args.out / "JMOD_IMAGE.tsv"
    with image_tsv.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(["module", "version", "jar", "descriptor_kind", "requires", "disposition"])
        for name, jar in sorted(ready.items()):
            actual, requires = describe(args.jdk_home, jar)
            if actual != name:
                raise ValueError("packaging module identity mismatch")
            writer.writerow([name, "source-lock", str(jar), "NAMED",
                             ",".join(row["module"] for row in requires), "MODULAR_JAR"])
    manifest = args.out / "DESCRIPTOR_MANIFEST.tsv"
    manifest.write_text("\t".join(DESCRIPTOR_HEADER) + "\n")
    legal = args.out / "legal"
    put_file(stage / "LICENSE.txt", legal / "lucene-LICENSE.txt")
    put_file(stage / "NOTICE.txt", legal / "lucene-NOTICE.txt")
    for path in (stage / "lucene/licenses").glob("*.txt"):
        put_file(path, legal / ("lucene-dependency-" + path.name))
    for path in (HERE / "legal").rglob("*"):
        if path.is_file():
            put_file(path, legal / path.relative_to(HERE / "legal"))
    run([sys.executable, args.packager, "--image-tsv", image_tsv,
         "--manifest", manifest, "--layer-root", args.out,
         "--out", args.out / "pack", "--jdk-home", args.jdk_home,
         "--add-modules", "java.se,jdk.incubator.vector", "--legal-notices", legal],
        args.out / "logs/jmod-pack.log")
    return image_tsv


def checks(args):
    classes = args.out / "test-classes"
    classes.mkdir()
    names = ["Jdk21ProviderCheck", "Jdk21ExpressionsCheck", "M3LuceneJmodCheck"]
    run([args.jdk_home / "bin/javac", "--enable-preview", "--release", "21",
         "--module-path", args.out / "jars", "--add-modules", "ALL-MODULE-PATH,jdk.incubator.vector",
         "-d", classes, *[HERE / "src/test/java" / (name + ".java") for name in names]],
        args.out / "logs/contract-test-compile.log")
    results = []
    for image in (False, True):
        label = "linked-image" if image else "module-path"
        java = args.out / "pack/image/bin/java" if image else args.jdk_home / "bin/java"
        module_args = ["--add-modules", "ALL-SYSTEM"] if image else [
            "--module-path", args.out / "jars", "--add-modules", "ALL-MODULE-PATH,jdk.incubator.vector"]
        for name in names:
            command = [java, "--enable-preview", "-ea",
                       "--enable-native-access=org.apache.lucene.core,org.apache.lucene.sandbox",
                       *module_args, "-cp", classes, name]
            if name == "M3LuceneJmodCheck":
                command += [args.out / (label + "-test-index")]
            run(command, args.out / "logs" / (label + "-" + name + ".log"))
            results.append({"runtime": label, "test": name, "result": "PASS"})
            print("PASS", label, name, flush=True)
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=HERE.parents[1])
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--packager", type=Path, required=True)
    parser.add_argument("--opennlp-module", type=Path, action="append", required=True)
    parser.add_argument("--dependency-dir", type=Path)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--maven-settings", type=Path)
    parser.add_argument("--recipe-runner", type=Path, required=True)
    parser.add_argument("--recipe-resources", type=Path)
    args = parser.parse_args()
    # Bound compiler/packager/test child JVMs in shared build workers.
    os.environ.setdefault("JAVA_TOOL_OPTIONS", "-Xmx512m")
    for name in ("source", "jdk_home", "out", "packager", "dependency_dir", "maven_settings",
                 "recipe_runner", "recipe_resources"):
        if getattr(args, name) is not None:
            setattr(args, name, getattr(args, name).resolve())
    args.opennlp_module = [p.resolve() for p in args.opennlp_module]
    if args.out.exists():
        parser.error("fresh output directory required: " + str(args.out))
    version = capture([args.jdk_home / "bin/java", "--version"])
    if not re.match(r"(?:openjdk|java) 21(?:[. +\n])", version):
        parser.error("a Java21 JDK with preview support is required")
    spec = importlib.util.spec_from_file_location("m3_existing_jmod_image", args.packager.with_name("jmod_image.py"))
    packager_helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(packager_helper)
    (args.out / "logs").mkdir(parents=True)
    stage, lock, source_receipt = stage_sources(args)
    originals, dependency_lock = dependencies(args)
    ready, descriptor_receipt = named_dependencies(args, originals, dependency_lock, packager_helper.flatten_release)
    modules = compile_lucene(args, stage, lock, ready)
    image_tsv = package(args, ready, stage)
    tests = checks(args)
    report = {"schema": "M3_LUCENE_CURRENT_FORK_JMOD_1", "status": "PASS",
              "source": lock["source"], "revision": lock["revision"],
              "source_lock_sha256": digest(HERE / "source-lock.json"),
              "dependency_lock_sha256": digest(HERE / "dependency-lock.json"),
              "java_release": 21, "preview_required": True, "jdk": version.strip(),
              "production_modules": modules, "named_modules": len(ready),
              "source_recipe_receipt": str(args.out / "source-overlay/SDK_RECEIPT.json"),
              "descriptor_recipe_receipt": str(args.out / "dependency-descriptors/SDK_RECEIPT.json"),
              "source_recipe_changes": source_receipt["first_application_changes"],
              "source_recipe_fixed_point_changes": source_receipt["fixed_point_changes"],
              "descriptor_recipe_fixed_point_changes": descriptor_receipt["fixed_point_changes"],
              "tests": tests, "image_manifest": str(image_tsv),
              "jar_sha256": {name: digest(jar) for name, jar in sorted(ready.items())},
              "qualifications": ["All29 production module base sources compiled; default Gradle25 lane preserved",
                  "Java21 preview FFM is explicit; existing scalar vectors run below25",
                  "Faiss Java binding compiles; optional native Faiss library is not supplied or executed",
                  "Original upstream Gradle25 full randomized suite is a separate donor gate",
                  "JAR timestamps fixed; existing JMOD packager owns JMOD/image timestamps"]}
    (args.out / "BUILD_REPORT.json").write_text(json.dumps(report, indent=2) + "\n")
    print("PASS", len(modules), "Lucene modules;", len(ready), "named modules;", image_tsv, flush=True)


if __name__ == "__main__":
    main()

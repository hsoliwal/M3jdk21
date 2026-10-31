#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build the pinned current Lucene/OpenNLP forks and the optional M3 source integration.

Source changes are canonical Maven/OpenRewrite recipe outputs. This driver verifies
their locks, compiles existing owners, and reuses the M3 JMOD packager. It does not
rewrite Java or replace the older vendored build.py lane.
"""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
MODULE = "jdk.m3.nlp.search"
EPOCH = (2000, 1, 1, 0, 0, 0)


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log, *, env=None):
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run(list(map(str, command)), stdout=output,
                                stderr=subprocess.STDOUT, env=env, check=False)
    if result.returncode:
        raise RuntimeError("command failed (%d): %s; see %s" %
                           (result.returncode, command[0], log))


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def verify_receiver(root):
    lock = json.loads((HERE / "current-source-lock.json").read_text(encoding="utf-8"))
    for module, entry in lock["modules"].items():
        for relative, expected in entry["files"].items():
            path = root / relative
            if not path.is_file() or sha256(path) != expected:
                raise ValueError("receiver source drift: " + relative)
        if entry.get("source_roots"):
            actual = {str(p.relative_to(root)) for source in entry["source_roots"]
                      for p in (root / source).rglob("*.java")}
            expected = {p for p in entry["files"] if p.endswith(".java")}
            if actual != expected:
                raise ValueError("receiver source coverage drift: " + module)
    return lock


def deterministic_jar(classes, target):
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as archive:
        for source in sorted(p for p in classes.rglob("*") if p.is_file()):
            item = zipfile.ZipInfo(str(source.relative_to(classes)), EPOCH)
            item.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(item, source.read_bytes())


def collect_jmods(build, jars, legal, owners):
    paths = sorted((build / "pack/jmods").glob("*.jmod"))
    if not paths:
        raise ValueError("donor produced no JMODs: " + str(build))
    receipt = {}
    for source in paths:
        module = source.stem
        entries = {}
        with zipfile.ZipFile(source) as archive:
            for item in archive.infolist():
                if item.is_dir():
                    continue
                name = item.filename
                if name.startswith("/") or ".." in Path(name).parts:
                    raise ValueError("unsafe JMOD entry: " + name)
                data = archive.read(item)
                if name.startswith("classes/"):
                    relative = name[len("classes/"):]
                    if relative in entries:
                        raise ValueError("duplicate JMOD class/resource: " + name)
                    entries[relative] = data
                elif name.startswith("legal/"):
                    # Donor legal paths are already scoped; reuse them across JMODs.
                    target = legal / name[len("legal/"):]
                    target.parent.mkdir(parents=True, exist_ok=True)
                    if target.exists() and target.read_bytes() != data:
                        raise ValueError("conflicting legal notice: " + name)
                    target.write_bytes(data)
                else:
                    raise ValueError("Unaccounted JMOD section; preserve it explicitly: " + name)
        if "module-info.class" not in entries:
            raise ValueError("donor JMOD has no explicit module descriptor: " + module)
        fingerprint = hashlib.sha256()
        for name, data in sorted(entries.items()):
            encoded = name.encode("utf-8")
            fingerprint.update(len(encoded).to_bytes(4, "big"))
            fingerprint.update(encoded)
            fingerprint.update(len(data).to_bytes(8, "big"))
            fingerprint.update(data)
        content_sha256 = fingerprint.hexdigest()
        if module in owners:
            if owners[module] != content_sha256:
                raise ValueError("different implementations claim the same module: " + module)
        else:
            owners[module] = content_sha256
            with zipfile.ZipFile(jars / (module + ".jar"), "w", zipfile.ZIP_DEFLATED) as archive:
                for name, data in sorted(entries.items()):
                    item = zipfile.ZipInfo(name, EPOCH)
                    item.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(item, data)
        receipt[module] = {"input_jmod_sha256": sha256(source), "content_sha256": content_sha256}
    return receipt


def compile_owner(jdk, root, out, module, entry):
    classes = out / "classes" / module
    classes.mkdir(parents=True)
    sources = [root / p for p in sorted(entry["files"])
               if p.endswith(".java") and p != entry["descriptor"]]
    argfile = out / (module + ".sources")
    argfile.write_text("\n".join('"' + str(p).replace("\\", "\\\\").replace('"', '\\"')
                                + '"' for p in sources) + "\n", encoding="utf-8")
    jars = sorted((out / "jars").glob("*.jar"))
    # Compile the exact existing classpath surface first. Descriptor publication is a separate
    # step so inherited exported-constructor lint does not mutate unrelated source contracts.
    run([jdk / "bin/javac", "--release", "21", "--enable-preview", "-encoding", "UTF-8",
         "-Xlint:all,-preview", "-Werror", "-classpath", os.pathsep.join(map(str, jars)),
         "-d", classes, "@" + str(argfile)], out / (module + "-compile.log"))
    run([jdk / "bin/javac", "--release", "21", "-Xlint:all,-module", "-Werror",
         "--module-path", os.pathsep.join(map(str, jars)), "--patch-module", module + "=" + str(classes),
         "-d", classes, root / entry["descriptor"]], out / (module + "-descriptor.log"))
    deterministic_jar(classes, out / "jars" / (module + ".jar"))


def verify_image(jdk, out):
    image = out / "pack/image"
    tests = out / "tests"
    tests.mkdir()
    services = tests / "META-INF/services"
    services.mkdir(parents=True)
    (services / "com.synexia.mindex.MIndexTensorBackend").write_text(
        "jdk.m3.nlp.search.test.SourceIntegrationProbe$ExternalBackend\n", encoding="utf-8")
    run([image / "bin/javac", "-source", "21", "-target", "21", "--enable-preview",
         "-Xlint:all,-preview", "-Werror", "--add-modules", MODULE,
         "-d", tests, HERE / "tests/SourceFactsProbe.java", HERE / "tests/SourceIntegrationProbe.java",
         HERE / "tests/ModelSourceProbe.java"],
        out / "probe-compile.log")
    for name in ("SourceFactsProbe", "SourceIntegrationProbe", "ModelSourceProbe"):
        command = [image / "bin/java", "--enable-preview", "--add-modules", MODULE,
                   "-ea", "-Xmx512m", "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
                   "-cp", tests, "jdk.m3.nlp.search.test." + name]
        if name == "SourceIntegrationProbe":
            command.append(out / "source-search-fixture")
        run(command, out / (name + ".log"))
    run([image / "bin/java", "--enable-preview", "-m",
         "org.apache.opennlp.tools/opennlp.tools.cmdline.CLI"], out / "opennlp-cli.log")
    run([image / "bin/java", "--validate-modules"], out / "validate-modules.log")
    run([image / "bin/java", "--list-modules"], out / "modules.txt")
    return {name: (out / (name + ".log")).read_text(encoding="utf-8").strip()
            for name in ("SourceFactsProbe", "SourceIntegrationProbe", "ModelSourceProbe")}


def build(args):
    root = args.receiver.resolve()
    jdk = args.jdk_home.resolve()
    for executable in ("java", "javac", "jar", "jmod", "jlink"):
        if not (jdk / "bin" / executable).is_file():
            raise ValueError("complete JDK 21 required: missing " + executable)
    lock = verify_receiver(root)
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    (out / "jars").mkdir()
    (out / "legal").mkdir()
    packager = root / "m3/tooling/jmod-recipes/tools/jmod_pack.py"
    opennlp = args.opennlp_source.resolve()
    lucene = args.lucene_source.resolve()
    environment = dict(os.environ, JAVA_HOME=str(jdk))
    command = [sys.executable, opennlp / "dev/m3-jmod/build.py", "--source-root", opennlp,
               "--jdk-home", jdk, "--packager", packager, "--out", out / "opennlp"]
    if args.opennlp_dependencies:
        command += ["--dependency-dir", args.opennlp_dependencies.resolve()]
    else:
        command += ["--maven", args.maven]
        if args.maven_settings:
            command += ["--maven-settings", args.maven_settings.resolve()]
    run(command, out / "opennlp-build.log", env=environment)
    owners = {}
    donor_receipts = {"opennlp": collect_jmods(out / "opennlp", out / "jars", out / "legal", owners)}
    command = [sys.executable, lucene / ".m3/current-jmods/build.py", "--source", lucene,
               "--jdk-home", jdk, "--out", out / "lucene", "--packager", packager,
               "--recipe-runner", args.recipe_runner.resolve(),
               "--recipe-resources", args.recipe_resources.resolve()]
    for jar in sorted((out / "jars").glob("*.jar")):
        command += ["--opennlp-module", jar]
    if args.lucene_dependencies:
        command += ["--dependency-dir", args.lucene_dependencies.resolve()]
    else:
        command += ["--maven", args.maven]
        if args.maven_settings:
            command += ["--maven-settings", args.maven_settings.resolve()]
    run(command, out / "lucene-build.log", env=environment)
    donor_receipts["lucene"] = collect_jmods(out / "lucene", out / "jars", out / "legal", owners)
    for module in ("com.m3.text", "jdk.m3.compiletime", MODULE):
        compile_owner(jdk, root, out, module, lock["modules"][module])
    with (out / "IMAGE.tsv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("artifact", "version", "jar", "module", "requires", "disposition"))
        for jar in sorted((out / "jars").glob("*.jar")):
            writer.writerow((jar.stem, "source-locked", jar, jar.stem, "-", "JMOD"))
    (out / "DESCRIPTORS.tsv").write_text("module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n")
    run([sys.executable, packager, "--jdk-home", jdk, "--image-tsv", out / "IMAGE.tsv",
         "--manifest", out / "DESCRIPTORS.tsv", "--layer-root", root, "--out", out / "pack",
         "--legal-notices", out / "legal", "--add-modules",
         "java.se,jdk.compiler,jdk.jlink,jdk.jartool,jdk.jdeps,jdk.unsupported,jdk.incubator.vector"],
        out / "pack.log")
    tests = verify_image(jdk, out)
    report = {"schema": "M3_CURRENT_FORK_NLP_SEARCH_1", "receiver": lock["receiver"],
              "source_lock_sha256": sha256(HERE / "current-source-lock.json"),
              "java_release": 21, "preview_required": True,
              "runtime_dependency_on_com_synexia_repository": False,
              "donor_jmods": donor_receipts,
              "module_jars": {p.stem: sha256(p) for p in sorted((out / "jars").glob("*.jar"))},
              "tests": tests}
    write_json(out / "BUILD_REPORT.json", report)
    print("M3_CURRENT_FORK_NLP_SEARCH_PASS modules=%d image=%s" %
          (len(report["module_jars"]), out / "pack/image"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--receiver", type=Path, default=ROOT)
    parser.add_argument("--opennlp-source", type=Path, required=True)
    parser.add_argument("--lucene-source", type=Path, required=True)
    parser.add_argument("--recipe-runner", type=Path, required=True)
    parser.add_argument("--recipe-resources", type=Path, required=True)
    parser.add_argument("--opennlp-dependencies", type=Path)
    parser.add_argument("--lucene-dependencies", type=Path)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--maven-settings", type=Path)
    build(parser.parse_args())


if __name__ == "__main__":
    main()

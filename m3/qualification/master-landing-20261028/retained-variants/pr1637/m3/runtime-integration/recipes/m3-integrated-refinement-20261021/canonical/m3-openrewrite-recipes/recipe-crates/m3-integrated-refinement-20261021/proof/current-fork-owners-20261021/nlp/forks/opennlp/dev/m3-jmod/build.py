#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Build the current OpenNLP source closure and reuse M3JDK21's JMOD packager.

This control compiles source; it does not transform source or generate descriptors.
The checked-in source/dependency locks and descriptors are canonical recipe outputs.
The original Maven reactor remains the authority for the complete donor test suite.
"""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile


HERE = Path(__file__).resolve().parent
CORE = (
    "opennlp-api", "opennlp-core/opennlp-runtime", "opennlp-core/opennlp-cli",
    "opennlp-core/opennlp-formats", "opennlp-core/opennlp-model-resolver",
    "opennlp-core/opennlp-ml/opennlp-ml-commons",
    "opennlp-core/opennlp-ml/opennlp-ml-maxent",
    "opennlp-core/opennlp-ml/opennlp-ml-perceptron",
    "opennlp-core/opennlp-ml/opennlp-ml-bayes",
    "opennlp-core/opennlp-ml/opennlp-ml-libsvm", "opennlp-tools",
)
EXTENSIONS = {
    "dl": "opennlp-core/opennlp-ml/opennlp-dl",
    "dl-gpu": "opennlp-core/opennlp-ml/opennlp-dl-gpu",
    "morfologik": "opennlp-extensions/opennlp-morfologik",
    "spellcheck": "opennlp-extensions/opennlp-spellcheck",
    "uima": "opennlp-extensions/opennlp-uima",
}
BUILT_EXTENSIONS = ("morfologik", "spellcheck", "uima")
DESCRIBED_DEPENDENCIES = {
    "libsvm": "libsvm-3.35.jar",
    "hppc": "hppc-0.7.2.jar",
    "jcommander": "jcommander-1.78.jar",
    "org.carrot2.morfologik.fsa": "morfologik-fsa-2.2.0.jar",
    "org.carrot2.morfologik.fsa_builders": "morfologik-fsa-builders-2.2.0.jar",
    "org.carrot2.morfologik.stemming": "morfologik-stemming-2.2.0.jar",
    "org.carrot2.morfologik.tools": "morfologik-tools-2.2.0.jar",
    "uimaj.core": "uimaj-core-3.6.1.jar",
}
PACKAGE = re.compile(r"^package ([\w.]+);", re.MULTILINE)
MODULE = "org.apache.opennlp.tools"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_paths(root, modules):
    return sorted(str(p.relative_to(root)) for module in modules
                  for p in (root / module / "src/main/java").rglob("*.java"))


def resource_paths(root, modules):
    return sorted(str(p.relative_to(root)) for module in modules
                  for p in (root / module / "src/main/resources").rglob("*") if p.is_file())


def source_poms(root):
    paths = {"pom.xml"}
    for module in (*CORE, *EXTENSIONS.values()):
        directory = Path(module)
        for owner in (directory, *directory.parents):
            candidate = owner / "pom.xml"
            if (root / candidate).is_file():
                paths.add(str(candidate))
    return paths


def inventory(root):
    sources = source_paths(root, CORE)
    extensions = {}
    for name, module in EXTENSIONS.items():
        extensions[name] = {
            "source_files": source_paths(root, (module,)),
            "resource_files": resource_paths(root, (module,)),
            "disposition": "BUILT" if name in BUILT_EXTENSIONS else "OPTIONAL_SEPARATE_DISTRIBUTION",
        }
    return {
        "schema": "OPENNLP_CURRENT_FORK_JMOD_1", "module": MODULE,
        "source_files": sources, "resource_files": resource_paths(root, CORE),
        "packages": sorted({match.group(1) for p in sources
                            if (match := PACKAGE.search((root / p).read_text()))}),
        "source_artifacts": list(CORE), "extensions": extensions,
    }


def run(command, log, cwd=None, env=None):
    with log.open("w", encoding="utf-8") as output:
        completed = subprocess.run(list(map(str, command)), cwd=cwd, env=env,
                                   stdout=output, stderr=subprocess.STDOUT, check=False)
    if completed.returncode:
        raise RuntimeError("command failed (%s); see %s" % (completed.returncode, log))


def source_file(root, relative, overrides):
    candidate = overrides / relative if overrides else None
    return candidate if candidate is not None and candidate.is_file() else root / relative


def verify_sources(root, info, overrides):
    lock = json.loads((HERE / "source-lock.json").read_text())
    paths = set(info["source_files"] + info["resource_files"])
    for extension in info["extensions"].values():
        paths.update(extension["source_files"] + extension["resource_files"])
    paths.update(source_poms(root))
    paths.update(("LICENSE", "NOTICE"))
    if paths != set(lock["files"]):
        raise ValueError("source coverage drift; update and qualify the canonical recipe")
    for relative in sorted(paths):
        file = source_file(root, relative, overrides)
        if digest(file) != lock["files"][relative]:
            raise ValueError("source hash drift: " + relative)
    return lock


def dependencies(args, out):
    lock = json.loads((HERE / "dependency-lock.json").read_text())
    directory = args.dependency_dir.resolve() if args.dependency_dir else out / "dependencies"
    if not args.dependency_dir:
        command = [args.maven, "-B", "-ntp", "-f", HERE / "dependencies.xml"]
        if args.maven_settings:
            command += ["-s", args.maven_settings.resolve()]
        command += ["org.apache.maven.plugins:maven-dependency-plugin:3.11.0:copy-dependencies",
                    "-DincludeScope=runtime", "-DoutputDirectory=" + str(directory)]
        environment = dict(os.environ, JAVA_HOME=str(args.jdk_home.resolve()))
        run(command, out / "dependencies.log", env=environment)
    jars = {}
    for name, row in lock["artifacts"].items():
        file = directory / name
        if not file.is_file() or digest(file) != row["sha256"]:
            raise ValueError("dependency missing or hash mismatch: " + str(file))
        jars[name] = file
    return jars, lock


def put_resource(destination, relative, content):
    target = destination / relative
    if target.exists() and target.read_bytes() != content:
        raise ValueError("nonidentical resource collision: " + relative)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)


def copy_resources(root, modules, destination, version):
    for relative in resource_paths(root, modules):
        file = root / relative
        path = relative.split("/src/main/resources/", 1)[1]
        data = file.read_bytes()
        if path == "opennlp/tools/util/opennlp.version":
            data = data.replace(b"${project.version}", version.encode("utf-8"))
        put_resource(destination, path, data)


def compile_sources(args, root, modules, classpath, destination, log):
    sources = [source_file(root, p, args.source_overrides) for p in source_paths(root, modules)]
    destination.mkdir(parents=True)
    argfile = destination.parent / (destination.name + ".sources")
    argfile.write_text("\n".join('"' + str(p).replace("\\", "\\\\").replace('"', '\\"')
                                + '"' for p in sources) + "\n")
    run([args.jdk_home / "bin/javac", "--release", "21", "-encoding", "UTF-8",
         "-classpath", os.pathsep.join(map(str, classpath)), "-d", destination,
         "@" + str(argfile)], log)


def make_jar(jdk, classes, target, log, module=None):
    command = [jdk / "bin/jar", "--create", "--file", target,
               "--date=2026-10-18T00:00:00Z"]
    if module:
        manifest = target.with_suffix(".manifest")
        content = "Manifest-Version: 1.0\nAutomatic-Module-Name: " + module + "\n"
        if module == MODULE:
            content += "Main-Class: opennlp.tools.cmdline.CLI\n"
        manifest.write_text(content + "\n", encoding="utf-8")
        command += ["--manifest", manifest]
    command += ["-C", classes, "."]
    run(command, log)


def merge_zlibsvm(jdk, jars, out):
    destination = out / "zlibsvm-classes"
    destination.mkdir()
    for name in ("zlibsvm-api-3.0.0.jar", "zlibsvm-core-3.0.0.jar"):
        with zipfile.ZipFile(jars[name]) as archive:
            for item in archive.infolist():
                if item.is_dir() or item.filename.upper() == "META-INF/MANIFEST.MF":
                    continue
                if item.filename.startswith("/") or ".." in Path(item.filename).parts:
                    raise ValueError("invalid dependency archive entry")
                put_resource(destination, item.filename, archive.read(item))
    target = out / "jars/zlibsvm-core-3.0.0.jar"
    make_jar(jdk, destination, target, out / "zlibsvm-jar.log")
    return target


def legal_notices(root, jars, destination):
    destination.mkdir()
    shutil.copyfile(root / "LICENSE", destination / "OPENNLP-LICENSE")
    shutil.copyfile(root / "NOTICE", destination / "OPENNLP-NOTICE")
    for name, jar in jars.items():
        with zipfile.ZipFile(jar) as archive:
            for item in archive.infolist():
                upper = item.filename.upper()
                if item.is_dir():
                    continue
                if any(word in upper for word in ("LICENSE", "NOTICE", "COPYING", "COPYRIGHT")):
                    safe = name + "-" + item.filename.replace("/", "_")
                    (destination / safe).write_bytes(archive.read(item))


def modular_jars(jdk, modules, out):
    """Expose the exact JMOD class view to downstream source compilers."""
    for module in modules:
        classes = out / "modular-classes" / module
        classes.mkdir(parents=True)
        with zipfile.ZipFile(out / "pack/jmods" / (module + ".jmod")) as archive:
            for entry in archive.infolist():
                if entry.is_dir() or not entry.filename.startswith("classes/"):
                    continue
                put_resource(classes, entry.filename.removeprefix("classes/"), archive.read(entry))
        make_jar(jdk, classes, out / "jars" / (module + ".jar"),
                 out / (module + "-modular-jar.log"))


def write_manifests(args, jars, aggregated, out):
    named = {
        "org.slf4j": "slf4j-api-2.0.20.jar",
        "org.slf4j.simple": "slf4j-simple-2.0.20.jar",
        "io.github.classgraph": "classgraph-4.8.197.jar",
        "org.apache.commons.xml.secure": "commons-secure-xml-1.1.0.jar",
    }
    with (out / "IMAGE.tsv").open("w", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("artifact", "version", "jar", "module", "requires", "disposition"))
        for module, name in sorted(named.items()):
            writer.writerow((name, "locked", jars[name], module, "-", "JMOD"))
    units = {MODULE: out / "jars/opennlp-tools.jar", "zlibsvm.core": aggregated}
    units.update({module: jars[name] for module, name in DESCRIBED_DEPENDENCIES.items()})
    units.update({"org.apache.opennlp." + name: out / "jars" / ("opennlp-" + name + ".jar")
                  for name in BUILT_EXTENSIONS})
    with (out / "DESCRIPTORS.tsv").open("w", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("module", "artifacts", "unit_jars", "target", "sha256", "generated"))
        for module, unit in sorted(units.items()):
            descriptor = HERE / "descriptors" / module / "module-info.java"
            writer.writerow((module, module, unit, str(descriptor.relative_to(HERE)),
                             digest(descriptor), descriptor))
    return sorted(set(named) | set(units))


def build(args):
    root = args.source_root.resolve()
    info = inventory(root)
    source_lock = verify_sources(root, info, args.source_overrides)
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    (out / "jars").mkdir()
    jars, dependency_lock = dependencies(args, out)
    classes = out / "classes/opennlp-tools"
    compile_sources(args, root, CORE, jars.values(), classes, out / "core-compile.log")
    copy_resources(root, CORE, classes, source_lock["version"])
    for name in ("LICENSE", "NOTICE"):
        put_resource(classes, "META-INF/" + name, (root / name).read_bytes())
    make_jar(args.jdk_home, classes, out / "jars/opennlp-tools.jar", out / "core-jar.log", MODULE)
    for name in BUILT_EXTENSIONS:
        extension = out / "classes" / ("opennlp-" + name)
        compile_sources(args, root, (EXTENSIONS[name],), [classes, *jars.values()],
                        extension, out / (name + "-compile.log"))
        copy_resources(root, (EXTENSIONS[name],), extension, source_lock["version"])
        make_jar(args.jdk_home, extension, out / "jars" / ("opennlp-" + name + ".jar"),
                 out / (name + "-jar.log"), "org.apache.opennlp." + name)
    aggregated = merge_zlibsvm(args.jdk_home, jars, out)
    legal_notices(root, jars, out / "legal")
    modules = write_manifests(args, jars, aggregated, out)
    run([sys.executable, args.packager.resolve(), "--jdk-home", args.jdk_home,
         "--image-tsv", out / "IMAGE.tsv", "--manifest", out / "DESCRIPTORS.tsv",
         "--layer-root", HERE, "--out", out / "pack", "--legal-notices", out / "legal",
         "--add-modules", "java.se,jdk.unsupported,jdk.compiler,jdk.jartool,jdk.jlink,jdk.jdeps"],
        out / "pack.log")
    modular_jars(args.jdk_home, modules, out)
    info.update({"donor_commit": source_lock["donor_commit"],
                 "source_lock_sha256": digest(HERE / "source-lock.json"),
                 "dependencies": dependency_lock, "modules": modules,
                 "javac_release": 21, "source_transformations_performed": False})
    (out / "BUILD.json").write_text(json.dumps(info, indent=2, sort_keys=True) + "\n")
    run([sys.executable, HERE / "tests/verify.py", "--jdk-home", args.jdk_home,
         "--source-root", root, "--build", out], out / "verification.log")
    print("OPENNLP_JMOD_PASS " + str(out / "pack/image"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, default=HERE.parents[1])
    parser.add_argument("--inventory", action="store_true")
    parser.add_argument("--jdk-home", type=Path)
    parser.add_argument("--out", type=Path)
    parser.add_argument("--packager", type=Path)
    parser.add_argument("--dependency-dir", type=Path)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--maven-settings", type=Path)
    parser.add_argument("--source-overrides", type=Path,
                        help="canonical template directory for pre-publication qualification")
    args = parser.parse_args()
    if args.inventory:
        print(json.dumps(inventory(args.source_root.resolve()), sort_keys=True))
        return
    for name in ("jdk_home", "out", "packager"):
        if getattr(args, name) is None:
            parser.error("--" + name.replace("_", "-") + " is required for a build")
    args.jdk_home = args.jdk_home.resolve()
    if args.source_overrides:
        args.source_overrides = args.source_overrides.resolve()
    build(args)


if __name__ == "__main__":
    main()

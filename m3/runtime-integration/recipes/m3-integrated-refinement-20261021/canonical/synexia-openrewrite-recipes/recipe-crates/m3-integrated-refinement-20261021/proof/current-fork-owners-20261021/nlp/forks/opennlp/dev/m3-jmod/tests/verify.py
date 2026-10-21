#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate real source coverage and behavior in an independently linked JVM."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile


HERE = Path(__file__).resolve().parent


def run(command, output, cwd):
    env = dict(os.environ)
    env.pop("CLASSPATH", None)
    result = subprocess.run(list(map(str, command)), cwd=cwd, env=env,
                            capture_output=True, text=True, check=False)
    output.write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError("probe failed; see " + str(output))
    return result.stdout + result.stderr


def expected_class(source):
    relative = source.split("/src/main/java/", 1)[1]
    return relative[:-5] + ".class"


def source_coverage(root, build, info):
    groups = [("org.apache.opennlp.tools", "opennlp-tools", info["source_files"],
               info["resource_files"])]
    groups += [("org.apache.opennlp." + name, "opennlp-" + name,
                extension["source_files"], extension["resource_files"])
               for name, extension in info["extensions"].items()
               if extension["disposition"] == "BUILT"]
    for module, artifact, sources, resources in groups:
        with zipfile.ZipFile(build / "jars" / (artifact + ".jar")) as jar:
            entries = {item.filename for item in jar.infolist() if not item.is_dir()}
            for path in sources:
                if path.endswith("package-info.java"):
                    continue
                if expected_class(path) not in entries:
                    raise AssertionError("production class missing from module: " + path)
            for path in resources:
                name = path.split("/src/main/resources/", 1)[1]
                data = (root / path).read_bytes()
                if name == "opennlp/tools/util/opennlp.version":
                    data = data.replace(b"${project.version}", b"3.0.0-SNAPSHOT")
                if name not in entries or jar.read(name) != data:
                    raise AssertionError("production resource missing or changed: " + path)
        with zipfile.ZipFile(build / "pack/jmods" / (module + ".jmod")) as jmod:
            embedded = {p.removeprefix("classes/") for p in jmod.namelist()
                        if p.startswith("classes/")}
            # The existing packager replaces JAR manifest module identity with
            # module-info.class; manifests are archive metadata, not source resources.
            if not entries - {"META-INF/MANIFEST.MF"} <= embedded:
                raise AssertionError("JMOD omits compiled class/resource entries: " + module)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--build", type=Path, required=True)
    args = parser.parse_args()
    build = args.build.resolve()
    info = json.loads((build / "BUILD.json").read_text())
    source_coverage(args.source_root.resolve(), build, info)
    image = build / "pack/image"
    test_modules = build / "test-modules"
    with tempfile.TemporaryDirectory(prefix="opennlp-jmod-isolated-") as temporary:
        cwd = Path(temporary)
        run([image / "bin/javac", "-source", "21", "-target", "21", "--system", image,
             "--module-source-path", HERE.parent / "src/test/java",
             "-d", test_modules, "-m", "org.apache.opennlp.m3.probe"],
            build / "test-compile.log", cwd)
        outputs = {}
        for name in ("CustomProviderProbe", "CoreProbe", "ImmutableTextProbe", "ExtensionProbe"):
            outputs[name] = run([image / "bin/java", "-Dorg.slf4j.simpleLogger.defaultLogLevel=warn",
                                 "--module-path", test_modules, "-m",
                                 "org.apache.opennlp.m3.probe/probe." + name],
                                build / (name + ".log"), cwd)
        expected = json.loads((HERE / "baseline-behavior.json").read_text())["core_behavior"]
        if "OPENNLP_CORE_BEHAVIOR=" + expected not in outputs["CoreProbe"]:
            raise AssertionError("named-module behavior differs from pinned donor baseline")
        cli = run([image / "bin/java", "-m", "org.apache.opennlp.tools/opennlp.tools.cmdline.CLI"],
                  build / "cli.log", cwd)
        if "TokenizerME" not in cli or "SentenceDetector" not in cli:
            raise AssertionError("actual OpenNLP CLI providers are missing")
        run([image / "bin/java", "--validate-modules"], build / "validate-modules.log", cwd)
    total = len(info["source_files"]) + sum(len(row["source_files"])
            for row in info["extensions"].values() if row["disposition"] == "BUILT")
    print("OPENNLP_JMOD_VERIFIED coreSources=" + str(len(info["source_files"]))
          + " builtProductionSources=" + str(total))


if __name__ == "__main__":
    main()

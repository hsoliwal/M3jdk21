#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify generated Collections Java/JNI owners on a Linux Java 21 receiver.

This executes the emitted target sources; it is not a transformation engine.
The OpenRewrite task recipe must materialize these sources before qualification.
An isolated patch-module run does not qualify a full M3JDK21 image or other OSes.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

SOURCES = ["src/java.base/share/classes/java/util/" + name + ".java"
           for name in ("Collections", "M3CollectionViews", "M3CollectionMasks")]
NATIVE = "src/java.base/share/native/libjava/M3CollectionMasks.c"
TESTS = ["test/jdk/java/util/Collections/" + name + ".java"
         for name in ("M3CollectionViewsTest", "M3CollectionMasksTest")]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--cc", default="cc")
    parser.add_argument("--temp-directory", type=Path)
    parser.add_argument("--native-temp-directory", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--large-capacity", action="store_true",
                        help="also allocate the separate 256 MiB >2^31 mask-capacity fixture")
    args = parser.parse_args()
    if sys.platform != "linux":
        parser.error("this qualification command has only been validated on Linux")
    repo = args.repository.resolve()
    jdk = args.java_home.resolve()
    receipt = {
        "scope": "GENERATED_SOURCE_JAVA21_PATCH_MODULE_AND_LINUX_JNI",
        "full_product_image": "PENDING", "other_operating_systems": "PENDING",
        "source_sha256": {p: hashlib.sha256((repo/p).read_bytes()).hexdigest()
                          for p in SOURCES + [NATIVE] + TESTS},
        "commands": [],
    }
    def run(command):
        command = [str(part) for part in command]
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        receipt["commands"].append({"command": command, "exit_code": result.returncode,
                                    "output": result.stdout})
        print(result.stdout, end="", flush=True)
        if result.returncode:
            raise RuntimeError("qualification command failed: " + " ".join(command))
        return result.stdout
    try:
        version = run([jdk/"bin/java", "-version"])
        if 'version "21.' not in version and 'version "21"' not in version:
            raise RuntimeError("Java 21 is required")
        with tempfile.TemporaryDirectory(prefix="m3-collections-", dir=args.temp_directory) as temporary, \
             tempfile.TemporaryDirectory(prefix="m3-collections-native-", dir=args.native_temp_directory) as native_temporary:
            work = Path(temporary)
            patch = work/"source"
            classes = work/"classes"
            headers = work/"headers"
            tests = work/"tests"
            bootstrap_source = work/"bootstrap-source"
            bootstrap_classes = work/"bootstrap-classes"
            for directory in (classes, headers, tests, bootstrap_classes):
                directory.mkdir(parents=True)
            inputs = []
            for relative in SOURCES:
                target = patch/Path(relative).relative_to("src/java.base/share/classes")
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(repo/relative, target)
                inputs.append(target)
            run([jdk/"bin/javac", "-J-Xmx256m", "--patch-module", "java.base="+str(patch),
                 "-h", headers, "-d", classes, *inputs])
            library = Path(native_temporary)/"libM3CollectionMasksQualification.so"
            run([args.cc, "-O3", "-std=c17", "-Wall", "-Wextra", "-Werror", "-fPIC", "-shared",
                 "-I"+str(jdk/"include"), "-I"+str(jdk/"include/linux"), "-I"+str(headers),
                 repo/NATIVE, "-o", library])
            symbols = run(["nm", "-D", "--defined-only", library])
            expected_symbol = "Java_java_util_M3CollectionMasks_selected0"
            if expected_symbol not in symbols.split():
                raise RuntimeError("generated JNI header/native export mismatch")
            run([jdk/"bin/javac", "-J-Xmx256m", "--patch-module", "java.base="+str(classes),
                 "-d", tests, repo/TESTS[0]])
            bootstrap_file = bootstrap_source/"java/util/M3CollectionMasksTest.java"
            bootstrap_file.parent.mkdir(parents=True)
            shutil.copyfile(repo/TESTS[1], bootstrap_file)
            run([jdk/"bin/javac", "-J-Xmx256m", "--patch-module",
                 "java.base="+str(classes)+os.pathsep+str(bootstrap_source),
                 "-d", bootstrap_classes, bootstrap_file])
            for mode in ([], ["-Xint"]):
                run([jdk/"bin/java", "-Xmx256m", *mode, "--patch-module", "java.base="+str(classes),
                     "-cp", tests, "M3CollectionViewsTest"])
            base = [jdk/"bin/java", "-Xmx256m", "-Xcheck:jni", "--patch-module",
                    "java.base="+str(classes)+os.pathsep+str(bootstrap_classes)]
            loaded = "-Dtest.m3.masks.native.library="+str(library)
            native = "-Djdk.m3.collectionMasks.native=true"
            for options, test_mode in (([loaded], "full"), ([], "consumer"),
                                       ([native, loaded], "consumer"), ([native], "consumer")):
                run([*base, *options, "java.util.M3CollectionMasksTest", test_mode])
            if args.large_capacity:
                run([jdk/"bin/java", "-Xmx512m", "-Xcheck:jni", "--patch-module",
                     "java.base="+str(classes)+os.pathsep+str(bootstrap_classes), loaded,
                     "java.util.M3CollectionMasksTest", "large"])
        receipt["status"] = "PASSED"
    except Exception as error:
        receipt["status"] = "FAILED"
        receipt["failure"] = str(error)
        raise
    finally:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(receipt, indent=2)+"\n")

if __name__ == "__main__":
    main()

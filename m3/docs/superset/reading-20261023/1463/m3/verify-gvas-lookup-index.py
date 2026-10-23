#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run the real GVAS Java/JNI lookup oracle with each bounded-index fixture."""
from __future__ import annotations

import hashlib
import importlib.util
import os
from pathlib import Path
import tempfile

ROOT = Path(__file__).resolve().parent


def main() -> None:
    spec = importlib.util.spec_from_file_location(
        "m3_gvas_literal_proof", ROOT / "verify-gvas-literal-precompute.py")
    if spec is None or spec.loader is None:
        raise RuntimeError("existing GVAS proof owner is unavailable")
    proof = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(proof)
    jdk = Path(os.environ.get("M3_JDK", os.environ.get(
        "JAVA_HOME", "/usr/lib/jvm/java-21-openjdk-amd64")))
    test = ROOT / "core/test/com/m3/text/M3GvasLookupIndexContractTest.java"
    test_class = "com.m3.text.M3GvasLookupIndexContractTest"
    fixtures = ("small", "normal", "collision64", "collision65", "collision")
    with tempfile.TemporaryDirectory(prefix="m3-gvas-index-") as temporary:
        work = Path(temporary)
        library = proof.compile_sources(work, jdk)
        proof.run(str(jdk / "bin/javac"), "--release", "21", "-Xlint:all", "-Werror",
                  "-cp", str(work), "-d", str(work), str(test))
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
        proof.run(str(jdk / "bin/java"), "-Xcheck:jni", "-ea", "-esa", "-cp", str(work),
                  test_class, "paths", str(library), str(work / "pathname-fixtures"))
        fault_test = ROOT / "lexicon/native/test/m3_fixed_address_path_fault_test.c"
        fault_binary = work / "native-path-fault-test"
        proof.run(os.environ.get("CC", "cc"), "-std=c17", "-Wall", "-Wextra", "-Werror",
                  "-I" + str(jdk / "include"), "-I" + str(jdk / "include/linux"),
                  str(fault_test), "-o", str(fault_binary))
        proof.run(str(fault_binary))
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
        for kind in fixtures:
            image = work / (kind + ".m3gvas")
            proof.run(str(jdk / "bin/java"), "-ea", "-cp", str(work), test_class,
                      "emit", str(image), kind)
            digest = hashlib.sha256(image.read_bytes()).hexdigest()
            proof.run(str(jdk / "bin/java"), "-Xcheck:jni", "-ea", "-cp", str(work),
                      test_class, kind, str(library), str(image), digest, "checks")
    print("M3_GVAS_LOOKUP_INDEX_CONTRACT_PASS fixtures=5")


if __name__ == "__main__":
    main()

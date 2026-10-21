#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""JDK21 self-contained quantize module: strict Java, legal JMOD and optional JNI parity."""
import argparse
import hashlib
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tempfile

MODULE = "com.m3.tools.model.quantize"
MAIN = MODULE + ".BlockInt8Quantizer"
TEST = MODULE + ".tests.BlockInt8QuantizerChecks"


def execute(command, timeout=240):
    completed = subprocess.run(command, capture_output=True, text=True,
                               timeout=timeout, check=True)
    return completed.stdout.strip()


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--jni", action="store_true", help="compile and run C11/JNI parity")
    args = cli.parse_args()
    root = Path(__file__).resolve().parent
    sources = root / "src/main/java"
    java = sources / "com/m3/tools/model/quantize/BlockInt8Quantizer.java"
    descriptor = sources / "module-info.java"
    oracle = root / "src/test/java/com/m3/tools/model/quantize/tests/BlockInt8QuantizerChecks.java"
    native = root / "native/model_quantize_jni.c"
    legal = root / "legal"
    inputs = [java, descriptor, oracle, native]
    rights = [legal / name for name in ("LICENSE.txt", "NOTICE.txt", "THIRD_PARTY_NOTICES.md")]
    for path in inputs + rights:
        if not path.is_file() or path.is_symlink():
            raise ValueError("missing or symlinked source/rights: " + str(path))
    baseline = {path: sha(path) for path in inputs + rights}
    home = (Path(os.environ["JAVA_HOME"]).resolve()
            if os.getenv("JAVA_HOME")
            else Path(shutil.which("javac")).resolve().parent.parent)
    suffix = ".exe" if os.name == "nt" else ""
    java_exe, javac, jmod = [str(home / "bin" / (name + suffix))
                             for name in ("java", "javac", "jmod")]
    if not execute([javac, "-version"]).startswith("javac 21"):
        # javac -version may be emitted on stderr by some distributions;
        # ask the installed JDK a second time, without accepting other versions.
        version = subprocess.run([javac, "-version"], text=True, capture_output=True,
                                 timeout=30, check=True)
        if not (version.stdout + version.stderr).strip().startswith("javac 21"):
            raise RuntimeError("exact Java 21 compiler required")
    with tempfile.TemporaryDirectory(prefix="m3-quantize-receiver-") as directory:
        work = Path(directory)
        classes, tests, headers = work / "classes", work / "tests", work / "headers"
        classes.mkdir()
        tests.mkdir()
        headers.mkdir()
        execute([javac, "--release", "21", "-Xlint:all,-module", "-Werror",
                 "-h", str(headers), "-d", str(classes), str(descriptor), str(java)])
        header = headers / "com_m3_tools_model_quantize_BlockInt8Quantizer.h"
        if not header.is_file():
            raise AssertionError("generated JNI header missing")
        header_text = header.read_text(encoding="utf-8")
        if "Java_com_m3_tools_model_quantize_BlockInt8Quantizer_nativeAbi" not in header_text \
                or "Java_com_m3_tools_model_quantize_BlockInt8Quantizer_packNative" not in header_text:
            raise AssertionError("JNI symbol contract mismatch")
        execute([javac, "--release", "21", "-Xlint:all", "-Werror",
                 "--module-path", str(classes), "--add-modules", MODULE,
                 "-d", str(tests), str(oracle)])
        smoke = execute([java_exe, "--module-path", str(classes),
                         "-m", MODULE + "/" + MAIN, "--self-test"])
        if smoke != "M3_JMOD_TEST_PASS model.quantize":
            raise AssertionError("module self-test: " + smoke)
        baseline_oracle = execute([java_exe, "-Xmx384m", "--module-path", str(classes),
                                   "--add-modules", MODULE, "-cp", str(tests), TEST])
        if not baseline_oracle.startswith("M3_QUANTIZE_JAVA_PASS assertions="):
            raise AssertionError("Java differential oracle: " + baseline_oracle)
        images = []
        for index in range(2):
            result = work / (MODULE + "." + str(index) + ".jmod")
            execute([jmod, "create", "--date", "2026-10-17T00:00:00Z",
                     "--class-path", str(classes),
                     "--legal-notices", str(legal),
                     "--main-class", MAIN, str(result)])
            description = execute([jmod, "describe", str(result)])
            listing = execute([jmod, "list", str(result)])
            if not description.startswith(MODULE + "\n") or \
                    "exports " + MODULE not in description:
                raise AssertionError("JMOD module description")
            for needed in ("legal/LICENSE.txt", "legal/NOTICE.txt",
                           "legal/THIRD_PARTY_NOTICES.md"):
                if needed not in listing:
                    raise AssertionError("JMOD missing required rights: " + needed)
            images.append(sha(result))
        if images[0] != images[1]:
            raise AssertionError("JMOD byte image not reproducible")
        native_result = "NOT_RUN"
        if args.jni:
            host = platform.system()
            if host not in ("Linux", "Darwin"):
                raise RuntimeError("Windows JNI needs separately verified native C lane")
            cc = shutil.which("cc")
            if not cc:
                raise RuntimeError("C compiler unavailable")
            library = work / ("libm3_quant.dylib" if host == "Darwin" else "libm3_quant.so")
            command = [cc, "-std=c11", "-O2", "-Wall", "-Wextra", "-Werror",
                       "-ffp-contract=off", "-fPIC", "-shared",
                       "-I" + str(home / "include"),
                       "-I" + str(home / "include" / ("darwin" if host == "Darwin" else "linux")),
                       str(native), "-o", str(library)]
            if host == "Linux":
                command.extend(["-fsanitize=undefined", "-fno-sanitize-recover=all"])
            command.append("-lm")
            execute(command)
            native_result = execute([java_exe, "-Xcheck:jni", "--module-path", str(classes),
                                     "-m", MODULE + "/" + MAIN,
                                     "--jni-test", str(library.resolve()), sha(library)])
            if native_result != "M3_JNI_TEST_PASS model.quantize":
                raise AssertionError("JNI differential runtime: " + native_result)
        for file, checksum in baseline.items():
            if sha(file) != checksum:
                raise AssertionError("source/legal mutated by proof: " + str(file))
        print("M3_CPU_INT8_RECEIVER_PASS JDK21=PASS JMOD_REPEAT=PASS"
              + " RIGHTS=PASS JNI=" + ("PASS" if args.jni else "NOT_RUN")
              + " FULL_JDK=NOT_RUN")
        print(baseline_oracle)
        if args.jni:
            print(native_result)


if __name__ == "__main__":
    main()

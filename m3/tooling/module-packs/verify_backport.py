#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Execute exact JDK-8368692 product source in isolated Java21 java.base patches.

This is runtime proof of one source backport, not a JDK build, jtreg execution,
Maven/JUnit execution, coverage report or compatibility certification.
"""
import argparse
import json
import os
from pathlib import Path
from build_pack import command, digest, java_home

SOURCE = Path("src/java.base/share/classes/sun/security/util/Password.java")
SOURCE_SHA256 = "5840ee0eac7e23c3a3834dddde70f059f512331a653a3d9ea80faa8d05f0abc4"
POLICY = "jdk.security.password.allowSystemIn"
CASES = (
    ("default", None, None, "ALLOW"),
    ("system-true", "true", None, "ALLOW"),
    ("system-false", "false", None, "DENY"),
    ("system-case-true", "TRUE", None, "ALLOW"),
    ("system-case-false", "FaLsE", None, "DENY"),
    ("security-true", None, "true", "ALLOW"),
    ("security-false", None, "false", "DENY"),
    ("system-wins-allow", "true", "false", "ALLOW"),
    ("system-wins-deny", "false", "true", "DENY"),
    ("system-wins-invalid-security", "true", "invalid", "ALLOW"),
    ("system-invalid", "invalid", None, "INVALID"),
    ("system-empty", "", None, "INVALID"),
    ("system-padded", " true ", None, "INVALID"),
    ("security-invalid", None, "invalid", "INVALID"),
)


def main(argv=None):
    """Pin product source, compile it, run fresh VM cases, emit honest evidence."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[3])
    args = parser.parse_args(argv)
    home = java_home(args.jdk)
    for variable in ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS"):
        if os.environ.get(variable):
            raise ValueError("Uncontrolled VM option environment: " + variable)
    repository = args.repo.resolve(strict=True)
    product = repository / SOURCE
    if digest(product) != SOURCE_SHA256:
        raise ValueError("PASSWORD_SOURCE_DRIFT: a new source requires a reviewed proof pin")
    output = args.out.resolve()
    if output.is_relative_to(home):
        raise ValueError("Output inside JAVA_HOME is forbidden")
    fixture = repository / "test/jdk/sun/security/util/Password"
    probe = fixture / "M3PasswordPolicyCases.java"
    before = {str(product): digest(product), str(probe): digest(probe)}
    output.mkdir(parents=True, exist_ok=False)
    patch = output / "patch"
    patch.mkdir()
    # Stage only the two pinned sources. Pointing javac at the full product tree
    # could implicitly compile unrelated modified java.base sources.
    staged = output / "source" / "sun/security/util"
    staged.mkdir(parents=True)
    staged_product, staged_probe = staged / product.name, staged / probe.name
    staged_product.write_bytes(product.read_bytes())
    staged_probe.write_bytes(probe.read_bytes())
    if (digest(staged_product) != before[str(product)]
            or digest(staged_probe) != before[str(probe)]):
        raise ValueError("Input changed while staging proof sources")
    # --release hides internal JDK APIs. Use the explicitly verified JDK21 system
    # modules, -source/-target 21 and an isolated patch; never a fabricated stub.
    command([home / "bin/javac", "-source", "21", "-target", "21",
             "-Xlint:all", "-Werror", "--patch-module", "java.base=" + str(output / "source"),
             "-d", patch, staged_product, staged_probe], output / "compile.log")
    executions = []
    for interpreter in (False, True):
        for name, system, security, expected in CASES:
            arguments = [home / "bin/java", "-Xcheck:jni"]
            if interpreter:
                arguments.append("-Xint")
            if system is not None:
                arguments.append("-D" + POLICY + "=" + system)
            arguments += ["--patch-module", "java.base=" + str(patch), "-m",
                          "java.base/sun.security.util.M3PasswordPolicyCases", expected]
            if security is not None:
                arguments.append(security)
            label = ("interpreter-" if interpreter else "jit-") + name
            text = command(arguments, output / (label + ".log"), 30)
            if "M3_PASSWORD_POLICY_PASS mode=" + expected not in text:
                raise ValueError("Missing policy proof: " + label)
            executions.append({"case": label, "expected": expected, "exit": 0})
    for path, checksum in before.items():
        if digest(Path(path)) != checksum:
            raise ValueError("Input changed during proof: " + path)
    classes = {str(path.relative_to(patch)): digest(path) for path in sorted(patch.rglob("*.class"))}
    receipt = {"backport": "JDK-8368692", "status": "LOCAL_PATCH_RUNTIME_PROVEN",
               "source": str(SOURCE), "source_sha256": SOURCE_SHA256,
               "fixture_sha256": before[str(probe)], "jdk_release": (home / "release").read_text(),
               "classes": classes, "executions": executions,
               "m3jdk21_product_acceptance": False, "jtreg": "NOT_RUN",
               "maven_junit_jacoco": "NOT_RUN", "console_path": "NOT_TESTED"}
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print("M3_PASSWORD_BACKPORT_PASS fresh_vms=" + str(len(executions)))


if __name__ == "__main__":
    main()

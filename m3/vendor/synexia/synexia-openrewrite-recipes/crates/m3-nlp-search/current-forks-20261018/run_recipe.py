#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run the existing OpenRewrite SDK snapshot/descriptor owner for this current-fork pack."""
import argparse
import os
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--kind", choices=("java", "text", "descriptor"), required=True)
    parser.add_argument("--crate", required=True, help="Crate id; descriptor mode accepts its manifest path")
    parser.add_argument("--resources", type=Path, required=True, help="Classpath resource root")
    parser.add_argument("--input-root", type=Path, required=True)
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--receipt", type=Path)
    parser.add_argument("--jdk", type=Path, required=True)
    args = parser.parse_args()
    classpath = HERE / "target/sdk-classpath.txt"
    if not classpath.is_file():
        parser.error("First run Maven test-compile dependency:build-classpath -Dmdep.outputFile=target/sdk-classpath.txt")
    parts = [args.resources.resolve(), HERE / "target/test-classes", HERE / "target/classes"]
    cp = os.pathsep.join(map(str, parts)) + os.pathsep + classpath.read_text().strip()
    receipt = args.receipt or args.output_root / "SDK_RECEIPT.json"
    working_directory = None
    if args.kind == "descriptor":
        args.crate = str(Path(args.crate).resolve())
        working_directory = str(Path(args.crate).parent)
    result = subprocess.run([str(args.jdk / "bin/java"), "-Xmx768m", "-Xcheck:jni", "-cp", cp,
                    "com.synexia.rewrite.M3NlpSearchCurrentForksRecipeRunner", args.kind, args.crate,
                    str(args.resources.resolve()), str(args.input_root.resolve()),
                    str(args.output_root.resolve()), str(receipt.resolve())], cwd=working_directory)
    sys.exit(result.returncode)

if __name__ == "__main__":
    main()

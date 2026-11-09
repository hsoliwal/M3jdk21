#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run a bounded jtreg selection on the actual built image and preserve every result."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import time


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("fixture", "jdk", "boot", "harness", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--native-path", type=Path)
    parser.add_argument("--java-option", action="append", default=[])
    parser.add_argument("--suite-timeout", type=int, default=1800)
    parser.add_argument("tests", nargs="+")
    args = parser.parse_args()
    fixture, jdk, boot, harness = (getattr(args, name).resolve(strict=True)
                                  for name in ("fixture", "jdk", "boot", "harness"))
    output = args.output.resolve() / args.label
    if output.exists():
        raise ValueError("suite evidence directory must be absent")
    binding = fixture / "SDK_BINDING.json"
    bound = json.loads(binding.read_text())
    for path, expected in bound["jdk_image_sha256"].items():
        if digest(jdk / path) != expected:
            raise ValueError("built image drift: " + path)
    for path, expected in bound["test_overlays_sha256"].items():
        if digest(fixture / "source" / path) != expected:
            raise ValueError("SDK test fixture drift: " + path)
    test_root = fixture / "source/test/jdk"
    for test in args.tests:
        path = Path(test)
        if path.is_absolute() or ".." in path.parts or not (test_root / path).is_file():
            raise ValueError("test must identify a staged exact source file: " + test)
    command = [str(boot / "bin/java"), "-Xmx512m", "-XX:ActiveProcessorCount=2",
               "-jar", str(harness / "lib/jtreg.jar"), "-verbose:summary", "-othervm",
               "-conc:1", "-timeoutFactor:2", "-timeoutHandlerTimeout:15", "-retain:all",
               "-jdk:" + str(jdk), "-dir:" + str(test_root),
               "-w:" + str(output / "work"), "-r:" + str(output / "report"),
               "-vmoptions:-Xmx512m -XX:ActiveProcessorCount=2"]
    if args.native_path:
        command.append("-nativepath:" + str(args.native_path.resolve(strict=True)))
    command.extend("-javaoption:" + option for option in args.java_option)
    command.extend(args.tests)
    output.mkdir(parents=True)
    receipt = {
        "status": "RUNNING", "command": command, "cwd": str(test_root),
        "started_utc": datetime.now(timezone.utc).isoformat(),
        "fixture_sdk_binding_sha256": digest(binding),
        "test_sources_sha256": {test: digest(test_root / test) for test in args.tests},
        "concurrency": 1, "default_test_heap": "512m", "suite_timeout_seconds": args.suite_timeout,
        "full_string_stage_accepted": False, "cuda_device": "NOT_RUN",
    }
    receipt_path = output / "RUN.json"
    receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
    start = time.monotonic()
    exit_code = 125
    signals = []
    environment = dict(os.environ, JAVA_HOME=str(boot))
    with (output / "jtreg.log").open("w") as log:
        process = subprocess.Popen(command, cwd=test_root, env=environment, stdout=log,
                                   stderr=subprocess.STDOUT, start_new_session=True)
        try:
            exit_code = process.wait(timeout=args.suite_timeout)
        except BaseException:
            for sig in (signal.SIGTERM, signal.SIGKILL):
                try:
                    os.killpg(process.pid, sig)
                    signals.append(sig.name)
                except ProcessLookupError:
                    pass
                if sig == signal.SIGTERM:
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        pass
            process.wait(timeout=5)
            exit_code = 124
    receipt.update(status="PASS" if exit_code == 0 else "FAIL", exit_code=exit_code,
                   seconds=round(time.monotonic() - start, 3), termination_signals=signals,
                   finished_utc=datetime.now(timezone.utc).isoformat(),
                   log_sha256=digest(output / "jtreg.log"))
    receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({key: receipt[key] for key in ("status", "exit_code", "seconds")}))
    print("evidence=" + str(output))
    sys.exit(exit_code)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Compile exact Apache-2.0 test references; never import them into the shipped module."""
import os
import pathlib
import subprocess

from synexia_reference import pinned_sources


def main() -> None:
    root = pathlib.Path(__file__).resolve().parents[1]
    jdk = pathlib.Path(os.environ["M3_JDK"])
    paths = [str(path) for path in pinned_sources(root / "compatibility/reference-src")]
    classes = root / "build/reference-test-classes/64a2ea61c73b548413fed6686a9daeeb0b9b0564"
    classes.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            str(jdk / "bin/javac"),
            "--release", "21",
            "--module-path", str(root / "build/com.m3.text.jar"),
            "--add-modules", "com.m3.text",
            "-d", str(classes),
            *paths,
            str(root / "compatibility/SynexiaContractTest.java"),
        ],
        check=True,
    )
    subprocess.run(
        [
            str(jdk / "bin/java"),
            "-ea",
            "--module-path", str(root / "build/com.m3.text.jar"),
            "--add-modules", "com.m3.text",
            "-cp", str(classes),
            "SynexiaContractTest",
        ],
        check=True,
    )


if __name__ == "__main__":
    main()

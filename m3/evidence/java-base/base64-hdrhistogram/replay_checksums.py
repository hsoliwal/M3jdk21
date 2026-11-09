#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""replay_checksums.py [repository root] : DONOR_OVERLAY is the custody CHECKSUMS_TO_JDK v1 already applies.

Runs CHECKSUMS_TO_JDK v1's overlay and texts through donor_overlay.run in --check mode on the repository. The landed
checksum files are a fixed point (exit 0, nothing changed) exactly when the shared custody writes the same bytes as the
recipe's own copy of it: the overlays, the retained originals, OVERLAY_SOURCES.json, PROVENANCE.json, verify_source.py,
the pom, the README and the relocated jdk.m3.codec files. Since DONOR_OVERLAY v2 the recipe calls donor_overlay
itself, and the replay is its own --check.
"""
import importlib.util
from pathlib import Path
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[4]
sys.dont_write_bytecode = True


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


recipe = load("checksums_to_jdk", ROOT / "m3/tooling/jdk-superset/hashing/tools/checksums_to_jdk.py")
donor_overlay = load("donor_overlay", ROOT / "m3/tooling/jdk-superset/retarget/tools/donor_overlay.py")
donor_overlay.REPO = ROOT
if not hasattr(recipe, "README_TEXT"):  # the recipe before DONOR_OVERLAY v2 carried its own custody
    recipe.README_TEXT = tuple(zip(recipe.README_OLD, recipe.README_NEW))
    recipe.RELOCATED = {recipe.RELOCATED_DONOR: (recipe.RELOCATED, recipe.RELOCATION, recipe.RELOCATED_PATH)}
sys.exit(donor_overlay.run(recipe, ["--check"]))

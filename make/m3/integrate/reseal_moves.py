#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# FLLDCIM: MRL - M3_TO_JDK_LAYOUT reseal: reuse the JDK_NAMING_RESEAL owner with this phase's MAPPING moves
"""Carry digest pins after an M3_TO_JDK_LAYOUT phase with the existing reseal owner (jdk-naming/reseal.py).

reseal.py takes its moves from the naming plan; here they come from MAPPING.tsv (the phase's MOVE/AMBIGUOUS rows).
Path tokens are carried by m3_integrate.py refs (root forms and relative forms); reseal.py's suffix path carrying is
switched off, so nested mirrors (before/m3/..., overlay/m3/...) keep their bytes. The integrate directory (MAPPING.tsv
pins base blob ids on purpose) and every RETIRE row are not resealed. Passes run to a fixed point and are squashed
into one commit on --ref; the ledger of carried values is written to make/m3/integrate/RESEAL_<phase>.tsv.
usage: python3 -I reseal_moves.py --root REPO --base BASE --tree REV --ref REF --phase P2 --reseal DIR/reseal.py
"""
from __future__ import annotations

import argparse
import importlib.util
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
PROTECT = ("make/m3/integrate/",)


def load(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--root", required=True)
    ap.add_argument("--base", required=True)
    ap.add_argument("--tree", required=True)
    ap.add_argument("--ref", required=True)
    ap.add_argument("--phase", required=True)
    ap.add_argument("--name", help="ledger name RESEAL_<name>.tsv (default: the phase); one per P1 leaf")
    ap.add_argument("--mapping", default=str(HERE / "MAPPING.tsv"))
    ap.add_argument("--reseal", required=True, help="path of jdk-naming/reseal.py (with jdk_naming.py beside it)")
    ap.add_argument("--message", default="M3_TO_JDK_LAYOUT: reseal digest pins after the moves (recipe JDK_NAMING_RESEAL)")
    a = ap.parse_args(argv)
    integrate = load(HERE / "m3_integrate.py", "m3_integrate")
    rows = integrate.load_mapping(a.mapping, set(a.phase.split(",")), {"MOVE", "AMBIGUOUS"})
    every = integrate.load_mapping(a.mapping, {"P1", "P2", "P3", "P4"}, {"RETIRE"})
    retired = {r[0] for r in every}
    moves = [(r[0], r[1]) for r in rows if r[1] != "RETIRE"]
    reseal = load(Path(a.reseal), "reseal_owner")
    jn = reseal.jn
    jn.plan_moves = lambda paths, only=(): {"moves": moves}
    # the naming owner's own files stay out; its NAMING_KEEP rows (names kept by the rename) are resealed here, since a
    # moved or rewritten file changes the digests they pin
    jn.is_self = lambda p: p.startswith(jn.SELF_PREFIX) or p in jn.SELF or p.startswith(PROTECT) or p in retired
    reseal.LEDGER = f"make/m3/integrate/RESEAL_{a.name or a.phase.replace(',', '_')}.tsv"
    original = reseal.rename_pins

    def digest_pins_only(root, base, rev):
        pins, unmapped, new = original(root, base, rev)
        for old in [o for o, k in pins.kind.items() if k == "PATH"]:
            del pins.map[old]
            del pins.kind[old]
        pins.clean()
        return pins, unmapped, new

    reseal.rename_pins = digest_pins_only
    sha = reseal.reseal_to_fixed_point(Path(a.root), a.base, a.tree, a.ref, a.message)
    print(f"RESEALED\t{a.ref}\t{sha}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

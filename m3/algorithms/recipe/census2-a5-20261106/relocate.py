#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_WAVE9_FLOW_CONTRACT v1 (census2 leaf P2-070, recipe-first port, never by hand).

Lineage: derived 2026-11-06 from m3/arrays/recipe/bitmatrix-20261106/relocate.py v3 (RELOCATE_BIT_MATRIX): the Java
path is kept (header, PACKAGE, import, TYPE and LITERAL rows, the //FLLDCIM: anchor, PROVENANCE.tsv with body_match
and body_match_renamed, NEC/OMIT rows printed as HELD lines, idempotence and --check); the C/JNI/CMake path is
removed (no native file here); two row kinds are added:

  EDIT rows name a declared edit of a donor file (its id in EDITS below) and pin the sha256 of the donor bytes it
  replaces. Edits run first, on the donor bytes, in map order: a SUB edit replaces a text that must occur exactly once,
  a SPAN edit replaces the bytes from a unique start text up to (not including) the next end text. A pin mismatch, an
  absent or repeated anchor refuses (exit 2) before anything is written. PROVENANCE.tsv adds body_match_edited: the
  target body equals the donor body after the EDIT rows and then the TYPE/LITERAL renames (only declared bytes differ).
  TEMPLATE rows name a module file the recipe emits from TEMPLATES below (the surefire entry of a relocated main
  contract, as m3/algorithms/src/test/java/jdk/m3/algorithm/M3MinCostFlowOracleTest.java is for the mincost-flow leaf);
  its sha256 is pinned in PROVENANCE.tsv with donor columns "-".

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/algorithms).

usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "relocate.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/algorithms/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "body_match_edited", "edits", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("NEC", "OMIT")
NAIVE = "na" + chr(0xEF) + "ve"
# The donor's own package, spelled in two parts: jdk_naming.py (rule PKG_COM_M3) rewrites the joined token in every
# tracked text, which would move this anchor off the donor bytes (RECIPE_MEMORY.tsv row 1).
DONOR_PACKAGE = "com" + ".m3.tools.cpu"

# The flow half of the wave9 contract, mapped onto the landed owner jdk.m3.algorithm.MinCostFlow (PR #2551):
# solve(vertices, from, to, capacity, unitCost, source, sink, demand) -> Result {flow, cost, edgeFlows}; it refuses
# self-loop edges and has no work budget. The rectangle-intersection half has no M3 owner (M3PackedSpatialIndex).
ADAPTER = """    //FLLDCIM: MRL - census2 P2-070 (crate m3-cpu-structured-wave9-20261031): the wave9 call shape over the landed owner
    /**
     * The wave9 call shape {@code long[] {flow, cost}} over the landed {@link M3MinCostFlow}. The owner refuses a
     * self-loop edge, so each instance that has one is first checked as that refusal and then solved without its
     * self-loops: costs are nonnegative and a self-loop moves no flow, so the exhaustive oracle's optimum over the
     * full instance is the optimum without them. The work budget has no owner counterpart and is not used.
     */
    private static long[] solve(int vertices, int[] from, int[] to, int[] cap, int[] cost,
            int source, int sink, int demand, long budget) {
        int kept = 0;
        for (int i = 0; i < from.length; i++) {
            if (from[i] != to[i]) { kept++; }
        }
        if (kept < from.length) {
            refuses(IllegalArgumentException.class,
                    () -> M3MinCostFlow.solve(vertices, from, to, cap, cost, source, sink, demand));
        }
        int[] f = new int[kept], t = new int[kept], c = new int[kept], w = new int[kept];
        for (int i = 0, k = 0; i < from.length; i++) {
            if (from[i] != to[i]) {
                f[k] = from[i];
                t[k] = to[i];
                c[k] = cap[i];
                w[k] = cost[i];
                k++;
            }
        }
        M3MinCostFlow.Result result = M3MinCostFlow.solve(vertices, f, t, c, w, source, sink, demand);
        return new long[] {result.flow(), result.cost()};
    }

"""

# A census2 addition (not donor text): one costed reverse-arc reroute. Mutant M1 of verify.sh (reverse residual arcs
# priced +cost) survives the donor's random half (also at 300,000 networks) and the module's donor oracle
# M3MinCostFlowOracle; on this diamond it returns cost 10 for the optimum 8 (RECIPE_MEMORY.tsv row 2).
REROUTE = """        //FLLDCIM: MRL - census2 P2-070 addition: a costed reverse-arc reroute; the first shortest path 0-1-2-3 is undone
        int[] rerouteFrom = {0, 0, 1, 1, 2}, rerouteTo = {1, 2, 2, 3, 3};
        int[] rerouteCap = {1, 1, 1, 1, 1}, rerouteCost = {1, 3, 1, 3, 1};
        long[] rerouteBest = {-1L, Long.MAX_VALUE};
        flowOracle(0, rerouteFrom, rerouteTo, rerouteCap, rerouteCost, new int[5], new long[4], 0, 3, 2, 0L, rerouteBest);
        check(Arrays.equals(rerouteBest, new long[] {2L, 8L}) && Arrays.equals(rerouteBest,
                solve(4, rerouteFrom, rerouteTo, rerouteCap, rerouteCost, 0, 3, 2, 10L)), "costed reverse-arc reroute");
"""

# id -> (kind, old or (start, end), new). The sha256 of the replaced donor bytes is pinned by the EDIT row of the map.
EDITS = {
    "E1_PACKAGE": ("SUB", "\nimport " + DONOR_PACKAGE + ".M3MinCostFlow;\nimport " + DONOR_PACKAGE + ".M3PackedSpatialIndex;\n",
                   "package jdk.m3.algorithm;\n\n"),
    "E2_DOC": ("SUB", "/** Independent exhaustive small-flow and " + NAIVE + " rectangle-intersection oracle. */\n",
               "/**\n * Independent exhaustive small-flow oracle: the flow half of the wave9 contract, mapped onto the"
               " landed\n * {@link M3MinCostFlow}. The rectangle-intersection half has no M3 owner and is not relocated."
               "\n */\n"),
    "E3_CALL_RANDOM": ("SUB", "long[] actual = M3MinCostFlow.solve(", "long[] actual = solve("),
    "E10_REROUTE_CASE": ("SUB", "        }\n        check(Arrays.equals(M3MinCostFlow.solve(2, new int[0], new int[0],\n",
                         "        }\n" + REROUTE + "        check(Arrays.equals(M3MinCostFlow.solve(2, new int[0], new int[0],\n"),
    "E4_CALL_EMPTY":("SUB", "check(Arrays.equals(M3MinCostFlow.solve(2, new int[0]", "check(Arrays.equals(solve(2, new int[0]"),
    "E5_DROP_BUDGET_REFUSAL": ("SUB", "        refuses(IllegalStateException.class,\n"
                               "                () -> M3MinCostFlow.solve(2, new int[] {0}, new int[] {1},\n"
                               "                        new int[] {1}, new int[] {1}, 0, 1, 1, 0L));\n",
                               "        // The owner has no work budget: the donor's zero-budget refusal has no counterpart.\n"),
    "E6_CAPACITY_REFUSAL": ("SUB", "new int[] {-1}, new int[] {1}, 0, 1, 1, 10L));", "new int[] {-1}, new int[] {1}, 0, 1, 1));"),
    "E7_COST_REFUSAL": ("SUB", "new int[] {1}, new int[] {-1}, 0, 1, 1, 10L));", "new int[] {1}, new int[] {-1}, 0, 1, 1));"),
    "E8_SPATIAL_TO_ADAPTER": ("SPAN", ("    private static int[] naive(", "    public static void main("), ADAPTER),
    "E9_MAIN": ("SUB", "        flowCases();\n        spatialCases();\n"
                "        System.out.println(\"M3_WAVE9_FLOW_SPATIAL_PASS checks=\" + checks);\n",
                "        flowCases();\n        System.out.println(\"M3_MINCOST_FLOW_WAVE9_PASS checks=\" + checks);\n"),
}

FIGURE = "M3_MINCOST_FLOW_WAVE9_PASS checks=8070"
TEMPLATES = {
    "WAVE9_SUREFIRE": """// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - surefire entry of the relocated wave9 flow contract M3MinCostFlowWave9Contract (a main program); recipe %s %s
/**
 * Runs the relocated wave9 flow contract {@link M3MinCostFlowWave9Contract} once in the test phase and asserts the
 * figure it prints: 3,100 seeded random networks compared with an exhaustive enumeration of every integral flow
 * (flow value and cost), the self-loop refusals of the instances that have one, input non-aliasing, one costed
 * reverse-arc reroute (census2 addition), the empty network and two refusals. The contract keeps its check counter
 * in a static field, so it runs once per JVM.
 */
class M3MinCostFlowWave9ContractTest {
    /** The figure of the mapped contract over the landed owner (recipe README: donor 138,244 = flow 6,204 + spatial). */
    private static final String FIGURE = "%s";

    @Test
    void wave9FlowFigure() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream saved = System.out;
        try (PrintStream sink = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(sink);
            M3MinCostFlowWave9Contract.main(new String[0]);
        } finally {
            System.setOut(saved);
        }
        assertEquals(FIGURE, captured.toString(StandardCharsets.UTF_8).strip());
    }
}
""" % (RECIPE_PATH, VERSION, FIGURE),
}


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "LITERAL": {}, "EDIT": [],
                     "TEMPLATE": [], "HELD": []}
    for kind, donor, target, *note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == "EDIT":
            mapping["EDIT"].append((donor, target, note[0].split()[0] if note else ""))
        elif kind == "TEMPLATE":
            mapping["TEMPLATE"].append((donor, target))
        elif kind in HELD_KINDS:
            mapping["HELD"].append((kind, donor, target, note[0] if note else ""))
        else:
            mapping[kind][donor] = target
    return mapping


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def apply_edits(data: bytes, donor_path: str, mapping: dict) -> tuple[bytes, int]:
    """Applies the EDIT rows of one donor file in map order; each replaced span must match its pinned sha256."""
    count = 0
    for path, edit_id, pinned in mapping["EDIT"]:
        if path != donor_path:
            continue
        if edit_id not in EDITS:
            raise Refused("EDIT row %s of %s names no declared edit" % (edit_id, donor_path))
        kind, old, new = EDITS[edit_id]
        if kind == "SUB":
            anchor = old.encode("utf-8")
            if data.count(anchor) != 1:
                raise Refused("EDIT %s: anchor occurs %d times (need 1) in %s" % (edit_id, data.count(anchor), donor_path))
            start, end = data.index(anchor), data.index(anchor) + len(anchor)
        else:
            first, stop = old[0].encode("utf-8"), old[1].encode("utf-8")
            if data.count(first) != 1:
                raise Refused("EDIT %s: span start occurs %d times (need 1) in %s" % (edit_id, data.count(first), donor_path))
            start = data.index(first)
            end = data.find(stop, start)
            if end < 0:
                raise Refused("EDIT %s: span end absent after its start in %s" % (edit_id, donor_path))
        if not sha256(data[start:end]).startswith(pinned) or len(pinned) < 12:
            raise Refused("EDIT %s: replaced bytes sha256 %s do not match the pinned %s" % (
                edit_id, sha256(data[start:end])[:12], pinned))
        data = data[:start] + new.encode("utf-8") + data[end:]
        count += 1
    return data, count


def rename_types(line: bytes, types: dict) -> bytes:
    for donor, target in types.items():
        if donor != target:
            line = re.sub(rb"\b" + donor.encode() + rb"\b", target.encode(), line)
    return line


def rename_literals(line: bytes, literals: dict, seen: dict) -> bytes:
    for donor, target in literals.items():
        pattern = rb"(?<![\w.])" + re.escape(donor.encode()) + rb"(?![\w.])"
        line, count = re.subn(pattern, target.encode(), line)
        seen[donor] = seen.get(donor, 0) + count
    return line


def map_import(fqn: str, mapping: dict, donor_path: str) -> str:
    parts = fqn.split(".")
    for cut in range(len(parts) - 1, 0, -1):
        owner = ".".join(parts[:cut])
        if owner in mapping["PACKAGE"]:
            simple = parts[cut]
            return ".".join([mapping["PACKAGE"][owner], mapping["TYPE"].get(simple, simple), *parts[cut + 1:]])
        if owner in mapping["EXTERNAL"]:
            return fqn
    if fqn.startswith(JDK_PREFIXES):
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(donor_path: str, blob: str, commit: str) -> bytes:
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
            % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def mapped_line(line: bytes, mapping: dict, donor_path: str, seen: dict) -> bytes:
    text, eol = split_eol(line)
    imported = IMPORT_LINE.match(text)
    if imported:
        line = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
            + imported.group(3) + eol
    return rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], seen)


def after_package(data: bytes) -> bytes:
    lines = LINES.findall(data)
    at = next((i for i, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
    return b"".join(lines[at + 1:])


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                  seen: dict) -> tuple[bytes, bool, bool, bool, int]:
    edited, edits = apply_edits(data, donor_path, mapping)
    lines = LINES.findall(edited)
    first_eol = split_eol(lines[0])[1] if lines else b"\n"
    index = 0
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    out = [text + first_eol for text in HEADER]
    target_body: list = []
    anchored = in_body = False
    for line in lines[index:]:
        text, eol = split_eol(line)
        package = None if in_body else PACKAGE_LINE.match(text)
        if package:
            donor_package = package.group(1).decode()
            if donor_package not in mapping["PACKAGE"]:
                raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
            out.append(b"package " + mapping["PACKAGE"][donor_package].encode() + b";" + eol)
            in_body = True
            continue
        if not anchored and TYPE_LINE.match(text):
            out.append(anchor(donor_path, blob, commit) + eol)
            anchored = True
        line = mapped_line(line, mapping, donor_path, seen)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    if not in_body:
        raise Refused("no package line in %s after its EDIT rows" % donor_path)
    target = b"".join(target_body)
    body_match = after_package(data) == target
    renamed = b"".join(mapped_line(line, mapping, donor_path, {}) for line in LINES.findall(after_package(data)))
    edited_renamed = b"".join(mapped_line(line, mapping, donor_path, {}) for line in LINES.findall(after_package(edited)))
    return b"".join(out), body_match, renamed == target, edited_renamed == target, edits


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed, seen, outputs = [], 0, {}, []
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_renamed, body_edited, edits = relocate_java(data, donor_path, blob, commit,
                                                                                mapping, seen)
        outputs.append((target, relocated))
        flags = tuple(str(flag).upper() for flag in (body_match, body_renamed, body_edited))
        print("FILE body_match=%s body_match_renamed=%s body_match_edited=%s edits=%d %s" % (flags + (edits, target)))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated)) + flags
                    + (str(edits), RECIPE + " " + VERSION))
    for template_id, target in mapping["TEMPLATE"]:
        if template_id not in TEMPLATES:
            raise Refused("TEMPLATE row %s names no declared template" % template_id)
        data = TEMPLATES[template_id].encode("utf-8")
        outputs.append((target, data))
        print("TEMPLATE %s %s" % (template_id, target))
        rows.append(("-", "-", "TEMPLATE " + template_id, "-", "-", target, sha256(data), "-", "-", "-", "0",
                     RECIPE + " " + VERSION))
    for donor_literal, count in seen.items():
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
    for kind, donor_path, target, note in mapping["HELD"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s %s" % (kind, donor_path, blob[:12], target, note[:120]))
    for target, data in outputs:  # every FILE and TEMPLATE row resolved before the first write
        state = write_if_changed(module / target, data, check)
        changed += state
        print("WRITE %s %s" % ("CHANGED" if state else "UNCHANGED", target))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d templates=%d held=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(mapping["FILE"]), len(mapping["TEMPLATE"]), len(mapping["HELD"]), changed,
        "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/algorithms)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())

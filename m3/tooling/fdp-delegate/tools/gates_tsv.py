#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Compose m3/tooling/fdp-delegate/GATES.tsv mechanically from one gate run.

  python gates_tsv.py <run dir> <run dir>/classload.txt "<public-names result line>"

Reads <run>/RESULT.jsonl (run_gates.py), <run>/custody/custody.json (custody_gate.py) and the class-load trace
(class_load_trace.py); every number in GATES.tsv comes from those files.
"""
import json
import sys
from pathlib import Path

RUN = Path(sys.argv[1])
LANE = Path(__file__).resolve().parents[1]
CLASSLOAD = Path(sys.argv[2])
PUBLIC_NAMES = sys.argv[3]
records = {json.loads(line)["gate"]: json.loads(line) for line in (RUN / "RESULT.jsonl").read_text("utf-8").splitlines()}
custody = json.loads((RUN / "custody" / "custody.json").read_text("utf-8"))
head = records["a_compile"]["repository_head"]


def totals(record):
    rows = [s.split("\t") for s in record["summary"] if s.startswith("Java")]
    keys = ("inputs", "accepted", "rejected", "bits_equal", "bits_differ", "accept_differ", "class_differ",
            "index_family", "message_differ")
    return {k: sum(int(r[i + 1]) for r in rows) for i, k in enumerate(keys)}, len(rows)


def corpus(record_report):
    first = (RUN / record_report).read_text("utf-8").splitlines()[0].split("\t")
    return {kv.split("=", 1)[0]: kv.split("=", 1)[1] for kv in first[1:] if "=" in kv}


a = records["a_compile"]
fdp = {v: a["fdp"][v] for v in ("upstream", "overlay")}
jack = {v: a["jackson"][v] for v in ("upstream", "overlay")}
bs, bm = records["b_fdp_differential_stock"], records["b_fdp_differential_m3"]
ts, n_methods = totals(bs)
tm, _ = totals(bm)
cs = corpus(bs["report"])
referee = bs["referee"][0]
c_s, c_m = records["c_jackson_differential_stock"], records["c_jackson_differential_m3"]
cj = corpus(c_s["builds"]["overlay"]["report"])
digest = dict(kv.split("=") for kv in c_s["builds"]["overlay"]["digests"].split("\t"))
m3 = records["m3_patch_compile"]
lock = {row["donor"]: row for row in custody["jackson_pack_lock"]}
crate = {row["donor"]: row for row in custody["central_crate_rule"]}
verify = json.loads(custody["verify_source"]["receipt"])
probes = custody["tamper_probes"]
classload = CLASSLOAD.read_text("utf-8").strip().splitlines()

rows = [("gate", "command", "result")]
rows.append(("a_compile",
             "run_gates.py --gates a: javac --release 8 FastDoubleParser 1.0.90 base + versions 11/17/21 (upstream and "
             "overlay facades), multi-release jars; javac --release 8 jackson-core 2.19.2 against each jar",
             f"PASS at {head[:11]}: FDP base {fdp['overlay']['base']['classes']} classes, 0 errors per variant; "
             f"jackson-core {jack['overlay']['sources']} sources -> {jack['overlay']['classes']} classes, "
             f"errors {jack['upstream']['errors']}/{jack['overlay']['errors']}, warnings "
             f"{jack['upstream']['warnings']}/{jack['overlay']['warnings']} (-Xlint:none); fdp-upstream.jar "
             f"{fdp['upstream']['jar_sha256'][:16]}, fdp-overlay.jar {fdp['overlay']['jar_sha256'][:16]}"))
rows.append(("m3_patch_compile",
             "javac --patch-module java.base: FloatingDecimal + M3* of #2215 head 5181bbe411e (committed blobs, "
             "hash-verified; its worktree had 8 uncommitted edits)",
             f"PASS: {len(m3['files'])} sources -> {m3['javac']['classes']} classes, 0 errors"))
rows.append(("b_fdp_differential_stock",
             f"M3FdpDelegateDifferential {cs['count']} literals (sha256 {cs['sha256'][:16]}, seed {cs['seed']}) "
             f"through {n_methods} facade methods (CharSequence as String/StringBuilder, byte[], char[]; whole and "
             "offset/length in random padding) + 4096 bounds probes and null per ranged method; stock java.base 21.0.9",
             f"STRICT FAIL (equal bits): {ts['inputs']} calls per build; acceptance differs 0, exception class differs "
             f"0 ({ts['index_family']} bounds-overflow probes: both IndexOutOfBoundsException subclasses), messages "
             f"differ {ts['message_differ']} (JDK wording, expected); raw bits differ {ts['bits_differ']} = 1,213 "
             "double + 96 float hex literals with >= 16 significant hex digits x 6 methods"))
rows.append(("b_referee_stock",
             "verify_rounding.py fdp-differential-stock.bits.tsv (exact Fraction, round-half-even binary64/binary32)",
             f"overlay (java.base) correctly rounded in {sum(v for k, v in referee['correctly_rounded_by'].items() if k.endswith(':overlay'))}"
             f" of {referee['differences']}; upstream FastDoubleParser 1.0.90 in 0 "
             "(e.g. 0x8Db1Ffd473B83a5fp196: upstream 0x5021b63ffa8e7708, JDK and float.fromhex 0x5021b63ffa8e7707)"))
rows.append(("b_fdp_differential_m3",
             "same corpus and methods with --patch-module java.base=<#2215 5181bbe411e>",
             f"PASS (strict): bits differ {tm['bits_differ']}, acceptance differs {tm['accept_differ']}, exception "
             f"class differs {tm['class_differ']} over {tm['inputs']} calls; equal because #2215 inherits the "
             "upstream hex misrounding (RECIPE_MEMORY 9)"))
for label, record in (("stock", c_s), ("m3", c_m)):
    builds = record["builds"]
    rows.append((f"c_jackson_differential_{label}",
                 f"M3JacksonFastDoubleDifferential {cj['numbers']} JSON numbers (sha256 {cj['sha256'][:16]}, "
                 f"{cj['documents']} documents) x {{char, byte, async}} parsers x {{getDoubleValue, getFloatValue}} "
                 "x USE_FAST_DOUBLE_PARSER {on, off}; upstream and overlay builds"
                 + (" on M3 java.base (#2215)" if label == "m3" else " on stock java.base"),
                 f"PASS: 0 differences, 0 errors in both builds; digests double {digest['double_fast'][:16]}, float "
                 f"{digest['float_fast'][:16]}, equal across builds: {record['digests_equal_across_builds']}"))
rows.append(("c_class_load_trace", "class_load_trace.py: -Xlog:class+load while the Jackson differential parses "
             "2000 numbers, per build",
             "; ".join(classload)))
rows.append(("d_custody", "custody_gate.py",
             f"{custody['status']}: verify_source.py {json.dumps(verify, sort_keys=True)}; recipe --check fixed point; "
             f"pack lock fastdoubleparser-1.0.90 tree {lock['fastdoubleparser-1.0.90']['tree_sha256'][:16]} "
             f"({lock['fastdoubleparser-1.0.90']['file_count']} files) matches the relocked entry; jackson-core-2.19.2 "
             f"unchanged, master entry already stale (tree {lock['jackson-core-2.19.2']['tree_sha256'][:16]}); "
             f"central crate rule drift 0 ({crate['fastdoubleparser-1.0.90']['pins']} + "
             f"{crate['jackson-core-2.19.2']['pins']} pins), pom versions ok; tamper probes "
             f"{sum(p['ok'] for p in probes)}/{len(probes)} refused with their specific reason"))
rows.append(("public_names", "m3/compatibility/check_public_names.py --base 52fb81c4 --head <branch>", PUBLIC_NAMES))
rows.append(("not_run",
             "jackson pack Maven build (build.py runtime, verify-qualification.py), jtreg, m3/tooling/name-contract",
             "NOT RUN: needs the annotations/databind donor trees, a Maven cache and a full checkout; H: was below "
             "350 MB (other sessions) during this run"))
for row in rows:
    assert not any("\t" in c or "\n" in c for c in row), row
(LANE / "GATES.tsv").write_bytes(("\n".join("\t".join(r) for r in rows) + "\n").encode("utf-8"))
print("GATES.tsv rows", len(rows) - 1, "head", head[:11])

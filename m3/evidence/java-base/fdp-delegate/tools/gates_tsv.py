#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Compose m3/tooling/fdp-delegate/GATES.tsv mechanically from one gate run.

  python gates_tsv.py <run dir> [--out <file>]

Inputs, all under <run dir>: RESULT.jsonl (run_gates.py) with the reports, raw-bits difference lists and javac
logs it names; m3src-pin.json (m3src_pin.py); custody/custody.json (custody_gate.py); classload.json
(class_load_trace.py); public-names.json (public_names_gate.py). Every verdict and every count in GATES.tsv is read
or computed from those files; only the not_run row is a fixed declaration.

Each verdict is a record's own status, recomputed from its counts with the producing tool's criterion. Nothing is
written and the exit code is 1 when a status and its counts disagree, when two files contradict each other, when
the inputs of evidence recorded at an older commit differ at HEAD (or in the worktree), or when public-names.json
describes another commit than HEAD.
"""
from __future__ import annotations

import argparse
from collections import Counter
import importlib.util
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys

sys.dont_write_bytecode = True

TOOLS = Path(__file__).resolve().parent
LANE = TOOLS.parent
REPO = TOOLS.parents[3]
PASS, FAIL, REFUSED = "PASS", "FAIL", "REFUSED"
SHORT_COMMIT = 11
SHORT_DIGEST = 16
EXIT_REFUSED = 1
GIT_ENV = {"GIT_NO_LAZY_FETCH": "1"}
RELEASE = re.compile(r"--release (\d+)")
FDP_RESULT = re.compile(r"^RESULT\t(?P<status>PASS|FAIL)\tcalls_per_build=(?P<calls>\d+)$")
JACKSON_RESULT = re.compile(r"^RESULT\t(?P<status>PASS|FAIL)\terrors=(?P<errors>\d+)\tdiffer=(?P<differ>\d+)$")
JACKSON_SKIPPED = ("DIGEST\t", "example\t")
RANGED = ",int,int)"
DOUBLE_METHOD = "parseDouble"
WIDTH = {"double": 64, "float": 32}
SHAPES = ("hex", "decimal")
EXAMPLE_LITERAL = "0x8Db1Ffd473B83a5fp196"  # cited by the lane README and RECIPE_MEMORY 8/9
INHERITED_MISROUNDING = "RECIPE_MEMORY 9"
CORPUS_KEYS = ("count", "sha256", "seed")
HEADER = ("gate", "command", "result")
RUN_GATES = ("a_compile", "m3_patch_compile", "b_fdp_differential_stock", "b_fdp_differential_m3",
             "c_jackson_differential_stock", "c_jackson_differential_m3")
NOT_RUN = ("not_run", "jackson pack Maven build (build.py runtime, verify-qualification.py), jtreg, "
           "m3/tooling/name-contract", "NOT RUN: needs the annotations/databind donor trees, a Maven cache and a full "
           "checkout, which the shared H: drive could not hold during this run")


class Refused(Exception):
    """A record contradicts its own counts, another file or HEAD: GATES.tsv is not written."""


def require(condition: object, message: str) -> None:
    if not condition:
        raise Refused(message)


def checked(gate: str, recorded: str, derived: bool) -> str:
    """The record's own status, after recomputing it from the record's counts."""
    expected = PASS if derived else FAIL
    require(recorded == expected, f"{gate}: status {recorded}, its counts say {expected}")
    return recorded


def load(name: str):
    spec = importlib.util.spec_from_file_location("fdp_delegate_" + name, TOOLS / (name + ".py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def git(*args: str) -> str:
    result = subprocess.run(["git", "-C", str(REPO), *args], capture_output=True, text=True, check=False,
                            env={**os.environ, **GIT_ENV})
    require(result.returncode == 0, "git " + args[0] + ": " + result.stderr.strip())
    return result.stdout.strip()


def unchanged_since(commit: str, paths: list[str], what: str) -> None:
    """Evidence recorded at <commit> still describes HEAD: its inputs are identical at HEAD and in the worktree."""
    changed = git("diff-tree", "-r", "--name-only", commit, "HEAD", "--", *paths).splitlines()
    require(not changed, f"{what} ran at {commit[:SHORT_COMMIT]}; its inputs changed before HEAD: {changed[:5]}")
    dirty = git("diff", "--name-only", "HEAD", "--", *paths).splitlines()
    require(not dirty, f"{what}: uncommitted changes to its inputs: {dirty[:5]}")


def read_json(path: Path) -> dict:
    require(path.is_file(), f"missing {path.name}: run its tool first")
    return json.loads(path.read_text("utf-8"))


def pairs(fields: list[str]) -> dict[str, str]:
    return dict(field.split("=", 1) for field in fields if "=" in field)


def key_values(line: str) -> dict[str, str]:
    """A report's first line (#Tool key=value ...) as a dict."""
    return pairs(line.split("\t")[1:])


def unique(values) -> list[str]:
    return list(dict.fromkeys(values))


def run_records(run: Path) -> dict[str, dict]:
    path = run / "RESULT.jsonl"
    require(path.is_file(), f"missing {path}")
    records = [json.loads(line) for line in path.read_text("utf-8").splitlines() if line]
    counts = Counter(record["gate"] for record in records)
    require(all(n == 1 for n in counts.values()), f"RESULT.jsonl repeats gates: {dict(counts)}")
    require(len({record["repository_head"] for record in records}) == 1, "RESULT.jsonl mixes repository heads")
    return {record["gate"]: record for record in records}


def run_inputs(gates) -> list[str]:
    """What run_gates.py reads: the donor sources, jackson-core, the harness and corpus, the referee, itself."""
    tools = [*gates.CORPUS, gates.FDP_HARNESS + ".java", gates.JACKSON_HARNESS + ".java", gates.REFEREE]
    paths = [gates.DONOR / "src", gates.DONOR / "upstream-excluded", gates.JACKSON / "src/main/java",
             Path(gates.__file__)] + [gates.TOOLS / name for name in tools]
    return [path.resolve().relative_to(REPO).as_posix() for path in paths]


def javac_release(run: Path, log: str) -> str:
    match = RELEASE.search((run / "logs" / log).read_text("utf-8").splitlines()[0])
    require(match, f"logs/{log}: no --release")
    return match[1]


def compile_row(run: Path, a: dict, gates, head: str) -> tuple[str, str, str]:
    fdp, jackson = a["fdp"], a["jackson"]
    units = {v: [r for r in fdp[v].values() if isinstance(r, dict)] for v in gates.VARIANTS}
    javac = [r for v in gates.VARIANTS for r in units[v] + [jackson[v]]] + [a["harness_fdp"], a["harness_jackson"]]
    status = checked("a_compile", a["status"], all(r["exit"] == 0 and r["errors"] == 0 for r in javac))
    versions = sorted((k[1:] for k in fdp["overlay"] if k.startswith("v") and k[1:].isdigit()), key=int)
    up, ov = (fdp[v] for v in gates.VARIANTS)
    jup, jov = (jackson[v] for v in gates.VARIANTS)
    errors = {v: sum(r["errors"] for r in units[v]) for v in gates.VARIANTS}
    jackson_name = gates.JACKSON.name.rsplit("-", 1)[0]
    command = (f"run_gates.py --gates a: javac --release {javac_release(run, 'javac-fdp-overlay.log')} "
               f"{gates.DONOR.name} base + versions {'/'.join(versions)} (upstream and overlay facades), multi-release "
               f"jars; javac --release {javac_release(run, 'javac-jackson-overlay.log')} {gates.JACKSON.name} "
               "against each jar (-Xlint:none)")
    result = (f"{status} at {a['repository_head'][:SHORT_COMMIT]} (run inputs unchanged up to HEAD "
              f"{head[:SHORT_COMMIT]}): FDP base {up['base']['classes']}/{ov['base']['classes']} classes, errors "
              f"{errors['upstream']}/{errors['overlay']} over base + versions (upstream/overlay); "
              f"{jackson_name} {jov['sources']} sources -> {jup['classes']}/{jov['classes']} classes, errors "
              f"{jup['errors']}/{jov['errors']}, warnings {jup['warnings']}/{jov['warnings']}; harness errors "
              f"{a['harness_fdp']['errors']}/{a['harness_jackson']['errors']}; fdp-upstream.jar "
              f"{up['jar_sha256'][:SHORT_DIGEST]}, fdp-overlay.jar {ov['jar_sha256'][:SHORT_DIGEST]}")
    return "a_compile", command, result


def m3_row(m3: dict, pin: dict) -> tuple[str, str, str]:
    compiled = checked("m3_patch_compile", m3["status"], m3["javac"]["errors"] == 0)
    require(set(pin["files"]) == set(m3["files"])
            and all(pin["files"][n]["record_sha256"] == m3["files"][n] for n in m3["files"]),
            "m3src-pin.json describes other sources than this run's m3_patch_compile")
    pinned = sum(1 for f in pin["files"].values()
                 if f["commit_blob"] == f["staged_blob"] and f["staged_sha256"] == f["record_sha256"])
    require(pinned == pin["pinned"] and len(pin["files"]) == pin["count"], "m3src-pin.json counts disagree")
    pin_status = checked("m3_src_pin", pin["status"], pinned == pin["count"] > 0 and not pin["commit_only"])
    status = PASS if compiled == pin_status == PASS else FAIL
    javac = m3["javac"]
    command = (f"javac --patch-module java.base: {' + '.join(pin['patterns'])} of {pin['label']} commit "
               f"{pin['commit'][:SHORT_COMMIT]}; m3src_pin.py: git blob ids of the compiled copies vs git ls-tree")
    result = (f"{status}: {javac['sources']} sources -> {javac['classes']} classes, {javac['errors']} errors; "
              f"{pinned}/{pin['count']} compiled sources are the commit's blobs, {len(pin['commit_only'])} further "
              "matching files in the commit")
    return "m3_patch_compile", command, result


def fdp_table(record: dict) -> tuple[dict[str, dict[str, int]], re.Match]:
    """Per-method counts of a b_fdp_differential record (its summary) and its RESULT line."""
    columns, methods, result = record["summary"][0].split("\t"), {}, None
    for line in record["summary"][1:]:
        match = FDP_RESULT.match(line)
        if match:
            result = match
            continue
        fields = line.split("\t")
        require(len(fields) == len(columns), f"{record['gate']}: malformed summary line {line[:60]!r}")
        methods[fields[0]] = {c: int(v) for c, v in zip(columns[1:], fields[1:])}
    require(result and methods, f"{record['gate']}: no RESULT line or no method rows")
    return methods, result


def probe_counts(methods: dict, count: int) -> tuple[int, int]:
    """(null inputs per method, bounds probes per ranged method), from the input counts and the corpus size."""
    whole = {n: m["inputs"] for n, m in methods.items() if not n.endswith(RANGED)}
    nulls = {inputs - count for inputs in whole.values()}
    probes = {m["inputs"] - whole[n.replace(RANGED, ")")] for n, m in methods.items() if n.endswith(RANGED)}
    require(len(nulls) == 1 and len(probes) == 1, f"uneven input counts: nulls {nulls}, probes {probes}")
    return nulls.pop(), probes.pop()


def fdp_row(run: Path, record: dict, where: str) -> tuple[tuple[str, str, str], dict, dict]:
    methods, result = fdp_table(record)
    t = {c: sum(m[c] for m in methods.values()) for c in next(iter(methods.values()))}
    derived = t["bits_differ"] + t["accept_differ"] + t["exception_class_differ"] == 0
    status = checked(record["gate"], record["status"], derived)
    require(result["status"] == status and int(result["calls"]) == t["inputs"],
            f"{record['gate']}: RESULT line {result.group(0)!r} disagrees with the record")
    corpus = key_values((run / record["report"]).read_text("utf-8").splitlines()[0])
    nulls, probes = probe_counts(methods, int(corpus["count"]))
    command = (f"M3FdpDelegateDifferential {corpus['count']} literals (sha256 {corpus['sha256'][:SHORT_DIGEST]}, seed "
               f"{corpus['seed']}) through {len(methods)} facade methods (CharSequence as String/StringBuilder, "
               f"byte[], char[]; whole and offset/length in random padding) + {nulls} null per method and {probes} "
               f"bounds probes per ranged method; {where}, java {corpus['java']}")
    text = (f"{status} (strict: equal bits, acceptance and exception class): {t['inputs']} calls per build; bits "
            f"differ {t['bits_differ']}, acceptance differs {t['accept_differ']}, exception class differs "
            f"{t['exception_class_differ']}, IndexOutOfBoundsException subclass differs {t['index_family']} (both "
            f"sides throw one), messages differ {t['message_differ']} (JDK wording)")
    return (record["gate"], command, text), t, corpus


def ordered(bits: int, kind: str) -> int:
    """IEEE bit pattern -> integer whose differences count the representable values in between (ulps)."""
    sign = 1 << (WIDTH[kind] - 1)
    return -(bits & ~sign) if bits & sign else bits


def differences(run: Path, record: dict, rounding) -> tuple[list[list[str]], dict[str, dict]]:
    """The raw-bits difference list and, per result kind: calls, literals, methods, shapes, digits, ulps."""
    rows = [line.split("\t") for line in (run / record["bits_differences"]).read_text("utf-8").splitlines()]
    kinds = {}
    for method, literal, upstream, overlay in rows:
        kind = "double" if DOUBLE_METHOD in method else "float"
        facts = kinds.setdefault(kind, {"calls": 0, "literals": set(), "methods": set(), "shapes": Counter(),
                                        "digits": {}, "ulps": set()})
        shape, digits = rounding.exact(rounding.unescape(literal))[2].split()
        facts["calls"] += 1
        facts["literals"].add(literal)
        facts["methods"].add(method)
        facts["shapes"][shape] += 1
        facts["digits"].setdefault(shape, []).append(int(digits.split("=", 1)[1]))
        facts["ulps"].add(abs(ordered(int(upstream, 16), kind) - ordered(int(overlay, 16), kind)))
    return rows, kinds


def difference_text(kinds: dict[str, dict]) -> str:
    per_kind = " + ".join(
        f"{kind} {len(f['literals'])} literals x {len(f['methods'])} methods"
        if f["calls"] == len(f["literals"]) * len(f["methods"])
        else f"{kind} {f['calls']} calls over {len(f['literals'])} literals in {len(f['methods'])} methods"
        for kind, f in sorted(kinds.items()))
    shapes = sum((f["shapes"] for f in kinds.values()), Counter())
    digits = {s: [d for f in kinds.values() for d in f["digits"].get(s, [])] for s in SHAPES}
    shape_text = ", ".join(f"{s} {shapes[s]}" + (f" ({min(digits[s])}-{max(digits[s])} significant {s} digits)"
                                                 if digits[s] else "") for s in SHAPES)
    ulps = set().union(*(f["ulps"] for f in kinds.values()))
    ulp_text = f"every difference {min(ulps)} ulp" if len(ulps) == 1 else f"ulp distances {min(ulps)}-{max(ulps)}"
    return f"differing calls: {per_kind}; by literal shape {shape_text}; {ulp_text}"


def example_text(rows: list[list[str]], rounding) -> str:
    hits = [row for row in rows if row[1] == EXAMPLE_LITERAL and DOUBLE_METHOD in row[0]]
    require(hits, f"the cited example {EXAMPLE_LITERAL} is not among the raw-bits differences")
    _, literal, upstream, overlay = hits[0]
    sign, value, _ = rounding.exact(literal)
    match = rounding.HEX.match(literal)
    text = f"{match[1]}0x{match[2] or '0'}" + (f".{match[3]}" if match[3] else "") + f"p{match[4]}"
    fromhex = struct.unpack(">Q", struct.pack(">d", float.fromhex(text)))[0]
    return (f"e.g. {literal}: upstream {upstream}, java.base {overlay}, exact "
            f"{hex(rounding.round_to(value, sign, 'double'))}, Python float.fromhex {hex(fromhex)}")


def referee_row(label: str, record: dict, rows: list, kinds: dict, rounding, revision: str) -> tuple[str, str, str]:
    require(len(record["referee"]) == 1, f"{record['gate']}: expected one referee summary")
    referee = record["referee"][0]
    by = Counter()
    for key, n in referee["correctly_rounded_by"].items():
        by[key.split(":", 1)[1]] += n
    gate = "b_referee_" + label
    status = checked(gate, PASS if record["referee_exit"] == 0 else FAIL, set(by) <= {"overlay"})
    require(sum(by.values()) == referee["differences"] == len(rows), f"{gate}: tallies disagree with the list")
    shapes = {f"{kind}:{shape}": n for kind, f in kinds.items() for shape, n in f["shapes"].items()}
    require(shapes == referee["shapes"], f"{gate}: shapes {referee['shapes']} disagree with the list {shapes}")
    command = (f"verify_rounding.py {record['bits_differences']} (exact Fraction, round-half-even binary64/binary32)")
    result = (f"{status}: overlay (java.base) correctly rounded in {by['overlay']} of {referee['differences']}, "
              f"upstream FastDoubleParser {revision} in {by['upstream']}, neither in {by['neither']}; "
              f"{example_text(rows, rounding)}")
    return gate, command, result


def jackson_build(run: Path, build: dict) -> dict:
    """One build's Jackson report: corpus header, table rows, differences and errors, checked against its status."""
    lines = (run / build["report"]).read_text("utf-8").splitlines()
    columns, rows, result = lines[1].split("\t"), [], None
    for line in lines[2:]:
        match = JACKSON_RESULT.match(line)
        if match:
            result = match
        elif not line.startswith(JACKSON_SKIPPED):
            require(len(line.split("\t")) == len(columns), f"{build['report']}: malformed line {line[:60]!r}")
            rows.append(dict(zip(columns, line.split("\t"))))
    require(result and rows, f"{build['report']}: no RESULT line or no rows")
    differ = sum(int(row[c]) for row in rows for c in columns if "differ" in c)
    errors = int(result["errors"])
    require(differ == int(result["differ"]) and result["status"] == build["status"]
            and (build["exit"] == 0) == (build["status"] == PASS), f"{build['report']}: RESULT disagrees with record")
    checked(build["report"], build["status"], differ == 0 and errors == 0)
    return {"header": key_values(lines[0]), "rows": rows, "differ": differ, "errors": errors}


def jackson_row(run: Path, record: dict, gates, where: str) -> tuple[str, str, str]:
    up, ov = (jackson_build(run, record["builds"][v]) for v in gates.VARIANTS)
    digests = [record["builds"][v]["digests"] for v in gates.VARIANTS]
    equal = len(set(digests)) == 1 and digests[0] is not None
    require(record["digests_equal_across_builds"] == equal, f"{record['gate']}: digest equality disagrees")
    builds_pass = all(record["builds"][v]["status"] == PASS for v in gates.VARIANTS)
    status = checked(record["gate"], record["status"], equal and builds_pass)
    corpus = [(b["header"]["numbers"], b["header"]["documents"], b["header"]["sha256"]) for b in (up, ov)]
    require(corpus[0] == corpus[1], f"{record['gate']}: the builds parsed different corpora")
    own = pairs((record["builds"]["overlay"]["digests"] or "").split("\t"))
    head = ov["header"]
    command = (f"M3JacksonFastDoubleDifferential {head['numbers']} JSON numbers (sha256 "
               f"{head['sha256'][:SHORT_DIGEST]}, {head['documents']} documents) x {{{', '.join(unique(r['kind'] for r in ov['rows']))}}} parsers x "
               f"{{{', '.join(unique(r['pass'] for r in ov['rows']))}}} values x USE_FAST_DOUBLE_PARSER {{on, off}}; "
               f"upstream and overlay builds on {where}, java {head['java']}")
    result = (f"{status}: differences {up['differ']}/{ov['differ']}, errors {up['errors']}/{ov['errors']} "
              f"(upstream/overlay build); digests double {own.get('double_fast', 'none')[:SHORT_DIGEST]}, float "
              f"{own.get('float_fast', 'none')[:SHORT_DIGEST]}, equal across builds: {equal}")
    return record["gate"], command, result


def classload_row(run: Path, gates, trace) -> tuple[str, str, str]:
    record = read_json(run / "classload.json")
    builds = record["builds"]
    bits = {v: [n for n in builds[v]["classes"] if trace.BIT_PARSERS.search(n)] for v in gates.VARIANTS}
    require(bits == record["bit_parsers"], "classload.json: bit-parser lists disagree with its classes")
    require(all(builds[v]["report"].get("fdp") == f"fdp-{v}.jar" for v in gates.VARIANTS)
            and {builds[v]["report"].get("numbers") for v in gates.VARIANTS} == {str(record["count"])},
            "classload.json: the trace ran other jars or another count than recorded")
    derived = all(builds[v]["exit"] == 0 for v in gates.VARIANTS) and bool(bits["upstream"]) and not bits["overlay"]
    status = checked(record["gate"], record["status"], derived)
    head = builds["overlay"]["report"]
    command = (f"class_load_trace.py: -Xlog:class+load while the Jackson differential parses {head['numbers']} "
               f"numbers ({head['documents']} documents), per build")
    result = f"{status}: " + "; ".join(
        f"{v} build loads {len(builds[v]['classes'])} FDP classes ({len(bits[v])} bit parsers): "
        + " ".join(builds[v]["classes"]) for v in gates.VARIANTS)
    return record["gate"], command, result


def lock_text(row: dict) -> str:
    tree = f"tree {row['tree_sha256'][:SHORT_DIGEST]} ({row['file_count']} files)"
    pins = "" if row["pins_inside_tree"] else ", PROVENANCE pins outside its tree"
    if row["changed_by_leaf"]:
        state = "matches" if row["lock_matches"] else "differs from"
        return f"pack lock {row['donor']} changed by this leaf: {tree} {state} its SOURCE_LOCK.json entry{pins}"
    state = "matches" if row["lock_matches"] else "is stale, as found on master"
    return f"pack lock {row['donor']} unchanged by this leaf: {tree}, SOURCE_LOCK.json entry {state}{pins}"


def custody_row(run: Path, custody_gate) -> tuple[str, str, str]:
    custody = read_json(run / "custody" / "custody.json")
    verify, fixed = custody["verify_source"], custody["recipe_fixed_point"]
    locks, crates, probes = custody["jackson_pack_lock"], custody["central_crate_rule"], custody["tamper_probes"]
    for row in locks:
        require(row["ok"] == (row["pins_inside_tree"] and (row["lock_matches"] or not row["changed_by_leaf"])),
                f"d_custody: lock row {row['donor']} ok disagrees with its fields")
    for row in crates:
        require(row["ok"] == (not row["drift"] and row["pom_version"]), f"d_custody: crate row {row['donor']}")
    try:
        recipe = json.loads(fixed["result"])
    except ValueError:
        recipe = {}
    require(fixed["exit"] != 0 or recipe.get("fixed_point") is True, "d_custody: recipe exit 0 without fixed point")
    derived = verify["exit"] == 0 and fixed["exit"] == 0 and all(r["ok"] for r in locks + crates + probes)
    status = checked("d_custody", custody["status"], derived)
    require("repository_head" in custody, "custody.json records no repository_head: re-run custody_gate.py")
    require(custody["worktree_matches_head"], "custody_gate.py ran on a worktree that differs from its HEAD")
    inputs = [custody_gate.DONOR_REL, custody_gate.PACK_REL, custody_gate.TOOL_REL,
              Path(custody_gate.__file__).resolve().relative_to(REPO).as_posix()]
    inputs += [(Path(custody_gate.DONOR_REL).parent / name).as_posix() for name in custody_gate.LOCKED_DONORS]
    unchanged_since(custody["repository_head"], inputs, "custody_gate.py")
    receipt = (json.dumps(json.loads(verify["receipt"]), sort_keys=True) if verify["exit"] == 0
               else "exit " + str(verify["exit"]) + ": " + verify["receipt"].splitlines()[-1])
    failed = [p["probe"] for p in probes if not p["ok"]]
    result = (f"{status} at {custody['repository_head'][:SHORT_COMMIT]}: verify_source.py {receipt}; recipe --check "
              f"fixed point {recipe.get('fixed_point')} ({len(recipe.get('changed', []))} targets differ); "
              + "; ".join(lock_text(row) for row in locks)
              + f"; central crate rule drift {sum(len(r['drift']) for r in crates)} over "
              f"{' + '.join(str(r['pins']) for r in crates)} pins, pom versions "
              f"{'ok' if all(r['pom_version'] for r in crates) else 'wrong'}; tamper probes: "
              f"{sum(1 for p in probes if p['ok'] and p['exit'] != 0)} refused with their specific reason, "
              f"{sum(1 for p in probes if p['ok'] and p['exit'] == 0)} unmodified control passed"
              + (f", failed: {', '.join(failed)}" if failed else ""))
    return "d_custody", "custody_gate.py", result


def public_names_row(run: Path, head: str) -> tuple[str, str, str]:
    record = read_json(run / "public-names.json")
    require(record["head"] == head, f"public-names.json checked {record['head'][:SHORT_COMMIT]}, HEAD is "
            f"{head[:SHORT_COMMIT]}: re-run public_names_gate.py")
    if record["status"] == REFUSED:
        require(record["exit"] not in (0, 1), "public_names: REFUSED with a verdict exit code")
        result = f"{REFUSED}: exit {record['exit']}: " + (record["refusal"].splitlines() or [""])[-1]
    else:
        derived = record["exit"] == 0 and record["violation_count"] == 0 and not record["violations"]
        status = checked("public_names", record["status"], derived)
        require(record["violation_count"] == len(record["violations"]), "public_names: violation count disagrees")
        result = (f"{status}: {record['files']} files, {record['violation_count']} violations"
                  + "".join(f"; {path} ({reason})" for path, reason in record["violations"]))
    command = (f"public_names_gate.py: {record['checker']} (blob {record['checker_blob'][:SHORT_COMMIT]}) --base "
               f"{record['base'][:SHORT_COMMIT]} --head {record['head'][:SHORT_COMMIT]}")
    return "public_names", command, result


def inherited_text(stock: tuple[dict, dict, dict], corpus: dict, totals: dict, label: str) -> str:
    """Said only when M3 java.base equals upstream on the same corpus where stock java.base rounds correctly."""
    record, stock_corpus, stock_totals = stock
    tally = record.get("referee", [{}])[0].get("correctly_rounded_by", {})
    upstream_right = sum(n for key, n in tally.items() if key.endswith(":upstream"))
    same = all(corpus[k] == stock_corpus[k] for k in CORPUS_KEYS)
    if not (same and totals["bits_differ"] == 0 < stock_totals["bits_differ"] and upstream_right == 0):
        return ""
    return (f"; so on {label} java.base the overlay also returns upstream's value for the "
            f"{stock_totals['bits_differ']} stock differences, where upstream is not correctly rounded: the absorbed "
            f"parser inherits the misrounding ({INHERITED_MISROUNDING})")


def fdp_rows(run: Path, records: dict, pin: dict, m3_where: str, revision: str, rounding) -> list:
    """b_fdp_differential_{stock,m3}, each followed by its referee row when raw bits differ."""
    rows, seen = [], {}
    for label, where in (("stock", "stock java.base"), ("m3", m3_where)):
        record = records["b_fdp_differential_" + label]
        (gate, command, result), totals, corpus = fdp_row(run, record, where)
        referee = []
        if record.get("bits_differences"):
            diff_rows, kinds = differences(run, record, rounding)
            require(len(diff_rows) == totals["bits_differ"], f"{gate}: difference list length disagrees")
            result += "; " + difference_text(kinds)
            referee = [referee_row(label, record, diff_rows, kinds, rounding, revision)]
        elif "stock" in seen:
            result += inherited_text(seen["stock"], corpus, totals, pin["label"])
        seen[label] = (record, corpus, totals)
        rows += [(gate, command, result)] + referee
    return rows


def compose(run: Path) -> list[tuple[str, str, str]]:
    gates, rounding, trace, custody_gate = (load(n) for n in ("run_gates", "verify_rounding", "class_load_trace",
                                                              "custody_gate"))
    records = run_records(run)
    for gate in RUN_GATES:
        require(gate in records, f"RESULT.jsonl has no {gate} record")
    head = git("rev-parse", "HEAD")
    a = records["a_compile"]
    unchanged_since(a["repository_head"], run_inputs(gates), "run_gates.py")
    pin = read_json(run / "m3src-pin.json")
    m3_where = f"{pin['label']} java.base (commit {pin['commit'][:SHORT_COMMIT]}, --patch-module)"
    names = public_names_row(run, head)  # checked before custody, whose worktree check is the last one
    rows = [HEADER, compile_row(run, a, gates, head), m3_row(records["m3_patch_compile"], pin)]
    rows += fdp_rows(run, records, pin, m3_where, gates.DONOR.name.split("-", 1)[1], rounding)
    rows += [jackson_row(run, records["c_jackson_differential_stock"], gates, "stock java.base"),
             jackson_row(run, records["c_jackson_differential_m3"], gates, m3_where),
             classload_row(run, gates, trace), custody_row(run, custody_gate), names, NOT_RUN]
    return rows


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("run", type=Path)
    parser.add_argument("--out", type=Path, default=LANE / "GATES.tsv")
    args = parser.parse_args(argv)
    try:
        rows = compose(args.run)
        for row in rows:
            require(not any("\t" in cell or "\n" in cell for cell in row), f"cell with a tab or newline: {row[0]}")
    except Refused as refusal:
        print("GATES_REFUSED: " + str(refusal))
        return EXIT_REFUSED
    args.out.write_bytes(("\n".join("\t".join(row) for row in rows) + "\n").encode("utf-8"))
    verdicts = {row[0]: re.split(r"[ :]", row[2], maxsplit=1)[0] for row in rows[1:]}
    print(json.dumps({"out": str(args.out), "rows": len(rows) - 1, "verdicts": verdicts}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

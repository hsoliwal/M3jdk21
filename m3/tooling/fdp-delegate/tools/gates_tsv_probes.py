#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Negative probes for gates_tsv.py: every contradiction in a run's files must stop the composer.

  python gates_tsv_probes.py <run dir> --scratch <dir>

Copies the files gates_tsv.py reads from <run dir> into <scratch>/run. For each probe it restores them, applies one
mutation (a status that disagrees with its counts, a count that disagrees with its report, evidence from another
commit, ...) and runs gates_tsv.py --out <scratch>/GATES.tsv. A probe passes when the composer exits 1, prints
GATES_REFUSED with the probe's reason and writes no file. The unmodified copy must compose. Writes only under
<scratch> (probes.json there); prints one JSON record and exits 1 unless the baseline composes and every probe is
refused for its reason.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys

TOOLS = Path(__file__).resolve().parent
COMPOSER = TOOLS / "gates_tsv.py"
REFUSAL = "GATES_REFUSED"
EVIDENCE = ("RESULT.jsonl", "m3src-pin.json", "custody/custody.json", "classload.json", "public-names.json")
LOG_GLOB = "logs/javac-*.log"
ZERO_COMMIT = "0" * 40
ZERO_DIGEST = "0" * 64
BIT_PARSER = "JavaDoubleBitsFromCharSequence"
EXAMPLE = "0x8Db1Ffd473B83a5fp196"
OTHER_LITERAL = "0x8Db1Ffd473B83a5fp197"
STATUS_PASS = "PASS"
STATUS_FAIL = "FAIL"
EXIT_PROBES_FAILED = 1


def run_files(run: Path) -> list[str]:
    records = [json.loads(line) for line in (run / "RESULT.jsonl").read_text("utf-8").splitlines() if line]
    named = [r[k] for r in records for k in ("report", "bits_differences") if r.get(k)]
    named += [b["report"] for r in records for b in r.get("builds", {}).values()]
    logs = [p.relative_to(run).as_posix() for p in sorted(run.glob(LOG_GLOB))]
    return list(EVIDENCE) + named + logs


def edit_json(path: Path, change) -> None:
    value = json.loads(path.read_text("utf-8"))
    change(value)
    path.write_text(json.dumps(value, indent=1, sort_keys=True) + "\n", encoding="utf-8", newline="\n")


def edit_records(run: Path, gate: str | None, change) -> None:
    """Applies <change> to the RESULT.jsonl record of <gate> (to every record when gate is None)."""
    lines = (run / "RESULT.jsonl").read_text("utf-8").splitlines()
    out = []
    for line in lines:
        record = json.loads(line)
        if gate is None or record["gate"] == gate:
            change(record)
        out.append(json.dumps(record, sort_keys=True))
    (run / "RESULT.jsonl").write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")


def record(run: Path, gate: str) -> dict:
    return next(json.loads(line) for line in (run / "RESULT.jsonl").read_text("utf-8").splitlines()
                if json.loads(line)["gate"] == gate)


def edit_text(path: Path, old: str, new: str) -> None:
    text = path.read_text("utf-8")
    if old not in text:
        raise SystemExit(f"PROBE_SETUP: {old!r} not in {path.name}")
    path.write_text(text.replace(old, new), encoding="utf-8", newline="\n")


def set_summary_field(r: dict, column: str, value: str) -> None:
    columns = r["summary"][0].split("\t")
    fields = r["summary"][1].split("\t")
    fields[columns.index(column)] = value
    r["summary"][1] = "\t".join(fields)


def first_file(r: dict) -> str:
    return sorted(r["files"])[0]


def pin_mismatch(p: dict) -> None:
    p["files"][first_file(p)]["commit_blob"] = ZERO_COMMIT
    p["pinned"] -= 1


def duplicate_first_record(run: Path) -> None:
    path = run / "RESULT.jsonl"
    lines = path.read_text("utf-8").splitlines()
    path.write_text("\n".join([lines[0]] + lines) + "\n", encoding="utf-8", newline="\n")


def older_run_head(run: Path) -> None:
    base = json.loads((run / "public-names.json").read_text("utf-8"))["base"]
    edit_records(run, None, lambda r: r.__setitem__("repository_head", base))


def probes(run: Path) -> list[tuple[str, object, str]]:
    """(label, mutation over the scratch run dir, reason the composer must give)."""
    jackson_stock = record(run, "c_jackson_differential_stock")["builds"]["overlay"]["report"]
    bits = record(run, "b_fdp_differential_stock")["bits_differences"]
    return [
        ("jackson m3 status FAIL, builds PASS", lambda d: edit_records(
            d, "c_jackson_differential_m3", lambda r: r.__setitem__("status", STATUS_FAIL)),
         "c_jackson_differential_m3: status FAIL, its counts say PASS"),
        ("jackson m3 overlay digest changed", lambda d: edit_records(
            d, "c_jackson_differential_m3", lambda r: r["builds"]["overlay"].__setitem__(
                "digests", r["builds"]["overlay"]["digests"] + "0")), "digest equality disagrees"),
        ("jackson stock RESULT differ=1 over a zero table", lambda d: edit_text(d / jackson_stock, "differ=0",
                                                                                "differ=1"),
         "RESULT disagrees with record"),
        ("fdp stock status PASS while bits differ", lambda d: edit_records(
            d, "b_fdp_differential_stock", lambda r: r.__setitem__("status", STATUS_PASS)),
         "b_fdp_differential_stock: status PASS, its counts say FAIL"),
        ("fdp m3 one bits difference under PASS", lambda d: edit_records(
            d, "b_fdp_differential_m3", lambda r: set_summary_field(r, "bits_differ", "1")),
         "b_fdp_differential_m3: status PASS, its counts say FAIL"),
        ("referee exit 1 with java.base right everywhere", lambda d: edit_records(
            d, "b_fdp_differential_stock", lambda r: r.__setitem__("referee_exit", 1)),
         "b_referee_stock: status FAIL, its counts say PASS"),
        ("cited example missing from the difference list", lambda d: edit_text(d / bits, EXAMPLE, OTHER_LITERAL),
         "the cited example"),
        ("jackson-core javac error under PASS", lambda d: edit_records(
            d, "a_compile", lambda r: r["jackson"]["overlay"].__setitem__("errors", 1)),
         "a_compile: status PASS, its counts say FAIL"),
        ("compiled M3 source differs from the pin", lambda d: edit_records(
            d, "m3_patch_compile", lambda r: r["files"].__setitem__(first_file(r), ZERO_DIGEST)),
         "m3src-pin.json describes other sources"),
        ("pin blob mismatch under PASS", lambda d: edit_json(d / "m3src-pin.json", pin_mismatch),
         "m3_src_pin: status PASS, its counts say FAIL"),
        ("class-load overlay run failed under PASS", lambda d: edit_json(
            d / "classload.json", lambda c: c["builds"]["overlay"].__setitem__("exit", 1)),
         "c_class_load_trace: status PASS, its counts say FAIL"),
        ("class-load overlay lists a bit parser", lambda d: edit_json(
            d / "classload.json", lambda c: c["builds"]["overlay"]["classes"].append(BIT_PARSER)),
         "bit-parser lists disagree"),
        ("custody control failed under PASS", lambda d: edit_json(
            d / "custody/custody.json", lambda c: c["tamper_probes"][0].update(ok=False, exit=1)),
         "d_custody: status PASS, its counts say FAIL"),
        ("custody crate drift under ok", lambda d: edit_json(
            d / "custody/custody.json", lambda c: c["central_crate_rule"][0].__setitem__("drift", ["x"])),
         "d_custody: crate row"),
        ("custody without repository_head", lambda d: edit_json(
            d / "custody/custody.json", lambda c: c.pop("repository_head")), "records no repository_head"),
        ("public names from another commit", lambda d: edit_json(
            d / "public-names.json", lambda p: p.__setitem__("head", ZERO_COMMIT)), "public-names.json checked"),
        ("public names violation under PASS", lambda d: edit_json(
            d / "public-names.json", lambda p: p.update(violations=[["a/b.py", "probe"]], violation_count=1)),
         "public_names: status PASS, its counts say FAIL"),
        ("RESULT.jsonl repeats a gate", duplicate_first_record, "RESULT.jsonl repeats gates"),
        ("run recorded at an older head", older_run_head, "its inputs changed before HEAD"),
    ]


def compose(run: Path, out: Path) -> tuple[int, str]:
    out.unlink(missing_ok=True)
    result = subprocess.run([sys.executable, "-I", str(COMPOSER), str(run), "--out", str(out)], capture_output=True,
                            text=True, check=False)
    return result.returncode, (result.stdout + result.stderr).strip()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("run", type=Path)
    parser.add_argument("--scratch", type=Path, required=True)
    args = parser.parse_args(argv)
    copy, out = args.scratch / "run", args.scratch / "GATES.tsv"
    shutil.rmtree(copy, ignore_errors=True)
    pristine = {rel: (args.run / rel).read_bytes() for rel in run_files(args.run)}

    def restore() -> None:
        for rel, data in pristine.items():
            (copy / rel).parent.mkdir(parents=True, exist_ok=True)
            (copy / rel).write_bytes(data)

    restore()
    code, text = compose(copy, out)
    baseline = {"exit": code, "output": text.splitlines()[-1] if text else "", "ok": code == 0 and out.is_file()}
    rows = []
    for label, mutate, reason in probes(copy):
        restore()
        mutate(copy)
        code, text = compose(copy, out)
        rows.append({"probe": label, "exit": code, "refusal": text.splitlines()[-1] if text else "",
                     "ok": code == 1 and REFUSAL in text and reason in text and not out.exists()})
    restore()
    passed = baseline["ok"] and all(row["ok"] for row in rows)
    summary = {"baseline": baseline, "probes": rows, "refused": sum(row["ok"] for row in rows), "count": len(rows),
               "status": STATUS_PASS if passed else STATUS_FAIL}
    (args.scratch / "probes.json").write_text(json.dumps(summary, indent=1, sort_keys=True) + "\n", encoding="utf-8",
                                              newline="\n")
    print(json.dumps({k: summary[k] for k in ("baseline", "refused", "count", "status")}, sort_keys=True))
    return 0 if passed else EXIT_PROBES_FAILED


if __name__ == "__main__":
    raise SystemExit(main())

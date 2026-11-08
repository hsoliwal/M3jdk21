# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""jit_inlining.py: the JIT stage of the hashing leaf, from HotSpot's own logs.

  jit_inlining.py scan <variant> <path> <LogCompilation xml> <PrintInlining text log> <summary tsv> <fails tsv>
  jit_inlining.py join <summary tsv> <before variants> <after variants> <JIT.tsv>
  jit_inlining.py readcost <variant> <path> <LogCompilation xml or .xml.gz> <READ_COST.tsv>
  jit_inlining.py roots <METHOD_SIZES.tsv> <variant> <ROOTS.tsv> <LogCompilation xml or .xml.gz>...

scan reads one run of M3HashAtomsPerf made with -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation
-XX:+PrintInlining (text log) and -XX:+LogCompilation (xml, one section per compiler thread, so no interleaving).
For every compiled method of jdk.internal.m3.hash (absorbed) and net.openhft.hashing (vendored) it records the
highest tier reached, the number of C2 compilations, and the inlining failures of its last C2 compilation (the code
that runs once the profile is stable; an OSR compilation counts only when there is no other), each failure typed by
its callee: READ (the byte reads: access classes, ByteArrayLittleEndian, VarHandle, Unsafe, ByteBuffer, String and
CharSequence reads), KERNEL (M3Maths, M3Primitives, Math), ATOM (another hashing method: a boundary between
separately compiled atoms) or OTHER. It also greps the text log for the failure messages the leaf watches (one
count per message, lines naming a hashing class). Appends to <summary tsv> and <fails tsv> (headers when new).

join writes JIT.tsv: one row per (path, impl, atom) with the tier reached and the failures before and after. The
variants are comma-separated lists of repeated runs (before-r1,before-r2,...): a cell is the value of every run, or
min..max when the runs differ (C2's inlining follows the profile, so counts move from run to run); runs_* says in how
many runs the atom was compiled. Per (path, impl) a total row sums the failures of every hashing method's last C2
compilation in each run (the figures the README quotes), and the text row gives each watched message of the
PrintInlining text log (message=min..max).

readcost measures what one byte read costs C2's inlining budget: for every M3Access/Access short-name read (i64,
u32, i32, u16, i16, u8, i8) that a C2 compilation of a hashing method inlined, the bytecode C2 charges to
DesiredMethodLimit for the read and everything inlined below it (ciMethod::code_size_for_inlining: a method inlined
because of @ForceInline counts 1 byte), and the raw bytecode size. One row per (impl, read): sites, median and
maximum budget bytes, median raw bytes.

roots is the view of the methods above FreqInlineSize (325 bytes): for every absorbed method METHOD_SIZES.tsv marks
gt_FreqInlineSize_325 under <variant>, and its vendored counterpart (the same method of the donor class; for an XXH3
atom of the split, the donor's monolith that holds it), over every C2 compilation of the given logs: its bytecode
size, the highest tier it reached as its own compilation, its C2 compilations, and how its callers' compilations
treated each call of it (inline_success and inline_fail reasons with counts). A method that is never compiled is
cold (only <clinit> here: it runs once, interpreted).

Every table these subcommands write starts with the SPDX lines ('#'); readers skip lines that start with '#'.
"""
import gzip
import os
import statistics
import re
import sys

NL = "\n"
TAB = "\t"
SPDX_LINES = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0",
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others")
COMMENT = "#"
LIST = ","
RANGE = ".."
ABSORBED = "jdk.internal.m3.hash."
VENDORED = "net.openhft.hashing."
C2_LEVEL = 4
ATTR = re.compile(r"(\w+)='([^']*)'")
ESCAPES = (("&apos;", "'"), ("&gt;", ">"), ("&lt;", "<"), ("&quot;", '"'), ("&amp;", "&"))
WATCHED = ("too big", "hot method too big", "callee is too large", "already compiled into a big method",
           "NodeCountInliningCutoff", "inlining too deep", "size > DesiredMethodLimit")
READ_HOLDERS = ("Access", "ByteArrayLittleEndian", "java.lang.invoke.VarHandle", "jdk.internal.misc.Unsafe",
                "sun.misc.Unsafe", "jdk.internal.misc.ScopedMemoryAccess", "java.nio.", "java.lang.String",
                "java.lang.CharSequence", "java.lang.AbstractStringBuilder", "java.lang.StringLatin1",
                "java.lang.StringUTF16", "jdk.internal.util.Preconditions", "java.lang.invoke.LambdaForm",
                "java.lang.invoke.Invokers")
KERNEL_HOLDERS = ("Maths", "Primitives", "java.lang.Math", "java.lang.Long", "java.lang.Integer")
SUMMARY_HEADER = ("variant", "path", "impl", "atom", "tier_reached", "c2_compiles", "last_c2_kind",
                  "read_fails", "kernel_fails", "atom_boundaries", "other_fails", "read_fail_reasons",
                  "text_watched_hits")
FAILS_HEADER = ("variant", "path", "impl", "atom", "compile_id", "kind", "caller", "bci", "callee", "reason",
                "type")
JIT_HEADER = ("path", "impl", "atom", "runs_before", "runs_after", "tier_before", "tier_after", "c2_compiles_before",
              "c2_compiles_after", "read_fails_before", "read_fails_after", "kernel_fails_before",
              "kernel_fails_after", "atom_boundaries_before", "atom_boundaries_after", "read_fail_reasons_before",
              "read_fail_reasons_after")
ABSENT = "-"
TEXT_ROW = "(PrintInlining text log, lines naming a hashing class)"
TOTAL_ROW = "(total: every hashing method's last C2 compilation)"
JOIN_NUMBERS = ("tier_reached", "c2_compiles", "read_fails", "kernel_fails", "atom_boundaries")
TOTAL_NUMBERS = ("read_fails", "kernel_fails", "atom_boundaries")


def unescape(s):
    for a, b in ESCAPES:
        s = s.replace(a, b)
    return s


def attrs(line):
    return {k: unescape(v) for k, v in ATTR.findall(line)}


def impl_of(name):
    return "absorbed" if name.startswith(ABSORBED) else "vendored" if name.startswith(VENDORED) else ""


def callee_type(holder):
    if holder.startswith(ABSORBED) or holder.startswith(VENDORED):
        simple = holder.rsplit(".", 1)[-1]
        if "Access" in simple:
            return "READ"
        if simple.startswith(("Maths", "M3Maths", "Primitives", "M3Primitives")):
            return "KERNEL"
        return "ATOM"
    if any(holder.startswith(h) or h in holder for h in READ_HOLDERS):
        return "READ"
    if any(holder.startswith(h) for h in KERNEL_HOLDERS):
        return "KERNEL"
    return "OTHER"


def task_name(method_attr):
    """'jdk.internal.m3.hash.XXH3 XXH3_128bits_internal (J[B...)J' -> holder, name."""
    holder, rest = method_attr.split(" ", 1)
    return holder, rest.split(" ", 1)[0]


KLASS_KEYS = ("id", "name")
METHOD_KEYS = ("id", "holder", "name")


def complete(a, keys):
    """False for an element the VM's exit cut off (LogCompilation closes the thread's log as a <fragment>)."""
    return all(k in a for k in keys)


def open_log(path):
    return gzip.open(path, "rt", encoding="utf-8", errors="replace") if path.endswith(".gz") else \
        open(path, encoding="utf-8", errors="replace")


def table_rows(path):
    """The rows of a tab-separated table (header first), lines starting with '#' skipped."""
    with open(path, encoding="utf-8") as f:
        lines = [line.rstrip(NL) for line in f if not line.startswith(COMMENT)]
    header = lines[0].split(TAB)
    return [dict(zip(header, line.split(TAB))) for line in lines[1:]]


def scan_xml(path):
    """Yields one dict per compile task: id, kind, level, holder, name, fails [(caller, bci, callee, reason)]."""
    task = None
    with open_log(path) as f:
        for raw in f:
            line = raw.strip()
            if line.startswith("<task "):
                a = attrs(line)
                holder, name = task_name(a["method"])
                task = {"id": a.get("compile_id", ""), "kind": a.get("compile_kind", "normal"),
                        "level": int(a.get("level", C2_LEVEL)), "holder": holder, "name": name, "fails": [],
                        "stamp": float(a.get("stamp", "0")), "klass": {}, "method": {}, "stack": [],
                        "call": None, "bci": ""}
                continue
            if task is None:
                continue
            if line.startswith("</task>"):
                yield task
                task = None
            elif line.startswith("<klass "):
                a = attrs(line)
                if complete(a, KLASS_KEYS):
                    task["klass"][a["id"]] = a["name"]
            elif line.startswith("<method "):
                a = attrs(line)
                if complete(a, METHOD_KEYS):
                    task["method"][a["id"]] = (task["klass"].get(a["holder"], a["holder"]), a["name"])
            elif line.startswith("<parse "):
                task["stack"].append(attrs(line).get("method"))
            elif line.startswith("</parse>"):
                if task["stack"]:
                    task["stack"].pop()
            elif line.startswith("<bc "):
                task["bci"] = attrs(line).get("bci", "")
            elif line.startswith("<call "):
                task["call"] = attrs(line).get("method")
            elif line.startswith("<inline_fail "):
                callee = task["method"].get(task["call"], ("?", "?"))
                caller = task["method"].get(task["stack"][-1], ("?", "?")) if task["stack"] else ("?", "?")
                task["fails"].append((caller, task["bci"], callee, attrs(line).get("reason", "")))


def text_hits(path):
    """Watched failure messages in the PrintInlining text log, per impl, on lines naming a hashing class."""
    hits = {"absorbed": {w: 0 for w in WATCHED}, "vendored": {w: 0 for w in WATCHED}}
    with open_log(path) as f:
        for line in f:
            for impl, marker in (("absorbed", ABSORBED), ("vendored", VENDORED)):
                if marker in line:
                    for w in WATCHED:
                        if w in line and not (w == "too big" and "hot method too big" in line):
                            hits[impl][w] += 1
    return hits


def append(path, header, rows):
    new = not os.path.exists(path)
    with open(path, "a", encoding="utf-8", newline=NL) as w:
        if new:
            w.write(NL.join(SPDX_LINES) + NL + TAB.join(header) + NL)
        w.write("".join(TAB.join(str(c) for c in r) + NL for r in rows))


def scan(variant, run_path, xml, text, summary, fails_out):
    tasks = {}
    for t in scan_xml(xml):
        impl = impl_of(t["holder"])
        if impl:
            tasks.setdefault((impl, t["holder"].rsplit(".", 1)[-1] + "." + t["name"]), []).append(t)
    hits = text_hits(text)
    rows, fail_rows = [], []
    for (impl, atom), ts in sorted(tasks.items()):
        c2 = [t for t in ts if t["level"] == C2_LEVEL]
        normal = [t for t in c2 if t["kind"] != "osr"]
        last = max(normal or c2, key=lambda t: t["stamp"]) if c2 else None
        counts = {"READ": 0, "KERNEL": 0, "ATOM": 0, "OTHER": 0}
        reasons = {}
        if last:
            for caller, bci, callee, reason in last["fails"]:
                kind = callee_type(callee[0])
                counts[kind] += 1
                if kind == "READ":
                    reasons[reason] = reasons.get(reason, 0) + 1
                fail_rows.append((variant, run_path, impl, atom, last["id"], last["kind"], ".".join(caller), bci,
                                  ".".join(callee), reason, kind))
        rows.append((variant, run_path, impl, atom, max(t["level"] for t in ts), len(c2),
                     last["kind"] if last else ABSENT, counts["READ"], counts["KERNEL"], counts["ATOM"],
                     counts["OTHER"], ";".join(f"{r}={n}" for r, n in sorted(reasons.items())) or ABSENT, ABSENT))
    for impl in ("absorbed", "vendored"):
        watched = ";".join(f"{w}={n}" for w, n in hits[impl].items() if n)
        rows.append((variant, run_path, impl, TEXT_ROW, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT, ABSENT,
                     ABSENT, watched or "none"))
    append(summary, SUMMARY_HEADER, rows)
    append(fails_out, FAILS_HEADER, fail_rows)
    total = sum(int(r[7]) for r in rows if r[2] == "absorbed" and r[3] != TEXT_ROW)
    print(f"{variant} {run_path}: {len(rows)} compiled hashing methods, absorbed read-site failures {total}; "
          f"text {dict((k, v) for k, v in hits['absorbed'].items() if v)}")


def span(values):
    """The value of every run, or min..max when the runs differ; ABSENT when no run has one."""
    nums = [int(v) for v in values if v != ABSENT]
    if not nums:
        return ABSENT
    lo, hi = min(nums), max(nums)
    return str(lo) if lo == hi else f"{lo}{RANGE}{hi}"


def reason_counts(cell):
    """'reason a=3;reason b=1' -> {'reason a': 3, 'reason b': 1} ('-' and 'none' are empty)."""
    out = {}
    if cell not in (ABSENT, "none", ""):
        for part in cell.split(";"):
            reason, _, n = part.rpartition("=")
            out[reason] = out.get(reason, 0) + int(n)
    return out


def reason_span(runs):
    """Per reason, its count in every run (0 in a run without it), as a span."""
    keys = sorted(set().union(*runs)) if runs else []
    return ";".join(f"{k}={span([str(r.get(k, 0)) for r in runs])}" for k in keys) or ABSENT


def join(summary, before, after, out):
    sides = (before.split(LIST), after.split(LIST))
    rows = {}
    for r in table_rows(summary):
        if any(r["variant"] in vs for vs in sides):
            rows.setdefault((r["path"], r["impl"]), {}).setdefault(r["atom"], {})[r["variant"]] = r
    table = []
    for (path, impl), atoms in sorted(rows.items()):
        total = [(path, impl, TOTAL_ROW)]
        for vs in sides:
            ran = [v for v in vs if any(v in runs for runs in atoms.values())]
            total.append(f"{len(ran)}/{len(vs)}")
        cells = {}
        for c in TOTAL_NUMBERS:
            for k, vs in enumerate(sides):
                cells[(c, k)] = span([str(sum(int(runs[v][c]) for a, runs in atoms.items()
                                              if a != TEXT_ROW and v in runs))
                                      for v in vs if any(v in runs for runs in atoms.values())])
        reasons = []
        for vs in sides:
            per_run = []
            for v in vs:
                merged = {}
                for a, runs in atoms.items():
                    if a != TEXT_ROW and v in runs:
                        for reason, n in reason_counts(runs[v]["read_fail_reasons"]).items():
                            merged[reason] = merged.get(reason, 0) + n
                if any(v in runs for runs in atoms.values()):
                    per_run.append(merged)
            reasons.append(reason_span(per_run))
        table.append(tuple(total[0]) + tuple(total[1:])
                     + tuple(cells.get((c, k), ABSENT) for c in JOIN_NUMBERS for k in (0, 1)) + tuple(reasons))
        for atom in [TEXT_ROW] + sorted(a for a in atoms if a != TEXT_ROW):
            runs = atoms.get(atom, {})
            present = [[runs[v] for v in vs if v in runs] for vs in sides]
            counts = tuple(f"{len(p)}/{len(vs)}" for p, vs in zip(present, sides))
            if atom == TEXT_ROW:
                numbers = (ABSENT,) * (2 * len(JOIN_NUMBERS))
                tail = tuple(reason_span([reason_counts(r["text_watched_hits"]) for r in p]) for p in present)
            else:
                numbers = tuple(span([r[c] for r in p]) for c in JOIN_NUMBERS for p in present)
                tail = tuple(reason_span([reason_counts(r["read_fail_reasons"]) for r in p]) for p in present)
            table.append((path, impl, atom) + counts + numbers + tail)
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write(NL.join(SPDX_LINES) + NL + TAB.join(JIT_HEADER) + NL + "".join(TAB.join(r) + NL for r in table))
    print(f"{out}: {len(table)} rows")


ROOTS_HEADER = ("variant", "impl", "class", "method", "bytes", "tier_reached", "c2_compiles", "inlined_at_call_sites",
                "not_inlined_at_call_sites", "logs")
ABSORBED_PREFIX = "M3"
# the XXH3 atoms of the split have no method of their own in the donor: the monolith that holds them stands in
SPLIT_MONOLITHS = {"XXH3_hashLong": ("XXH3_64bits_internal", "XXH3_128bits_internal"),
                   "XXH3_len_17to128_128b": ("XXH3_128bits_internal",),
                   "XXH3_len_129to240_128b": ("XXH3_128bits_internal",)}
COLD = "cold (never compiled)"


def call_decisions(path, targets, seen):
    """Adds one log's C2 facts for every target (impl, class, method) to seen."""
    task, callee = None, None
    with open_log(path) as f:
        for raw in f:
            line = raw.strip()
            if line.startswith("<task "):
                a = attrs(line)
                holder, name = task_name(a["method"])
                level = int(a.get("level", C2_LEVEL))
                key = (impl_of(holder), holder.rsplit(".", 1)[-1], name)
                if key in targets:
                    fact = seen[key]
                    fact["tier"] = max(fact["tier"], level)
                    fact["c2"] += level == C2_LEVEL
                    fact["bytes"] = a.get("bytes", fact["bytes"])
                task, callee = ({"klass": {}, "method": {}} if level == C2_LEVEL else None), None
            elif task is None:
                continue
            elif line.startswith("</task>"):
                task = None
            elif line.startswith("<klass "):
                a = attrs(line)
                if complete(a, KLASS_KEYS):
                    task["klass"][a["id"]] = a["name"]
            elif line.startswith("<method ") and complete(attrs(line), METHOD_KEYS):
                a = attrs(line)
                holder = task["klass"].get(a["holder"], a["holder"])
                key = (impl_of(holder), holder.rsplit(".", 1)[-1], a["name"])
                task["method"][a["id"]] = key
                if key in targets:
                    seen[key]["bytes"] = a.get("bytes", seen[key]["bytes"])
            elif line.startswith("<call "):
                callee = task["method"].get(attrs(line).get("method"))
            elif line.startswith(("<inline_success ", "<inline_fail ")) and callee in targets:
                kind = "inlined" if line.startswith("<inline_success ") else "not"
                reason = attrs(line).get("reason", "")
                bucket = seen[callee][kind]
                bucket[reason] = bucket.get(reason, 0) + 1
                callee = None


def donor_class(cls):
    return cls[len(ABSORBED_PREFIX):] if cls.startswith(ABSORBED_PREFIX) else cls


def roots(sizes, variant, out, logs):
    targets = {}
    for r in table_rows(sizes):
        if r["variant"] == variant and r["gt_FreqInlineSize_325"] == "yes":
            cls, method = r["class"], r["method"]
            donor = donor_class(cls)
            targets[("absorbed", cls, method)] = (int(r["bytecode_bytes"]), r["bytecode_bytes"])
            for m in SPLIT_MONOLITHS.get(method, (method,)):
                targets.setdefault(("vendored", donor, m), (int(r["bytecode_bytes"]), ABSENT))
    seen = {k: {"tier": 0, "c2": 0, "bytes": b, "inlined": {}, "not": {}} for k, (_, b) in targets.items()}
    for log in logs:
        call_decisions(log, targets, seen)
    order = sorted(targets, key=lambda k: (-targets[k][0], donor_class(k[1]), k[0], k[2]))
    rows = []
    for key in order:
        fact = seen[key]
        tier = str(fact["tier"]) if fact["tier"] else COLD
        rows.append((variant,) + key + (fact["bytes"], tier, fact["c2"],
                                         ";".join(f"{r}={n}" for r, n in sorted(fact["inlined"].items())) or ABSENT,
                                         ";".join(f"{r}={n}" for r, n in sorted(fact["not"].items())) or ABSENT,
                                         len(logs)))
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write(NL.join(SPDX_LINES) + NL + TAB.join(ROOTS_HEADER) + NL
                + "".join(TAB.join(str(c) for c in r) + NL for r in rows))
    for r in rows:
        print(TAB.join(str(c) for c in r))


SHORT_READS = ("i64", "u32", "i32", "u16", "i16", "u8", "i8")
READ_COST_HEADER = ("variant", "path", "impl", "read", "inlined_sites", "budget_bytes_median", "budget_bytes_max",
                    "raw_bytes_median")
FORCED = "force inline by annotation"


def read_costs(path):
    """(impl, holder.name) -> [(budget, raw)] for every inlined short-name read in C2 compilations."""
    costs, frames, methods, klass, reason, level, impl = {}, [], {}, {}, "", 0, ""
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as f:
        for raw in f:
            line = raw.strip()
            if line.startswith("<task "):
                a = attrs(line)
                level, impl = int(a.get("level", C2_LEVEL)), impl_of(a["method"])
                frames, methods, klass, reason = [], {}, {}, ""
            elif level != C2_LEVEL or not impl:
                continue
            elif line.startswith("<klass "):
                a = attrs(line)
                if complete(a, KLASS_KEYS):
                    klass[a["id"]] = a["name"]
            elif line.startswith("<method "):
                a = attrs(line)
                if complete(a, METHOD_KEYS):
                    methods[a["id"]] = (klass.get(a["holder"], a["holder"]), a["name"], int(a.get("bytes", "0")))
            elif line.startswith("<inline_success "):
                reason = attrs(line).get("reason", "")
            elif line.startswith("<parse "):
                holder, name, size = methods.get(attrs(line).get("method"), ("?", "?", 0))
                budget = 1 if reason == FORCED and frames else size
                frames.append([holder, name, budget, size, []])
                reason = ""
            elif line.startswith("</parse>") and frames:
                holder, name, budget, size, kids = frames.pop()
                if frames:
                    frames[-1][2] += budget
                    frames[-1][3] += size
                    frames[-1][4].append(holder.rsplit(".", 1)[-1])
                    if name in SHORT_READS and holder.endswith("Access"):
                        through = "+".join(sorted(set(kids))) or "(not inlined)"
                        key = (impl_of(holder) or impl, holder.rsplit(".", 1)[-1] + "." + name + " -> " + through)
                        costs.setdefault(key, []).append((budget, size))
    return costs


def readcost(variant, run_path, xml, out):
    rows = []
    for (impl, read), xs in sorted(read_costs(xml).items()):
        budgets, raws = [b for b, _ in xs], [r for _, r in xs]
        rows.append((variant, run_path, impl, read, len(xs), int(statistics.median(budgets)), max(budgets),
                     int(statistics.median(raws))))
    append(out, READ_COST_HEADER, rows)
    for r in rows:
        print(TAB.join(str(c) for c in r))


def main():
    if sys.argv[1] == "scan":
        scan(*sys.argv[2:8])
    elif sys.argv[1] == "join":
        join(*sys.argv[2:6])
    elif sys.argv[1] == "readcost":
        readcost(*sys.argv[2:6])
    elif sys.argv[1] == "roots":
        roots(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5:])
    else:
        raise SystemExit(__doc__)


main()

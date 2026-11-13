# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""jit_inline.py summarize <out.tsv> <vm_log.xml>...
   jit_inline.py merge <before.tsv> <after.tsv> <size_before.tsv> <size_after.tsv> <JIT.tsv>

Gate JIT_INLINE v1 (CPU, read-only) over a HotSpot 21 run with
  -XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining -XX:+LogVMOutput -XX:-DisplayVMOutput
  -XX:LogFile=<vm_log.xml>
The VM log keeps the tty text with a <writer thread='N'/> mark whenever the writing thread changes, so the text is
first split back into one stream per thread (compiler threads print concurrently). In a compiler thread's stream a
PrintCompilation line opens a compile (tier 1-3 = C1, 4 = C2; '%' = OSR) and the PrintInlining lines after it are
that compile's inlining tree (two more spaces of indent per level; the caller of a line is the nearest shallower
inlined line, else the compile's root method).

summarize: one row per method of the jdk.internal.math M3* types and FloatingDecimal, counts summed over the logs:
  method bytes own_max_tier compiles osr_compiles c2_inlined c1_inlined c2_failures c1_failures
own_max_tier is the highest tier of the method's own compiles ('-' = never compiled on its own); c2_inlined /
c1_inlined count inlining-tree lines that inlined it; *_failures list reason=count over the lines that did not, each
reason qualified by the caller for C2 (caller>reason=count; C1 reasons alone), sorted.
merge: JIT.tsv, one row per method of either summary or either size table (jit_size.py) of the M3 types and the
FloatingDecimal wiring:
  atom bytes_before bytes_after tier_before tier_after c2_inlined_before c2_inlined_after
  inlining_failures_before inlining_failures_after
tier = own_max_tier, or 'inlined:4' when the method has no own compile but a C2 compile inlined it.
inlining_failures = the C2 failures with the reasons that the JIT stage gates on (too big, hot method too big,
callee is too large, already compiled into a big method, already compiled into a medium method,
NodeCountInliningCutoff, inlining too deep, size > DesiredMethodLimit); C1 reasons and policy reasons
(low call site frequency, don't inline Throwable constructors, ...) stay in the summaries."""
import html
import re
import sys

DEFAULT_METHODS = r"^jdk\.internal\.math\.(M3\w+|FloatingDecimal)(\$\w+)?::"
WRITER = re.compile(r"<writer thread='(\d+)'/>")
TAG = re.compile(r"</?\w[^>]*>")
COMPILE = re.compile(r"^\s*\d+\s+\d+\s+([%sbn! ]*?)\s*([0-4])\s+(\S+::\S+?)(?: @ \d+)? \((\d+) bytes\)(.*)$")
# the indent may carry C2 call-site marks ('!' = inside an exception handler, 'm' = method handle)
INLINE = re.compile(r"^([ !m]*)@ (\d+)\s+(\S+::\S+) (?:\((\d+) bytes\)|\(not loaded\))\s*(.*?)\s*$")
GATED = ("too big", "hot method too big", "callee is too large", "already compiled into a big method",
         "already compiled into a medium method", "NodeCountInliningCutoff", "inlining too deep",
         "size > DesiredMethodLimit")
SUCCESS = ("inline", "intrinsic", "force inline", "accessor")


def streams(path):
    """{thread: [lines]} of the <tty> text, de-interleaved by writer marks."""
    text = open(path, encoding="utf-8", errors="replace").read()
    start = text.find("<tty>")
    end = text.find("</tty>", start)
    body = text[start + len("<tty>"):end if end >= 0 else len(text)]
    out, thread, pos = {}, "main", 0
    for m in WRITER.finditer(body):
        out.setdefault(thread, []).append(body[pos:m.start()])
        thread, pos = m.group(1), m.end()
    out.setdefault(thread, []).append(body[pos:])
    return {t: html.unescape(TAG.sub("", "".join(parts))).splitlines() for t, parts in out.items()}


def success(msg):
    return msg.startswith(SUCCESS) or msg.startswith("(intrinsic")


def repaired(lines, stats):
    """Logical lines of one thread stream. C1 prints an inlining line in pieces; when another thread writes in
    between, the VM log ends the piece with a line break, so fragments are joined until they form a whole
    PrintInlining line (whitespace-only pieces are kept as the indent of the line they start). A fragment run that
    never completes is dropped and counted in stats['unrepaired']."""
    out, buf = [], ""
    for ln in lines:
        whole = INLINE.match(ln)
        if COMPILE.match(ln) or (whole and whole.group(5)):
            if whole and ln.startswith("@") and buf and not buf.strip():
                ln = buf + ln  # the indent was its own piece
                stats["joined"] += 1
            elif buf.strip():
                stats["unrepaired"] += 1
            buf = ""
            out.append(ln)
            continue
        buf += ln
        m = INLINE.match(buf)
        if m and m.group(5):
            out.append(buf)
            stats["joined"] += 1
            buf = ""
        elif not buf.strip():
            buf = ln  # only the latest whitespace piece can be an indent
    if buf.strip():
        stats["unrepaired"] += 1
    return out


def summarize(out_path, paths):
    want = re.compile(DEFAULT_METHODS)
    rows = {}

    def row(method, size):
        r = rows.setdefault(method, {"bytes": size, "tier": 0, "compiles": 0, "osr": 0, "c2": 0, "c1": 0,
                                     "f2": {}, "f1": {}})
        r["bytes"] = r["bytes"] or size
        return r

    stats = {"joined": 0, "unrepaired": 0}
    for raw in (lines for path in paths for lines in streams(path).values()):
        root, tier, stack, base = None, None, [], None
        for ln in repaired(raw, stats):
            c = COMPILE.match(ln)
            if c:
                flags, t, method, size, rest = c.groups()
                if "made not entrant" in rest or "made zombie" in rest:
                    continue
                root, tier, stack, base = method, int(t), [], None
                if want.search(method):
                    r = row(method, int(size))
                    r["tier"] = max(r["tier"], tier)
                    r["osr" if "%" in flags else "compiles"] += 1
                continue
            i = INLINE.match(ln)
            if not i or root is None:
                continue
            indent, _, callee, size, msg = i.groups()
            base = len(indent) if base is None else min(base, len(indent))
            depth = (len(indent) - base) // 2
            del stack[depth:]
            caller = stack[-1] if stack else root
            ok = success(msg)
            stack.append(callee if ok else "!" + callee)
            if not want.search(callee):
                continue
            r = row(callee, int(size) if size else 0)
            key = caller.split("::")[-1] if caller.startswith("jdk.internal.math.") else caller
            if ok:
                r["c2" if tier == 4 else "c1"] += 1
            else:
                bucket = r["f2" if tier == 4 else "f1"]
                # C1 trees lose their indent when pieces interleave: C1 reasons carry no caller
                reason = "%s>%s" % (key.lstrip("!"), msg) if tier == 4 else msg
                bucket[reason] = bucket.get(reason, 0) + 1
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("method\tbytes\town_max_tier\tcompiles\tosr_compiles\tc2_inlined\tc1_inlined\tc2_failures\tc1_failures\n")
        for m in sorted(rows):
            r = rows[m]
            f.write("%s\t%d\t%s\t%d\t%d\t%d\t%d\t%s\t%s\n" % (
                m, r["bytes"], r["tier"] or "-", r["compiles"], r["osr"], r["c2"], r["c1"],
                "; ".join("%s=%d" % kv for kv in sorted(r["f2"].items())) or "-",
                "; ".join("%s=%d" % kv for kv in sorted(r["f1"].items())) or "-"))
    gated = sum(n for r in rows.values() for k, n in r["f2"].items() if k.split(">", 1)[1].startswith(GATED))
    sys.stdout.write("METHODS\t%d\tC2_GATED_FAILURES\t%d\tJOINED_FRAGMENTS\t%d\tUNREPAIRED_FRAGMENTS\t%d\n"
                     % (len(rows), gated, stats["joined"], stats["unrepaired"]))
    return 0


def load(path):
    rows = {}
    for n, ln in enumerate(open(path, encoding="utf-8")):
        f = ln.rstrip("\n").split("\t")
        if n:
            rows[f[0]] = f
    return rows


def sizes(path):
    """{class::name: bytes} from a jit_size.py table (overloads keep the largest)."""
    out = {}
    for n, ln in enumerate(open(path, encoding="utf-8")):
        f = ln.rstrip("\n").split("\t")
        if n == 0 or f[3] == "-":
            continue
        sig = f[1]
        name = "<clinit>" if sig == "static {};" else sig.split("(", 1)[0].split()[-1].rsplit(".", 1)[-1]
        if "$" not in f[0] and name == f[0]:
            name = "<init>"
        key = "jdk.internal.math.%s::%s" % (f[0], name)
        out[key] = max(out.get(key, 0), int(f[3]))
    return out


def gated(cell):
    if cell == "-":
        return []
    return [p for p in cell.split("; ") if p.split(">", 1)[1].rsplit("=", 1)[0].startswith(GATED)]


def tier(r):
    if r is None:
        return "-"
    if r[2] != "-":
        return r[2]
    return "inlined:4" if int(r[5]) > 0 else ("inlined:c1" if int(r[6]) > 0 else "-")


def merge(before, after, size_before, size_after, out_path):
    b, a = load(before), load(after)
    sb, sa = sizes(size_before), sizes(size_after)
    leaf = re.compile(r"^jdk\.internal\.math\.(M3\w+(\$\w+)?::|FloatingDecimal::(parseDouble|parseFloat)$)")
    names = sorted(m for m in set(b) | set(a) | set(sb) | set(sa) if leaf.search(m))
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("atom\tbytes_before\tbytes_after\ttier_before\ttier_after\tc2_inlined_before\tc2_inlined_after"
                "\tinlining_failures_before\tinlining_failures_after\n")
        for m in names:
            rb, ra = b.get(m), a.get(m)
            cells = (m.replace("jdk.internal.math.", ""), str(sb.get(m, "-")), str(sa.get(m, "-")),
                     tier(rb), tier(ra), rb[5] if rb else "-", ra[5] if ra else "-",
                     "; ".join(gated(rb[7])) if rb else "-", "; ".join(gated(ra[7])) if ra else "-")
            f.write("\t".join(c or "-" for c in cells) + "\n")  # an empty cell is written as '-'
    total_b = sum(int(p.rsplit("=", 1)[1]) for m in names if m in b for p in gated(b[m][7]))
    total_a = sum(int(p.rsplit("=", 1)[1]) for m in names if m in a for p in gated(a[m][7]))
    sys.stdout.write("ATOMS\t%d\tGATED_FAILURES_BEFORE\t%d\tAFTER\t%d\n" % (len(names), total_b, total_a))
    return 0


def main(argv):
    if argv[1] == "summarize":
        return summarize(argv[2], argv[3:])
    if argv[1] == "merge":
        return merge(*argv[2:7])
    raise SystemExit(__doc__)


if __name__ == "__main__":
    sys.exit(main(sys.argv))

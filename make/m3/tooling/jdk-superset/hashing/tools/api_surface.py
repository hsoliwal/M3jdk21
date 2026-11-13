# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""api_surface.py <javap> <vendored classes dir> <absorbed classes dir> <out.tsv>

API_SURFACE v1: the public/protected members of the facade and access template types, vendored
(net.openhft.hashing.T) against absorbed (jdk.internal.m3.hash.M3T), from javap -protected. Members are normalized
to "[static ]return name(params)" with erased type variables and simple type names, donor types named M3T. Every
vendored member is KEPT (same normalized member in the absorbed type) or DROPPED (with the recipe rule and reason);
an absorbed member without a vendored one is ADDED (there must be none). Exit 1 if anything is ADDED or unexplained."""
import re
import subprocess
import sys

TAB = "\t"
NL = "\n"
NONE = "-"
DONOR = "net.openhft.hashing."
M3 = "jdk.internal.m3.hash."
TYPES = ("Access", "LongHashFunction", "LongTupleHashFunction")
MODIFIERS = {"public", "protected", "abstract", "final", "native", "synchronized", "default"}
DROP_REASONS = (
    (r"\bhashMemory\(", "FACADE_DROP_RAW_MEMORY v1", "reads a raw address (sun.misc.Unsafe); no byte[]/ByteBuffer form"),
    (r"\bhashBooleans?\(", "FACADE_DROP_RAW_MEMORY v1", "reads the JVM's boolean storage (sun.misc.Unsafe)"),
    (r"\bhash(?:Chars\(char|Shorts\(short|Ints\(int|Longs\(long)\[\]", "FACADE_DROP_RAW_MEMORY v1",
     "reads a typed array's memory (sun.misc.Unsafe); no byte[]/ByteBuffer form"),
    (r"\bunsafe\(\)", "ACCESS_DROP_UNSAFE v1", "raw object/address access (sun.misc.Unsafe)"),
)


def simple(t, tvars):
    t = re.sub(r"<.*>", "", t.strip())
    dims = t.count("[]")
    base = t.replace("[]", "").strip()
    base = tvars.get(base, base)
    name = base.rsplit(".", 1)[-1]
    if base.startswith(DONOR):
        name = "M3" + name
    return name + "[]" * dims


def split_params(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        depth += (ch == "<") - (ch == ">")
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    return [p for p in (x.strip() for x in out + [cur]) if p]


def members(javap, classes, fqn):
    text = subprocess.run([javap, "-protected", "-cp", classes, fqn], capture_output=True, text=True, check=True).stdout
    found = set()
    for line in text.splitlines():
        line = line.strip().rstrip(";")
        m = re.match(r"(.*?)\b([\w$]+)\((.*)\)(?: throws .*)?$", line)
        if not m or line.startswith(("class ", "public class", "public abstract class")) or "{" in line:
            continue
        head, name, params = m.groups()
        if head.endswith("."):
            found.add("<init>(" + ",".join(simple(p, {}) for p in split_params(params)) + ")")
            continue
        words, tvars = head.split(), {}
        tparams = re.match(r"(.*?)<(.+?)>\s+(\S+)\s*$", head)
        if tparams:
            for tv in split_params(tparams.group(2)):
                parts = tv.split(" extends ")
                tvars[parts[0].strip()] = parts[1].split("&")[0].strip() if len(parts) > 1 else "java.lang.Object"
            words = (tparams.group(1) + " " + tparams.group(3)).split()
        is_static = "static" in words
        rest = [w for w in words if w not in MODIFIERS and w != "static"]
        if not rest:
            continue
        ret = simple(rest[-1], tvars)
        sig = ",".join(simple(p, tvars) for p in split_params(params))
        found.add(("static " if is_static else "") + f"{ret} {name}({sig})")
    return found


def main():
    javap, vendored, absorbed, out = sys.argv[1:5]
    rows, bad = [], []
    for t in TYPES:
        old = members(javap, vendored, DONOR + t)
        new = members(javap, absorbed, M3 + "M3" + t)
        for m in sorted(old | new):
            if m in old and m in new:
                rows.append((f"M3{t}", m, "KEPT", NONE, NONE))
            elif m in old:
                why = next(((r, w) for p, r, w in DROP_REASONS if re.search(p, m)), None)
                rows.append((f"M3{t}", m, "DROPPED") + (why or (NONE, "UNEXPLAINED")))
                if not why:
                    bad.append(m)
            else:
                rows.append((f"M3{t}", m, "ADDED", NONE, NONE))
                bad.append(m)
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write("type\tmember\tstatus\trule\treason\n" + "".join(TAB.join(r) + NL for r in rows))
    counts = {s: sum(r[2] == s for r in rows) for s in ("KEPT", "DROPPED", "ADDED")}
    print(" ".join(f"{k.lower()}={v}" for k, v in counts.items()), f"unexplained={len(bad)}")
    sys.exit(1 if bad else 0)


main()

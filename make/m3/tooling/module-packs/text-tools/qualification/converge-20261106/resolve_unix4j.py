"""resolve_unix4j.py <m3-root> <dev-dir> <mout> <merge.tsv> <final-root> : M3_TEXT_TOOLS_CONVERGE v1 decisions.
- EQUIVALENT (JsonLinesTools, StructuredCli): M3 copy stays (differences are header/imports only).
- KEEP_OURS_RETAIN (NativeUtf16Literal): M3's paged CharSequence kernel stays live; the donor's String kernel is kept
  byte for byte under m3/qualification/master-landing-20261028/retained-variants/pr2498/.
- UNION (TextTools, m3textio.c): both appended regions are kept, M3's first.
Writes every ADD/MERGED/SAME/resolved file under <final-root> at its M3 path; prints one decision row per file."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from merge_unix4j import relocate, set_package, target  # noqa: E402

RETAIN = "m3/qualification/master-landing-20261028/retained-variants/pr2498/"
DECIDE = {"JsonLinesTools.java": "OURS", "StructuredCli.java": "OURS", "NativeUtf16Literal.java": "OURS_RETAIN",
          "TextTools.java": "UNION", "m3textio.c": "UNION"}


def drop_known(ours, theirs, min_block=5):
    """theirs minus every run of >= min_block lines that already appears verbatim in ours (no duplicate methods)."""
    import difflib
    keep = [True] * len(theirs)
    for blk in difflib.SequenceMatcher(None, ours, theirs, autojunk=False).get_matching_blocks():
        if blk.size >= min_block:
            for k in range(blk.b, blk.b + blk.size):
                keep[k] = False
    return [l for l, k in zip(theirs, keep) if k]


C_FUNC = __import__("re").compile(r"^(?:static|JNIEXPORT)\b[^;{]*?\b(\w+)\s*\(", __import__("re").M)


def dedupe_c(text):
    """Keep the first top-level definition of every C function name (M3's copy comes first in the union)."""
    lines, out, seen, i = text.split("\n"), [], set(), 0
    while i < len(lines):
        head = "\n".join(lines[i:i + 2])
        m = C_FUNC.match(head)
        if m and not lines[i].rstrip().endswith(";") and "(" in head:
            j = i
            while j < len(lines) and lines[j] != "}":
                j += 1
            name = m.group(1)
            if name in seen:
                i = j + 1
                continue
            seen.add(name)
            out += lines[i:j + 1]
            i = j + 1
            continue
        out.append(lines[i])
        i += 1
    return "\n".join(out)


def union(text):
    out, part, o, t = [], None, [], []
    for line in text.split("\n"):
        if line.startswith("<<<<<<< "):
            part, o, t = "o", [], []
        elif line.startswith("||||||| ") and part:
            part = "b"
        elif line == "=======" and part:
            part = "t"
        elif line.startswith(">>>>>>> ") and part:
            out += o + drop_known(o, t)
            part = None
        elif part == "o":
            o.append(line)
        elif part == "t":
            t.append(line)
        elif part is None:
            out.append(line)
    return "\n".join(out)


m3_root, dev, mout, final = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]), Path(sys.argv[5])
for row in Path(sys.argv[4]).read_text(encoding="utf-8").splitlines():
    kind, rel, tpath = row.split("\t")
    if kind in ("SKIP_DUP", "UNMAPPED"):
        continue
    if kind == "HOLD":  # relocated donor copy kept byte for byte, not compiled (module decision pending)
        src = mout / "hold" / tpath[len(RETAIN):]
        (final / tpath).parent.mkdir(parents=True, exist_ok=True)
        (final / tpath).write_text(src.read_text(encoding="utf-8"), encoding="utf-8", newline="")
        print(f"HOLD\t{tpath}")
        continue
    dst = final / tpath
    dst.parent.mkdir(parents=True, exist_ok=True)
    if kind.startswith("CONFLICT"):
        how = DECIDE[Path(tpath).name]
        if how == "UNION":
            merged = union((mout / "conflicts" / tpath).read_text(encoding="utf-8"))
            if tpath.endswith(".c"):
                merged = dedupe_c(merged)
            dst.write_text(merged, encoding="utf-8", newline="")
        else:
            dst.write_text((m3_root / tpath).read_text(encoding="utf-8"), encoding="utf-8", newline="")
            if how == "OURS_RETAIN":
                t = target(rel)
                r = final / (RETAIN + tpath)
                r.parent.mkdir(parents=True, exist_ok=True)
                r.write_text(set_package(relocate((dev / rel).read_text(encoding="utf-8")), t[1]), encoding="utf-8", newline="")
                print(f"RETAINED\t{RETAIN + tpath}")
        print(f"{how}\t{tpath}")
    else:
        src = mout / tpath
        dst.write_text(src.read_text(encoding="utf-8"), encoding="utf-8", newline="")
        print(f"{kind}\t{tpath}")

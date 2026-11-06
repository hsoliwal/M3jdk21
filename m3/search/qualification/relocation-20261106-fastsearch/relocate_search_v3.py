"""M3_SEARCH_RELOCATE v3 (text lane): leaf 4, top-K selection, reciprocal rank fusion and pair-sum search from
com.synexia synexia-fastsearch into m3/search (jdk.m3.search).

usage: python -I relocate_search_v3.py <donor-main-root> <donor-test-root> <native-dir> <out-root> <spec.tsv>
spec.tsv rows: kind<TAB>donor-relative-path (kind = main | test | native).
Rules:
  packages com.synexia.fastsearch.knowledge.index / com.synexia.fastsearch.problem -> jdk.m3.search
  com.synexia.common.progress.ProgressMonitor -> jdk.m3.collections.M3ProgressMonitor (same methods used:
    beginTask, subTask, worked, done, checkCanceled)
  every leaf type <T> -> M3<T> (file renamed); JNI Java_com_synexia_fastsearch_problem_<T>_ -> Java_jdk_m3_search_M3<T>_
A provenance line with the donor git blob follows the SPDX header. Prints one ledger row per file."""
import hashlib
import re
import sys
from pathlib import Path

PKGS = ("com.synexia.fastsearch.knowledge.index", "com.synexia.fastsearch.problem")


def blob_id(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def main() -> int:
    main_root, test_root, nat_root, out, spec = map(Path, sys.argv[1:6])
    rows = [r.split("\t") for r in spec.read_text().splitlines() if r.strip()]
    types = {Path(p).stem: "M3" + Path(p).stem for k, p in rows if k == "main"}
    for kind, rel in rows:
        src = {"main": main_root, "test": test_root, "native": nat_root}[kind] / rel
        raw = src.read_bytes()
        s = raw.decode("utf-8")
        if kind == "native":
            for a, b in types.items():
                s = s.replace(f"Java_com_synexia_fastsearch_problem_{a}_", f"Java_jdk_m3_search_{b}_")
                s = s.replace(f"Java_com_synexia_fastsearch_knowledge_index_{a}_", f"Java_jdk_m3_search_{b}_")
            dst = out / f"m3/search/src/main/native/{src.name}"
        else:
            s = s.replace("com.synexia.common.progress.ProgressMonitor", "jdk.m3.collections.M3ProgressMonitor")
            s = re.sub(r"(?<![\w$.])ProgressMonitor(?![\w$])", "M3ProgressMonitor", s)
            for p in PKGS:
                s = re.sub(rf"(?<![\w$]){re.escape(p)}(?![\w$])", "jdk.m3.search", s)
            for a, b in types.items():
                s = re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, s)
            s = re.sub(r"^import jdk\.m3\.search\.\w+;\n", "", s, flags=re.M)  # now the same package
            name = types.get(src.stem, src.stem) + ".java"
            dst = out / f"m3/search/src/{'main' if kind == 'main' else 'test'}/java/jdk/m3/search/{name}"
        lines = s.split("\n")
        at = next((i + 1 for i in range(len(lines) - 1, -1, -1) if lines[i].lstrip("/* ").startswith("SPDX-")), 0)
        lines.insert(at, f"// Relocated from com.synexia synexia-fastsearch/{rel} (blob {blob_id(raw)[:12]}) by M3_SEARCH_RELOCATE v3.")
        s = "\n".join(lines)
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(s.encode("utf-8"))
        print(f"{kind}\t{rel}\t{blob_id(raw)}\t{dst.relative_to(out).as_posix()}\t{hashlib.sha256(s.encode()).hexdigest()[:16]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

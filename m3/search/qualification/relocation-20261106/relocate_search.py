"""M3_SEARCH_RELOCATE v1 (text lane): relocate the pure-Java m3-fast-search kernels from com.synexia into the
M3jdk21 m3/search module.

usage: python -I relocate_search.py <leaf_main.txt> <leaf_test.txt> <src-root> <out-root> <rules.tsv>
Rules (rules.tsv, applied in order, word-bounded unless kind=literal):
  package  jdk.internal.m3.search -> jdk.m3.search (package lines and qualified names)
  import   jdk.internal.m3.job.IProgressMonitor -> jdk.m3.collections.M3ProgressMonitor
  call     IProgressMonitor.noop() -> new M3DefaultProgressMonitor() (+ import jdk.m3.collections.M3DefaultProgressMonitor)
  type     <Kernel> -> M3<Kernel> for every relocated main class (file renamed too)
Comments, strings and SPDX headers move with the code; a provenance line naming the donor blob is added under the
SPDX header. Prints one ledger row per file: source path, git blob of the source, output path, sha256 of output."""
import hashlib
import re
import subprocess
import sys
from pathlib import Path


def blob_id(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def main() -> int:
    mains, tests, src, out, rules_tsv = sys.argv[1:6]
    rules = [r.split("\t") for r in Path(rules_tsv).read_text(encoding="utf-8").splitlines()
             if r.strip() and not r.startswith("#")]
    files = [(p, "main") for p in Path(mains).read_text().split()] + [(p, "test") for p in Path(tests).read_text().split()]
    types = {Path(p).stem: "M3" + Path(p).stem for p, kind in files if kind == "main" and Path(p).stem != "package-info"}
    for p, kind in files:
        raw = (Path(src) / p).read_bytes()
        s = raw.decode("utf-8")
        for kind_r, a, b in rules:
            if kind_r == "literal":
                s = s.replace(a, b)
            else:
                s = re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, s)
        for a, b in types.items():
            s = re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, s)
        if "new M3DefaultProgressMonitor()" in s and "import jdk.m3.collections.M3DefaultProgressMonitor;" not in s:
            s = s.replace("import jdk.m3.collections.M3ProgressMonitor;",
                          "import jdk.m3.collections.M3DefaultProgressMonitor;\nimport jdk.m3.collections.M3ProgressMonitor;", 1)
        head = f"// Relocated from com.synexia m3-fast-search/{('src/main/java' if kind == 'main' else 'src/test/java')}/jdk/internal/m3/search/{Path(p).name} (blob {blob_id(raw)[:12]}) by M3_SEARCH_RELOCATE v1.\n"
        lines = s.split("\n")
        at = next((i + 1 for i in range(len(lines) - 1, -1, -1) if lines[i].startswith("// SPDX-")), 0)
        lines.insert(at, head.rstrip("\n"))
        s = "\n".join(lines)
        stem = Path(p).stem
        name = (types.get(stem, stem)) + ".java"
        dst = Path(out) / f"m3/search/src/{'main' if kind == 'main' else 'test'}/java/jdk/m3/search/{name}"
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(s.encode("utf-8"))
        print(f"{kind}\t{p}\t{blob_id(raw)}\t{dst.relative_to(out).as_posix()}\t{hashlib.sha256(s.encode()).hexdigest()[:16]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

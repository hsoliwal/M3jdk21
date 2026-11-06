"""M3_SEARCH_RELOCATE v2 (text lane): v1 plus the native leaf.

usage: python -I relocate_search_v2.py <main.txt> <test.txt> <native.txt> <known-types.txt> <src-root> <out-root> <rules.tsv>
- Java: same rules as v1; the type map now covers every donor class of the search family (known-types.txt, one
  donor simple name per line). A donor name already starting with M3 keeps its name.
- C (native.txt): JNI symbols Java_com_synexia_m3_search_<Donor>_ become Java_jdk_m3_search_<M3 name>_, and
  "com/synexia/m3/search/<Donor>" class-name strings become "jdk/m3/search/<M3 name>". Files land in
  m3/search/src/main/native/.
Prints one ledger row per file: kind, donor path, donor git blob, output path, output sha256 prefix."""
import hashlib
import re
import sys
from pathlib import Path


def blob_id(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def m3(name: str) -> str:
    return name if name.startswith("M3") else "M3" + name


def main() -> int:
    mains, tests, natives, known, src, out, rules_tsv = sys.argv[1:8]
    rules = [r.split("\t") for r in Path(rules_tsv).read_text(encoding="utf-8").splitlines()
             if r.strip() and not r.startswith("#")]
    types = {n: m3(n) for n in Path(known).read_text().split() if n != "package-info"}
    files = ([(p, "main") for p in Path(mains).read_text().split()] + [(p, "test") for p in Path(tests).read_text().split()]
             + [(p, "native") for p in Path(natives).read_text().split()])
    for p, kind in files:
        raw = (Path(src) / p).read_bytes()
        s = raw.decode("utf-8")
        stem = Path(p).stem
        if kind == "native":
            for a, b in types.items():
                s = s.replace(f"Java_com_synexia_m3_search_{a}_", f"Java_jdk_m3_search_{b}_")
                s = s.replace(f"com/synexia/m3/search/{a}", f"jdk/m3/search/{b}")
            dst = Path(out) / f"m3/search/src/main/native/{Path(p).name}"
            origin = f"m3-fast-search/src/main/native/{Path(p).name}"
            lead = "//"
        else:
            for kind_r, a, b in rules:
                s = s.replace(a, b) if kind_r == "literal" else re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, s)
            for a, b in types.items():
                if a != b:
                    s = re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, s)
            if "new M3DefaultProgressMonitor()" in s and "import jdk.m3.collections.M3DefaultProgressMonitor;" not in s:
                s = s.replace("import jdk.m3.collections.M3ProgressMonitor;",
                              "import jdk.m3.collections.M3DefaultProgressMonitor;\nimport jdk.m3.collections.M3ProgressMonitor;", 1)
            name = types.get(stem, stem) + ".java"
            dst = Path(out) / f"m3/search/src/{'main' if kind == 'main' else 'test'}/java/jdk/m3/search/{name}"
            origin = f"m3-fast-search/src/{'main' if kind == 'main' else 'test'}/java/com/synexia/m3/search/{Path(p).name}"
            lead = "//"
        lines = s.split("\n")
        at = next((i + 1 for i in range(len(lines) - 1, -1, -1) if lines[i].lstrip("/* ").startswith("SPDX-")), 0)
        lines.insert(at, f"{lead} Relocated from com.synexia {origin} (blob {blob_id(raw)[:12]}) by M3_SEARCH_RELOCATE v2.")
        s = "\n".join(lines)
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(s.encode("utf-8"))
        print(f"{kind}\t{p}\t{blob_id(raw)}\t{dst.relative_to(out).as_posix()}\t{hashlib.sha256(s.encode()).hexdigest()[:16]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

"""relocate_tests.py <donor-test-root> <out> [<text-tools-root>] : relocate m3-unix4j-m3 src/test with the
main-source map (merge_unix4j.relocate). A harness joins the package whose classes it exercises: structured tools ->
jdk.m3.structured, io/os -> jdk.m3.io, else jdk.m3.text. Explicit imports are added for every main class it names
from another package. Test-scope adaptations (each printed as ADAPT):
  - CoreSelfTest: the call into M3TextToolSelfTest is dropped (M3TextToolExecutor is HOLD in this receive).
  - NativeLineBatchOrderSelfTest, M3TextToolSelfTest: not received (donor-only ProgressState API / HOLD owner).
C files land under <out>/c or <out>/native with JNI names rewritten."""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from merge_unix4j import relocate, set_package  # noqa: E402

SKIP = {"NativeLineBatchOrderSelfTest": "donor-only com.synexia ProgressState API",
        "M3TextToolSelfTest": "exercises M3TextToolExecutor (HOLD)"}
src, out = Path(sys.argv[1]), Path(sys.argv[2])
tt = Path(sys.argv[3] if len(sys.argv) > 3 else "final/m3/tooling/module-packs/text-tools") / "src"
owners = {}
for mod in ("jdk.m3.text", "jdk.m3.io", "jdk.m3.structured"):
    for p in (tt / mod / mod.replace(".", "/")).glob("*.java"):
        owners[p.stem] = mod
for f in sorted(src.rglob("*")):
    if not f.is_file():
        continue
    rel = f.relative_to(src).as_posix()
    if f.suffix == ".java" and f.stem in SKIP:
        print(f"SKIP\t{rel}\t{SKIP[f.stem]}")
        continue
    text = relocate(f.read_text(encoding="utf-8"))
    if f.suffix != ".java":
        dst = out / ("c" if "/c/" in rel else "native") / f.name
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(text, encoding="utf-8", newline="")
        print(f"{rel}\t{dst.relative_to(out).as_posix()}")
        continue
    if f.stem == "CoreSelfTest":  # its suite calls cross into jdk.m3.structured; those harnesses run on their own
        for t in ("M3TextToolSelfTest", "StructuredTextSelfTest", "JsonLinesSelfTest", "StructuredCliSelfTest"):
            text = re.sub(rf"^\s*{t}\.main\(new String\[0\]\);\n", "", text, flags=re.M)
        print(f"ADAPT\t{rel}\tdropped 4 suite calls into jdk.m3.structured / HOLD harnesses (each runs as its own entry)")
    body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", text, flags=re.S)
    used = {n for n in owners if re.search(rf"(?<![\w.]){n}\b", body)}
    mods = {owners[n] for n in used}
    if re.search(r"\bDelimited(?:IndexContract|LegacyContract|AllocationProbe)\b", body):
        mods.add("jdk.m3.structured")  # harnesses of the structured contracts share their package
    pkg = "jdk.m3.structured" if "jdk.m3.structured" in mods else ("jdk.m3.io" if "jdk.m3.io" in mods else "jdk.m3.text")
    text = set_package(text, pkg)
    local = set(re.findall(r"\b(?:class|record|interface|enum)\s+(\w+)", body))
    imports = sorted(f"import {owners[n]}.{n};" for n in used - local if owners[n] != pkg
                     and f"import {owners[n]}.{n};" not in text)
    if imports:
        text = re.sub(r"^(package [\w.]+;\n)", lambda m: m.group(1) + "\n" + "\n".join(imports) + "\n", text, count=1, flags=re.M)
    dst = out / pkg.replace(".", "/") / f.name
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(text, encoding="utf-8", newline="")
    print(f"{rel}\t{dst.relative_to(out).as_posix()}")

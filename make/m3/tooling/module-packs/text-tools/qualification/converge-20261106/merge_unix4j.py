"""M3_TEXT_TOOLS_CONVERGE v1: three-way receive of com.synexia m3-unix4j-m3 (develop) into M3jdk21
m3/tooling/module-packs/text-tools, against the pinned receiving base (com.synexia 3b5c4be7630f, SOURCE_REF.json).

usage: python -I merge_unix4j.py <base-dir> <develop-dir> <m3-root> <out-dir>
R() relocates a donor file the way the existing receiver did:
  jdk.internal.m3.unix4jm3            -> jdk.m3.text   (StructuredTextTools, StructuredCli, JsonLinesTools -> jdk.m3.structured)
  jdk.internal.m3.unix4jm3.io / .os   -> jdk.m3.io
  jdk.internal.m3.unix4jm3.nativecore -> jdk.m3.text
  jdk.internal.m3.common.progress.ProgressMonitor -> java.util.concurrent.ProgressMonitor (the M3 java.base API M3File uses)
  JNI symbols Java_jdk_internal_m3_unix4jm3[_sub]_<C>_ and "jdk/internal/m3/unix4jm3/..." class names follow the same map.
Per file: ours = M3 current, base = R(base), theirs = R(develop):
  ADD (no ours, no base) | MERGED (git merge-file clean) | SAME | CONFLICT (written with markers to out/conflicts/)
  | M3_DROPPED (base and develop exist, M3 has no file at the target: the receiver renamed or removed it).
The donor's src/main/java/jdk/m3/io copies are skipped (byte-identical to jdk/internal/m3/unix4jm3/io)."""
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

STRUCT = {"StructuredTextTools", "StructuredCli", "JsonLinesTools"}
HOLD = {"M3TextToolExecutor"}  # needs jdk.m3.text package-private members and jdk.m3.structured: module decision pending
JNI_BLOCK = re.compile(r"^JNIEXPORT [^\n]*?\b(Java_jdk_internal_m3_unix4jm3_\w+)\s*\n.*?^\}\n", re.S | re.M)
PREFIX = "m3/tooling/module-packs/text-tools/"


def target(rel: str):
    p = Path(rel)
    if rel.startswith("src/main/c/"):
        return PREFIX + "native/" + p.name
    m = re.match(r"src/main/java/jdk/internal/m3/unix4jm3/(?:(io|os|nativecore)/)?([A-Za-z0-9_]+)\.java$", rel)
    if not m:
        return None
    sub, name = m.groups()
    pkg = "jdk.m3.io" if sub in ("io", "os") else ("jdk.m3.structured" if name in STRUCT and not sub else "jdk.m3.text")
    return PREFIX + f"src/{pkg}/{pkg.replace('.', '/')}/{name}.java", pkg


def pkg_of(name, sub):
    if sub in ("io", "os"):
        return "jdk.m3.io"
    if not sub and name in STRUCT:
        return "jdk.m3.structured"
    return "jdk.m3.text"


def relocate(text: str) -> str:
    if "jdk.internal.m3.common.progress.ProgressMonitor" in text:  # M3 java.base spelling of the same contract
        text = text.replace(".checkCanceled()", ".checkCancelled()").replace(".isCanceled()", ".isCancelled()")
    text = text.replace("jdk.internal.m3.common.progress.ProgressMonitor", "java.util.concurrent.ProgressMonitor")
    # The donor C exports every JNI entry twice (com.synexia and jdk.m3 names); M3 keeps only its own name.
    twins = set(re.findall(r"\bJava_jdk_m3_(?:text|io|structured)_(\w+)", text))

    def strip_twin(m):
        suffix = re.sub(r"^Java_jdk_internal_m3_unix4jm3_(?:(?:io|os|nativecore)_)?", "", m.group(1))
        return "" if suffix in twins else m.group(0)
    text = JNI_BLOCK.sub(strip_twin, text)
    text = re.sub(r"\bcom_m3_unix4jm3_(?:(io|os|nativecore)_)?([A-Z][A-Za-z0-9]*)\.h\b",
                  lambda m: pkg_of(m.group(2), m.group(1)).replace(".", "_") + "_" + m.group(2) + ".h", text)
    text = text.replace("public final class NativeProcessPipeline {\n",
                        "public final class NativeProcessPipeline {\n  public NativeProcessPipeline() { }\n")

    def jni(m):
        sub, cls = m.group(1), m.group(2)
        return "Java_" + pkg_of(cls, sub).replace(".", "_") + "_" + cls + "_"
    text = re.sub(r"Java_jdk_internal_m3_unix4jm3_(?:(io|os|nativecore)_)?([A-Z][A-Za-z0-9]*)_", jni, text)

    def slash(m):
        sub, cls = m.group(1), m.group(2)
        return pkg_of(cls, sub).replace(".", "/") + "/" + cls
    text = re.sub(r"jdk/internal/m3/unix4jm3/(?:(io|os|nativecore)/)?([A-Z][A-Za-z0-9]*)", slash, text)

    def dotted(m):
        sub, cls = m.group(1), m.group(2)
        return pkg_of(cls, sub) + "." + cls
    text = re.sub(r"com\.synexia\.unix4jm3\.(?:(io|os|nativecore)\.)?([A-Z][A-Za-z0-9]*)", dotted, text)
    text = re.sub(r"com\.synexia\.unix4jm3\.(io|os)\b", "jdk.m3.io", text)
    text = re.sub(r"com\.synexia\.unix4jm3(\.nativecore)?\b", "jdk.m3.text", text)
    return text


def set_package(text: str, pkg) -> str:
    return re.sub(r"^package [\w.]+;", f"package {pkg};", text, count=1, flags=re.M) if pkg else text


def main() -> int:
    base_dir, dev_dir, m3_root, out = map(Path, sys.argv[1:5])
    rows = []
    for f in sorted(dev_dir.rglob("*")):
        if not f.is_file():
            continue
        rel = f.relative_to(dev_dir).as_posix()
        if rel.startswith("src/main/java/jdk/m3/io/"):
            rows.append(("SKIP_DUP", rel, ""))
            continue
        t = target(rel)
        if t is None:
            rows.append(("UNMAPPED", rel, ""))
            continue
        tpath, pkg = (t if isinstance(t, tuple) else (t, None))
        theirs = set_package(relocate(f.read_text(encoding="utf-8")), pkg)
        bfile = base_dir / rel
        base = set_package(relocate(bfile.read_text(encoding="utf-8")), pkg) if bfile.exists() else None
        ofile = m3_root / tpath
        ours = ofile.read_text(encoding="utf-8") if ofile.exists() else None
        dst = out / tpath
        if Path(tpath).stem in HOLD:
            rows.append(("HOLD", rel, "m3/qualification/master-landing-20261028/retained-variants/pr2498/" + tpath))
            hold = out / "hold" / tpath
            hold.parent.mkdir(parents=True, exist_ok=True)
            hold.write_text(theirs, encoding="utf-8", newline="")
            continue
        if ours is None and base is None:
            kind, result = "ADD", theirs
        elif ours is None:
            kind, result = "M3_DROPPED", theirs
            dst = out / "m3-dropped" / tpath
        elif ours == theirs:
            kind, result = "SAME", ours
        else:
            with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
                fo, fa, ft = (Path(d) / n for n in ("ours", "base", "theirs"))
                fo.write_text(ours, encoding="utf-8", newline="")
                fa.write_text(base if base is not None else "", encoding="utf-8", newline="")
                ft.write_text(theirs, encoding="utf-8", newline="")
                r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(fo), str(fa), str(ft)], capture_output=True)
            result = r.stdout.decode("utf-8")
            kind = "MERGED" if r.returncode == 0 else f"CONFLICT={r.returncode}"
            if r.returncode != 0:
                dst = out / "conflicts" / tpath
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(result, encoding="utf-8", newline="")
        rows.append((kind, rel, tpath))
    for k, rel, tp in rows:
        print(f"{k}\t{rel}\t{tp}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

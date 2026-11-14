"""relocate_cp.py <donor-type-dir> <out-dir> : INDEXSTRING_RELOCATE_CLASSPOOL v1 (census2 P2-099).
Relocates the synexia-indexstring class-metadata pool family (MIndexClassPool + MIndexClass, MIndexClassKind,
MIndexClassMember, MIndexClassMemberSpec, MIndexClassSnapshot, MIndexClassSpec, MIndexMemberKind) into
m3/ports/indexstring, package jdk.internal.m3.indexstring.type. Text rules:
  1. package com.synexia.indexstring.type -> jdk.internal.m3.indexstring.type (JDK naming rule PKG).
  2. The donor text pool MatIndexStringPool (an MIndexString token pool) -> the existing M3JDK owner
     jdk.m3.intern.M3InternSpace: internText(languageId, s).handle() -> Math.toIntExact(textSpace.internString(s))
     (atom ids are dense 1..2^24); value(h).materialize()/toTuple() -> textSpace.string(h); the IndexResolver
     constructor -> MIndexClassPool(int languageId) over a fresh space; retainedTextTokenCount (donor token count)
     -> retainedTextAtomCount (distinct interned strings).
  3. Name accessors returning MIndexString return String (the donor value type has no M3JDK owner).
textFeature keeps its hash: the donor pool's javaHash is String.hashCode over the same UTF-16 units.
Refuses when a rule site is missing or repeated, or when a donor-only type survives outside comments."""
import re
import sys
from pathlib import Path

src, out = Path(sys.argv[1]), Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
FILES = ["MIndexClassPool", "MIndexClass", "MIndexClassKind", "MIndexClassMember", "MIndexClassMemberSpec",
         "MIndexClassSnapshot", "MIndexClassSpec", "MIndexMemberKind"]
DONOR = re.compile(r"\b(MatIndexStringPool|MatIndexString|IndexResolver|SubMIndexString)\b|\bMIndexString\b(?!/)")


def sub(text, old, new, name, count=1):
    n = text.count(old)
    if n != count:
        sys.exit(f"REFUSE {name}: site {old[:60]!r} found {n}x, expected {count}")
    return text.replace(old, new)


def code_only(text):
    return re.sub(r"//[^\n]*|/\*.*?\*/", " ", text, flags=re.S)


for name in FILES:
    t = (src / f"{name}.java").read_text(encoding="utf-8").replace("\r\n", "\n")
    t = sub(t, "package com.synexia.indexstring.type;", "package jdk.internal.m3.indexstring.type;", name)
    if name in ("MIndexClass", "MIndexClassMember"):
        t = sub(t, "import com.synexia.indexstring.MIndexString;\n", "", name)
        t = re.sub(r"\bpublic MIndexString (\w+\()", r"public String \1", t)
        # A String names itself; the donor called materialize() on its MIndexString accessor.
        t = t.replace("binaryName().materialize()", "binaryName()")
    if name == "MIndexClassPool":
        for imp in ("IndexResolver", "MIndexString", "MatIndexString", "MatIndexStringPool"):
            t = sub(t, f"import com.synexia.indexstring.{imp};\n", "", name)
        t = sub(t, "import java.util.Objects;\n", "import java.util.Objects;\nimport jdk.m3.intern.M3InternSpace;\n", name)
        t = sub(t, "  private final MatIndexStringPool textPool;\n", "  private final M3InternSpace textSpace;\n", name)
        t = sub(t, "  public MIndexClassPool(IndexResolver resolver, int languageId) {\n"
                   "    this(new MatIndexStringPool(Objects.requireNonNull(resolver, \"resolver\")), languageId);\n  }\n",
                "  /** A pool over its own fresh text space. */\n  public MIndexClassPool(int languageId) {\n"
                "    this(new M3InternSpace(), languageId);\n  }\n", name)
        t = sub(t, "  public MIndexClassPool(MatIndexStringPool textPool, int languageId) {\n"
                   "    this.textPool = Objects.requireNonNull(textPool, \"textPool\");\n",
                "  /** A pool over a caller-owned text space; text handles are atom ids of that space. */\n"
                "  public MIndexClassPool(M3InternSpace textSpace, int languageId) {\n"
                "    this.textSpace = Objects.requireNonNull(textSpace, \"textSpace\");\n", name)
        t = sub(t, "  public MatIndexStringPool textPool() { return textPool; }\n",
                "  public M3InternSpace textSpace() { return textSpace; }\n", name)
        t = sub(t, "  public long retainedTextTokenCount() { return textPool.retainedTokenCount(); }\n",
                "  /** Distinct interned strings of this pool's text space. */\n"
                "  public long retainedTextAtomCount() { return textSpace.atoms().size(); }\n", name)
        n = len(re.findall(r"textPool\.internText\(languageId, ([^)]*\(\))\)\.handle\(\)", t))
        if n != 5:
            sys.exit(f"REFUSE {name}: {n} internText sites, expected 5")
        t = re.sub(r"textPool\.internText\(languageId, ([^)]*\(\))\)\.handle\(\)", r"textHandle(\1)", t)
        t = sub(t, "  private MIndexString text(int textHandle) {\n"
                   "    return new MIndexString(textPool.value(textHandle).toTuple());\n  }\n",
                "  private String text(int textHandle) {\n    return textSpace.string(textHandle);\n  }\n\n"
                "  /** Atom ids of an M3InternSpace are dense (1..2^24), so they fit the int handle lanes. */\n"
                "  private int textHandle(String value) {\n    return Math.toIntExact(textSpace.internString(value));\n  }\n", name)
        t = sub(t, "    return textPool.value(textHandle).materialize();\n", "    return textSpace.string(textHandle);\n", name)
        t = sub(t, "    return textPool.value(textHandle).materialize().equals(value);\n",
                "    return textSpace.string(textHandle).equals(value);\n", name)
        t = sub(t, "    MatIndexString value = textPool.value(textHandle);\n", "    String value = textSpace.string(textHandle);\n", name)
        t = re.sub(r"\n  MIndexString (\w+\()", r"\n  String \1", t)
        t = re.sub(r"\n  MIndexString memberDescriptor\(", r"\n  String memberDescriptor(", t)
    left = sorted(set(m.group(0) for m in DONOR.finditer(code_only(t))))
    if left or "textPool" in code_only(t):
        sys.exit(f"REFUSE {name}: donor-only names survive in code: {left} textPool={'textPool' in code_only(t)}")
    (out / f"{name}.java").write_bytes(t.encode("utf-8"))
    print(f"{name}\t{len(t.encode())} bytes")

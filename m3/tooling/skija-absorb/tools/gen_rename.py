# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""gen_rename.py <view_dir> <types_classes.txt> <owner_pkg> : from skija_patterns.py's OWNER_MAP.tsv, JNI_BINDINGS.tsv,
CC_CLASSREFS.tsv and JAVA_CLASSREFS.tsv, emit the mechanical rename package of the Skija absorption (CPU only,
deterministic):

  TYPE_RENAME.tsv          old FQN -> new FQN (Skija types and io.github.humbleui.types)
  JNI_RENAME.tsv           old JNI symbol -> new JNI symbol (short and long names; same mangling as the JVM)
  CC_STRING_RENAME.tsv     the string ledger, old -> new: the Java string literals that name a donor class for a
                           run-time lookup (rows whose file is a .java source; absorb_rename.py applies them), then the
                           C++ class-name strings (FindClass / descriptors; leaf 2)
  M3SkijaToGraphics.yml    OpenRewrite declarative recipe: one org.openrewrite.java.ChangeType per type, for the
                           absorbed sources and every downstream consumer (SWT Render M3, GPU M3)
Nested types follow their owner; the recipe renames the top-level type and javac resolves the members."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))


def jni_mangle(s):
    out = []
    for ch in s:
        out.append("_1" if ch == "_" else ch if ch.isalnum() else "_" if ch in "./" else "_0%04x" % ord(ch))
    return "".join(out)


def rows(p):
    with open(p, encoding="utf-8") as f:
        next(f)
        return [l.rstrip("\n").split("\t") for l in f if l.strip()]


CLASS_FILE_SUFFIX = ".class"
BINARY_REF = re.compile(r"io([./])github\1humbleui(?:\1[\w$]+)+")


def java_string_rows(view, tmap):
    """String-ledger rows of JAVA_CLASSREFS.tsv. A class-file resource "T.class" resolves in the declaring type's
    package (Class.getResource), so it maps to the renamed simple name only while the rename keeps T and the declaring
    type in one package; a binary name, dotted or slashed, maps every type reference in it (a nested type follows its
    owner). Anything else is UNMAPPED, which absorb_rename.py refuses."""
    path = os.path.join(view, "JAVA_CLASSREFS.tsv")
    out = []
    for f, line, jtype, s in (rows(path) if os.path.exists(path) else []):
        new = "UNMAPPED"
        stem = s[:-len(CLASS_FILE_SUFFIX)] if s.endswith(CLASS_FILE_SUFFIX) else None
        if stem and "/" not in stem and "." not in stem:
            target = tmap.get(jtype.rsplit(".", 1)[0] + "." + stem)
            if target and jtype in tmap and target.rsplit(".", 1)[0] == tmap[jtype].rsplit(".", 1)[0]:
                new = target.rsplit(".", 1)[1] + CLASS_FILE_SUFFIX
        else:
            refs = {m.group(0): m.group(1) for m in BINARY_REF.finditer(s)}
            mapped = {r: tmap.get(r.partition("$")[0].replace(sep, ".")) for r, sep in refs.items()}
            if mapped and all(mapped.values()):
                new = BINARY_REF.sub(lambda m: mapped[m.group(0)].replace(".", m.group(1))
                                     + "".join(m.group(0).partition("$")[1:]), s)
        out.append((f, line, s, new))
    return out


def main():
    view, types_list, owner = sys.argv[1], sys.argv[2], sys.argv[3]
    tmap = {r[0]: r[1] for r in rows(os.path.join(view, "OWNER_MAP.tsv"))}
    for t in open(types_list, encoding="utf-8").read().split():
        if t.startswith("io.github.humbleui.types."):
            n = t.rsplit(".", 1)[1]
            tmap[t] = owner + ".M3" + n
    w = lambda n, head, rs: open(os.path.join(view, n), "w", encoding="utf-8", newline="\n").write(
        "\t".join(head) + "\n" + "".join("\t".join(r) + "\n" for r in rs))
    w("TYPE_RENAME.tsv", ("old_fqn", "new_fqn"), sorted(tmap.items()))
    jni = []
    for jt, meth, sym, cc, exp in rows(os.path.join(view, "JNI_BINDINGS.tsv")):
        if cc == "-":
            continue
        old_short = "Java_" + jni_mangle(jt.rsplit(".", 1)[0]) + "_" + jni_mangle(jt.rsplit(".", 1)[1]) + "_" + jni_mangle(meth)
        nt = tmap[jt]
        new_short = "Java_" + jni_mangle(nt.rsplit(".", 1)[0]) + "_" + jni_mangle(nt.rsplit(".", 1)[1]) + "_" + jni_mangle(meth)
        if not sym.startswith(old_short):
            raise SystemExit(f"binding {sym} does not start with {old_short}")
        jni.append((sym, new_short + sym[len(old_short):], cc))
    w("JNI_RENAME.tsv", ("old_symbol", "new_symbol", "cc_file"), sorted(set(jni)))
    slash = {k.replace(".", "/"): v.replace(".", "/") for k, v in tmap.items()}
    cc = java_string_rows(view, tmap)
    for f, line, s in rows(os.path.join(view, "CC_CLASSREFS.tsv")):
        core = s.lstrip("L").rstrip(";")
        top, _, nested = core.partition("$")
        new = slash.get(top)
        cc.append((f, line, s, (("L" if s.startswith("L") else "") + new + ("$" + nested if nested else "") + (";" if s.endswith(";") else "")) if new else "UNMAPPED"))
    w("CC_STRING_RENAME.tsv", ("cc_file", "line", "old", "new"), cc)
    with open(os.path.join(view, "M3SkijaToGraphics.yml"), "w", encoding="utf-8", newline="\n") as y:
        y.write("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n# Generated by gen_rename.py from OWNER_MAP.tsv; do not edit by hand.\n")
        y.write("---\ntype: specs.openrewrite.org/v1beta/recipe\nname: com.synexia.m3.graphics.M3SkijaToGraphics\n")
        y.write("displayName: Skija and HumbleUI types to the M3 graphics owner\n")
        y.write("description: One ChangeType per absorbed type (TYPE_RENAME.tsv). Applies to the absorbed sources and to every consumer.\n")
        y.write("recipeList:\n")
        for old, new in sorted(tmap.items()):
            y.write(f"  - org.openrewrite.java.ChangeType:\n      oldFullyQualifiedTypeName: {old}\n      newFullyQualifiedTypeName: {new}\n      ignoreDefinition: false\n")
    java = sum(1 for r in cc if r[0].endswith(".java"))
    print(f"types={len(tmap)} jni={len(jni)} cc_strings={len(cc) - java} java_strings={java} "
          f"unmapped_cc={sum(1 for r in cc if r[3] == 'UNMAPPED')}")


main()

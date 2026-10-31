# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""skija_patterns.py <skija_root> <dll_exports.txt> <out_dir> [<owner_pkg> <owner_internal_pkg>]

CPU-only, read-only projection of a Skija source tree (shared/java* + platform/cc) for its absorption into M3jdk. The
Java source of every class is the one mr_select.py selects (MR_SELECT v1: the highest Multi-Release version <= 21 of
shared/java and shared/java<K>, the class a Java 21 runtime loads from the published jar):

  CLASSES.tsv      one row per top-level Java type: package, type, kind, supertype, pattern (SKIJA-PATTERN v1, closed
                   set; anything unmatched is RESIDUE, the typed @IAntiPattern row), native/public method counts, lines
  JNI_BINDINGS.tsv one row per Java native method: owner type, method, JNI symbol (JNI 1.1 mangling), the C++ file that
                   defines it, whether the shipped dll exports it
  CC_CLASSREFS.tsv one row per "io/github/humbleui/..." class-name string in the C++ glue (FindClass / signatures)
  JAVA_CLASSREFS.tsv one row per Java string literal that names a donor class for a run-time lookup: a class-file
                   resource "T.class" of a donor type T of the declaring package (Class.getResource), or a binary name
                   (dotted or slashed); text that only mentions a type (messages, toString) is not a lookup
  OWNER_MAP.tsv    Skija type -> M3 owner type (package io.github.humbleui.skija[.x] -> <owner_pkg>[.x], impl ->
                   <owner_internal_pkg>, type T -> M3T); the parameter of the rename recipes
  SUMMARY.tsv      counts per pattern and per binding state

Recognition is lexical (regex over comment- and string-masked text); it proposes, it never decides equivalence."""
import os
import re
import sys
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mr_select import select_sources  # noqa: E402  (MR_SELECT v1, the sibling tool)

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', re.S)
PKG = re.compile(r'^\s*package\s+([\w.]+)\s*;', re.M)
TYPE = re.compile(r'^(?:public\s+|protected\s+|private\s+|abstract\s+|final\s+|sealed\s+|non-sealed\s+|static\s+|strictfp\s+)*'
                  r'(class|interface|enum|record|@interface)\s+(\w+)(?:\s*<[^{]*?>)?([^{]*)\{', re.M)
EXTENDS = re.compile(r'\bextends\s+([\w.]+)')
NATIVE = re.compile(r'\bnative\s+[\w.<>\[\], ?]+\s+(\w+)\s*\(')
PUBLIC_METHOD = re.compile(r'^\s*public\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+|default\s+)*[\w.<>\[\], ?]+\s+(\w+)\s*\(', re.M)
JNI_DEF = re.compile(r'\b(Java_io_github_humbleui_\w+)\s*\(')
CC_REF = re.compile(r'"(L?io/github/humbleui/[\w/$]+;?)"')
INFRA = {"Managed", "RefCnt", "Native", "Library", "Cleanable", "Stats", "Platform", "BufferUtil", "ResultCodec"}


def mask(text):
    return MASK.sub(lambda m: m.group(0)[0] + " " * (len(m.group(0)) - 2) + m.group(0)[-1] if len(m.group(0)) > 1 else " ", text)


def jni_mangle(s):
    out = []
    for ch in s:
        if ch == "_":
            out.append("_1")
        elif ch.isalnum():
            out.append(ch)
        elif ch in "./":
            out.append("_")
        else:
            out.append("_0%04x" % ord(ch))
    return "".join(out)


LOMBOK_VALUE = re.compile(r'@(?:lombok\.)?(?:Data|Value|AllArgsConstructor|Getter|EqualsAndHashCode|With|ToString|RequiredArgsConstructor)\b')
IMPLEMENTS = re.compile(r'\bimplements\s+([\w.<>, ]+)')
INSTANCE_FIELD = re.compile(r'^\s*(?:public\s+|protected\s+|private\s+)?(?:final\s+)?(?!static\b)(?!return\b)[\w.<>\[\]]+\s+_?\w+\s*(?:=[^;]*)?;', re.M)
JAVA_LEXEME = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
CLASS_FILE_LITERAL = re.compile(r'^([A-Za-z_$][\w$]*)\.class$')
BINARY_NAME_LITERAL = re.compile(r'io[./]github[./]humbleui(?:[./][a-z_$][\w$]*)*[./][A-Z][\w$]*')


def pattern(kind, sup, natives, name, pkg, head="", tail="", body=""):
    if kind in ("interface", "@interface"):
        return "INTERFACE"
    if kind == "enum":
        return "ENUM"
    if kind == "record":
        return "VALUE_RECORD"
    if pkg.endswith(".impl") and name in INFRA:
        return "IMPL_INFRA"
    if sup.endswith("RefCnt"):
        return "MANAGED_REFCNT"
    if sup.endswith("Managed"):
        return "MANAGED_OWNED"
    if sup.endswith("Native"):
        return "NATIVE_HANDLE"
    if natives:
        return "NATIVE_STATIC_FACADE"
    if LOMBOK_VALUE.search(head):
        return "VALUE_DATA"
    if sup or IMPLEMENTS.search(tail):
        return "SUBTYPE_PURE"
    pubs = PUBLIC_METHOD.findall(body)
    if pubs and len(re.findall(r'^\s*public\s+static\b', body, re.M)) >= len(pubs) and not INSTANCE_FIELD.search(body):
        return "STATIC_UTIL"
    if INSTANCE_FIELD.search(body):
        return "VALUE_DATA"
    return "RESIDUE"


def java_rows(root):
    """(path from root, text) of the MR_SELECT v1 source of every class, in the walk order of shared/java; a class that
    has only versioned sources follows, sorted. Module descriptors are not classes."""
    _, selected, _ = select_sources(root)
    base = os.path.join(root, "shared", "java")
    seen = set()
    for dirpath, dirs, files in os.walk(base):
        dirs.sort()
        for f in sorted(files):
            if f.endswith(".java") and f != "module-info.java":
                rel = os.path.relpath(os.path.join(dirpath, f), base).replace(os.sep, "/")
                seen.add(rel)
                yield source_row(root, selected[rel][1])
    for rel in sorted(set(selected) - seen):
        yield source_row(root, selected[rel][1])


def source_row(root, p):
    return os.path.relpath(p, root).replace(os.sep, "/"), open(p, encoding="utf-8", errors="replace").read()


def java_classrefs(sources):
    """JAVA_CLASSREFS rows (path, line, declaring type, literal) of the string literals that name a donor class for a
    run-time lookup: "T.class" with T a donor type of the declaring package, or a binary name with a type segment."""
    names = {}
    for _, pkg, name, _ in sources:
        names.setdefault(pkg, set()).add(name)
    refs = []
    for rel, pkg, name, text in sources:
        for m in JAVA_LEXEME.finditer(text):
            body = m.group(0)[1:-1] if m.group(0).startswith('"') else None
            if body is None:
                continue
            cf = CLASS_FILE_LITERAL.match(body)
            if (cf and cf.group(1) in names.get(pkg, ())) or BINARY_NAME_LITERAL.search(body):
                refs.append((rel, text.count("\n", 0, m.start()) + 1, pkg + "." + name, body))
    return refs


def main():
    root, exports_path, out = sys.argv[1], sys.argv[2], sys.argv[3]
    owner = sys.argv[4] if len(sys.argv) > 4 else "jdk.m3.graphics"
    owner_int = sys.argv[5] if len(sys.argv) > 5 else "jdk.internal.m3.graphics"
    os.makedirs(out, exist_ok=True)
    exports = {l.strip() for l in open(exports_path, encoding="utf-8", errors="replace") if l.strip()}
    cc_defs, cc_refs = {}, []
    for dirpath, _, files in os.walk(os.path.join(root, "platform", "cc")):
        for f in sorted(files):
            if f.endswith((".cc", ".cpp", ".hh", ".h")):
                p = os.path.join(dirpath, f); rel = os.path.relpath(p, root).replace(os.sep, "/")
                text = open(p, encoding="utf-8", errors="replace").read()
                for m in JNI_DEF.finditer(text):
                    cc_defs.setdefault(m.group(1), rel)
                for m in CC_REF.finditer(text):
                    cc_refs.append((rel, text.count("\n", 0, m.start()) + 1, m.group(1)))
    classes, bindings, owners, sources = [], [], [], []
    for rel, text in java_rows(root):
        mt = mask(text)
        pm = PKG.search(mt)
        pkg = pm.group(1) if pm else ""
        tm = TYPE.search(mt)
        if not tm:
            classes.append((rel, pkg, "-", "-", "-", "RESIDUE", 0, 0, text.count("\n") + 1)); continue
        kind, name, tail = tm.group(1), tm.group(2), tm.group(3)
        em = EXTENDS.search(tail) if kind != "interface" else None
        sup = em.group(1) if em else ""
        nat = NATIVE.findall(mt)
        pub = PUBLIC_METHOD.findall(mt)
        pat = pattern(kind, sup, nat, name, pkg, mt[:tm.start()], tail, mt[tm.end():])
        classes.append((rel, pkg, name, kind, sup or "-", pat, len(nat), len(pub), text.count("\n") + 1))
        sources.append((rel, pkg, name, text))
        for meth in nat:
            sym = "Java_" + jni_mangle(pkg) + "_" + jni_mangle(name) + "_" + jni_mangle(meth)
            if sym not in cc_defs and nat.count(meth) > 1:
                # overloaded natives bind by the JNI long name (short name + "__" + mangled argument descriptor)
                longs = sorted(s for s in cc_defs if s.startswith(sym + "__"))
                for ln in longs[:nat.count(meth)]:
                    bindings.append((pkg + "." + name, meth, ln, cc_defs[ln], "yes" if ln in exports else "no"))
                if longs:
                    continue
            bindings.append((pkg + "." + name, meth, sym, cc_defs.get(sym, "-"), "yes" if sym in exports else "no"))
        sub = pkg[len("io.github.humbleui.skija"):] if pkg.startswith("io.github.humbleui.skija") else "." + pkg
        target_pkg = (owner_int + sub[len(".impl"):]) if sub.startswith(".impl") else owner + sub
        target = name if name.startswith("M3") else "M3" + name
        owners.append((pkg + "." + name, target_pkg + "." + target, pat))
    w = lambda n, head, rows: open(os.path.join(out, n), "w", encoding="utf-8", newline="\n").write(
        "\t".join(head) + "\n" + "".join("\t".join(map(str, r)) + "\n" for r in rows))
    w("CLASSES.tsv", ("path", "package", "type", "kind", "supertype", "pattern", "natives", "public_methods", "lines"), classes)
    w("JNI_BINDINGS.tsv", ("java_type", "method", "jni_symbol", "cc_file", "dll_export"), bindings)
    w("CC_CLASSREFS.tsv", ("cc_file", "line", "class_string"), cc_refs)
    w("JAVA_CLASSREFS.tsv", ("java_file", "line", "java_type", "class_string"), java_classrefs(sources))
    w("OWNER_MAP.tsv", ("skija_type", "m3_type", "pattern"), sorted(owners))
    pc = Counter(r[5] for r in classes)
    bc = Counter(("defined" if b[3] != "-" else "undefined") + "/" + ("exported" if b[4] == "yes" else "not_exported") for b in bindings)
    orphan = len([s for s in exports if s.startswith("Java_io_github_humbleui") and s not in {b[2] for b in bindings}])
    rows = [("pattern", k, v) for k, v in sorted(pc.items())] + [("binding", k, v) for k, v in sorted(bc.items())]
    rows += [("dll_exports_without_java_native", "-", orphan), ("cc_class_strings", "-", len(cc_refs)),
             ("cc_jni_definitions", "-", len(cc_defs))]
    w("SUMMARY.tsv", ("dimension", "key", "count"), rows)


main()

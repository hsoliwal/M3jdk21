# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""skija_patterns.py <skija_root> <dll_exports.txt> <out_dir> [<owner_pkg> <owner_internal_pkg>]

CPU-only, read-only projection of a Skija source tree (shared/java + platform/cc) for its absorption into M3jdk:

  CLASSES.tsv      one row per top-level Java type: package, type, kind, supertype, pattern (SKIJA-PATTERN v1, closed
                   set; anything unmatched is RESIDUE, the typed @IAntiPattern row), native/public method counts, lines
  JNI_BINDINGS.tsv one row per Java native method: owner type, method, JNI symbol (JNI 1.1 mangling), the C++ file that
                   defines it, whether the shipped dll exports it
  CC_CLASSREFS.tsv one row per "io/github/humbleui/..." class-name string in the C++ glue (FindClass / signatures)
  OWNER_MAP.tsv    Skija type -> M3 owner type (package io.github.humbleui.skija[.x] -> <owner_pkg>[.x], impl ->
                   <owner_internal_pkg>, type T -> M3T); the parameter of the rename recipes
  SUMMARY.tsv      counts per pattern and per binding state

Recognition is lexical (regex over comment- and string-masked text); it proposes, it never decides equivalence."""
import os
import re
import sys
from collections import Counter

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', re.S)
PKG = re.compile(r'^\s*package\s+([\w.]+)\s*;', re.M)
TYPE = re.compile(r'^(?:public\s+|protected\s+|private\s+|abstract\s+|final\s+|sealed\s+|non-sealed\s+|static\s+|strictfp\s+)*'
                  r'(class|interface|enum|record|@interface)\s+(\w+)(?:\s*<[^{]*?>)?([^{]*)\{', re.M)
EXTENDS = re.compile(r'\bextends\s+([\w.]+)')
NATIVE = re.compile(r'\bnative\s+[\w.<>\[\], ?]+\s+(\w+)\s*\(')
PUBLIC_METHOD = re.compile(r'^\s*public\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+|default\s+)*[\w.<>\[\], ?]+\s+(\w+)\s*\(', re.M)
JNI_DEF = re.compile(r'\b(Java_io_github_humbleui_\w+)\s*\(')
CC_REF = re.compile(r'"((?:\\.|[^"\\\n])*io/github/humbleui/(?:\\.|[^"\\\n])*)"')
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
    base = os.path.join(root, "shared", "java")
    for dirpath, _, files in os.walk(base):
        for f in sorted(files):
            if f.endswith(".java"):
                p = os.path.join(dirpath, f)
                yield os.path.relpath(p, root).replace(os.sep, "/"), open(p, encoding="utf-8", errors="replace").read()


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
    classes, bindings, owners = [], [], []
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
    w("OWNER_MAP.tsv", ("skija_type", "m3_type", "pattern"), sorted(owners))
    pc = Counter(r[5] for r in classes)
    bc = Counter(("defined" if b[3] != "-" else "undefined") + "/" + ("exported" if b[4] == "yes" else "not_exported") for b in bindings)
    orphan = len([s for s in exports if s.startswith("Java_io_github_humbleui") and s not in {b[2] for b in bindings}])
    rows = [("pattern", k, v) for k, v in sorted(pc.items())] + [("binding", k, v) for k, v in sorted(bc.items())]
    rows += [("dll_exports_without_java_native", "-", orphan), ("cc_class_strings", "-", len(cc_refs)),
             ("cc_jni_definitions", "-", len(cc_defs))]
    w("SUMMARY.tsv", ("dimension", "key", "count"), rows)


main()

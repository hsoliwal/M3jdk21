# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""lombok_patterns.py <stage_dir> <lane_dir> <java> <lombok.jar> <openrewrite_lombok.config> <USAGE_CENSUS.tsv>
                     [CLASS_COMBINATIONS.tsv] :
LOMBOK-PATTERN v1 (CPU). Writes one representative source per census annotation group (CASES below; plus the
accessors chain/prefix cases SPEC.md covers, and C68-C86: equality rank, accessor prefix order and one case per
class-level annotation combination of CLASS_COMBINATIONS.tsv), runs `lombok delombok` on them under each config
profile, and runs PatternDiff.java (javac parser, one AST) to turn every expansion into generated-member rows.
  profiles  default      config.stopBubbling only (com.synexia has no lombok.config)
            openrewrite  the M3JDK UPSTREAM_LOMBOK.config verbatim (the 13 vendored rewrite modules)
            accessors-chain-config / accessors-prefix-config  one case each (lombok.accessors.chain / .prefix)
Output <lane_dir>/PATTERNS.tsv: rows identical under default and openrewrite merge into profile "*"; delombok
diagnostics become WARNING rows. Gate: every ANNOTATION group of USAGE_CENSUS.tsv is covered by a case and, when
CLASS_COMBINATIONS.tsv is given, every (section, shape) row of it matches a case's class annotations
(lombok_combinations.classify), else exit 1.
Stub sources (JsonCreator, slf4j, the OpenRewrite nullness annotations) only let delombok resolve names."""
import collections
import importlib.util
import os
import re
import shutil
import subprocess
import sys

STOP = "config.stopBubbling = true\n"
PROFILE_CONFIG = {
    "default": STOP,
    "accessors-chain-config": STOP + "lombok.accessors.chain = true\n",
    "accessors-prefix-config": STOP + "lombok.accessors.prefix += f\nlombok.accessors.prefix += m_\n",
}
OPENREWRITE = "openrewrite"
BOTH = ("default", OPENREWRITE)
MERGED = "*"
F3 = ["private int count;", "private boolean active;", "private String name;"]
F3_FINAL = ["private final int count;", "private final boolean active;", "private final String name;"]
F3_BARE = ["int count;", "boolean active;", "String name;"]
L = "lombok."
EH = "lombok.EqualsAndHashCode"
FD = "lombok.experimental.FieldDefaults"
AC = "lombok.experimental.Accessors"
# id, census keys (item|target|attributes), baseline, profiles, imports, class annotations, kind, extends, members, extra
CASES = [
    ("C01", [L + "Value|TYPE:CLASS|-"], "", BOTH, [L + "Value"], ["@Value"], "class", "", F3_BARE, []),
    ("C02", [L + "With|TYPE:CLASS|-"], "C25", BOTH, [L + "AllArgsConstructor", L + "With"],
     ["@AllArgsConstructor", "@With"], "class", "", F3_FINAL, []),
    ("C03", [L + "With|FIELD|-"], "C25", BOTH, [L + "AllArgsConstructor", L + "With"], ["@AllArgsConstructor"],
     "class", "", F3_FINAL[:2] + ["@With", F3_FINAL[2]], []),
    ("C04", [L + "With|FIELD|value=AccessLevel.PRIVATE"], "C25", BOTH,
     [L + "AccessLevel", L + "AllArgsConstructor", L + "With"], ["@AllArgsConstructor"], "class", "",
     F3_FINAL[:2] + ["@With(AccessLevel.PRIVATE)", F3_FINAL[2]], []),
    ("C05", [L + "With|FIELD|value=AccessLevel.NONE"], "C25", BOTH,
     [L + "AccessLevel", L + "AllArgsConstructor", L + "With"], ["@AllArgsConstructor", "@With"], "class", "",
     ["@With(AccessLevel.NONE)"] + F3_FINAL, []),
    ("C06", [EH + "|TYPE:CLASS|-"], "", BOTH, [EH], ["@EqualsAndHashCode"], "class", "", F3, []),
    ("C07", [EH + "|TYPE:CLASS|callSuper=false"], "", BOTH, [EH], ["@EqualsAndHashCode(callSuper = false)"],
     "class", "C07Base", F3, ["class C07Base {", "}"]),
    ("C08", [EH + "|TYPE:CLASS|callSuper=false;onlyExplicitlyIncluded=true", EH + ".Include|FIELD|-"], "", BOTH,
     [EH], ["@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)"], "class", "C08Base",
     F3[:2] + ["@EqualsAndHashCode.Include", F3[2]], ["class C08Base {", "}"]),
    ("C09", [EH + "|TYPE:CLASS|callSuper=true"], "", BOTH, [EH], ["@EqualsAndHashCode(callSuper = true)"],
     "class", "C09Base", F3, ["class C09Base {", "}"]),
    ("C10", [EH + "|TYPE:CLASS|callSuper=true;onlyExplicitlyIncluded=true", EH + ".Include|FIELD|-"], "", BOTH,
     [EH], ["@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)"], "class", "C10Base",
     F3[:2] + ["@EqualsAndHashCode.Include", F3[2]], ["class C10Base {", "}"]),
    ("C11", [EH + "|TYPE:CLASS|exclude=<field-list>"], "", BOTH, [EH], ['@EqualsAndHashCode(exclude = "active")'],
     "class", "", F3, []),
    ("C12", [EH + "|TYPE:CLASS|onlyExplicitlyIncluded=true", EH + ".Include|FIELD|-"], "", BOTH, [EH],
     ["@EqualsAndHashCode(onlyExplicitlyIncluded = true)"], "class", "",
     F3[:2] + ["@EqualsAndHashCode.Include", F3[2]], []),
    ("C13", [EH + ".Exclude|FIELD|-"], "", BOTH, [EH], ["@EqualsAndHashCode"], "class", "",
     F3[:1] + ["@EqualsAndHashCode.Exclude", F3[1], F3[2]], []),
    ("C14", [L + "Getter|FIELD|-"], "", BOTH, [L + "Getter"], [], "class", "",
     ["@Getter", F3[0], "@Getter", F3[1], "@Getter", F3[2]], []),
    ("C15", [L + "Getter|TYPE:CLASS|-"], "", BOTH, [L + "Getter"], ["@Getter"], "class", "",
     F3 + ["private static int shared;"], []),
    ("C16", [L + "Getter|TYPE:ENUM|-"], "", BOTH, [L + "Getter"], ["@Getter"], "enum", "",
     ['ONE(1, "one");', "private final int code;", "private final String label;", "C16(int code, String label) {",
      "    this.code = code;", "    this.label = label;", "}"], []),
    ("C17", [L + "Setter|FIELD|-"], "", BOTH, [L + "Setter"], [], "class", "",
     ["@Setter", F3[0], "@Setter", F3[1], "@Setter", F3[2]], []),
    ("C18", [L + "Setter|FIELD|value=AccessLevel.PACKAGE"], "", BOTH, [L + "AccessLevel", L + "Setter"], [],
     "class", "", [F3[0], "@Setter(AccessLevel.PACKAGE)", F3[2]], []),
    ("C19", [L + "Setter|TYPE:CLASS|-"], "", BOTH, [L + "Setter"], ["@Setter"], "class", "",
     F3 + ["private final int fixed = 1;"], []),
    ("C20", [L + "ToString|TYPE:CLASS|-"], "", BOTH, [L + "ToString"], ["@ToString"], "class", "", F3, []),
    ("C21", [L + "ToString|TYPE:CLASS|onlyExplicitlyIncluded=true", L + "ToString.Include|FIELD|-"], "", BOTH,
     [L + "ToString"], ["@ToString(onlyExplicitlyIncluded = true)"], "class", "",
     F3[:2] + ["@ToString.Include", F3[2]], []),
    ("C22", [L + "RequiredArgsConstructor|TYPE:CLASS|-"], "", BOTH, [L + "RequiredArgsConstructor"],
     ["@RequiredArgsConstructor"], "class", "", [F3_FINAL[0], F3[1], F3_FINAL[2]], []),
    ("C23", [L + "RequiredArgsConstructor|TYPE:CLASS|access=AccessLevel.PRIVATE"], "", BOTH,
     [L + "AccessLevel", L + "RequiredArgsConstructor"], ["@RequiredArgsConstructor(access = AccessLevel.PRIVATE)"],
     "class", "", [F3_FINAL[0], F3[1], F3_FINAL[2]], []),
    ("C24", [L + "AllArgsConstructor|TYPE:CLASS|-"], "", BOTH, [L + "AllArgsConstructor"], ["@AllArgsConstructor"],
     "class", "", F3, []),
    ("C25", [L + "AllArgsConstructor|TYPE:CLASS|-"], "", BOTH, [L + "AllArgsConstructor"], ["@AllArgsConstructor"],
     "class", "", F3_FINAL, []),
    ("C26", [L + "AllArgsConstructor|TYPE:CLASS|access=AccessLevel.PRIVATE"], "", BOTH,
     [L + "AccessLevel", L + "AllArgsConstructor"], ["@AllArgsConstructor(access = AccessLevel.PRIVATE)"], "class",
     "", F3_FINAL, []),
    ("C27", [L + "AllArgsConstructor|TYPE:CLASS|access=AccessLevel.PUBLIC"], "", BOTH,
     [L + "AccessLevel", L + "AllArgsConstructor"], ["@AllArgsConstructor(access = AccessLevel.PUBLIC)"], "class",
     "", F3_FINAL, []),
    ("C28", [L + "AllArgsConstructor|TYPE:CLASS|onConstructor_={@JsonCreator}"], "", BOTH,
     [L + "AllArgsConstructor", "com.fasterxml.jackson.annotation.JsonCreator"],
     ["@AllArgsConstructor(onConstructor_ = {@JsonCreator})"], "class", "", F3_FINAL, []),
    ("C29", [L + "NoArgsConstructor|TYPE:CLASS|-"], "", BOTH, [L + "NoArgsConstructor"], ["@NoArgsConstructor"],
     "class", "", F3, []),
    ("C30", [L + "Data|TYPE:CLASS|-"], "", BOTH, [L + "Data"], ["@Data"], "class", "", [F3_FINAL[0], F3[1], F3[2]],
     []),
    ("C31", [L + "Builder|TYPE:CLASS|-"], "", BOTH, [L + "Builder"], ["@Builder"], "class", "", F3_FINAL, []),
    ("C32", [L + "Builder.Default|FIELD|-"], "", BOTH, [L + "Builder"], ["@Builder"], "class", "",
     ["@Builder.Default", "private int count = 7;", "private final String name;"], []),
    ("C33", [L + "Builder|CONSTRUCTOR|-"], "", BOTH, [L + "Builder"], [], "class", "",
     [F3_FINAL[0], F3_FINAL[2], "@Builder", "C33(int count, String name) {", "    this.count = count;",
      "    this.name = name;", "}"], []),
    ("C34", [L + 'Builder|TYPE:CLASS|builderClassName="TestEntityBuilder"'], "", BOTH, [L + "Builder"],
     ['@Builder(builderClassName = "TestEntityBuilder")'], "class", "", F3_FINAL, []),
    ("C35", [FD + "|TYPE:CLASS|level=AccessLevel.PRIVATE"], "", BOTH, [L + "AccessLevel", FD],
     ["@FieldDefaults(level = AccessLevel.PRIVATE)"], "class", "", ["int count;", "String name;"], []),
    ("C36", [FD + "|TYPE:CLASS|level=AccessLevel.PRIVATE;makeFinal=true", L + "experimental.NonFinal|FIELD|-"], "",
     BOTH, [L + "AccessLevel", FD, L + "experimental.NonFinal"],
     ["@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)"], "class", "",
     ["int count = 1;", "@NonFinal", "String name;"], []),
    ("C37", [FD + "|TYPE:CLASS|level=AccessLevel.PROTECTED;makeFinal=true"], "", BOTH, [L + "AccessLevel", FD],
     ["@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)"], "class", "", ["int count = 1;"], []),
    ("C38", [L + "experimental.NonFinal|FIELD|-"], "", BOTH, [L + "Value", L + "experimental.NonFinal"],
     ["@Value"], "class", "", ["int count;", "@NonFinal", "String name;"], []),
    ("C39", [AC + "|TYPE:CLASS|chain=true;fluent=true"], "", BOTH, [L + "Getter", L + "Setter", AC],
     ["@Accessors(fluent = true, chain = true)", "@Getter", "@Setter"], "class", "", F3, []),
    ("C40", [AC + "|TYPE:CLASS|fluent=true"], "", BOTH, [L + "Getter", L + "Setter", AC],
     ["@Accessors(fluent = true)", "@Getter", "@Setter"], "class", "", F3, []),
    ("C41", [AC + "|TYPE:CLASS|chain=true"], "", BOTH, [L + "Getter", L + "Setter", AC],
     ["@Accessors(chain = true)", "@Getter", "@Setter"], "class", "", F3, []),
    ("C42", [AC + '|TYPE:CLASS|prefix="f"'], "", BOTH, [L + "Getter", L + "Setter", AC],
     ['@Accessors(prefix = "f")', "@Getter", "@Setter"], "class", "",
     ["private int fCount;", "private boolean fActive;", "private String fName;", "private String plain;"], []),
    ("C43", [L + "experimental.Delegate|FIELD|excludes=<class-list>"], "", BOTH, [L + "experimental.Delegate"], [],
     "class", "", ["private interface Excluded {", "    void remove();", "}", "@Delegate(excludes = Excluded.class)",
                   "private final java.util.Iterator<String> it;", "public C43(java.util.Iterator<String> it) {",
                   "    this.it = it;", "}"], []),
    ("C44", [L + "SneakyThrows|METHOD|value=<class-list>"], "", BOTH, [L + "SneakyThrows"], [], "class", "",
     ["@SneakyThrows(java.io.IOException.class)", "public String read() {",
      '    throw new java.io.IOException("read");', "}"], []),
    ("C45", [L + "experimental.UtilityClass|TYPE:CLASS|-"], "", BOTH, [L + "experimental.UtilityClass"],
     ["@UtilityClass"], "class", "", ["private final int limit = 3;", "public int twice(int x) {",
                                       "    return x * limit;", "}"], []),
    ("C46", [L + "extern.slf4j.Slf4j|TYPE:CLASS|-"], "", BOTH, [L + "extern.slf4j.Slf4j"], ["@Slf4j"], "class", "",
     ["public void run() {", '    log.info("run");', "}"], []),
    ("C47", [EH + "|TYPE:CLASS|-"], "", BOTH, [EH], ["@EqualsAndHashCode"], "class", "",
     ["private long size;", "private float ratio;", "private double weight;", "private int[] codes;",
      "private Object[] refs;", "private char initial;"], []),
    ("C49", ["config:lombok.accessors.chain=true"], "", ("accessors-chain-config",), [L + "Getter", L + "Setter"],
     ["@Getter", "@Setter"], "class", "", F3, []),
    ("C50", ["config:lombok.accessors.prefix+=f,m_"], "", ("accessors-prefix-config",), [L + "Getter", L + "Setter"],
     ["@Getter", "@Setter"], "class", "", ["private int fCount;", "private boolean m_active;", "private String plain;"],
     []),
    ("C51", [L + "Getter|TYPE:CLASS|-", L + "Setter|TYPE:CLASS|-"], "", BOTH, [L + "Getter", L + "Setter"],
     ["@Getter", "@Setter"], "class", "", F3, []),
    ("C68", ["rule:equals-rank"], "", BOTH, [EH, L + "ToString"], ["@EqualsAndHashCode", "@ToString"], "class", "",
     ["private String name;", "private Integer boxed;", "private int count;", "private java.lang.Long total;",
      "private boolean active;"], []),
    ("C69", ["combo:Value+With+Builder+AllArgsConstructor+Builder.Default"], "", BOTH,
     [L + "AllArgsConstructor", L + "Builder", L + "Value", L + "With"],
     ["@Value", "@With", "@Builder", "@AllArgsConstructor"], "class", "",
     ["@Builder.Default", 'String text = "";', "int count;", 'String kind = "fixed";'], []),
    ("C70", ["combo:Data+NoArgsConstructor+AllArgsConstructor+Builder+Builder.Default"], "", BOTH,
     [L + "AllArgsConstructor", L + "Builder", L + "Data", L + "NoArgsConstructor"],
     ["@Data", "@NoArgsConstructor", "@AllArgsConstructor", "@Builder"], "class", "",
     ["@Builder.Default", "private boolean strict = true;", "private int count;"], []),
    ("C71", ["combo:FieldDefaults+With+declared-constructor"], "", BOTH, [L + "AccessLevel", L + "With", FD],
     ["@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)", "@With"], "class", "",
     ["int count;", "String name;", "C71(int count, String name) {", "    this.count = count;",
      "    this.name = name;", "}"], []),
    ("C72", ["combo:FieldDefaults(makeFinal)+Data+With"], "", BOTH, [L + "AccessLevel", L + "Data", L + "With", FD],
     ["@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)", "@Data", "@With"], "class", "",
     ["int count;", "String name;"], []),
    ("C73", ["combo:Value+RequiredArgsConstructor+NonFinal"], "", BOTH,
     [L + "RequiredArgsConstructor", L + "Value", L + "experimental.NonFinal"], ["@Value", "@RequiredArgsConstructor"],
     "class", "", ["String name;", "@NonFinal", "transient boolean seen;"], []),
    ("C74", ["combo:Data+AllArgsConstructor+With"], "", BOTH, [L + "AllArgsConstructor", L + "Data", L + "With"],
     ["@Data", "@AllArgsConstructor", "@With"], "class", "", ["private int count;", "private String name;"], []),
    ("C75", ["combo:Value+Builder"], "", BOTH, [L + "Builder", L + "Value"], ["@Value", "@Builder"], "class", "",
     ["int count;", "String name;"], []),
    ("C76", ["combo:Value+AllArgsConstructor(onConstructor_)+With"], "", BOTH,
     [L + "AllArgsConstructor", L + "Value", L + "With", "com.fasterxml.jackson.annotation.JsonCreator"],
     ["@Value", "@AllArgsConstructor(onConstructor_ = {@JsonCreator})", "@With"], "class", "",
     ["int count;", "String name;"], []),
    ("C77", ["rule:accessors-prefix-order"], "", BOTH, [L + "Getter", AC],
     ['@Accessors(prefix = {"_", "__", ""})', "@Getter"], "class", "",
     ["private int __count;", "private int _size;", "private int plain;"], []),
    ("C78", ["combo:Value+With"], "", BOTH, [L + "Value", L + "With"], ["@Value", "@With"], "class", "",
     ["int count;", "String name;"], []),
    ("C79", ["combo:FieldDefaults(makeFinal)+RequiredArgsConstructor+AllArgsConstructor(PRIVATE)+With+NonFinal"], "",
     BOTH, [L + "AccessLevel", L + "AllArgsConstructor", L + "RequiredArgsConstructor", L + "With", FD,
            L + "experimental.NonFinal"],
     ["@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)", "@RequiredArgsConstructor",
      "@AllArgsConstructor(access = AccessLevel.PRIVATE)", "@With"], "class", "",
     ["int count;", "@NonFinal", "transient String cache;"], []),
    ("C80", ["combo:FieldDefaults(makeFinal)+RequiredArgsConstructor+With+Data"], "", BOTH,
     [L + "AccessLevel", L + "Data", L + "RequiredArgsConstructor", L + "With", FD],
     ["@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)", "@RequiredArgsConstructor", "@With", "@Data"],
     "class", "", ["int count;", "String name;"], []),
    ("C81", ["combo:RequiredArgsConstructor+FieldDefaults(makeFinal)+With"], "", BOTH,
     [L + "AccessLevel", L + "RequiredArgsConstructor", L + "With", FD],
     ["@RequiredArgsConstructor", "@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)", "@With"], "class",
     "", ["int count;", "String name;"], []),
    ("C82", ["combo:Data+Builder+AllArgsConstructor"], "", BOTH, [L + "AllArgsConstructor", L + "Builder", L + "Data"],
     ["@Data", "@Builder", "@AllArgsConstructor"], "class", "", ["private int count;", "private String name;"], []),
    ("C83", ["combo:Data+NoArgsConstructor+AllArgsConstructor"], "", BOTH,
     [L + "AllArgsConstructor", L + "Data", L + "NoArgsConstructor"],
     ["@Data", "@NoArgsConstructor", "@AllArgsConstructor"], "class", "",
     ["private int count;", "private String name;"], []),
    ("C84", ["combo:FieldDefaults(makeFinal)+RequiredArgsConstructor+AllArgsConstructor(PRIVATE)+Data+NonFinal"], "",
     BOTH, [L + "AccessLevel", L + "AllArgsConstructor", L + "Data", L + "RequiredArgsConstructor", L + "With", FD,
            L + "experimental.NonFinal"],
     ["@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)", "@RequiredArgsConstructor",
      "@AllArgsConstructor(access = AccessLevel.PRIVATE)", "@Data"], "class", "",
     ["@NonFinal", "transient String cache;", "@With", "int count;", "String name;"], []),
    ("C85", ["combo:Data+NoArgsConstructor"], "", BOTH, [L + "Data", L + "NoArgsConstructor"],
     ["@Data", "@NoArgsConstructor"], "class", "", ["private int count;", "private String name;"], []),
    ("C86", ["combo:Value+Builder+AllArgsConstructor+Builder.Default+field-With"], "", BOTH,
     [L + "AllArgsConstructor", L + "Builder", L + "Value", L + "With"], ["@Value", "@Builder", "@AllArgsConstructor"],
     "class", "", ["@With", "@Builder.Default", 'String id = "id";', "String name;"], []),
]
STUBS = {
    "com/fasterxml/jackson/annotation/JsonCreator.java":
        "package com.fasterxml.jackson.annotation;\n\npublic @interface JsonCreator {\n}\n",
    "org/slf4j/Logger.java": "package org.slf4j;\n\npublic interface Logger {\n    void info(String msg);\n}\n",
    "org/slf4j/LoggerFactory.java": "package org.slf4j;\n\npublic final class LoggerFactory {\n"
                                    "    public static Logger getLogger(Class<?> c) {\n        return null;\n    }\n}\n",
    "org/openrewrite/internal/lang/NonNull.java": "package org.openrewrite.internal.lang;\n\npublic @interface NonNull {\n}\n",
    "org/openrewrite/internal/lang/Nullable.java":
        "package org.openrewrite.internal.lang;\n\npublic @interface Nullable {\n}\n",
}
DIAG = re.compile(r"[\\/]rep[\\/](?P<case>C\d\d)\.java:(?P<line>\d+): (?P<sev>warning|error): (?P<msg>.*)$")
INDENT = "    "
PATTERN_HEADER = "pattern\tprofile\tcase\tcensus_keys\towner\tkind\tchange\tfields\tsignature\tannotations\tbody\n"
NONE = "-"
ANNOTATION_NAME = re.compile(r"@([A-Za-z][A-Za-z0-9_.]*)")
BUILDER_DEFAULT_MEMBER = "@Builder.Default"
COMBINATIONS_TOOL = "lombok_combinations.py"
COMBINATIONS_MODULE = "lombok_combinations"


def source(case):
    cid, _, _, _, imports, annotations, kind, extends, members, extra = case
    lines = ["package rep;", ""] + ["import %s;" % i for i in sorted(imports)] + [""] + annotations
    lines.append("public %s %s%s {" % (kind, cid, " extends " + extends if extends else ""))
    lines += [INDENT + m for m in members] + ["}"] + extra
    return "\n".join(lines) + "\n"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def census_groups(census):
    with open(census, encoding="utf-8") as f:
        next(f)
        rows = [line.rstrip("\n").split("\t") for line in f]
    return sorted({"%s|%s|%s" % (r[2], r[3], r[4]) for r in rows if r[0] == "ANNOTATION"})


def combinations_tool():
    """lombok_combinations.py next to this file, loaded by path (works under python -I too)."""
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), COMBINATIONS_TOOL)
    spec = importlib.util.spec_from_file_location(COMBINATIONS_MODULE, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def uncovered_combinations(path):
    """(section, shape) rows of CLASS_COMBINATIONS.tsv that no case's class annotations reproduce."""
    with open(path, encoding="utf-8") as f:
        next(f)
        wanted = {tuple(line.rstrip("\n").split("\t")[:2]) for line in f}
    classify = combinations_tool().classify
    covered = {g for c in CASES for g in classify({ANNOTATION_NAME.match(a).group(1) for a in c[5]},
                                                    BUILDER_DEFAULT_MEMBER in c[8])}
    return sorted(wanted - covered)


def run_profile(profile, cases, stage, java, jar, tool):
    src, out = os.path.join(stage, profile), os.path.join(stage, profile + "-out")
    shutil.rmtree(src, ignore_errors=True)
    shutil.rmtree(out, ignore_errors=True)
    write(os.path.join(src, "lombok.config"), PROFILE_CONFIG[profile])
    for rel, text in STUBS.items():
        write(os.path.join(src, rel), text)
    for c in cases:
        write(os.path.join(src, "rep", c[0] + ".java"), source(c))
    p = subprocess.run([java, "-jar", jar, "delombok", "--encoding=UTF-8", "-d", out, src], capture_output=True,
                       text=True)
    if p.returncode != 0:
        raise SystemExit("delombok failed for %s:\n%s" % (profile, (p.stdout + p.stderr)[:2000]))
    diags = [DIAG.search(line) for line in (p.stdout + p.stderr).splitlines()]
    table = os.path.join(stage, profile + "-cases.tsv")
    write(table, "case\tcensus_keys\tbaseline\n" + "".join("%s\t%s\t%s\n" % (c[0], ";".join(c[1]), c[2])
                                                           for c in cases))
    rows_path = os.path.join(stage, profile + "-patterns.tsv")
    print(subprocess.run([java, tool, table, src, out, profile, rows_path], check=True, capture_output=True,
                         text=True).stdout.strip())
    with open(rows_path, encoding="utf-8") as f:
        next(f)
        rows = [line.rstrip("\n").split("\t") for line in f]
    keys = {c[0]: ";".join(c[1]) for c in cases}
    for d in (x for x in diags if x):
        rows.append([profile, d.group("case"), keys[d.group("case")], NONE, d.group("sev").upper(), NONE, NONE,
                     d.group("msg").strip(), NONE, "line " + d.group("line")])
    return rows


def merge(rows):
    by_rest = collections.defaultdict(set)
    for r in rows:
        by_rest[tuple(r[1:])].add(r[0])
    merged = []
    for rest, profiles in by_rest.items():
        if set(BOTH) <= profiles:
            merged.append((MERGED,) + rest)
            profiles = profiles - set(BOTH)
        merged += [(p,) + rest for p in profiles]
    return sorted(merged, key=lambda r: (r[1], r[3], r[4], r[5], r[7], r[0]))


def main(argv):
    stage, lane, java, jar, openrewrite_config, census = argv[1:7]
    with open(openrewrite_config, encoding="utf-8") as f:
        PROFILE_CONFIG[OPENREWRITE] = f.read()
    covered = {k for c in CASES for k in c[1]}
    uncovered = [g for g in census_groups(census) if g not in covered]
    missing = uncovered_combinations(argv[7]) if len(argv) > 7 else []
    tool = os.path.join(os.path.dirname(os.path.abspath(__file__)), "PatternDiff.java")
    profiles = sorted({p for c in CASES for p in c[3]})
    rows = []
    for profile in profiles:
        rows += run_profile(profile, [c for c in CASES if profile in c[3]], stage, java, jar, tool)
    merged = merge(rows)
    counter = collections.Counter()
    with open(os.path.join(lane, "PATTERNS.tsv"), "w", encoding="utf-8", newline="\n") as f:
        f.write(PATTERN_HEADER)
        for r in merged:
            counter[r[1]] += 1
            f.write("P-%s-%02d\t%s\n" % (r[1], counter[r[1]], "\t".join(r)))
    kinds = collections.Counter(r[5] for r in merged)
    print("PATTERNS rows=%d cases=%d kinds=%s uncovered_census_groups=%d uncovered_combinations=%d"
          % (len(merged), len(CASES), dict(sorted(kinds.items())), len(uncovered), len(missing)))
    for g in uncovered:
        print("UNCOVERED " + g)
    for section, shape in missing:
        print("UNCOVERED_COMBINATION %s %s" % (section, shape))
    sys.exit(1 if uncovered or missing else 0)


if __name__ == "__main__":
    main(sys.argv)

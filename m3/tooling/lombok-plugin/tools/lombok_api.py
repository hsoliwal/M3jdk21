# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""lombok_api.py <lombok.jar> <javap> <out_api.tsv> <out_config_keys.tsv> : LOMBOK-API v1 (CPU, read-only).

Lists the public Lombok API carried by the jar (lombok/, lombok/experimental/, lombok/extern/<logger>/), classifies
every type from its class file with `javap -v` (ANNOTATION | ENUM | CLASS | INTERFACE), and records for each
annotation its @Target, its @Retention and every element with its type and default value. A second table lists the
configuration keys the jar's lombok.ConfigurationKeys declares (string constants of the form lombok.* / config.*).
Both tables are the closed vocabularies the census resolves against: a name outside them is UNKNOWN, never guessed.
Re-running on the same jar gives byte-identical output (rows sorted, no timestamps)."""
import re
import subprocess
import sys
import zipfile

API_ENTRY = re.compile(r"^lombok/(?:experimental/|extern/[a-z0-9]+/)?[A-Za-z][A-Za-z0-9$]*\.class$")
SKIP_ENTRY = re.compile(r"ConfigurationKeys\$\d+|package-info")
CLASSFILE = re.compile(r"^Classfile .*!/(?P<path>lombok/[^ ]+)\.class$")
CLASS_FLAGS = re.compile(r"^  flags: \(0x[0-9a-f]+\) (?P<flags>.*)$")
ELEMENT = re.compile(r"^  public abstract (?P<type>[\w.$\[\]<>]+) (?P<name>\w+)\(\);$")
DEFAULT_VALUE = re.compile(r"^        (?P<value>\S.*)$")
META_START = re.compile(r"^    java\.lang\.annotation\.(?P<meta>Target|Retention)\($")
META_VALUE = re.compile(r"^      value=(?P<value>.*)$")
ENUM_CONST = re.compile(r"L[\w/$]+;\.(\w+)")
CONFIG_KEY = re.compile(r"= Utf8\s+(?P<key>(?:lombok|config)\.[A-Za-z0-9.]+[A-Za-z0-9])\s*$")
CONFIG_CLASS = "lombok.ConfigurationKeys"
FLAG_KIND = (("ACC_ANNOTATION", "ANNOTATION"), ("ACC_ENUM", "ENUM"), ("ACC_INTERFACE", "INTERFACE"))
KIND_CLASS = "CLASS"
NONE = "-"
API_HEADER = "fqn\tkind\ttarget\tretention\telements\n"
KEYS_HEADER = "config_key\n"


def api_classes(jar):
    with zipfile.ZipFile(jar) as z:
        names = [n for n in z.namelist() if API_ENTRY.match(n) and not SKIP_ENTRY.search(n)]
    return sorted(n[:-len(".class")].replace("/", ".") for n in names)


def javap(tool, jar, classes):
    return subprocess.run([tool, "-v", "-cp", jar] + classes, check=True, capture_output=True,
                          text=True, encoding="utf-8").stdout.splitlines()


def kind_of(flags):
    return next((k for f, k in FLAG_KIND if f in flags), KIND_CLASS)


def parse_blocks(lines):
    """One dict per class file: fqn, kind, target, retention, elements [(name, type, default)]."""
    blocks, cur, pending, meta = [], None, None, None
    for line in lines:
        m = CLASSFILE.match(line)
        if m:
            cur = {"fqn": m.group("path").replace("/", ".").replace("$", "."), "kind": None,
                   "target": NONE, "retention": NONE, "elements": []}
            blocks.append(cur)
            continue
        if cur is None:
            continue
        m = CLASS_FLAGS.match(line)
        if m and cur["kind"] is None:
            cur["kind"] = kind_of(m.group("flags"))
            continue
        m = ELEMENT.match(line)
        if m:
            pending = [m.group("name"), m.group("type").replace("$", "."), NONE]
            cur["elements"].append(pending)
            continue
        if pending is not None and line.strip().startswith("default_value:"):
            continue
        m = DEFAULT_VALUE.match(line)
        if m and pending is not None and pending[2] == NONE:
            pending[2] = ",".join(ENUM_CONST.findall(m.group("value"))) or m.group("value")
            continue
        m = META_START.match(line)
        if m:
            meta = m.group("meta").lower()
            continue
        m = META_VALUE.match(line)
        if m and meta is not None:
            cur[meta] = ",".join(ENUM_CONST.findall(m.group("value")))
            meta = None
    return blocks


def config_keys(lines):
    keys = set()
    inside = False
    for line in lines:
        m = CLASSFILE.match(line)
        if m:
            inside = m.group("path").replace("/", ".") == CONFIG_CLASS
            continue
        if inside:
            k = CONFIG_KEY.search(line)
            if k:
                keys.add(k.group("key"))
    return sorted(keys)


def main(argv):
    jar, tool, out_api, out_keys = argv[1:5]
    classes = api_classes(jar)
    lines = javap(tool, jar, classes)
    blocks = sorted(parse_blocks(lines), key=lambda b: b["fqn"])
    with open(out_api, "w", encoding="utf-8", newline="\n") as f:
        f.write(API_HEADER)
        for b in blocks:
            elements = ";".join("%s:%s=%s" % (n, t, d) for n, t, d in b["elements"]) or NONE
            f.write("\t".join([b["fqn"], b["kind"] or KIND_CLASS, b["target"], b["retention"], elements]) + "\n")
    keys = config_keys(lines)
    with open(out_keys, "w", encoding="utf-8", newline="\n") as f:
        f.write(KEYS_HEADER)
        f.writelines(k + "\n" for k in keys)
    annotations = sum(1 for b in blocks if b["kind"] == "ANNOTATION")
    print("LOMBOK-API v1 types=%d annotations=%d config_keys=%d" % (len(blocks), annotations, len(keys)))


if __name__ == "__main__":
    main(sys.argv)

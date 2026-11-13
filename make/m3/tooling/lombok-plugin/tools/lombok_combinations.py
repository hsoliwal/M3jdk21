# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""lombok_combinations.py <OCCURRENCES.tsv> <out.tsv> :
LOMBOK-COMBINATIONS v1 (CPU). Projects the census stage file OCCURRENCES.tsv (written by lombok_census.py) onto the
class-level annotation combinations that decide which constructors, with-ers and builder members Lombok generates
(SPEC.md rules 9 and 10, @With). One row per (section, shape): the number of classes and the first example.
  WITH_TYPE        classes with type-level @With; shape = their constructor-relevant class annotations
  CONSTRUCTOR_MIX  classes with @Value or @Data plus an explicit constructor annotation or class-level @Builder
  BUILDER_DEFAULT  classes with a @Builder.Default field; shape = their constructor-relevant class annotations
A class is (origin, path, enclosing class, simple name) for type annotations; a field's class is (origin, path,
simple name). Rows are sorted; a re-run over the same file is byte-identical. lombok_patterns.py requires a case for
every row (classify() below)."""
import collections
import sys

LOMBOK = "lombok."
VALUE, DATA, BUILDER, WITH = "Value", "Data", "Builder", "With"
EXPLICIT = ("AllArgsConstructor", "RequiredArgsConstructor", "NoArgsConstructor")
RELEVANT = frozenset((VALUE, DATA, BUILDER) + EXPLICIT)
BUILDER_DEFAULT = "Builder.Default"
ANNOTATION, FIELD, TYPE_PREFIX = "ANNOTATION", "FIELD", "TYPE"
WITH_SECTION, MIX_SECTION, DEFAULT_SECTION = "WITH_TYPE", "CONSTRUCTOR_MIX", "BUILDER_DEFAULT"
NONE = "-"
HEADER = "section\tshape\tclasses\texample\n"


def read(path):
    """(class annotations, class examples, classes with @Builder.Default fields) from OCCURRENCES.tsv."""
    annotations, examples, defaults = collections.defaultdict(set), {}, set()
    with open(path, encoding="utf-8") as f:
        head = next(f).rstrip("\n").split("\t")
        for line in f:
            r = dict(zip(head, line.rstrip("\n").split("\t")))
            if r["kind"] != ANNOTATION or not r["item"].startswith(LOMBOK):
                continue
            name = r["item"][len(LOMBOK):]
            if r["target"].startswith(TYPE_PREFIX):
                key = (r["origin"], r["path"], r["owner"], r["member"])
                where = (r["path"], int(r["line"]))
                annotations[key].add(name)
                examples[key] = min(examples.get(key, where), where)
            elif r["target"] == FIELD and name == BUILDER_DEFAULT:
                defaults.add((r["origin"], r["path"], r["owner"]))
    return annotations, examples, defaults


def shape(names, keep):
    return ",".join(sorted(names & keep)) or NONE


def classify(names, has_default):
    """(section, shape) pairs of one class: names = simple Lombok names of its class-level annotations, has_default =
    it has a @Builder.Default field. lombok_patterns.py applies the same function to its cases (coverage gate)."""
    out = []
    if WITH in names:
        out.append((WITH_SECTION, shape(names, RELEVANT)))
    if names & {VALUE, DATA} and names & (set(EXPLICIT) | {BUILDER}):
        out.append((MIX_SECTION, shape(names, RELEVANT)))
    if has_default:
        out.append((DEFAULT_SECTION, shape(names, RELEVANT | {WITH})))
    return out


def rows(annotations, examples, defaults):
    groups = collections.defaultdict(list)
    for key, names in annotations.items():
        for group in classify(names, key[:2] + key[3:] in defaults):
            groups[group].append(key)
    out = []
    for (section, sh), keys in sorted(groups.items()):
        path, line = min(examples[k] for k in keys)
        out.append("%s\t%s\t%d\t%s:%d\n" % (section, sh, len(keys), path, line))
    return out


def main(argv):
    annotations, examples, defaults = read(argv[1])
    out = rows(annotations, examples, defaults)
    with open(argv[2], "w", encoding="utf-8", newline="\n") as f:
        f.write(HEADER + "".join(out))
    total = collections.Counter()
    for r in out:
        section, sh, n = r.split("\t")[:3]
        total[section] += int(n)
        if section == WITH_SECTION and not {VALUE, EXPLICIT[0]} & set(sh.split(",")):
            total[WITH_SECTION + "_without_Value_AllArgsConstructor"] += int(n)
    print("LOMBOK-COMBINATIONS v1 rows=%d %s" % (len(out), " ".join("%s=%d" % kv for kv in sorted(total.items()))))


if __name__ == "__main__":
    main(sys.argv)

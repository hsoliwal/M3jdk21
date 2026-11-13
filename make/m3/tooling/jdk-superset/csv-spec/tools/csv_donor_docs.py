# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""csv_donor_docs.py <vendor_root> <out_methods.tsv> <out_features.tsv> <out_extra.tsv> : CSV-DONOR-DOCS v1.

Evidence for the CSV-CAPABILITY v1 review, read from the vendored donor sources only:
  methods   every public method of the donor configuration classes (the classes api_superset.py reads) with
            the first sentence of its Javadoc: donor, Class.method, first sentence. One row per Class.method
            (the first documented overload).
  features  the enum-valued options api_superset.py could not see because they are constants, not setters:
            jackson CsvParser.Feature / CsvGenerator.Feature, commons-csv QuoteMode / DuplicateHeaderMode,
            univocity UnescapedQuoteHandling: donor, feature:<Enum>.<CONSTANT>, first Javadoc sentence.
  extra     keys outside CSV_API_SUPERSET v1, normalized with its key() rule (verb prefix and Enabled/Settings
            suffix dropped): source gap-config = univocity CommonParserSettings / CommonWriterSettings (the v1
            file regex missed the two-word class names); source operation = the record-level operation classes
            (univocity AbstractParser, AbstractWriter, Record; commons-csv CSVParser, CSVPrinter, CSVRecord).
            Columns: source, donor, key, Class.method names, first Javadoc sentence of the first documented one.
CPU, read-only. Rows are sorted; a re-run on the same sources is byte-identical."""
import os
import re
import sys

CONFIG = {
    "univocity-parsers": re.compile(r"/(Csv|Tsv|FixedWidth)?(Parser|Writer|Common)?Settings\.java$|"
                                    r"/(Csv|Tsv|FixedWidth)?Format\.java$|/CommonSettings\.java$"),
    "commons-csv": re.compile(r"/CSVFormat\.java$"),
    "jackson-dataformat-csv": re.compile(r"/CsvSchema\.java$|/CsvParser\.java$|/CsvGenerator\.java$|"
                                         r"/CsvMapper\.java$"),
}
FEATURE_FILES = {
    "jackson-dataformat-csv": re.compile(r"/Csv(Parser|Generator)\.java$"),
    "commons-csv": re.compile(r"/(QuoteMode|DuplicateHeaderMode)\.java$"),
    "univocity-parsers": re.compile(r"/UnescapedQuoteHandling\.java$"),
}
SKIP_DIRS = ("/annotations/",)
EXTRA = {
    "gap-config": {"univocity-parsers": re.compile(r"/Common(Parser|Writer)Settings\.java$")},
    "operation": {"univocity-parsers": re.compile(r"/common/(AbstractParser|AbstractWriter)\.java$|/record/Record\.java$"),
                  "commons-csv": re.compile(r"/CSV(Parser|Printer|Record)\.java$")},
}
VERB = re.compile(r"^(set|with|enable|disable|is|get|has|use|without)(?=[A-Z])")
SKIP_NAMES = {"toString", "hashCode", "equals", "clone", "build", "builder", "copy", "main", "getClass"}
DOC_METHOD = re.compile(r"/\*\*(?P<doc>(?:(?!\*/).)*)\*/\s*(?:@\w+(?:\([^)]*\))?\s*)*"
                        r"public\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+)*(?:<[^>]+>\s*)?"
                        r"[\w.<>\[\], ?]+\s+(?P<name>\w+)\s*\(", re.S)
ENUM_HEAD = re.compile(r"\benum\s+(?P<enum>\w+)[^{]*\{")
COMMENT = re.compile(r"/\*.*?\*/|//[^\n]*", re.S)
ENUM_CONST = re.compile(r"(?:/\*\*(?P<doc>(?:(?!\*/).)*)\*/\s*)?(?:@\w+(?:\([^)]*\))?\s*)*"
                        r"\b(?P<name>[A-Z][A-Z0-9_]+)\b\s*(?:\([^)]*\))?\s*(?=,|;|$|\{)", re.S)
TAG = re.compile(r"\{@\w+\s+([^}]*)\}")
HTML = re.compile(r"<[^>]+>")
SENTENCE_END = re.compile(r"(?<=[.!?])\s")
MAX_SENTENCE = 220
NONE = "-"


def first_sentence(doc):
    lines = [re.sub(r"^\s*\*\s?", "", l) for l in doc.splitlines()]
    text = " ".join(l for l in lines if not l.strip().startswith("@"))
    text = re.sub(r"\s+", " ", HTML.sub("", TAG.sub(r"\1", text))).strip()
    if not text:
        return NONE
    return SENTENCE_END.split(text, 1)[0][:MAX_SENTENCE].rstrip().replace("\t", " ")


def java_files(root, donor, rx):
    base = os.path.join(root, donor)
    for dirpath, _, files in os.walk(base):
        for f in files:
            p = os.path.join(dirpath, f).replace(os.sep, "/")
            if rx.search(p) and not any(s in p for s in SKIP_DIRS):
                yield p


def methods(root):
    rows = {}
    for donor, rx in CONFIG.items():
        for p in java_files(root, donor, rx):
            cls = os.path.basename(p)[:-len(".java")]
            with open(p, encoding="utf-8", errors="replace") as f:
                text = f.read()
            for m in DOC_METHOD.finditer(text):
                key = (donor, "%s.%s" % (cls, m.group("name")))
                if key not in rows or rows[key] == NONE:
                    rows[key] = first_sentence(m.group("doc"))
    return sorted((d, k, s) for (d, k), s in rows.items())


def features(root):
    rows = set()
    for donor, rx in FEATURE_FILES.items():
        for p in java_files(root, donor, rx):
            with open(p, encoding="utf-8", errors="replace") as f:
                text = f.read()
            cls = os.path.basename(p)[:-len(".java")]
            masked = COMMENT.sub(lambda m: " " * len(m.group(0)), text)
            for e in ENUM_HEAD.finditer(masked):
                body = text[e.end():constants_end(masked, e.end())]
                owner = e.group("enum") if e.group("enum") == cls else "%s.%s" % (cls, e.group("enum"))
                for c in ENUM_CONST.finditer(body):
                    rows.add((donor, "feature:%s.%s" % (owner, c.group("name")),
                              first_sentence(c.group("doc") or "")))
    return sorted(rows)


def constants_end(masked, start):
    """Offset of the ';' or '}' that ends an enum's constant list (depth 0, comments masked)."""
    depth = 0
    for i in range(start, len(masked)):
        ch = masked[i]
        if ch in "({":
            depth += 1
        elif ch in ")}":
            if depth == 0:
                return i
            depth -= 1
        elif ch == ";" and depth == 0:
            return i
    return len(masked)


def key(name):
    k = VERB.sub("", name)
    k = re.sub(r"(Enabled|Setting|Settings)$", "", k)
    return k[:1].lower() + k[1:] if k else name


def extra(root):
    rows = {}
    for source, donors in EXTRA.items():
        for donor, rx in donors.items():
            for p in java_files(root, donor, rx):
                cls = os.path.basename(p)[:-len(".java")]
                with open(p, encoding="utf-8", errors="replace") as f:
                    text = f.read()
                for m in DOC_METHOD.finditer(text):
                    name = m.group("name")
                    if name in SKIP_NAMES or name == cls:
                        continue
                    entry = rows.setdefault((source, donor, key(name)), [set(), NONE])
                    entry[0].add("%s.%s" % (cls, name))
                    if entry[1] == NONE:
                        entry[1] = first_sentence(m.group("doc"))
    return sorted((s, d, k, ";".join(sorted(e[0])), e[1]) for (s, d, k), e in rows.items())


def write(path, header, rows):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(header)
        f.writelines("\t".join(r) + "\n" for r in rows)


def main(argv):
    root, out_methods, out_features, out_extra = argv[1:5]
    m, ft, ex = methods(root), features(root), extra(root)
    write(out_methods, "donor\tmethod\tfirst_sentence\n", m)
    write(out_features, "donor\tfeature\tfirst_sentence\n", ft)
    write(out_extra, "source\tdonor\tkey\tmethods\tfirst_sentence\n", ex)
    print("CSV-DONOR-DOCS v1 methods=%d documented=%d features=%d extra=%d (gap-config=%d operation=%d)"
          % (len(m), sum(1 for r in m if r[2] != NONE), len(ft), len(ex),
             sum(1 for r in ex if r[0] == "gap-config"), sum(1 for r in ex if r[0] == "operation")))


if __name__ == "__main__":
    main(sys.argv)

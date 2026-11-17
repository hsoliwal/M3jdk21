#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright (c) 2026 Hitesh Soliwal and contributors
"""Write the WORD TAB COUNT input of one registry lexicon (m3/lexicon/m3-lexicon-registry.tsv).

usage: frequency_counts.py RANK_RULE LEXICON.tsv.gz OUTPUT [ORACLE.tsv.gz ...] [--corpus PATH]

LEXICON.tsv.gz is a measured word list: a header, then word_id TAB word TAB weight, with
backslash, tab, LF and CR in a word written as two-character escapes. The count of a word
follows RANK_RULE:
  local-count, source-count   the word's own weight
  oracle-count:KEY            the summed weight of the word in the ORACLE lists (0 if absent)
  codebase-token:CORPUS       whole-token occurrences in the corpus at --corpus, comments and
                              strings included: an edge of the word that is a W character must
                              not touch another W character (the leaf-1 keyword rule); other
                              edges are free, so "(" counts every "(". W = [A-Za-z0-9_$] for
                              java.base/java/util@... (the top-level java.base/java/util/*.java
                              members of a JDK src.zip) and W = [A-Za-z0-9_] for cpython/Lib@...
                              (the top-level *.py files of a CPython Lib directory)
Words holding a tab, CR or LF cannot be WORD TAB COUNT input and are left out. OUTPUT is UTF-8
with LF line ends, sorted the way LexiconTool build --frequency ranks: count descending, then
UTF-16 code units, so line N holds rank N - 1. One summary line goes to stdout.
"""
import gzip
import hashlib
import pathlib
import re
import sys
import zipfile

ESCAPE = re.compile(r"\\(.)")
UNESCAPED = {"\\": "\\", "t": "\t", "n": "\n", "r": "\r"}
JAVA_MEMBER = re.compile(r"java\.base/java/util/[A-Za-z0-9_]+\.java")


def read_lexicon(path):
    """Measured (word, weight) pairs in file order; weight is None when the column is empty."""
    pairs = []
    with gzip.open(path, "rt", encoding="utf-8", newline="") as stream:
        if stream.readline() != "word_id\tword\tweight\n":
            raise SystemExit(f"{path}: unexpected header")
        for number, line in enumerate(stream, start=1):
            word_id, word, weight = line.rstrip("\n").split("\t")
            if int(word_id) != number:
                raise SystemExit(f"{path}: word ids are not 1..N")
            word = ESCAPE.sub(lambda match: UNESCAPED[match.group(1)], word)
            pairs.append((word, int(weight) if weight else None))
    return pairs


def corpus(rule, location):
    """(text, word class, fingerprint) of a codebase-token corpus."""
    files = []
    if rule.startswith("java.base/java/util@"):
        with zipfile.ZipFile(location) as archive:
            for name in sorted(archive.namelist()):
                if JAVA_MEMBER.fullmatch(name):
                    files.append((name, archive.read(name)))
        word = "A-Za-z0-9_$"
    elif rule.startswith("cpython/Lib@"):
        for path in sorted(pathlib.Path(location).glob("*.py")):
            files.append((path.name, path.read_bytes()))
        word = "A-Za-z0-9_"
    else:
        raise SystemExit("unknown codebase-token corpus " + rule)
    listing = "".join(f"{name}\t{hashlib.sha256(data).hexdigest()}\n" for name, data in files)
    text = "\n".join(data.decode("utf-8") for _, data in files)
    fingerprint = hashlib.sha256(listing.encode("utf-8")).hexdigest()
    return text, word, f"files={len(files)} corpus_sha256={fingerprint}"


def token_counts(words, text, word_class):
    """Whole-token counts: a word-character edge must not touch another word character."""
    runs = {}
    for run in re.findall(f"[{word_class}]+", text):
        runs[run] = runs.get(run, 0) + 1
    edge = re.compile(f"[{word_class}]")
    counts = {}
    for word in words:
        first, last = bool(edge.fullmatch(word[0])), bool(edge.fullmatch(word[-1]))
        if first and last and re.fullmatch(f"[{word_class}]+", word):
            counts[word] = runs.get(word, 0)
        elif not first and not last:
            counts[word] = text.count(word)
        else:
            counts[word] = bounded_count(text, word, first, last, edge)
    return counts


def bounded_count(text, word, first, last, edge):
    """Leftmost non-overlapping matches whose word-character edges touch no W character."""
    count, start = 0, 0
    while (at := text.find(word, start)) >= 0:
        end = at + len(word)
        if ((not first or at == 0 or not edge.fullmatch(text[at - 1]))
                and (not last or end == len(text) or not edge.fullmatch(text[end]))):
            count, start = count + 1, end
        else:
            start = at + 1
    return count


def main(argv):
    location = None
    if "--corpus" in argv:
        at = argv.index("--corpus")
        location = argv[at + 1]
        argv = argv[:at] + argv[at + 2:]
    rule, lexicon, output, oracles = argv[0], argv[1], argv[2], argv[3:]
    pairs = read_lexicon(lexicon)
    kept = [(word, weight) for word, weight in pairs if not re.search("[\t\r\n]", word)]
    note = ""
    if rule in ("local-count", "source-count"):
        counts = {word: weight for word, weight in kept}
        if any(weight is None for weight in counts.values()):
            raise SystemExit(f"{lexicon}: {rule} needs a weight on every word")
    elif rule.startswith("oracle-count:"):
        if not oracles:
            raise SystemExit("oracle-count needs at least one oracle list")
        oracle = {}
        for path in oracles:
            for word, weight in read_lexicon(path):
                oracle[word] = oracle.get(word, 0) + weight
        counts = {word: oracle.get(word, 0) for word, _ in kept}
    elif rule.startswith("codebase-token:"):
        text, word_class, note = corpus(rule[len("codebase-token:"):], location)
        counts = token_counts([word for word, _ in kept], text, word_class)
    else:
        raise SystemExit("unknown rank rule " + rule)
    ranked = sorted(counts.items(), key=lambda item: (-item[1], item[0].encode("utf-16-be")))
    body = "".join(f"{word}\t{count}\n" for word, count in ranked).encode("utf-8")
    pathlib.Path(output).write_bytes(body)
    nonzero = sum(1 for _, count in ranked if count)
    print(f"words={len(ranked)} excluded={len(pairs) - len(kept)} nonzero={nonzero} "
          f"counts_sha256={hashlib.sha256(body).hexdigest()} {note}".rstrip())


if __name__ == "__main__":
    main(sys.argv[1:])

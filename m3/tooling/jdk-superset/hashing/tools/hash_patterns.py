# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hash_patterns.py <jdk.internal.m3.hash sources dir> <METHOD_SIZES.tsv> <PATTERNS.tsv>

The patternize stage of the hashing leaf: every atom (every method method_sizes.py found in the compiled classes,
less the compiler's bridge methods) and every read site (each call of a short-name read in an algorithm file, each
backing read in an access file) is classified into the closed set HASH-PATTERN v1 below, with a typed residue (what
the pattern does not say). An atom no rule matches is UNKNOWN_PATTERN; the stage passes only with none. Owners: an
atom that JUnit proved to serve two donor computations names them (keep one owner).

HASH-PATTERN v1 (roles of the leaf README; members):
  ALGORITHM  ALG_LENGTH_DISPATCH  picks the computation by input length (returns an atom's value)
             ALG_LENGTH_CLASS     hashes one bounded input-length class
             ALG_STREAM           loops over the whole input (unbounded length)
             ALG_MIX              combines values; reads at most a fixed number of input bytes or key-table entries
             ALG_PRIMITIVE        hashes a fixed-size primitive (hashLong, dualHashInt, ...)
             ALG_ADAPT            hash(T, access, off, len): byte order of the access, then the kernel
             ALG_SETUP            factories, seeds, secrets, constructors, serialization hooks
  FACADE     FACADE_ENTRY         public input forms (byte[], ByteBuffer, String, ...): range check, access, kernel
             FACADE_FACTORY       the static function factories (xx(), xx3(), ...)
             FACADE_ADAPTER       DualHashFunction: tuple to long adapters and result checks
             FACADE_SETUP         constructors, result arrays, bit lengths
  ACCESS     ACCESS_READ          reads n bytes of an input at an offset
             ACCESS_ORDER         byte order of an input, reverse accesses
             ACCESS_SETUP         access factories, constructors
  KERNEL     KERNEL_MATH          128-bit multiply helpers
             KERNEL_PRIMITIVE     widening, byte order of primitives
             KERNEL_CHECK         range checks
             KERNEL_SETUP         constructors, static state
  READ SITE  READ_INPUT           an algorithm reads the hashed input through its access
             READ_KEY             an algorithm reads key material (XXH3 secrets) through the byte[] access
             READ_BACKING         an access reads its carrier (byte[], ByteBuffer, CharSequence, Unsafe)
Residue types: LENGTH(lo..hi), DISPATCH(atoms), STREAM(unit), WIDTH(n), CHECK(kind), OFFSET(form), OWNER(...),
CARRIER(kind), NONE.
"""
import csv
import os
import re
import sys

NL = "\n"
TAB = "\t"
VERSION = "HASH-PATTERN v1"
MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
HEADER = ("kind", "file", "line", "owner", "member", "role", "pattern", "residue", "version")
ACCESS_TYPES = ("M3Access", "M3ByteArrayAccess", "M3ByteBufferAccess", "M3CharSequenceAccess")
FACADE_TYPES = ("M3LongHashFunction", "M3LongTupleHashFunction", "M3DualHashFunction")
KERNEL_TYPES = {"M3Maths": "KERNEL_MATH", "M3MathsJDK9": "KERNEL_MATH", "M3Primitives": "KERNEL_PRIMITIVE",
                "M3Util": "KERNEL_CHECK"}
ALGORITHM_TYPES = ("M3XxHash", "M3XXH3", "M3WyHash", "M3MurmurHash_3", "M3CityAndFarmHash_1_1", "M3MetroHash")
SETUP_MEMBERS = {"<init>", "<clinit>", "readResolve", "finalize", "seed", "bitsLength", "newResultArray"}
FACTORIES = {"xx", "xx3", "xx128low", "xx128", "wy_3", "murmur_3", "city_1_1", "farmNa", "farmUo", "metro"}
WIDTHS = (("Long", 8), ("i64", 8), ("Int", 4), ("u32", 4), ("i32", 4), ("Short", 2), ("u16", 2), ("i16", 2),
          ("Byte", 1), ("u8", 1), ("i8", 1))
SHORT_READS = ("i64", "u32", "i32", "u16", "i16", "u8", "i8")
PRIMITIVES = re.compile(r"^(dual)?[hH]ash(Long|Int|Short|Char|Byte|Void|NativeLong)$")
XXH3_CLASS = re.compile(r"^XXH3_len_(\d+)to(\d+)_(64b|128b)$")
LENGTH_CLASSES = {"hash1To3Bytes": "1..3", "hash4To7Bytes": "4..7", "hash8To16Bytes": "8..16",
                  "hashLen17To32": "17..32", "cityHashLen33To64": "33..64", "naHashLen33To64": "33..64"}
DISPATCHERS = {"XXH3_64bits_internal": "XXH3_len_0to16_64b|XXH3_len_17to128_64b|XXH3_len_129to240_64b|XXH3_hashLong",
               "XXH3_128bits_internal": "XXH3_len_0to16_128b|XXH3_len_17to128_128b|XXH3_len_129to240_128b|XXH3_hashLong",
               "XXH3_len_0to16_64b": "XXH3_len_9to16_64b|XXH3_len_4to8_64b|XXH3_len_1to3_64b|length 0",
               "XXH3_len_0to16_128b": "XXH3_len_9to16_128b|XXH3_len_4to8_128b|XXH3_len_1to3_128b|length 0",
               "hashLen0To16": "hash8To16Bytes|hash4To7Bytes|hash1To3Bytes|length 0"}
STREAMS = {"XXH3_hashLong": "1024-byte blocks of 64-byte stripes", "xxHash64": "32-byte stripes",
           "wyHash64": "48-byte rounds", "metroHash64": "32-byte rounds", "hash": "16-byte blocks",
           "cityHash64": "64-byte chunks after the length classes", "naHash64": "64-byte chunks after the length classes",
           "naHash64WithSeeds": "via naHash64", "uoHash64WithSeeds": "64-byte chunks after the length classes"}
MIXES = {"XXH64_avalanche", "XXH3_avalanche", "XXH3_rrmxmx", "XXH3_mix16B", "XXH128_mix32B_once", "XXH3_mix2Accs",
         "XXH3_mergeAccs", "fmix64", "mixK1", "mixK2", "shiftMix", "hashLen16", "mul", "uoH", "_wymum", "_wyr3",
         "u64Rorate32"}
OWNERS = {"XXH3_hashLong": "OWNER(XXH3_hashLong_64b, low half of XXH3_hashLong_128b; M3HashJitAtomsTest.xxh3HashLong)",
          "XXH3_mergeAccs": "OWNER(the three XXH3_mergeAccs folds; M3HashJitAtomsTest.xxh3MergeAccs)"}
ALG_SETUP = re.compile(r"^(as\w+|naWith\w*|uoWith\w*|XXH3_initCustomSecret)$")
BACKING = re.compile(r"\b(ByteArrayLittleEndian\.get(?:Long|Int|Short)|UNSAFE\.get(?:Long|Int|Short)Unaligned"
                     r"|input\.charAt|input\.get(?:Long|Int|Short)?)\(|\binput\[\(int\) offset\]")
SHORT_CALL = re.compile(r"\b(\w+)\.(" + "|".join(SHORT_READS) + r")\(")


def width(member):
    return next((w for k, w in WIDTHS if k in member), 0)


def access_atom(owner, member):
    if member in ("byteOrder", "reverseAccess", "newDefaultReverseAccess"):
        return "ACCESS_ORDER", "NONE"
    if member.startswith("get") or member in SHORT_READS:
        unchecked = owner == "M3ByteArrayAccess" and member in SHORT_READS[:5]
        check = "CHECK(none; callers check [off, off + len))" if unchecked else "CHECK(carrier)"
        return "ACCESS_READ", f"WIDTH({width(member)}); {check}"
    if member in ("ix", "address"):
        return "ACCESS_READ", "OFFSET(byte offset to carrier index)"
    if member in ("toByteBuffer", "toNativeCharSequence", "toCharSequence", "charSequenceAccess",
                  "nativeCharSequenceAccess"):
        return "ACCESS_SETUP", "NONE"
    return None


def facade_atom(owner, member):
    if member in FACTORIES:
        return "FACADE_FACTORY", "NONE"
    if owner.startswith("M3DualHashFunction"):
        return "FACADE_ADAPTER", "NONE"
    if member == "unsafeHash":
        return "FACADE_ENTRY", "CHECK(Objects.checkFromIndexSize: the byte[] entry of the unchecked reads)"
    if member.startswith("hash"):
        return "FACADE_ENTRY", "NONE"
    return None


def algorithm_atom(owner, member):
    m = XXH3_CLASS.match(member)
    if member in DISPATCHERS:
        return "ALG_LENGTH_DISPATCH", f"DISPATCH({DISPATCHERS[member]})"
    if m:
        return "ALG_LENGTH_CLASS", f"LENGTH({m.group(1)}..{m.group(2)}); WIDTH({m.group(3)})"
    if member in LENGTH_CLASSES:
        return "ALG_LENGTH_CLASS", f"LENGTH({LENGTH_CLASSES[member]})"
    if member in STREAMS and not (member == "hash" and owner != "M3MurmurHash_3"):
        return "ALG_STREAM", f"STREAM({STREAMS[member]})" + ("; " + OWNERS[member] if member in OWNERS else "")
    if member in MIXES:
        return "ALG_MIX", OWNERS.get(member, "NONE")
    if PRIMITIVES.match(member):
        return "ALG_PRIMITIVE", "NONE"
    if member in ("hash", "dualHash"):
        return "ALG_ADAPT", "NONE"
    if ALG_SETUP.match(member):
        return "ALG_SETUP", "NONE"
    return None


def classify(cls, member):
    owner = cls.split("$")[0]
    if member in SETUP_MEMBERS:
        role = ("ACCESS" if owner in ACCESS_TYPES else "FACADE" if owner in FACADE_TYPES
                else "KERNEL" if owner in KERNEL_TYPES else "ALGORITHM")
        return role, {"ACCESS": "ACCESS_SETUP", "FACADE": "FACADE_SETUP", "KERNEL": "KERNEL_SETUP",
                      "ALGORITHM": "ALG_SETUP"}[role], "NONE"
    if owner in ACCESS_TYPES:
        hit = access_atom(owner, member)
        return ("ACCESS",) + hit if hit else None
    if owner in FACADE_TYPES:
        hit = facade_atom(cls, member)
        return ("FACADE",) + hit if hit else None
    if owner in KERNEL_TYPES:
        return "KERNEL", KERNEL_TYPES[owner], "NONE"
    if owner in ALGORITHM_TYPES:
        hit = algorithm_atom(owner, member)
        return ("ALGORITHM",) + hit if hit else None
    return None


def masked(s):
    return MASK.sub(lambda m: "".join(c if c == NL else " " for c in m.group(0)), s)


def call_args(text, start):
    depth, i = 1, start
    while depth:
        depth += {"(": 1, ")": -1}.get(text[i], 0)
        i += 1
    return text[start:i - 1]


def offset_form(arg):
    expr = arg.split(",", 1)[1].strip() if "," in arg else ""
    if re.fullmatch(r"[\d\s+*()]*(\+?\s*BYTE_BASE)?[\d\s+*()]*|BYTE_BASE[\d\s+*()]*", expr):
        return "OFFSET(constant)"
    if "length" in expr or "len" in re.findall(r"\b\w+\b", expr):
        return "OFFSET(end-relative)"
    if re.search(r"\b(i|s|n|offStripe|offBlock|offSec)\b", expr):
        return "OFFSET(loop)"
    if re.fullmatch(r"(off|offset|offIn)(\s*\+\s*[\w\d\s*+()]+)?", expr):
        return "OFFSET(base-relative)"
    return "OFFSET(computed)"


def read_sites(src, spans):
    rows = []
    for name in sorted(os.listdir(src)):
        if not name.endswith(".java"):
            continue
        text = masked(open(os.path.join(src, name), encoding="utf-8").read())
        owner = name[:-len(".java")]
        for m in list(SHORT_CALL.finditer(text)) + list(BACKING.finditer(text)):
            line = text.count(NL, 0, m.start()) + 1
            member = next((s[2] for s in sorted(spans.get(name, []), key=lambda s: s[1] - s[0])
                           if s[0] <= line <= s[1]), "?")
            if m.re is SHORT_CALL:
                if owner in ACCESS_TYPES:
                    continue  # the access template's own delegation, not a read site of an algorithm
                args = call_args(text, m.end())
                carrier = "KEY" if m.group(1) == "unsafeLE" else "INPUT"
                pattern = "READ_" + carrier
                residue = f"WIDTH({width(m.group(2))}); {offset_form(args)}; CARRIER({m.group(1)})"
            else:
                pattern = "READ_BACKING"
                residue = f"CARRIER({m.group(1) or 'input[(int) offset]'}); WIDTH({width(m.group(0)) or 1})"
            rows.append(("READ_SITE", name, str(line), owner, member, "ACCESS" if pattern == "READ_BACKING" else
                         "ALGORITHM", pattern, residue, VERSION))
    return rows


def main():
    src, sizes, out = sys.argv[1:4]
    atoms, spans, unknown = [], {}, []
    with open(sizes, encoding="utf-8") as f:
        for r in csv.DictReader(f, delimiter=TAB):
            spans.setdefault(r["source_file"], []).append((int(r["start_line"]), int(r["end_line"]), r["method"]))
            if "bridge" in r["flags"]:
                continue
            hit = classify(r["class"], r["method"])
            if hit is None:
                unknown.append(r["class"] + "." + r["method"])
                hit = ("?", "UNKNOWN_PATTERN", "NONE")
            atoms.append(("ATOM", r["source_file"], r["start_line"], r["class"], r["method"]) + hit + (VERSION,))
    rows = atoms + read_sites(src, spans)
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write(TAB.join(HEADER) + NL + "".join(TAB.join(r) + NL for r in rows))
    counts = {}
    for r in rows:
        counts[(r[0], r[6])] = counts.get((r[0], r[6]), 0) + 1
    print(f"atoms={len(atoms)} read_sites={len(rows) - len(atoms)} unknown={len(unknown)}")
    for (kind, pattern), n in sorted(counts.items()):
        print(f"  {kind}\t{pattern}\t{n}")
    if unknown:
        print("UNKNOWN_PATTERN: " + ", ".join(unknown))
    sys.exit(1 if unknown else 0)


main()

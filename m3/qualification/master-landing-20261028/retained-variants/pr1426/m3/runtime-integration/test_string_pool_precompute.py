#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Mutation tests of the actual pool section of check-m3string-invariants.py.

The source gate remains the authority. AST extraction executes its complete existing
native-owner lifecycle section and its prepared-metadata section without invoking
unrelated source-tree checks. No Java implementation is substituted or executed.
"""
import ast
import os
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
CHECKER = Path(os.environ.get("M3_POOL_CHECKER", str(
    ROOT / "m3/runtime-integration/check-m3string-invariants.py")))
POOL = Path(os.environ.get("M3_POOL_FIXTURE", str(
    ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java")))


def extract_gate():
    tree = ast.parse(CHECKER.read_text(encoding="utf-8"), filename=str(CHECKER))
    mask = [node for node in tree.body
            if isinstance(node, ast.FunctionDef) and node.name == "java_mask_non_code"]
    starts = [index for index, node in enumerate(tree.body)
              if isinstance(node, ast.Assign) and any(
                  isinstance(target, ast.Name) and target.id == "pool_code"
                  for target in node.targets)]
    ends = [index for index, node in enumerate(tree.body)
            if isinstance(node, ast.For) and isinstance(node.target, ast.Name)
            and node.target.id == "required_mechanical_name_mapping"]
    if len(mask) != 1 or len(starts) != 1 or len(ends) != 1 or starts[0] >= ends[0]:
        raise AssertionError("actual source gate extraction boundary drift")
    section = tree.body[starts[0]:ends[0]]
    if not any(isinstance(node, ast.FunctionDef) and node.name == "check_pool_precompute"
               for node in section):
        raise AssertionError("actual prepared pool gate missing from extracted section")
    return compile(ast.Module(body=mask + section, type_ignores=[]), str(CHECKER), "exec")


GATE = extract_gate()


def check_source(source):
    def fail(message):
        raise AssertionError(message)
    exec(GATE, {"pool": source, "re": re, "fail": fail})


def replace_one(source, old, new):
    if old not in source:
        raise AssertionError("mutation source anchor drift: " + old)
    return source.replace(old, new, 1)


class PoolPrecomputeMutationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = POOL.read_text(encoding="utf-8")

    def test_actual_candidate_accepted(self):
        check_source(self.source)

    def test_mechanical_contract_mutations_rejected(self):
        mutations = [
            ("retired bucket retry", "if (bucket.retired) continue;", ""),
            ("local retirement", "retireEmptyLocalBucket(fingerprint, bucket);", ""),
            ("tuple retirement", "retireEmptyTupleBucket(routeKey, bucket);", ""),
            ("local bucket identity", "LOCAL.remove(fingerprint, bucket)", "LOCAL.remove(fingerprint)"),
            ("tuple bucket identity", "TUPLES.remove(routeKey, bucket)", "TUPLES.remove(routeKey)"),
            ("retirement emptiness", "bucket.values.isEmpty() && LOCAL.remove", "LOCAL.remove"),
            ("retirement monitor", "synchronized (bucket) {", "synchronized (LOCAL) {"),
            ("tuple cleanup allocation", "retireEmptyTupleBucket(Long routeKey,", "retireEmptyTupleBucket(long routeKey,"),
            ("weak local ownership", "extends WeakReference<M3StringAtom>", "extends SoftReference<M3StringAtom>"),
            ("native once guard", "if (!released) {", "if (true) {"),
            ("native accounting", "release(retainedBytes);", "release(0L);"),
            ("release comment spoof", "reference.releaseNative();", "/* reference.releaseNative(); */"),
            ("coder compact strings", "return String.COMPACT_STRINGS && (geometry", "return true && (geometry"),
            ("coder Latin-1 boundary", "latin1 &= unit <= 0xff;", "latin1 &= unit <= 0xffff;"),
            ("cold coder endian", "? (char) ((first << 8) | second)", "? (char) (first | (second << 8))"),
            ("prepared coder endian", "char unit = bigEndian\n                        ? (char) ((first << 8) | second)",
             "char unit = bigEndian\n                        ? (char) (first | (second << 8))"),
            ("slot cap", "MAX_EXACT_INDEX_SLOTS = 1 << 22", "MAX_EXACT_INDEX_SLOTS = 1 << 30"),
            ("probe cap", "MAX_EXACT_PROBES = 64", "MAX_EXACT_PROBES = Integer.MAX_VALUE"),
            ("bounded construction", "while (probe < MAX_EXACT_PROBES && rows[slot] != 0)", "while (rows[slot] != 0)"),
            ("optional slot allocation", "rows = new int[capacity];\n            } catch (OutOfMemoryError unavailable) {\n                return NO_EXACT_INDEX;",
             "rows = new int[capacity];\n            } catch (OutOfMemoryError unavailable) {\n                throw unavailable;"),
            ("optional coder allocation", "return new byte[count];\n            } catch (OutOfMemoryError unavailable) {\n                return null;",
             "return new byte[count];\n            } catch (OutOfMemoryError unavailable) {\n                throw unavailable;"),
            ("hash is candidate", "if (equalsChars(value, offset, logicalLength, offsets[row])) return row;", "return row;"),
            ("compact exact comparison", "if (unit != mappedUnit(mappedOffset, index)) return false;", "if (false) return false;"),
            ("ambiguous binary fallback", "if (encoded < 0) return findUnitBinary(unit);", ""),
            ("ambiguous bit decode", "int row = (encoded & Integer.MAX_VALUE) - 1;", "int row = encoded - 1;"),
            ("ambiguous insertion", "rows[slot] |= Integer.MIN_VALUE;", ""),
            ("shared stable record offset", "ids[i]=r[3]&Integer.MAX_VALUE;", "ids[i]=r[3];"),
            ("mandatory atoms before metadata", "M3StringAtom[] atoms = new M3StringAtom[count];\n            byte[] coders = allocateCoders(count);",
             "byte[] coders = allocateCoders(count);\n            M3StringAtom[] atoms = new M3StringAtom[count];"),
            ("reverse prepared coder", "? coders[row]", "? String.LATIN1"),
        ]
        for label, old, new in mutations:
            with self.subTest(contract=label):
                mutated = replace_one(self.source, old, new)
                with self.assertRaises(AssertionError, msg="source gate accepted " + label):
                    check_source(mutated)


if __name__ == "__main__":
    unittest.main()

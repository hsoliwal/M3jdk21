<<<<<<< HEAD
#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Mutation tests of the actual Pool and frozen-index source gate.

Retain all 29 existing Pool semantic refusals across the actual split owners.
Additional cases bind the new displacement owner and fixed-address admission.
AST extraction executes the complete actual lifecycle/precompute section; no
Java runtime implementation is substituted or executed.
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
FROZEN_INDEX = Path(os.environ.get("M3_FROZEN_INDEX_FIXTURE", str(
    ROOT / "src/java.base/share/classes/java/lang/M3FrozenLexiconIndex.java")))


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
    definitions = [node for node in section if isinstance(node, ast.FunctionDef)
                   and node.name == "check_pool_precompute"]
    calls = [node.value for node in section if isinstance(node, ast.Expr)
             and isinstance(node.value, ast.Call) and isinstance(node.value.func, ast.Name)
             and node.value.func.id == "check_pool_precompute"]
    if (len(definitions) != 1 or len(calls) != 1 or calls[0].keywords
            or [ast.dump(arg) for arg in calls[0].args]
            != [ast.dump(ast.Name(id=name, ctx=ast.Load())) for name in ("pool", "frozen_index")]):
        raise AssertionError("actual Pool/index source gate owner binding drift")
    return compile(ast.Module(body=mask + section, type_ignores=[]), str(CHECKER), "exec")


GATE = extract_gate()


def check_sources(sources):
    def fail(message):
        raise AssertionError(message)
    exec(GATE, {"pool": sources["pool"], "frozen_index": sources["frozen_index"],
                "re": re, "fail": fail})


def replace_one(source, old, new):
    if old not in source:
        raise AssertionError("mutation source anchor drift: " + old)
    return source.replace(old, new, 1)


MUTATIONS = [('retired bucket retry', 'pool', 'if (bucket.retired) continue;', ''),
 ('local retirement', 'pool', 'retireEmptyLocalBucket(fingerprint, bucket);', ''),
 ('tuple retirement', 'pool', 'retireEmptyTupleBucket(routeKey, bucket);', ''),
 ('local bucket identity', 'pool', 'LOCAL.remove(fingerprint, bucket)', 'LOCAL.remove(fingerprint)'),
 ('tuple bucket identity', 'pool', 'TUPLES.remove(routeKey, bucket)', 'TUPLES.remove(routeKey)'),
 ('retirement emptiness', 'pool', 'bucket.values.isEmpty() && LOCAL.remove', 'LOCAL.remove'),
 ('retirement monitor', 'pool', 'synchronized (bucket) {', 'synchronized (LOCAL) {'),
 ('tuple cleanup allocation',
  'pool',
  'retireEmptyTupleBucket(Long routeKey,',
  'retireEmptyTupleBucket(long routeKey,'),
 ('weak local ownership',
  'pool',
  'extends WeakReference<M3StringAtom>',
  'extends SoftReference<M3StringAtom>'),
 ('native once guard', 'pool', 'if (!released) {', 'if (true) {'),
 ('native accounting', 'pool', 'release(retainedBytes);', 'release(0L);'),
 ('release comment spoof', 'pool', 'reference.releaseNative();', '/* reference.releaseNative(); */'),
 ('coder compact strings', 'pool', 'return String.COMPACT_STRINGS && (geometry', 'return true && (geometry'),
 ('coder Latin-1 boundary', 'pool', 'latin1 &= unit <= 0xff;', 'latin1 &= unit <= 0xffff;'),
 ('cold coder endian', 'pool', '? (char) ((first << 8) | second)', '? (char) (first | (second << 8))'),
 ('prepared coder endian',
  'pool',
  'char unit = bigEndian\n                        ? (char) ((first << 8) | second)',
  'char unit = bigEndian\n                        ? (char) (first | (second << 8))'),
 ('slot cap', 'frozen_index', 'MAX_SLOTS = 1 << 22', 'MAX_SLOTS = 1 << 30'),
 ('probe cap',
  'frozen_index',
  'MAX_DISPLACEMENT_ATTEMPTS = 1 << 20',
  'MAX_DISPLACEMENT_ATTEMPTS = Integer.MAX_VALUE'),
 ('bounded construction', 'frozen_index', 'seedIndex < MAX_SEEDS', 'true'),
 ('optional slot allocation',
  'frozen_index',
  '} catch (ArithmeticException | OutOfMemoryError unavailable) {\n            return EMPTY;',
  '} catch (ArithmeticException | OutOfMemoryError unavailable) {\n            throw unavailable;'),
 ('optional coder allocation',
  'pool',
  'return new byte[count];\n'
  '            } catch (OutOfMemoryError unavailable) {\n'
  '                return null;',
  'return new byte[count];\n'
  '            } catch (OutOfMemoryError unavailable) {\n'
  '                throw unavailable;'),
 ('hash is candidate',
  'pool',
  'return equalsChars(value, offset, logicalLength, offsets[row]) ? row : -1;',
  'return row;'),
 ('compact exact comparison',
  'pool',
  'if (unit != mappedUnit(mappedOffset, index)) return false;',
  'if (false) return false;'),
 ('ambiguous binary fallback', 'pool', 'if (encoded < 0) return findUnitBinary(unit);', ''),
 ('ambiguous bit decode', 'pool', 'int row = (encoded & Integer.MAX_VALUE) - 1;', 'int row = encoded - 1;'),
 ('ambiguous insertion', 'frozen_index', 'tableRows[slot] |= Integer.MIN_VALUE;', ''),
 ('shared stable record offset', 'pool', 'ids[i]=r[3]&Integer.MAX_VALUE;', 'ids[i]=r[3];'),
 ('mandatory atoms before metadata',
  'pool',
  'M3StringAtom[] atoms = new M3StringAtom[count];\n            byte[] coders = allocateCoders(count);',
  'byte[] coders = allocateCoders(count);\n            M3StringAtom[] atoms = new M3StringAtom[count];'),
 ('reverse prepared coder', 'pool', '? coders[row]', '? String.LATIN1')]

ADDITIONAL_MUTATIONS = [('frozen owner declaration', 'frozen_index', 'final class M3FrozenLexiconIndex', 'final class OtherIndex'),
 ('immutable displacement lane',
  'frozen_index',
  'private final int[] displacements;',
  'private int[] displacements;'),
 ('immutable row lane', 'frozen_index', 'private final int[] rows;', 'private int[] rows;'),
 ('length lane validation', 'frozen_index', 'if (hashes.length != lengths.length)', 'if (false)'),
 ('half-full grouping capacity', 'frozen_index', 'Math.multiplyExact(hashes.length, 2)', 'hashes.length'),
 ('admission seed cap', 'frozen_index', 'MAX_SEEDS = 64', 'MAX_SEEDS = Integer.MAX_VALUE'),
 ('admission displacement bound', 'frozen_index', 'displacement < attempts', 'true'),
 ('complete hash-length grouping key',
  'frozen_index',
  'key(hashes[row], lengths[row])',
  'key(hashes[row], 0)'),
 ('nonzero candidate encoding', 'frozen_index', 'tableRows[slot] = row + 1;', 'tableRows[slot] = row;'),
 ('candidate empty-index miss',
  'frozen_index',
  'if (rows.length == 0) return 0;',
  'if (rows.length == 0) return 1;'),
 ('candidate seed binding', 'frozen_index', 'mix64(key(hash, length) ^ seed)', 'mix64(key(hash, length))'),
 ('candidate displacement binding', 'frozen_index', 'displacements[bucket]', 'displacements[0]'),
 ('candidate row mask', 'frozen_index', '& (rows.length - 1)', '& rows.length'),
 ('candidate exact row selection', 'frozen_index', 'return rows[slot];', 'return rows[bucket];'),
 ('same-bucket base alias rejection',
  'frozen_index',
  'if (hasAliasedBaseSlots(offsets, bucketGroups, baseSlotByGroup)) return null;',
  ''),
 ('placement collision rejection', 'frozen_index', 'if (rows[slot] != 0)', 'if (false)'),
 ('ambiguity survives placement',
  'frozen_index',
  'rows[candidates[ordinal]] = groups.rows[group];',
  'rows[candidates[ordinal]] = groups.rows[group] & Integer.MAX_VALUE;'),
 ('failed seed refuses publication', 'frozen_index', 'if (!placed) return null;', ''),
 ('unsigned Java hash packing',
  'frozen_index',
  'Integer.toUnsignedLong(hash) << Integer.SIZE',
  '(long) hash'),
 ('UTF-16 length packing', 'frozen_index', '| Integer.toUnsignedLong(length)', '| 0L'),
 ('index owner publication',
  'pool',
  'this.exactIndex = M3FrozenLexiconIndex.build(javaHashes, lengths);',
  'this.exactIndex = M3FrozenLexiconIndex.empty();'),
 ('published row lane identity',
  'pool',
  'this.exactRows = exactIndex.rows();',
  'this.exactRows = new int[0];'),
 ('fixed admission needs complete index',
  'pool',
  'if (result.offsets.length != 0 && !result.exactIndex.available())',
  'if (false)'),
 ('fixed unpublished mapping cleanup',
  'pool',
  'if (ownsMapping && !published) unmapLexicon0(address, raw.capacity());',
  ''),
 ('fixed exact mapping address', 'pool', '|| ((DirectBuffer) raw).address() != address', ''),
 ('fixed complete image pin',
  'pool',
  'if (!MessageDigest.isEqual(expected, digestFullImage(mapped)))',
  'if (false)'),
 ('char candidate complete key verification',
  'pool',
  'if (javaHashes[row] != hash || lengths[row] != logicalLength) return -1;',
  ''),
 ('Latin-1 source range binding',
  'pool',
  'return findCompactBytes(value, offset, logicalLength, String.LATIN1);',
  'return findCompactBytes(value, 0, logicalLength, String.LATIN1);')]


class PoolPrecomputeMutationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sources = {"pool": POOL.read_text(encoding="utf-8"),
                       "frozen_index": FROZEN_INDEX.read_text(encoding="utf-8")}

    def test_actual_candidate_accepted(self):
        check_sources(self.sources)

    def check_mutations(self, cases):
        for label, owner, old, new in cases:
            with self.subTest(contract=label, owner=owner):
                mutated = dict(self.sources)
                mutated[owner] = replace_one(mutated[owner], old, new)
                with self.assertRaises(AssertionError, msg="source gate accepted " + label):
                    check_sources(mutated)

    def test_mechanical_contract_mutations_rejected(self):
        self.assertEqual(len(MUTATIONS), 29)
        self.check_mutations(MUTATIONS)
        print("RETAINED_POOL_MUTATIONS_REFUSED=29")

    def test_split_owner_contract_mutations_rejected(self):
        self.assertEqual(len(ADDITIONAL_MUTATIONS), 28)
        self.check_mutations(ADDITIONAL_MUTATIONS)
        print("FROZEN_INDEX_ADDITIONAL_MUTATIONS_REFUSED=28")


if __name__ == "__main__":
    unittest.main(verbosity=2)
||||||| merged common ancestors
=======
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
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf

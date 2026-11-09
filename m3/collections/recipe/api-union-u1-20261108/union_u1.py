# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""union_u1.py <m3 tree root>   API_UNION U1 (user decision 2026-11-08: admit the LongLongSortedMap union, +14 classes).

The Synexia LongLongSortedMap contract declares int filterInPlace(LongLongPredicate) (retain the entries the predicate
accepts, return the number removed); the one owner jdk.m3.collections.LongLongSortedMap lacks it, which holds 14 Synexia
classes. U1 grows the owner, no second class:
  1. jdk.internal.m3.collections.LongLongPredicate becomes public jdk.m3.collections.LongLongPredicate (beside its twin
     LongLongConsumer) - done by curate_rename_e.py with the one-row map U1_MAP.tsv (CURATE_CORE=jdk.internal.m3.collections);
  2. LongLongSortedMap gains filterInPlace as a default method over its own contract (keysToArray / valuesToArray /
     remove), the JDK way of growing a public interface (Collection.removeIf): no implementer breaks; Synexia
     implementations absorbed later override it with their packed versions.
Step 2 is an exact, idempotent edit (refuses when the anchor is missing)."""
import os
import sys

root = sys.argv[1]
p = os.path.join(root, "m3", "collections", "src", "main", "java", "jdk", "m3", "collections", "LongLongSortedMap.java")
t = open(p, encoding="utf-8", newline="").read()
if "int filterInPlace(LongLongPredicate predicate)" in t:
    print("U1 already applied"); sys.exit(0)
nl = "\r\n" if "\r\n" in t else "\n"
a = "package jdk.m3.collections;" + nl + nl
b = ("package jdk.m3.collections;" + nl + nl + "import java.util.Objects;" + nl + "import java.util.stream.IntStream;" + nl + nl)
anchor = "    long higherKey(long key, long defaultValue);" + nl + nl
add = nl.join([
    "    /**",
    "     * Removes every entry the predicate rejects, keeping the entries it accepts.",
    "     *",
    "     * <p>The default works over this contract (a key/value snapshot, then {@link #remove(long)}); implementations with",
    "     * packed storage override it with an in-place compaction.</p>",
    "     *",
    "     * @param predicate retention test over a key and its value",
    "     * @return the number of entries removed",
    "     * @throws NullPointerException if {@code predicate} is null",
    "     */",
    "    default int filterInPlace(LongLongPredicate predicate) {",
    "        Objects.requireNonNull(predicate, \"predicate\");",
    "        long[] keys = keysToArray();",
    "        long[] values = valuesToArray();",
    "        int[] drop = IntStream.range(0, keys.length).filter(i -> !predicate.test(keys[i], values[i])).toArray();",
    "        IntStream.of(drop).forEach(i -> remove(keys[i]));",
    "        return drop.length;",
    "    }",
    "", ""])
if t.count(a) != 1 or t.count(anchor) != 1:
    raise SystemExit("U1 anchors not found exactly once")
t = t.replace(a, b, 1).replace(anchor, anchor + add, 1)
open(p, "w", encoding="utf-8", newline="").write(t)
print("U1 applied: LongLongSortedMap.filterInPlace (default)")

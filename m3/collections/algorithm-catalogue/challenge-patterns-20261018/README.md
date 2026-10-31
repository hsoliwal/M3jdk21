# Challenge patterns for M3 collections

This catalogue maps **14 official problem pages across LeetCode and HackerRank** to collection algorithm categories. See [primary sources and applicability](PRIMARY_SOURCES.tsv) and the [FOSS reuse ledger](FOSS_REUSE_DECISION.tsv). This is source research only. No solution, editorial or submission code was copied; the challenge pages are references for ideas, not implementation-code donors.

- **Balanced build and AVL:** LC108/1382 and HackerRank Self Balancing Tree. The existing [M3TreeMap implementation](https://github.com/hsoliwal/M3jdk21/blob/f19934e045f7e0f896704940a786106153ad4dc2/m3/collections/src/main/java/com/m3/collections/M3TreeMap.java) already supplies an O(n) sorted builder using owned arrays and indexed links, with the attributed fastutil shape atom. Reuse it rather than add another tree framework.
- **Bitmap and indexed state:** LC2166/338 and HackerRank Java BitSet/Flipping Bits suggest mask, cardinality and fixed-width boundary cases. They do not supply a complete slot rank/select index or collection lifecycle contract.
- **Compaction and ranges:** LC26/283/34/88 and HackerRank Array Manipulation distinguish stable filtering, boundary search, merging and offline numeric updates. A logarithmic query frontier does not eliminate worst-case linear state shifting; writing k output elements still requires k writes.
- **Indexed sequences and counts:** HackerRank Dynamic Array/Sparse Arrays provide additional offset/index and counted-query categories.

Every implementation candidate needs an immutable licensed donor, functional proof against the required contracts, and comparable before/after measurements. Array examples alone do not establish collection view, Entry, callback, exception or mutation semantics. Complexity bounds in the table distinguish explicit challenge requirements from our analysis; none is a measured M3 speedup.

The pinned [lookup qualification](https://github.com/hsoliwal/M3jdk21/blob/f19934e045f7e0f896704940a786106153ad4dc2/m3/collections/qualification/tree-lookup-validation-20261018/README.md) preserves the prior inconclusive churn comparison. This catalogue makes no peak-performance or implementation-admission claim.

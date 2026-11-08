# Forward port: com.synexia m3-fast-search changes after the 2026-11-06 receipt

M3jdk21 received `m3/search` from com.synexia on 2026-11-06 (#2493/#2495 and the BMH cancellation receipt). Every
received file names its donor blob on a provenance line. com.synexia kept changing `m3-fast-search` afterwards,
through 2026-11-07. `fwd_search.py` (M3_SEARCH_FORWARD v1) carries those changes over.

## Method

For each received file whose donor blob differs from develop `06082e32476e`:

- **base:** the blob named by master's provenance line, relocated by the recipe that produced master (see below);
- **theirs:** develop's blob, relocated the same way;
- **ours:** master's file.

The recipe applies, in order:

1. `rules_forward.tsv`, one new row placed first;
2. `rules_v1b.tsv` and `rules_v2.tsv` from `../relocation-20261106-*`;
3. the M3<Type> renames from `LEDGERS_JOINED.tsv`;
4. JNI symbol renames;
5. the JDK-naming rewrite;
6. the provenance line.

For all 8 changed files (6 Java, 2 C), the relocated base equals master byte for byte. So the recipe is exact,
and develop's version is taken whole. 13 develop tests absent from master were relocated the same way. Ten are
received; the other three are recorded in `DECISION.tsv`. The new rule maps the fully qualified
`com.synexia.job.ProgressSnapshot snapshot()` before the generic `ProgressSnapshot snapshot()` literal.

## What changed

- ByteBuffer count-only scans for BMH and KMP (one automaton, the same cancellation checkpoints as visits).
- The BMH ByteBuffer KMP fallback keeps the caller's cancellation monitor.
- `ByteOccurrences` buffer counting goes through those scans.
- Minimum subsequence window: specialised one-unit, two-unit and many-unit Java loops, plus the matching C17 path.
- Batch subsequence: impossible patterns are pruned, and JNI traversal stops once exhausted.

## Gates

`GATES.tsv`. Runner: `gate.sh <tree> <out>` (Windows JDK 21, MinGW; the com.m3 dependency jars and JUnit console 1.11.4
from the local Maven repositories). `gate_native.sh` builds the seven `CMakeLists.txt` libraries.

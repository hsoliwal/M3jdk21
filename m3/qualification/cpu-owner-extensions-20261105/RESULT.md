# cpu-tools / text-tools owner extensions (census2 P2-002..015, P2-038..040, P2-071), 2026-11-06

Source: com.synexia synexia-openrewrite-recipes/recipe-crates/m3-cpu-owner-extensions-20261105. It is a stack of 8 hash-pinned sub-crates sealed before the JDK naming rename fe35ea98841, so the manifests pin pre-rename paths and bytes.

HASH_PINNED_CHAIN v2 (crate_chain2.py) resolves every target as follows:
1. The start is the target's bytes at the rename parent 06495fea4157. That start must be a pinned preimage.
2. Follow the pinned pre->post edges. The final dataset bytes are checked against their pinned sha256.
3. jdk_naming.rewrite_text is applied to both the start and the final, and jdk_naming.target_path gives the master path.
4. Verdicts:
   - APPLY when master == named(start), i.e. no drift since the rename;
   - NEW when the target is absent on both sides;
   - otherwise a 3-way merge (named_apply.py).

Per-target verdicts are in VERDICTS.tsv. fork_check.py resolved the forks:
- Intervals and M3ArrayGraph: two byte-identical postimages.
- ExactSearch: the continuation chain java-jni-continuation > batch-search > prefix-profile. It holds every code line of the top-level variant, which differs only by 12 Javadoc lines.

m3textio.c had drifted on master. It merges with a single add/add hunk at end of file, resolved by union: master's UTF-8 line kernel, then the crate's three #includes of the new headers. The new headers export exactly the JNI symbols declared by ExactSearch (summaryNative0, summaryRangesNative0, prefixProfileNative0) and EditDistance (withinNative0) after renaming.

m3/arrays M3Arrays (P2-001) is excluded here and handed to com-synexia-0e.

## Gate

coe_gate.sh mirrors cpu-tools/build.py on stock JDK 21.0.9 + MinGW gcc, on master 33ada7681786. The packs are unchanged up to 99ca5db70d8f.

| step | live | leaf |
| --- | --- | --- |
| modules javac -Xlint:all,-module -Werror, text-tools/src;cpu-tools/src (java.base Progress* + jdk.compiler M3 owners patched) | 0 errors, 0 warnings | 0 errors, 0 warnings |
| gcc -std=c11 -Wall -Wextra -Werror m3textio.c (with the new headers), m3textops.c | 0 diagnostics | 0 diagnostics |
| build.py FIXTURES, run with -Xcheck:jni | 15/16 | 15/16, same failing fixture |

The failing fixture on both sides is M3CpuToolsTest ("CLI explicit search truncation"), a pre-existing failure on Windows with an identical trace.

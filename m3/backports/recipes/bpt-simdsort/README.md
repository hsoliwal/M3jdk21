# BPT-SIMDSORT: SIMD sort and partition intrinsics for Arrays.sort (x86_64)

Donor: dragonwell-project/dragonwell21 aa23a662b35f, "[JIT] Backport libsimdsort for Arrays.sort" (Kuai Wei,
Alibaba). It covers:
- JDK-8309130: AVX512 sort for int, long, float and double arrays;
- JDK-8319577: AVX2 sort for int and float arrays;
- JDK-8317976: SIMD sort tuned for AMD Zen 4 (the AVX512 path is off on family 19h).

Its 12 native files equal openjdk/jdk master, so they include 8317763, 8321688, 8351233 and 8342807. See
FOSS_REUSE_DECISION.tsv.

## Apply (APPLY_RECEIPT.tsv)

The base is M3jdk21 master 16e35bee. The patch has 36 paths, +3890 / -506 lines.
- 9 of the 23 modified files are byte-equal to the donor's parent 2c3bc06d4269.
- 14 HotSpot files differ: M3 is at 21.0.14 and the donor carries its own VM changes. `git apply --3way`
  merged all 14 with **0 conflicts**.
- Static check: each new symbol (`_array_sort`, `_array_partition`, the `_arraySort` and `_arrayPartition`
  intrinsics, `CPU_FAMILY_AMD_17H/19H`) is defined exactly once.

## Proof status

| Check | State |
|-------|-------|
| javac `-Xlint:all -Werror` of the new DualPivotQuicksort, patched into java.base (JDK 21) | PASS |
| `Sorting` (jtreg test) on the Java fallback path, `--patch-module java.base` | PENDING: the session's auto-mode classifier blocked local execution of donor-derived code |
| Linux x86_64 image build: HotSpot + libsimdsort (`make images`) | PENDING: needs WSL, which the release lane is using during the RC freeze |
| `Sorting` with intrinsics (AVX2 / AVX512 host), `-XX:DisableIntrinsic=_arraySort,_arrayPartition` variant, and the C2 variant | PENDING |
| JMH `ArraysSort` against the stock 21.0.14 image | PENDING |

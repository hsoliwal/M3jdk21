# commons-codec computes CRC-32 and CRC-32C with java.base (2026-11-09, CHECKSUMS_TO_JDK v1)

User rules: M3JDK is one JDK, so M3 integrates in place and is never a side layer; and the JDK owns one fast
implementation that every consumer uses. java.base owns CRC-32 (`java.util.zip.CRC32`) and CRC-32C
(`java.util.zip.CRC32C`), and HotSpot compiles both to the processor's CRC instructions. commons-codec carries Hadoop's
table-driven copies of both (`digest.PureJavaCrc32` and `PureJavaCrc32C`, about 640 lines each), which were written
to beat Java 6's JNI CRC32. M3 has three copies of each: `m3/vendor/third_party/commons-codec-1.17.1`, `-1.22.1`, and
the relocated module `jdk.m3.codec` (`third_party-m3/relocated/commons-codec-1.22.1`), which ships in the JDK image.

## Change

- Recipe `make/m3/tooling/jdk-superset/hashing/tools/checksums_to_jdk.py` (re-runs to a fixed point; `--check` writes
  nothing). Both classes keep their public members (constructor, `getValue`, `reset`, both `update` methods, the
  `final` modifiers) and delegate to a `java.util.zip.CRC32` or `CRC32C` field. The lookup tables go, and the class
  Javadoc gains one paragraph naming java.base.
- The donors compile with `--release 8` (their poms), and `CRC32C` is Java 9 API. `PureJavaCrc32C` therefore gets its
  delegate through one `MethodHandle` to the `CRC32C` constructor, as Guava's `Hashing.crc32c()` and snappy-java's
  `SnappyFramed` do. Raising the release instead would change every class in the jar; this way the other classes stay
  byte-identical (see Maven below).
- Custody is the donors' overlay mechanism (FDP_DELEGATES_TO_JDK, JACKSON_NUMBERS_TO_JDK):
  - each upstream file stays byte-identical under `upstream-excluded/`, pinned in `PROVENANCE.json`;
  - `OVERLAY_SOURCES.json` lists the overlays;
  - `verify_source.py` re-derives each overlay through the recipe. The regenerated script also fixes the old
    template's two bugs: the repository root (the old script looked for `m3/vendor/m3/vendor/...`) and the missing
    `revision` key.
- Poms and READMEs: the name, description and README describe the overlay.
- `jdk.m3.codec` gets the same overlay through the lib-intake name mapping, and copies of `PROVENANCE.json` and
  `THIRD_PARTY_NOTICES.md`. The relocated file is still the mapped third_party file.

## Gates

| Gate | Result |
|---|---|
| recipe | apply, then `--check`: fixed point |
| `verify_source.py` | 1.17.1 and 1.22.1: PASS, 2 overlays each; PASS with each Maven-built jar |
| javac (`run_gate.sh`, `-Xlint:all`) | rc=0 and 0 errors for 1.17.1 (107 classes), 1.22.1 (143) and `jdk.m3.codec` (128), on master and on the branch. The table loops' `[fallthrough]` warnings go (60 -> 46, 67 -> 55, 69 -> 57), and none are added |
| differential (`CrcDifferential`, `GATE.txt`) | 3 seeds x 200,000 samples per build (1.17.1, 1.22.1, `jdk.m3.codec`). Each sample drives both classes and `java.util.zip.CRC32`/`CRC32C` through one random call script: `update(byte[], off, len)` in random chunks of inputs up to 70,000 bytes, `update(int)` with arbitrary ints, `reset`, `getValue`. A quarter of the samples also make one invalid `update(byte[], off, len)` call. The branch equals java.util.zip on every sample, and master equals the branch on every valid call |
| invalid calls (`classify.sh`) | 1,136 / 1,199 / 1,236 samples per seed differ, each in its invalid call. Master silently ignored a negative `len`, and an `off` past the end with `len` 0; it now throws `ArrayIndexOutOfBoundsException`, as the `Checksum.update(byte[], int, int)` contract specifies. Counts over all runs: CRC-32 8,931, CRC-32C 10,713 |
| Maven (`run_maven.sh`, `MAVEN_BUILD.txt`) | both poms build. Branch jar vs master jar differ only in `PureJavaCrc32.class`, `PureJavaCrc32C.class`, the pom and `PROVENANCE.json`. No class holds a CRC table constant any more (master: exactly these two). The classes shrink from 27,863 to 980 bytes and from 28,171 to 2,431 bytes |

## Speed (`CrcSpeed`, commons-codec 1.22.1, JDK 21.0.12, Linux on x86-64)

Nanoseconds per `reset` + `update(byte[], 0, size)` + `getValue`, best of 7:

| size | CRC-32 master | CRC-32 branch | CRC-32C master | CRC-32C branch |
|---|---|---|---|---|
| 16 B | 6.9 | 10.7 | 9.0 | 3.6 |
| 64 B | 21.4 | 18.1 | 22.7 | 5.3 |
| 256 B | 96.2 | 25.0 | 88.7 | 8.9 |
| 1 KiB | 393.3 | 33.3 | 342.7 | 26.7 |
| 4 KiB | 1,594.8 | 101.0 | 1,394.6 | 103.3 |
| 64 KiB | 25,645.0 | 1,477.3 | 22,935.3 | 1,578.5 |

CRC-32C is 2.5x faster at 16 bytes and 14.5x faster at 64 KiB. CRC-32 is 3.8x to 17x faster from 256 bytes, but
slower at 16 bytes: 6.9 -> 10.7 ns, the fixed cost of `CRC32.update`'s intrinsic stub.

## Not changed here

- Guava (`m3/vendor/third_party/guava` and `m3/collections/implementations/guava-33.4.8`): `Hashing.crc32c()` already
  picks `java.util.zip.CRC32C` at runtime on Java 9 and later; `Crc32cHashFunction` is its Java 8 fallback.
- snappy-java: `SnappyFramed` picks `CRC32C` the same way. Its `PureJavaCrc32C` is the fallback and a public class,
  so it stays.
- xz: `check.CRC32` already wraps `java.util.zip.CRC32`. M3's `M3FileCrcShadow` uses `java.util.zip.CRC32C`.
- commons-codec `Crc16`, `XXHash32`, `MurmurHash2` and `MurmurHash3`: java.base has no public counterpart.

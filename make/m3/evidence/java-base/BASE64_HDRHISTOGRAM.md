# HdrHistogram calls java.util.Base64 directly (2026-11-09, BASE64_TO_JDK v1)

User rules: M3JDK is one JDK, so M3 integrates in place and is never a side layer; and the JDK owns one fast
implementation that every consumer uses. HdrHistogram 2.1.12's package-private `Base64Helper` used reflection to find
`java.util.Base64`, with `javax.xml.bind.DatatypeConverter` as a fallback, so that one source tree runs on Java SE 6
to 9. In M3JDK (Java SE 21) the reflection always found `java.util.Base64`, and the fallback was dead code
(`java.xml.bind` left the JDK in 11). This applies to the vendored donor `m3/vendor/third_party/HdrHistogram` and to
the relocated module `jdk.m3.hdrhistogram` in the JDK image.

## Change

- Recipe `make/m3/tooling/jdk-superset/retarget/tools/base64_to_jdk.py` (re-runs to a fixed point; `--check` writes
  nothing):
  - `printBase64Binary` -> `Base64.getEncoder().encodeToString`;
  - `parseBase64Binary` -> `Base64.getDecoder().decode`;
  - the reflective state goes (four static fields and the static initializer);
  - the import `java.lang.reflect.Method` becomes `java.util.Base64`, which is Java 8 API and within the pom's
    `--release 8`;
  - the class Javadoc gains one paragraph.
- Custody: the new shared module `make/m3/tooling/jdk-superset/retarget/tools/donor_overlay.py` (DONOR_OVERLAY v1), the
  donors' overlay mechanism in one place:
  - the upstream file stays byte-identical under `upstream-excluded/`;
  - `PROVENANCE.json` and `OVERLAY_SOURCES.json` pin the overlay, its base and the recipe;
  - `verify_source.py` re-derives the overlay. The regenerated script fixes the old template's repository root and
    `revision` key; on master, HdrHistogram's `verify_source.py` stops at FileNotFoundError.
  - Pom and README describe the overlay. The relocated module gets the name-mapped overlay.
- DONOR_OVERLAY v1 is a faithful extraction. `base64-hdrhistogram/replay_checksums.py` replays CHECKSUMS_TO_JDK v1
  through it in `--check` mode on the landed checksum files: fixed point, nothing changed (`DONOR_OVERLAY_REPLAY.txt`).
- `make/m3/tooling/jdk-superset/retarget/tools/maven_gate.sh` is the reusable Maven gate (build master and branch, run
  `verify_source.py` with the jar, compare the jars).

## Gates

| Gate | Result |
|---|---|
| recipe | apply, then `--check`: fixed point |
| `verify_source.py` | PASS, 1 overlay; PASS with the Maven-built jar |
| javac (`run_gate.sh`, `-Xlint:all`) | rc=0, 0 errors, warning counts unchanged: HdrHistogram 96 classes (`--release 8`), `jdk.m3.hdrhistogram` 88 classes (`--release 21`), on master and on the branch |
| differential (`Base64HelperDifferential`, `GATE.txt`) | 3 seeds x 100,000 samples per build, master vs branch. Each sample encodes random bytes (up to 20,000), decodes the text and a corrupted copy of it, and every 50th sample also writes a random histogram with `HistogramLogWriter`, reads it back with `HistogramLogReader` and decodes it with `Histogram.fromString`. Every encoding, every round trip and every histogram log are identical |
| failures | When decoding fails, the exception changes. Master wrapped every failure in `UnsupportedOperationException("Failed to use platform's base64 decode method")`; the branch throws `java.util.Base64`'s own: `IllegalArgumentException` for malformed input (329,802 calls) and `NullPointerException` for null (6). Decodes that succeed return the same bytes |
| Maven (`MAVEN_BUILD.txt`) | the pom builds. The branch jar differs from the master jar only in `Base64Helper.class`, the pom and `PROVENANCE.json` |

Speed (`Base64HelperSpeed`, ns per call, JDK 21.0.12, Linux on x86-64): the reflective call's cost goes. At 16 bytes,
encode drops from 21.0 to 15.8 ns and decode from 19.8 to 18.7 ns. From 256 bytes on, `java.util.Base64`'s own work
dominates and the numbers are within run-to-run noise: for 4 KiB encode, 771 -> 771 ns in one run and 733 -> 836 ns in
the other.

## Not changed here

- commons-net `util.Base64` (deprecated upstream in favour of `java.util.Base64`) and commons-codec `binary.Base64`.
  Both have line-length, URL-safe and lenient-decoding options that need their own differential.
- snakeyaml 2.1 and 2.2 `Base64Coder`. Upstream snakeyaml replaced it with `java.util.Base64` in later versions, and
  the vendored 2.6 has none.

# JGit's util.Base64 runs on java.util.Base64 (2026-11-09, JGIT_BASE64_TO_JDK v1)

User rules: M3JDK is one JDK, so M3 integrates in place and is never a side layer; and the JDK owns one fast
implementation that every consumer uses. JGit 5.13.3 carries its own base64, `org.eclipse.jgit.util.Base64` (derived
from the iHarder public-domain library), for HTTP authentication, Amazon S3 and WalkEncryption. java.base owns base64:
`java.util.Base64` is Java 8 API, so the donor's `--release 8` can call it directly. This applies to the vendored donor
`m3/vendor/third_party/org.eclipse.jgit`; JGit has no relocated (lib-intake) copy.

## Change

- Recipe `m3/tooling/jdk-superset/retarget/tools/jgit_base64_to_jdk.py` on DONOR_OVERLAY v2 (re-runs to a fixed
  point; `--check` writes nothing). The class and its four public methods stay:
  - `encodeBytes(source, off, len)` encodes with `java.util.Base64.getEncoder()` over `ByteBuffer.wrap(source, off,
    len)`, so the input is not copied. The private `encode3to4` goes.
  - `decode(source, off, len)` uses `java.util.Base64.getDecoder()` when `source` is not null and the range is valid,
    non-empty and a multiple of four bytes long. There java.base accepts exactly the canonical form: alphabet
    characters, with `=` padding only in the last group. On that form JGit's loop gives the same bytes. Any other input
    goes to the upstream loop, kept unchanged as the private `decodeLenient`, with its own results, exceptions and
    messages. That covers white space (JGit skips it), bytes with the high bit set (JGit drops the bit), an unpadded
    final group (JGit drops it), input after the padding (JGit ignores it), invalid characters, invalid ranges and
    null.
  - `encodeBytes(byte[])` and `decode(String)` are unchanged and still call the three-argument forms. The class
    Javadoc gains one paragraph.
- Custody as for the other donor overlays:
  - the upstream file stays byte-identical under `upstream-excluded/`;
  - `PROVENANCE.json` and `OVERLAY_SOURCES.json` pin the overlay, its base and the recipe;
  - `verify_source.py` (regenerated from the DONOR_OVERLAY template) re-derives the overlay;
  - the pom and the README describe the overlay.

## Gates (`base64-jgit/run_gate.sh`, `GATE.txt`; JDK 21.0.12, Linux on x86-64)

| Gate | Result |
|---|---|
| recipe | apply, then `--check`: fixed point |
| Maven | JavaEWAH and slf4j-api (the donor's source-built dependencies) installed from master. The donor pom builds on master and on the branch; `verify_source.py` passes with the branch jar (879 files, 861 production, 1 overlay). The branch jar differs from the master jar only in `util/Base64.class`, the pom and `PROVENANCE.json` |
| javac `-Xlint:all,-options --release 8`, all donor sources | rc=0 and 0 errors on master and branch; 100 warnings on each, none in `util/Base64.java` |
| differential (`JgitBase64Differential`) | 3 seeds x 100,000 samples, 300,023 calls per seed, master jar vs branch jar line by line. Every decode call gives the same result or the same exception and message. The cases are canonical, white space, unpadded, high bit, data after the padding, `=` inside, URL-safe alphabet, random characters, sub-range of a larger array, `decode(String)` of Latin-1 text, invalid ranges and null. Every `encodeBytes` call with a valid range gives the same text |
| what changes | `encodeBytes` with an invalid range: 8 calls per seed. These now throw `IndexOutOfBoundsException` from `ByteBuffer.wrap`. Before, a negative length threw `NegativeArraySizeException`, a range past the array threw `ArrayIndexOutOfBoundsException`, and a zero length returned `""` whatever the offset (`-1,0` and `5,0` on a 4-byte array) |

Speed (`JgitBase64Speed`, ns per call, best of 7 rounds after warm-up):

| bytes | encode master -> branch | decode, canonical | decode, MIME text with line breaks (JGit's lenient path) |
|---|---|---|---|
| 16 | 23.2 -> 20.3 | 35.5 -> 23.6 | 36.2 -> 24.3 (no line break at this size: canonical) |
| 256 | 178 -> 46 | 408 -> 45 | 436 -> 775 |
| 4,096 | 3,837 -> 814 | 6,901 -> 771 | 9,129 -> 6,469 |
| 65,536 | 49,141 -> 13,772 | 106,097 -> 13,876 | 120,177 -> 112,802 |

Encoding is 3.6 to 4.7x faster from 256 bytes, and canonical decoding 7.6 to 9.2x. Non-canonical input pays for one rejected
`java.util.Base64` attempt when its length is a multiple of four: at 256 bytes of line-wrapped text that is +339 ns
(the exception), and larger inputs amortize it. JGit's own callers pass single-line base64 (HTTP authentication
tokens, S3 MD5 digests and signatures, WalkEncryption parameters).

## Not changed here

- JGit's `util.Hex`: `java.util.HexFormat` is Java 17 API, beyond the donor's `--release 8`.
- commons-codec `binary.Base64` and commons-net `util.Base64`: line-length, URL-safe and lenient-decoding options that
  need their own differential. commons-net 3.11.1 already calls `java.util.Base64` internally.
- Guava `Hashing.crc32c()` and snappy-java's framed format already choose `java.util.zip.CRC32C` at run time on Java 9
  and later; their table copies (`Crc32cHashFunction`, `PureJavaCrc32C`) are Java 8 fallbacks that M3JDK never selects.

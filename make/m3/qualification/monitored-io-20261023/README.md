<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native java.base monitored I/O receiver

Product owner: `hsoliwal/M3JDK21`. Canonical Maven/OpenRewrite recipe: [Synexia PR #13181](https://github.com/hsoliwal/com.synexia/pull/13181), source-sealed revision `e10c4bebe70a9b699bb3d41c14c6319557c0ed7f`. **No Synexia runtime dependency, wrapper SDK, secondary IO owner or JNI symbol.**

Frozen receiving parent: `9360755ec2988370a1bd77c5d66ba659a73af37c`. Before applying these outputs, all four actual product Java sources matched their complete canonical preimages byte-for-byte. No existing method signature/override is replaced; new methods are opt-in and named `readMonitored`, `readNBytesMonitored`, `transferToMonitored` or `writeMonitored`.

The monitor is caller-owned and borrowed; the operation does not begin/finish it, flush or close a stream/writer. Cancellation is cooperative at delegate boundaries, not a promise to unblock a blocked provider. Completed reads or fully completed writes alone receive progress credit. Nonconforming zero-progress positive reads fail with `IOException` rather than infinite loops.

## Canonical source SHA-256 custody
```tsv
src/java.base/share/classes/java/io/InputStream.java	3f2a8698a8e1dd61500508ecd1c6fec873744699aa2d447bb33ec00ce0f26d1d	897aa4367fcb4d3a2e859700172a5d5fa4e228b0aa08015c014a016a9c2101d9	InputStream.java.txt
src/java.base/share/classes/java/io/OutputStream.java	a929163b54781939b622e7c6f111f24bea541aa2f6d68be38d3ea2b0048a90d1	efe5a88ed89b4aa8e22fb8c1a6751cea4179e3252cca1e8524305b1d5da93eff	OutputStream.java.txt
src/java.base/share/classes/java/io/Reader.java	36dea94f5a42542ddfab043a274eb6f6021680f5cdba1c07e14607a10279ebb2	3ef19580d26cbb75a432af1a3aa14a8d945f74fab303985c31b7aedf60f880d9	Reader.java.txt
src/java.base/share/classes/java/io/Writer.java	bfd7e7c4c9443823a23cc53094832ca1b694076778c84dbc2773b67aaabe979e	cf08daabd3ad6e78550920f1954ca412fc847d843d29f7cae7d7ae004a48ca10	Writer.java.txt
test/jdk/java/io/MonitoredIoOperations.java	ABSENT	4fe0aaf53b7855c5d27591a66140abb1634a0143d07ae307237aaa671345090f	MonitoredIoOperations.java.txt
```

## Admission and gaps
- Byte-for-byte preimage comparison on master: PASS.
- Host Java21 patched-source compilation and normal/-Xint jtreg-class run: provided by `verify-host-patch.sh` and PR workflow; results pending.
- Canonical Maven/OpenRewrite fixed-point/drift refusal, full modified M3JDK21/HotSpot java.base build, native/product jtreg and Windows/macOS/JNI parity: **NOT VERIFIED**; retain DRAFT/HOLD.
- Source licences are retained; product files carry the original OpenJDK GPLv2-with-Classpath-exception notices, qualification documentation is Apache-2.0.

This receiver contains only product modifications and bounded acceptance artifacts, not recipes.

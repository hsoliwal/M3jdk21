<!-- SPDX-License-Identifier: Apache-2.0 -->
# census3-gaps-20261107 (m3/file)

Census pass 3 member-gap leaf of lane com-synexia-0e on `m3/file`: **P3-012** (the `jdk.m3.file` M3File family vs
develop `com.synexia.indexstring.file.MIndexFile*`, 12 source files). Census pins: com.synexia develop
`78cf2ad4c7e5`, M3jdk21 master `2274e18709b8` (`S:/m3-scratch/eb-conv/census3`); donor re-read at develop-live head
`78cf2ad4c7e5`, owner at master `0581643118d7`. The owner is the 2026-10-18 port (0a50a282ac3 "Integrate the complete
M3File port", unified into `m3/file` by 453f3e12ba5 FILE_UNIFY v2) over `M3FileText`/`M3FileResolver`, the file
projections of the donor `MIndexString`/`IndexResolver` contracts; the donor added the UTF-8 channel facades, the
channel writer and the indexed-text readers on 2026-11-02, after that port.

| file | content |
|---|---|
| `DECISION.tsv` | one decision per unexplained MEMBER_GAPS row (82): RESTORE 16, PRESENT_EVOLVED 65, NEC 1 |
| `apply.py` | recipe CENSUS3_GAPS v1 (from `m3/collections/qualification/census-small-batch-20261106/apply.py`): hash-pinned anchored edits over 6 owners, `--check` fixed point, refuses drift (exit 2) |
| `EDITS.tsv` | file, before/after sha256, leaf, donor blob, rule (written by apply.py) |
| `oracle/census3/` | seeded differential driver + `OwnerFile`; `oracle/SIDE_MAP.tsv` + `mkdonor.py` generate the donor side |
| `differential.sh` | compiles both sides, runs default JIT and `-Xint`, requires byte-identical output |
| `mutants.py` | 9 mutants of the restored members, replayed through the differential |
| `GATES.tsv` | gates with numbers |

RESTORE (16): `writeUtf8To(WritableByteChannel)` on `M3File`, `M3FileProjection`, `M3FileSpan` and
`writeUtf8To(M3FileResolver, WritableByteChannel)` on `M3FileSnapshot` (develop 2298f0638e3c, 1a2733080a6f,
5cb480dacfdf, 0cde6220fbc2: indexed UTF-8 channel facades through `M3FileText.writeUtf8To`);
`M3FileEventWriter.forChannel(WritableByteChannel)` (396a0e0263b8, wire format unchanged);
`M3FileEventReader.read(InputStream)` (no-op sink), `readIndexedText`, `readProjectionText`, `readSelectedText` and the
private `IndexedTextCollector` (f7e15c7731ed: indexed-text / kind-selection projection of the one validated event pass).

PRESENT_EVOLVED (65): every `MIndexString`/`IndexResolver`/`M3StringPool` signature row is the same member over
`M3FileText`/`M3FileResolver`/`M3FileTextPool` (`asMIndexString()` is `asM3String()`); the reader's `MAX_FRAME_BYTES` is
the public `DEFAULT_MAX_FRAME_BYTES` + caller bound, `MAX_TOKEN_COUNT`/`readCount(int, String)` is
`readCount(DataInputStream, String)`, `TOKEN_PAYLOAD_BYTES` is `TOKEN_FRAME_BYTES`, `Summary.attributesBeforeRead/
AfterRead` are `attributesBefore/After`, `readLength`/`requireStarted`/`requireEmpty`/`readExact` are
`readInt`/`requireStarted(boolean, String)`/`requireExhausted`/`readFully + invalid(..)`.

NEC (1): `Summary.lexiconCount` (the owner record has 6 components; adding one changes its canonical constructor).

Tests: the donor proofs `MIndexEventChannelWriterProof`, `MIndexEventReaderIndexedTextRecoveryProof` and
`MIndexFileUtf8ChannelFacadeProof` are ported as `M3FileCensus3Test` (3 methods) over the live `M3FileDictionary`
resolver. Not ported: the proof's `MappedMIndexFile` section (`MappedM3File` is outside this leaf) and its
zero-progress channel refusal.

**Owner observation (not a census3 member, no edit):** `M3FileText.writeUtf8To(WritableByteChannel)` loops with
`Thread.onSpinWait()` while a channel keeps returning 0 bytes, so a zero-progress channel hangs the caller; the donor
`MIndexString.writeUtf8To` throws `IOException` for it (`MIndexFileUtf8ChannelFacadeProof` "no-progress rejected").
The restored facades inherit the owner behaviour. Left to the `M3FileText` owner.

**Owner observation 2 (not a census3 member, no edit):** the owner's frame validation (`readFrames`, the one
validation authority behind the restored readers) is stricter than the donor's on random single-bit tampering: in a
4,000-episode probe, 139 of 1,000 randomly tampered MIDXEVT2 copies were refused by the owner and accepted by the donor,
or refused as `IOException` where the donor throws `EOFException`. Intact streams, truncated streams and streams with
a flipped terminal-digest bit behave identically on both sides; the differential compares those.

Master build on this machine: `exec-maven-plugin` `file-source-class-content-contract` (test phase) runs `python3`,
which is not on PATH (exit 9009), and `M3FileDeltaTest.fullSourceClassAndCliContract` fails ("CLI receipt version");
both pre-existing on master, identical on the head (`GATES.tsv`).

Replay: `python -I apply.py --check` (exit 0, changed=0); `mvn -B -ntp -f m3/file/pom.xml clean package
-Dmaven.test.failure.ignore=true`; build the donor classes from develop `78cf2ad4c7e5` (javac -sourcepath over the
donor's indexstring, common, job, native-interop, algo, classic-tools and cop modules + re2j, lombok, commons-codec);
then `bash differential.sh "<donor-classes>;<re2j;lombok;commons-codec>" "m3/file/target/classes;<m3-collections jar>;<m3-arrays jar>" <out> 4000`
and `python -I mutants.py <repo> "<head classpath>" <out> <work> "<donor classpath>" 1000`.

# M3jdk21 x jdk21u — resolution proposals per conflicted hunk (2026-10-19)

**Dry run. NEC: suggestions only.** Nothing here was applied to any branch; every row is a proposal for the M3 owners to accept, change or reject. Companion to `REPORT-20261019-jdk21u.md` (same branch): fork master e30a8429d9 x jdk21u bb6b279495, merge base 890adb6410 (jdk-21+35), 23 conflicted files, 186 hunks as numbered by `git merge-tree --write-tree` (the merge-tree result tree is `4b4bd8350c` in `S:/m3-scratch/worktrees/m3jdk21-sync.git`).

Row format: **hunk** (merge-tree hunk number @ line in the conflicted result, ours/theirs line counts) | **upstream change** (JBS id, owning jdk21u commit of the theirs lines by blame) | **fork (M3) side** | **proposal** | **proof**.

Proposal vocabulary: `take upstream` (theirs replaces ours), `keep fork` (ours stays, theirs dropped), `union` (both blocks kept, order stated), `3-way` (exact merged text given). Proof names a jtreg test path on the jdk21u side and/or an M3 test under `test/jdk/java/lang/String/` or `test/jdk/java/lang/StringBuilder/` of the fork; "owners confirm" marks a test name chosen from the fork tree without reading it.

Prerequisites that are NOT conflicts but that the String rows rely on (auto-merged, verify after merge): `StringUTF16.java` must carry 8311906's `coderFromArrayLen(byte[], int)` and the never-null `compress(...)` overloads; `StringLatin1.java`/`StringCoding.java` as upstream.

## A. `src/java.base/share/classes/java/lang/String.java` — 10 hunks

Fork structure to respect: constructors admit an `M3String` first (`maybeAdmit*`), then build a compatibility `candidate`/`candidateCoder`; the `byte[]`+`Charset` decode lives in a static helper that returns `new M3Construction(bytes, coder)` instead of assigning `this.value/this.coder`. Upstream's constructors assign fields directly, so every hunk below is the same two changes (8311906: `StringUTF16.compress` never returns null, the coder comes from `coderFromArrayLen`; 8320570: `StringUTF16.newBytesFor(length)` instead of `new byte[length << 1]`) re-expressed in the fork's shape.

| hunk | upstream change | fork (M3) side | proposal | proof |
|---|---|---|---|---|
| h1 @376 (4/4) `String(int[] codePoints, int offset, int count)` | 8311906 fbc9fad823f: `StringUTF16.compress(int[],..)` + `coderFromArrayLen` replace `StringLatin1.toBytes` null-check | after `maybeAdmitCodePoints`, fills `candidate`/`candidateCoder`, then `if (candidate == null) candidate = StringUTF16.toBytes(..)` | 3-way: `if (COMPACT_STRINGS) { candidate = StringUTF16.compress(codePoints, offset, count); candidateCoder = StringUTF16.coderFromArrayLen(candidate, count); }` then the fork's existing `if (candidate == null)` fallback (now only for `!COMPACT_STRINGS`) and `this.value/coder/m3` assignments unchanged | `test/jdk/java/lang/String/StringRacyConstructor.java`, `test/jdk/java/lang/String/Chars.java`; M3: `M3StringCodePointGeometryTest` (owners confirm) |
| h2 @657 (9/11) UTF-8 Latin1 scan loop in the static decode helper | 8311906: decode from a stable copy `latin1` indexed by `sp` (the surrounding `latin1`/`sp` prelude already auto-merged) | fork side is the old `bytes[offset]`/`dst` loop with comments stripped | take upstream (the 11 theirs lines verbatim: `sp < length`, `latin1[sp]`, `latin1[dp++]`, `sp++`, `sp--`) | `test/jdk/java/lang/String/StringRacyConstructor.java`, `test/jdk/java/lang/String/CompactString/MaxSizeUTF16String.java` |
| h3 @686 (1/3) Latin1 result of the UTF-8 path | 8311906: result array is `latin1` | fork returns `new M3Construction(dst, LATIN1)` | 3-way: `return new M3Construction(latin1, LATIN1);` | same as h2 |
| h4 @700 (1/10) UTF-16 result of the UTF-8 path + `!COMPACT_STRINGS` branch | 8311906 (`utf16` array) and 8320570 779204c9412 (`newBytesFor`) | fork returns `new M3Construction(dst, UTF16)` and keeps the `!COMPACT_STRINGS` fall-through after the `if` (`byte[] dst = new byte[length << 1]` at result line 715) | 3-way: `return new M3Construction(utf16, UTF16);` for the hunk, plus apply 8320570 to the fork's fall-through: `byte[] dst = StringUTF16.newBytesFor(length);` (result line 715) | `test/jdk/java/lang/String/CompactString/NegativeSize.java` (8320570), `MaxSizeUTF16String.java` |
| h5 @733 (2/11) US-ASCII fast path | 8320570: `newBytesFor` in the non-ASCII branch; otherwise unchanged | fork returns `M3Construction(copyOfRange, LATIN1)` and falls through to its own `new byte[length << 1]` loop (result line 750) | keep fork for the hunk; apply 8320570 to the fall-through: `byte[] dst = StringUTF16.newBytesFor(length);` (result line 750) | `NegativeSize.java`; M3: `M3StringUtf16DifferentialTest` |
| h6 @759 (14/23) `ArrayDecoder` fast paths (ASCII-compatible, Latin1-decodable) | 8311906 touches only the `compress` call below (h7); the theirs lines here are upstream's unchanged block re-aligned by the merge | fork restructured the block into `return new M3Construction(..)` forms; semantics equal to upstream's | keep fork (ours) | `test/jdk/java/nio/file/Files/ReadWriteString.java`; M3: `M3StringUtf16DifferentialTest` |
| h7 @807 (4/4) `ArrayDecoder` compress of the decoded `char[]` | 8311906: `compress` never null + `coderFromArrayLen` (theirs text shows `caLen` because the merge aligned the fork's first site with upstream's second site) | fork: `bs = compress(ca,0,clen); if (bs != null) return new M3Construction(bs, LATIN1);` then `return new M3Construction(StringUTF16.toBytes(ca,0,clen), UTF16);` | 3-way: `if (COMPACT_STRINGS) { byte[] val = StringUTF16.compress(ca, 0, clen); return new M3Construction(val, StringUTF16.coderFromArrayLen(val, clen)); }` then the fork's UTF16 return. Then check the fork's second compress site (non-`ArrayDecoder` path, variable `caLen`, after result line 822) received the same change by auto-merge; apply the same pattern if it did not | `StringRacyConstructor.java`; M3: `M3StringUtf16DifferentialTest` |
| h8 @2458 (21/6) `regionMatches(int, String, int, int)` | 8316879 6a4e4920193: early `if (len <= 0) return true;` (CompactStrings-disabled mismatch) | fork inserts the M3 coordinate/facts gate (`m3()`, `slice`, `sameCoordinate`, `javaHash`) before the byte comparison and reads `value()`/`other.value()` | 3-way: upstream's guard first, then the fork block unchanged: `// Any strings match if len <= 0` / `if (len <= 0) { return true; }` / `M3String leftM3 = m3(); ...` / `byte[] tv = value(); byte[] ov = other.value();` | `test/jdk/java/lang/String/RegionMatches.java`; M3: `M3StringMixedEqualityGatesTest` |
| h9 @5492 (6/4) `private String(char[] value, int off, int len, Void sig)` | 8311906: `compress` never null + `coderFromArrayLen` | fork: `maybeAdmit(value, off, len)` first, else `candidate = COMPACT_STRINGS ? compress(..) : null; candidateCoder = candidate == null ? UTF16 : LATIN1; if (candidate == null) candidate = toBytes(..)` | keep the `maybeAdmit` block (ours); 3-way on the fork's candidate lines (result 5508-5509): `byte[] candidate = COMPACT_STRINGS ? StringUTF16.compress(value, off, len) : null;` / `byte candidateCoder = candidate == null ? UTF16 : StringUTF16.coderFromArrayLen(candidate, len);` (the `== null` fallback now serves `!COMPACT_STRINGS` only). M3 note: `maybeAdmit` reads the caller's `char[]` a second time; under 8311906's contract (contents unspecified if the array is modified during construction, but never an exception) the admission must snapshot once or re-verify against `candidate` | `StringRacyConstructor.java`, `test/hotspot/jtreg/compiler/intrinsics/string/TestStringConstructionIntrinsics.java`; M3: `M3StringCharArrayCompositionTest`, `M3StringCharArrayHistoryTest` (owners confirm) |
| h10 @5528 (12/12) `String(AbstractStringBuilder asb, Void sig)` | 8311906: in the `maybeLatin1` branch `compress` never null + `coderFromArrayLen` | fork: `maybeAdmitCompact(val, 0, length, coder)` first, else candidate flow (Latin1 copy / compress-if-maybeLatin1 / UTF16 copy) | keep the `maybeAdmitCompact` block (ours); 3-way on the fork's else-branch (result 5563-5570): `if (COMPACT_STRINGS && asb.maybeLatin1) { candidate = StringUTF16.compress(val, 0, length); candidateCoder = StringUTF16.coderFromArrayLen(candidate, length); } else { candidate = Arrays.copyOfRange(val, 0, length << 1); candidateCoder = UTF16; }` | `test/jdk/java/lang/StringBuilder/RacingSBThreads.java`; M3: `test/jdk/java/lang/StringBuilder/M3CompositeBuilderTest.java` |

## A2. `src/java.base/share/classes/java/lang/AbstractStringBuilder.java` — 2 hunks

| hunk | upstream change | fork (M3) side | proposal | proof |
|---|---|---|---|---|
| h1 @390 (1/4) `charAt(int)` | 8351000 9f0c5be77fe: snapshot `coder`, `value`, and `count = min(count, value.length >> coder)` before `checkIndex` (racy reads) | fork calls `m3EnsureFlat()` first (materialize the composite builder lane into the flat `value` before indexed access) | 3-way, in this order: `m3EnsureFlat();` / `byte coder = this.coder;` / `byte[] value = this.value;` / `// Ensure count is less than or equal to capacity (racy reads and writes can produce inconsistent values)` / `int count = Math.min(this.count, value.length >> coder);` then the common `checkIndex(index, count); if (coder == LATIN1) ...` | `test/jdk/java/lang/StringBuilder/RacingSBThreads.java`; M3: `M3CompositeBuilderTest` |
| h2 @459 (1/1) `codePointBefore(int)` | 8351000: local `byte[] value = this.value;` | fork: `m3EnsureFlat();` | 3-way: `m3EnsureFlat();` / `byte[] value = this.value;` | same |

Also from 8311906/8351000 in this file (auto-merged, not conflicts): `initBytes(char[],..)` uses `coderFromArrayLen`; `putCharSB` after `inflate()` in the two `appendChars` loops; 8359454 23bd6b50dbb removes `putStringAt(int, String, int, int)`. Verify the fork's composite-builder lane (R5-BUILDER, 237f97cdb6e) does not call the removed overload.

## C. HotSpot OS layer — 3 hunks

The fork backports "report open file descriptors" (2edaaa0d8d4 Linux, 13868fa5e46 BSD/macOS, plus auto-merged declarations in `os.hpp`, `os_bsd.hpp`, `os_aix.cpp`, `os_windows.cpp` and the call in `vmError.cpp`). jdk21u has no `print_open_file_descriptors` anywhere. The conflicts are adjacent insertions only.

| hunk | upstream change | fork (M3) side | proposal | proof |
|---|---|---|---|---|
| `os_bsd.cpp` h1 @2534 (41/21) | 8320890 4d4c47d92f: adds `os::pd_dll_unload(...)` at end of file | adds `os::print_open_file_descriptors` + `os::Bsd::print_open_file_descriptors` at the same place | union: upstream's `pd_dll_unload` block first, then the fork's two functions | `test/hotspot/jtreg/runtime/ErrorHandling/` hs_err smoke (owners pick); build of macOS/BSD port; `make test TEST=gtest:os` |
| `os_linux.cpp` h1 @96 (1/1) includes | 8295159 7e4185a74e: `# include <fenv.h>` | `# include <dirent.h>` (for `/proc/self/fd`) | union: both includes, `<dirent.h>` then `<fenv.h>` | `test/hotspot/jtreg/compiler/floatingpoint/TestSubnormalFloat.java`, `TestSubnormalDouble.java` (8295159); Linux build |
| `os_linux.cpp` h2 @5792 (27/21) | 8320890 4d4c47d92f: `os::pd_dll_unload(...)` | `os::print_open_file_descriptors` (50 ms bounded `/proc/self/fd` scan) | union: upstream's `pd_dll_unload` block first, then the fork's function | as os_bsd h1, Linux |

## D. Repository files — 2 hunks

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| `.github/workflows/build-cross-compile.yml` h1 @116 (2/1) | 8314656 5cd6929a2c: drop `debian-ports-archive-keyring` from the apt line | fork 6aa67d17700 already dropped the gcc version pins (mirroring jdk21u) and kept the keyring | take upstream (the fork's change on these lines equals upstream's except the keyring line, which upstream removed) | the workflow parses as YAML; the cross-compile GHA job when Actions are enabled |
| `README.md` h1 @1 (268/8) | 8343474 22d5e0d1f8: jdk21u project README | the 279-line M3 homepage (15 fork commits) | keep fork | none (documentation) |

## B. Post-quantum crypto (JEP 496/497 snapshot vs the official jdk21u backports) — 17 files, 169 hunks

Correction to `REPORT-20261019-jdk21u.md`: group B has 169 hunks, not 170. The totals are 12 (A) + 169 (B) + 3 (C) + 2 (D) = 186.

### Does the official backport supersede the fork snapshot byte for byte?

"Exact" means the fork's blob is identical to the blob of that file at one jdk21u commit, and jdk21u head is a later revision of the same file. The fork then has no line of its own in that file.

The diff stat is `git diff --numstat` from the fork blob to the jdk21u-head blob. For non-exact files the nearest upstream revision is the one with the smallest line diff to the fork.

| file | conflict | hunks | diff stat fork -> jdk21u head | supersedes byte for byte? |
|---|---|---|---|---|
| `classes/com/sun/crypto/provider/ML_KEM.java` | add/add | 31 | +135/-72 | YES: fork blob = jdk21u 6de158a4cc3 exactly; 0 fork-only lines |
| `classes/com/sun/crypto/provider/ML_KEM_Impls.java` | add/add | 12 | +80/-52 | YES: fork blob = jdk21u 6de158a4cc3 exactly; 0 fork-only lines |
| `classes/com/sun/crypto/provider/SunJCE.java` | content | 1 | +17/-22 | NO: fork = nearest upstream rev 890adb6410d plus 28 fork-only lines / minus 1 |
| `classes/sun/security/pkcs/NamedPKCS8Key.java` | add/add | 8 | +91/-28 | YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines |
| `classes/sun/security/provider/ML_DSA.java` | add/add | 32 | +534/-116 | YES: fork blob = jdk21u 825e716d475 exactly; 0 fork-only lines |
| `classes/sun/security/provider/ML_DSA_Impls.java` | add/add | 10 | +85/-25 | YES: fork blob = jdk21u 77ca63d6a22 exactly; 0 fork-only lines |
| `classes/sun/security/provider/NamedKEM.java` | add/add | 16 | +26/-31 | YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines |
| `classes/sun/security/provider/NamedKeyFactory.java` | add/add | 14 | +118/-82 | NO: fork = nearest upstream rev d0185ba9d1e plus 16 fork-only lines / minus 21 |
| `classes/sun/security/provider/NamedKeyPairGenerator.java` | add/add | 10 | +61/-24 | YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines |
| `classes/sun/security/provider/NamedSignature.java` | add/add | 17 | +26/-31 | YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines |
| `classes/sun/security/provider/SHA3.java` | content | 10 | +190/-114 | NO: fork = nearest upstream rev 890adb6410d plus 151 fork-only lines / minus 20 |
| `classes/sun/security/provider/SHA3Parallel.java` | add/add | 2 | +15/-1 | YES: fork blob = jdk21u 6de158a4cc3 exactly; 0 fork-only lines |
| `classes/sun/security/provider/SunEntries.java` | content | 1 | +7/-2 | NO: fork = nearest upstream rev ae88cd99b00 plus 1 fork-only lines / minus 2 |
| `classes/sun/security/util/KeyUtil.java` | content | 1 | +88/-42 | NO: fork = nearest upstream rev 890adb6410d plus 7 fork-only lines / minus 1 |
| `classes/sun/security/util/SignatureUtil.java` | content | 1 | +5/-7 | NO: fork = nearest upstream rev d0185ba9d1e plus 6 fork-only lines / minus 4 |
| `classes/sun/security/x509/NamedX509Key.java` | add/add | 2 | +3/-2 | YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines |
| `conf/security/java.security` | content | 1 | +166/-17 | NO: fork = nearest upstream rev 890adb6410d plus 19 fork-only lines / minus 0 |

- **10 of 17 files: yes.** They are byte-identical to an earlier jdk21u revision:
  - 6de158a4cc3 (8298390): ML_KEM, ML_KEM_Impls, SHA3Parallel;
  - 825e716d475 (8345512): ML_DSA;
  - 77ca63d6a22 (8345533): ML_DSA_Impls;
  - d0185ba9d1e (8340327): NamedKEM, NamedKeyPairGenerator, NamedSignature, NamedPKCS8Key, NamedX509Key.
  - Taking jdk21u head wholesale loses no fork content.
- **7 files: no.** Each carries fork-only lines. All of them are covered by the per-hunk rows below:
  - `SunJCE.java`: its own ML-KEM registration, the same 12 entries as upstream. Also 2 javadoc/description lines outside the hunk, which auto-merge.
  - `NamedKeyFactory.java`, `KeyUtil.java`, `SignatureUtil.java`: a Java 21 fallback written before jdk21u had `KeyUtil.getParams`. 8340327 now provides it.
  - `SHA3.java`: a byte-state XOF engine (151 fork-only lines), used only inside SHA3.java (checked in `sun.security`, `com.sun.crypto`, `javax.crypto`, `java.security`). Upstream's long[] state matches the HotSpot SHA3 intrinsic changes (8333867, 8341527), which auto-merge into the fork because the fork never touched them.
  - `SunEntries.java`: one `KeySize` line moved.
  - `java.security`: the fork-only `jdk.security.password.allowSystemIn` property, implemented by the fork's `Password.java`.
- **Proposal for group B:** take jdk21u head for all 17 files, except:
  - `java.security`: union (keep the fork's `allowSystemIn` block, hunk 23.1).
  - **SignatureUtil, auto-merged and not a conflict:** the merge keeps the fork's `default -> kAlg.contains("KEM") ? null : kAlg;` (result line 512) where jdk21u has `default -> null;`. Owners decide. Taking upstream wholesale for this file restores `null`.
- **Proof for the whole group:**
  - `test/jdk/sun/security/provider/acvp/Launcher.java` (drives `ML_KEM_Test`, `ML_DSA_Test`, `SHA_Test`);
  - `test/jdk/sun/security/provider/all/Deterministic.java`;
  - `test/jdk/sun/security/provider/named/NamedKeyFactoryTest.java`, `named/NamedKeys.java`, `named/NamedEdDSA.java`;
  - `test/jdk/sun/security/provider/pqc/PrivateKeyEncodings.java`;
  - `test/jdk/sun/security/provider/MessageDigest/SHAKEhash.java`, `SHAKEsqueeze.java`;
  - fork test `test/jdk/sun/security/util/Password/M3PasswordPolicyCases.java` and upstream `test/jdk/sun/security/util/Password/EmptyIn.java` for 23.1.
  - Test paths in the rows are relative to `test/jdk/sun/security/provider/` unless they start with `java/`.

Row legend:
- "older jdk21u text" means the fork lines in the hunk exist verbatim in the named upstream revision.
- "no lines" means upstream added lines that the fork's earlier snapshot did not have.
- The upstream change column lists the owning jdk21u commits of the theirs-side lines, by `git blame` over 890adb6410..bb6b279495, as "JBS sha xlines".

### `ML_KEM.java` — supersede: YES: fork blob = jdk21u 6de158a4cc3 exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8351412 b1b1cb5089 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h2 @35 (0/1) | 8349721 64b3ba44f7 x1  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h3 @82 (1/1) | 8349721 64b3ba44f7 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h4 @207 (1/20) | 8349721 64b3ba44f7 x20  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h5 @370 (2/1) | 8349721 64b3ba44f7 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h6 @521 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h7 @535 (1/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h8 @550 (1/1) | 8345512 825e716d47 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h9 @563 (1/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h10 @576 (0/6) | 8347938 1e68f11823 x6  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h11 @592 (1/1) | 8298390 6de158a4cc x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h12 @607 (1/1) | 8345512 825e716d47 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h13 @641 (1/1) | 8345512 825e716d47 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h14 @710 (4/6) | 8298390 6de158a4cc x3 8347938 1e68f11823 x3  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h15 @768 (2/2) | 8345512 825e716d47 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h16 @779 (1/1) | 8347608 4245a2f675 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h17 @872 (1/1) | 8347608 4245a2f675 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h18 @892 (1/1) | 8349721 64b3ba44f7 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h19 @1076 (5/7) | 8349721 64b3ba44f7 x7  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h20 @1101 (12/15) | 8349721 64b3ba44f7 x12 8298390 6de158a4cc x3  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h21 @1144 (1/2) | 8349721 64b3ba44f7 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h22 @1240 (9/10) | 8349721 64b3ba44f7 x9 8298390 6de158a4cc x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h23 @1276 (3/5) | 8349721 64b3ba44f7 x5  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h24 @1303 (8/10) | 8349721 64b3ba44f7 x10  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h25 @1332 (11/13) | 8349721 64b3ba44f7 x13  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h26 @1371 (1/3) | 8349721 64b3ba44f7 x3  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h27 @1500 (9/7) | 8349721 64b3ba44f7 x6 8298390 6de158a4cc x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h28 @1527 (10/11) | 8374755 b1af42335a x8 8349721 64b3ba44f7 x3  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h29 @1691 (9/10) | 8349721 64b3ba44f7 x10  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h30 @1725 (2/3) | 8349721 64b3ba44f7 x3  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h31 @1742 (5/23) | 8347608 4245a2f675 x18 8298390 6de158a4cc x3 8345512 825e716d47 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |

### `ML_KEM_Impls.java` — supersede: YES: fork blob = jdk21u 6de158a4cc3 exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h2 @33 (3/6) | 8298390 6de158a4cc x3 8347938 1e68f11823 x3  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h3 @53 (11/12) | 8347938 1e68f11823 x12  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h4 @94 (19/21) | 8347938 1e68f11823 x14 8298390 6de158a4cc x7  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h5 @162 (2/33) | 8347938 1e68f11823 x33  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h6 @225 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h7 @236 (2/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h8 @258 (21/12) | 8298390 6de158a4cc x7 8347938 1e68f11823 x3 8345512 825e716d47 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h9 @298 (2/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h10 @309 (4/4) | 8298390 6de158a4cc x2 8347938 1e68f11823 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h11 @324 (4/4) | 8298390 6de158a4cc x2 8347938 1e68f11823 x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |
| h12 @339 (5/5) | 8347938 1e68f11823 x3 8298390 6de158a4cc x2  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, all/Deterministic.java |

### `SunJCE.java` — supersede: NO: fork = nearest upstream rev 890adb6410d plus 28 fork-only lines / minus 1

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @769 (19/11) | 8298390 6de158a4cc x11  | fork's own ML-KEM KEM/KeyPairGenerator/KeyFactory registration from 26320a320fe; the same 12 class entries as upstream, lines wrapped | take upstream (entry sets identical) | acvp/ML_KEM_Test.java |

### `NamedPKCS8Key.java` — supersede: YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @28 (5/2) | 8340327 d0185ba9d1 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h2 @47 (0/1) | 8347938 1e68f11823 x1  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h3 @62 (0/22) | 8347938 1e68f11823 x22  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h4 @94 (9/15) | 8347938 1e68f11823 x10 8340327 d0185ba9d1 x5  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h5 @126 (22/38) | 8347938 1e68f11823 x33 8340327 d0185ba9d1 x5  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h6 @200 (3/9) | 8347938 1e68f11823 x8 8340327 d0185ba9d1 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h7 @234 (3/5) | 8347938 1e68f11823 x4 8340327 d0185ba9d1 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |
| h8 @255 (0/13) | 8347938 1e68f11823 x13  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, pqc/PrivateKeyEncodings.java |

### `ML_DSA.java` — supersede: YES: fork blob = jdk21u 825e716d475 exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h2 @32 (3/5) | 8348561 80b91394c9 x3 8298387 ae88cd99b0 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h3 @59 (1/1) | 8348561 80b91394c9 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h4 @117 (33/274) | 8348561 80b91394c9 x274  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h5 @594 (2/2) | 8347606 3e6b2aa826 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h6 @606 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h7 @616 (2/2) | 8347606 3e6b2aa826 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h8 @635 (0/48) | 8347938 1e68f11823 x48  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h9 @711 (13/13) | 8347606 3e6b2aa826 x11 8298387 ae88cd99b0 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h10 @755 (1/0) | lines removed in the jdk21u range | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h11 @830 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h12 @840 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h13 @908 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h14 @1021 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h15 @1109 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h16 @1119 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h17 @1128 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h18 @1167 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h19 @1260 (37/84) | 8348561 80b91394c9 x73 8347606 3e6b2aa826 x6 8298387 ae88cd99b0 x5  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h20 @1478 (1/1) | 8348561 80b91394c9 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h21 @1488 (2/2) | 8347606 3e6b2aa826 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h22 @1519 (2/2) | 8348561 80b91394c9 x1 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h23 @1543 (1/13) | 8348561 80b91394c9 x12 8351034 d16750b027 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h24 @1572 (7/17) | 8348561 80b91394c9 x13 8298387 ae88cd99b0 x3 8351034 d16750b027 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h25 @1605 (7/5) | 8298387 ae88cd99b0 x3 8348561 80b91394c9 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h26 @1635 (0/12) | 8348561 80b91394c9 x11 8351034 d16750b027 x1  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h27 @1655 (1/7) | 8348561 80b91394c9 x7  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h28 @1673 (11/35) | 8348561 80b91394c9 x29 8298387 ae88cd99b0 x4 8351034 d16750b027 x2  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h29 @1755 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h30 @1773 (1/1) | 8347606 3e6b2aa826 x1  | older jdk21u text, identical to 825e716d475 | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h31 @1835 (0/1) | 8349721 64b3ba44f7 x1  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |
| h32 @1852 (0/18) | 8347606 3e6b2aa826 x18  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java, all/Deterministic.java |

### `ML_DSA_Impls.java` — supersede: YES: fork blob = jdk21u 77ca63d6a22 exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h2 @33 (0/4) | 8347938 1e68f11823 x4  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java |
| h3 @46 (0/19) | 8347938 1e68f11823 x19  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java |
| h4 @79 (10/10) | 8298387 ae88cd99b0 x5 8347938 1e68f11823 x5  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h5 @118 (14/20) | 8347938 1e68f11823 x12 8298387 ae88cd99b0 x8  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h6 @181 (2/33) | 8347938 1e68f11823 x33  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h7 @242 (10/10) | 8298387 ae88cd99b0 x5 8347938 1e68f11823 x5  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h8 @281 (4/4) | 8298387 ae88cd99b0 x2 8347938 1e68f11823 x2  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h9 @304 (4/4) | 8298387 ae88cd99b0 x2 8347938 1e68f11823 x2  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |
| h10 @319 (4/4) | 8298387 ae88cd99b0 x2 8347938 1e68f11823 x2  | older jdk21u text, identical to 77ca63d6a22 | take upstream | acvp/ML_DSA_Test.java |

### `NamedKEM.java` — supersede: YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h2 @49 (1/0) | lines removed in the jdk21u range | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h3 @60 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h4 @69 (10/8) | 8340327 d0185ba9d1 x4 8347938 1e68f11823 x4  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h5 @101 (2/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h6 @121 (3/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h7 @135 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h8 @148 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h9 @163 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h10 @192 (2/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h11 @204 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h12 @214 (5/5) | 8340327 d0185ba9d1 x3 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h13 @233 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h14 @242 (11/11) | 8340327 d0185ba9d1 x7 8347938 1e68f11823 x4  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h15 @277 (5/5) | 8340327 d0185ba9d1 x3 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |
| h16 @302 (5/5) | 8340327 d0185ba9d1 x3 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, acvp/ML_KEM_Test.java |

### `NamedKeyFactory.java` — supersede: NO: fork = nearest upstream rev d0185ba9d1e plus 16 fork-only lines / minus 21

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h2 @33 (0/1) | 8340327 d0185ba9d1 x1  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h3 @52 (1/0) | lines removed in the jdk21u range | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h4 @71 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h5 @95 (8/8) | 8347938 1e68f11823 x5 8340327 d0185ba9d1 x3  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h6 @119 (22/28) | 8347938 1e68f11823 x25 8340327 d0185ba9d1 x3  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h7 @177 (40/52) | 8347938 1e68f11823 x42 8340327 d0185ba9d1 x10  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h8 @277 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h9 @302 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h10 @347 (0/6) | 8347938 1e68f11823 x6  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h11 @372 (18/23) | 8340327 d0185ba9d1 x16 8347938 1e68f11823 x7  | fork's Java 21 RAW-key fallback: "Java 21 has no public AsymmetricKey#getParams contract", derives the parameter set from the algorithm name | take upstream: jdk21u now ships `KeyUtil.getParams` (8340327) and its 3-case lookup, which covers the fork's fallback | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h12 @423 (2/0) | lines removed in the jdk21u range | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h13 @432 (5/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |
| h14 @445 (0/13) | 8347938 1e68f11823 x13  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, named/NamedKeys.java |

### `NamedKeyPairGenerator.java` — supersede: YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h2 @43 (1/0) | lines removed in the jdk21u range | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h3 @62 (9/15) | 8347938 1e68f11823 x12 8340327 d0185ba9d1 x3  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h4 @97 (2/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h5 @117 (0/28) | 8347938 1e68f11823 x28  | no lines (fork snapshot predates the addition) | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h6 @169 (3/3) | 8347938 1e68f11823 x2 8340327 d0185ba9d1 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h7 @196 (9/9) | 8347938 1e68f11823 x6 8340327 d0185ba9d1 x3  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h8 @223 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h9 @247 (4/5) | 8347938 1e68f11823 x5  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |
| h10 @265 (1/4) | 8347938 1e68f11823 x4  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java, all/Deterministic.java |

### `NamedSignature.java` — supersede: YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h2 @47 (1/0) | lines removed in the jdk21u range | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h3 @60 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h4 @69 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h5 @83 (10/8) | 8340327 d0185ba9d1 x4 8347938 1e68f11823 x4  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h6 @109 (5/4) | 8347938 1e68f11823 x3 8340327 d0185ba9d1 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h7 @128 (5/4) | 8347938 1e68f11823 x4  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h8 @159 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h9 @174 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h10 @185 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h11 @196 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h12 @221 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h13 @233 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h14 @242 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h15 @254 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h16 @270 (5/5) | 8340327 d0185ba9d1 x3 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |
| h17 @295 (5/5) | 8340327 d0185ba9d1 x3 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |

### `SHA3.java` — supersede: NO: fork = nearest upstream rev 890adb6410d plus 151 fork-only lines / minus 20

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @152 (14/30) | 8338587 1bf953eef0 x16 8333867 2f72ca130a x8 8320192 a4626db71c x5 base b40b6a2e8e x1  | fork's byte-state engine (200-byte `state`, `lanes` conversion) for `digest()` of an XOF; used only inside SHA3.java | take upstream (long[] state from 8333867, matches the jdk21u HotSpot SHA3 intrinsic 8341527 that auto-merges) | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h2 @203 (5/8) | 8338587 1bf953eef0 x8  | fork's `implSqueeze(byte[],int,int)` / `implSqueeze(int)` on the byte state | take upstream (8338587 internal XOF methods) | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h3 @223 (12/53) | 8338587 1bf953eef0 x53  | fork's absorb/finish on the byte state | take upstream | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h4 @295 (3/0) | lines removed in the jdk21u range | fork-only state reset lines (`Arrays.fill(state, (byte)0)`, `lanes`) | take upstream (`Arrays.fill(state, 0L)`) | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h5 @310 (2/1) | 8333867 2f72ca130a x1  | fork's `bytes2Lanes`/`lanes2Bytes` around `keccak` | take upstream (`keccak(state)` on long[]) | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h6 @342 (11/4) | 8338587 1bf953eef0 x4  | fork's argument checks on `keccak(long[])` (`requireNonNull`, length 25) | take upstream (no checks; the method is internal, callers pass the 25-lane state) | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h7 @476 (2/0) | lines removed in the jdk21u range | older jdk21u text, identical to 890adb6410d | take upstream | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h8 @524 (21/23) | 8338587 1bf953eef0 x23  | fork's lane write-back into the byte state | take upstream | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h9 @577 (6/5) | 8338587 1bf953eef0 x5  | fork-only helper lines of the byte-state engine | take upstream | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |
| h10 @595 (28/59) | 8338587 1bf953eef0 x40 8354305 c68365aaa0 x19  | fork's `SHA3XOF` streaming class (update/squeeze/reset) | take upstream (8354305 SHAKE128/SHAKE256 digests + 8338587 XOF methods) | MessageDigest/SHAKEhash.java, MessageDigest/SHAKEsqueeze.java, acvp/SHA_Test.java |

### `SHA3Parallel.java` — supersede: YES: fork blob = jdk21u 6de158a4cc3 exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8348561 80b91394c9 x1  | older jdk21u text, identical to 6de158a4cc3 | take upstream | acvp/ML_KEM_Test.java, acvp/ML_DSA_Test.java |
| h2 @42 (0/14) | 8348561 80b91394c9 x14  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_KEM_Test.java, acvp/ML_DSA_Test.java |

### `SunEntries.java` — supersede: NO: fork = nearest upstream rev ae88cd99b00 plus 1 fork-only lines / minus 2

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @197 (0/1) | base 319a3b9947 x1  | no lines (fork snapshot predates the addition) | take upstream | acvp/ML_DSA_Test.java |

### `KeyUtil.java` — supersede: NO: fork = nearest upstream rev 890adb6410d plus 7 fork-only lines / minus 1

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @195 (6/2) | 8340327 d0185ba9d1 x2  | fork's `instanceof NamedX509Key` / `NamedPKCS8Key` branches in `getKeySize`-style lookup | take upstream (`key instanceof PrivateKey || PublicKey` + `getParams(key)`, 8340327) | named/NamedKeyFactoryTest.java, java/security/KeyAgreement/Generic.java |

### `SignatureUtil.java` — supersede: NO: fork = nearest upstream rev d0185ba9d1e plus 6 fork-only lines / minus 4

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @391 (5/2) | 8340327 d0185ba9d1 x2  | fork's `instanceof NamedPKCS8Key` + `EdECKey` branches | take upstream (`KeyUtil.getParams(key) instanceof NamedParameterSpec`, 8340327); see the SignatureUtil note below | named/NamedEdDSA.java, acvp/ML_DSA_Test.java |

### `NamedX509Key.java` — supersede: YES: fork blob = jdk21u d0185ba9d1e exactly; 0 fork-only lines

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @2 (1/1) | 8347938 1e68f11823 x1  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java |
| h2 @78 (1/2) | 8347938 1e68f11823 x2  | older jdk21u text, identical to d0185ba9d1e | take upstream | named/NamedKeyFactoryTest.java |

### `java.security` — supersede: NO: fork = nearest upstream rev 890adb6410d plus 19 fork-only lines / minus 0

| hunk | upstream change | fork side | proposal | proof |
|---|---|---|---|---|
| h1 @1602 (17/84) | 8368032 400d4c96da x43 8347938 1e68f11823 x24 8381796 024300be71 x16 8379515 5adb1609d1 x1  | fork-only security property block `jdk.security.password.allowSystemIn` (17 lines), implemented by the fork's `sun/security/util/Password.java` (auto-merged) | union: upstream's 84 lines first, then the fork's `allowSystemIn` block | pqc/PrivateKeyEncodings.java |

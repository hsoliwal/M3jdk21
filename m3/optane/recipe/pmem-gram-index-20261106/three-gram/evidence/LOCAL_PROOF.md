<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 PMEM three-distinct-gram source proof — 2026-11-05

## Exact original owner
- Parent: hsoliwal/com.synexia #16590 at commit ef1e2d9cb70a20ff650bfa47d63617f4ddcad722.
- Only PmemCorpusGramIndex.java and its existing JUnit change. Original PmemCorpusSearch.java, PmemCorpusSelectedSearch.java, POSIX JNI C17 and all public APIs remain untouched.
- Two complete PRE source images and two reviewed POST images belong to the canonical M3HashPinnedJavaSnapshotRecipe.
- Original Java SHA-256: 85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd.
- Candidate Java SHA-256: 86c79682e7c373034a5f086c7241602b2203459ca188b32a02f60e42321780b5.
- Original JUnit SHA-256: ac7afd7cad94cc2c7c7a3fb63ac67216a879705518e370707ac7ff2a995adac9.
- Candidate JUnit SHA-256: 6ef88217cba20945cdab10fac70d60e55abc398587b786c1af2b288a24e3cb62.

## Executed Java 21 proof
- Strict javac --release 21 -Xlint:all -Werror on original and candidate plus API-compatible PMEM source-image stubs: PASS.
- 20,000 deterministic independent scalar-search source records, **166,887 scalar assertions per implementation**, same returned match root d404938cc6c0e8c860b468276fe8dba4b84d8fbd75008e887da583b18152fce7: PASS in JIT and -Xint.
- Published ThreeGramPublicationProof.java Git blob d6820305fa4efcae2ce6c9b7265b33bf664fef56: **115,937 scalar oracle assertions per version** (JIT and -Xint). Same semantic root e0fd91745ffe6c4aae09374070c9ebcba3e0478b5d0ffb41714f18496e5a8d7f. Original 11,000 vs candidate 2,000 KMP candidates, identical 2,000 exact matches.
- javap -p -s original/candidate compiled private class descriptors: byte-identical.
- Source and test Git blobs created from the exact reviewed recipe resources. No third-party donor source or JDK runtime copied.

## HOLD / not yet executed
- Real Maven/OpenRewrite SDK Recipe.run first pass, fixed point and source drift refusal.
- Original PMEMSDK real Java FILE backend, POSIX C17 JNI/JMOD/JLink, native sanitizer and target-native acceptance.
- Production performance/RSS measurements on 1 GB mapped code and 20,000 x 50 KB files.
- Full Synexia provenance and history convergence across hsoliwal/*.
- Chrome AI on-device availability, 20,000 actual model invocations, complete authenticated quality-plugin receipts.
- Do not merge or promote to M3JDK21 or Eclipse without these executed gates.

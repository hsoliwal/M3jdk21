# Hierarchy identifier hash encoding

`M3ASTHierarchy` continues to use schema `m3.ast.l0-l5/1`, SHA-256,
the same public interfaces, and the same accepted identifier domain.
The existing four-byte big-endian payload-length framing remains in place.
Identifiers with well-formed UTF-16 retain exactly the original UTF-8 payload
and therefore every previously valid root. The new behavior repairs the
encoding only for identifiers containing isolated surrogate code units.

## Accepted strings and the counterexample

The token contract accepts a bounded nonblank Java `String`, excluding the
hierarchy separator, carriage return and newline. It has never rejected
isolated UTF-16 surrogates. Java's `String.getBytes(Charset)` replaces malformed
input while encoding; it can encode an isolated surrogate and `?` identically.
Consequently two different admitted identifiers could previously have an
identical hash preimage. The retained baseline regression demonstrates that
failure using the actual canonical hierarchy owner.

## Disjoint extension

For a well-formed string, the payload is its original UTF-8 bytes. For a string
containing an isolated surrogate, the payload is the byte `FF`, followed by
each Java UTF-16 code unit in big-endian order. The length is exactly
`1 + 2 * value.length()`, including the discriminator.

No well-formed UTF-8 byte sequence contains `FF`. Thus the two payload domains
are disjoint. Within the extension domain, equal payloads have the same
number of code units and the same two bytes for every code unit; they represent
the same Java string. The length prefix prevents payload boundaries from
being confused with adjacent fields. This proves injective identifier
preimage encoding over the admitted domain. It does not claim that SHA-256
itself is mathematically collision-free.

The encoding scan takes O(n) time and O(1) extra space. The well-formed lane
retains its existing UTF-8 allocation; the malformed lane streams UTF-16
directly into the existing digest. No JNI or parser dependency is added.

## Compatibility and cache handling

This is a backward-compatible extension for every well-formed identifier.
An old root involving a malformed identifier is ambiguous and must be
recomputed from the original identifier and file facts. Such roots are not
silently promoted to exact identities. The source successor receipt records
the precise old and new source hashes and canonical recipe commit. The
historical `SOURCE_BORROW.tsv` remains unchanged.

The default verifier accepts the original pinned owner when no successor is
present. Once the successor is present it requires the exact reviewed
transition, the immutable canonical publication pin, the complete sealed
recipe custody packet, and all actual SDK-produced target bytes. It checks
retained SDK qualification using the existing canonical `receive.py`, then
compiles and runs both Java contracts twice. Missing, drifted, partially
received, or unpinned successors fail closed.

## Qualification scope

The new standalone test freezes a fingerprint of 60 existing valid roots
across all five identifier scopes and checks all 2,048 isolated surrogate
code units at each scope, including mixed-pair and length-boundary cases.
It performs 10,311 checks. The existing 2,000-file contract retains 39,550
checks, 17,967 atoms and the same root. The existing JUnit wrapper runs both
contracts in the independent target Maven module. Public API comparison
matches the original source.

The custody packet retains actual OpenRewrite SDK results and five SDK
qualification tests for each of the canonical and target datasets. Running
`verify_receiver.py` verifies that retained qualification; it does not rerun
Maven/OpenRewrite. To rerun SDK qualification, copy the custody packet into a
scratch checkout, relocate `qualified/<dataset>` into `target/<dataset>` only
for retained-result inspection, and run its POM with a fresh output directory
and `-Drecipe.owner.root=<copy>/sdk-owner`. Rebuilding the checked-in custody
packet in place changes immutable evidence and is intentionally refused.

No whole Synexia reactor, JDK image, jtreg compiler integration, JNI, custom
Maven/Gradle fork build, parser cache persistence, or performance improvement
is claimed by this bounded receiver proof.

## Sources and attribution

- Existing owner: `hsoliwal/com.synexia`,
  `synexia-indexstring/src/main/java/com/synexia/indexstring/ast/MIndexASTHierarchy.java`,
  baseline `98368e107f4a205d852ef958cd48c1ee633219b1`.
- Existing recipe owners and task crate: `synexia-openrewrite-recipes`,
  `m3-collections-contract-refinement-20261017`; Apache-2.0 notices retained.
- [Java 21 String API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#getBytes(java.nio.charset.Charset)):
  malformed-input replacement semantics and Java UTF-16 representation.
- [RFC 3629](https://www.rfc-editor.org/rfc/rfc3629): valid UTF-8 excludes `FF`.

The adaptation reuses Synexia's existing digest, hierarchy and recipe owners.
No third-party implementation text was copied from the reference documents.

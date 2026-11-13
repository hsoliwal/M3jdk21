<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST multi-frontend coverage: Eclipse, Maven, Gradle, Jackson

## Baseline and scope

M3JDK21 already shares original Java compiler AST tasks in Maven/Gradle
(compilation-owned M3AST, merged PR #1382), provides compiler M3AST L0-L5
hierarchy, and has a merged optional Eclipse substrate. The JSON M3AST and
token regex are separate draft PRs, not merged runtime owners.

The missing surface is an interoperable source-coordinate and occurrence
index, not an excuse to replace any original parser or synthesize semantic
information that the parser did not provide.

## Implemented additive slice

M3ASTIndex: indexed immutable view over an existing M3File owner and primitive
lanes. Eight int lanes per occurrence: kind ID, parent, UTF-16 start/end,
flags, firstChild, nextSibling, child count. Bounded temporary builder
holds the last-child pointer and small string kind dictionary. No
Map.Entry-backed node storage, per-occurrence ASTNode payload, or second
String owner. Unknown ranges are exactly (-1,-1), never guessed. Exact source
text is available only from verified UTF-16 coordinates. No row index is
globally stable outside its source snapshot.

M3ASTIngress: explicitly bounded source/AST projection functions:
* Eclipse JDT: uses the original JDT DOM public structural properties and
  UTF-16 positions, marks recovered/malformed nodes. No live AST handles
  survive projection. Real JDT/PDE/OSGi/working-copy test is still required.
* Maven POM: secure namespace-aware JDK XML DOM, rejects DOCTYPE and
  external entities, preserves duplicate elements, comments, CDATA,
  processing instructions and attributes. DOM lacks exact child text spans
  and effective model facts; those remain unavailable. Maven ModelBuilder
  retains inheritance/profile/plugin semantic authority.
* Gradle Groovy/Kotlin: exact lexical envelopes for identifiers, comments,
  quoted strings and balanced blocks. This is syntax-only, not Groovy/Kotlin
  AST semantic binding, plugin resolution or Gradle tooling model. Real
  Gradle provider remains javac compilation authority.
* Jackson: reads existing public jdk.m3.jackson.M3AST occurrence handles,
  preserving repeated references as distinct ordered occurrences. Kinds
  and topology only: original generic JSON M3AST currently has no verified
  parser-to-M3File source spans, so JSON coordinate ranges remain unknown.
  The public-handle bridge is NOT an allocation-free 100 MB fast path.

The original parser AST is not replaced or copied. The new primitive owner
is a detached *source index* only, not a compiler, effective build model,
JDT binding store or JSON parser.

## Source custody

Canonical source and recipe owner: hsoliwal/com.synexia.
Product source owner: hsoliwal/M3jdk21 under the existing jdk.m3.parser JMOD.
Original JDK String/M3 String owner is unchanged. Eclipse and Jackson sources
are not copied; JDT/Jackson are optional API-shaped access paths. Maven XML
uses standard JDK java.xml. Gradle lexical indexing is first-party code.

Canonical recipe: exact Git blob pinned Java inputs plus production
module-info.java/build.py preimages. Maven profile drives exact verify/apply.
The receiver must reach a no-change fixed point on replay; source drift and
path collisions refuse before any writes. It is not proof of attributed
OpenRewrite Java refactoring.

## Required qualification

Strict Java 21 compilation; real JDK XML XXE refusal, source/UTF-16 bounds,
multi-frontend occurrence order, Gradle comment/string decoys and group
errors, JDT/JSON public-shape tests, full original parser suite/JMOD linked
image. Real Eclipse JDT target-platform binding and reconciliation, Maven
effective-model parity, Gradle Groovy/Kotlin compiler/model parity, actual
Jackson module integration, 100 MB JSON AST round-trip/search and memory
measurements, modified full M3JDK/jtreg, cross-platform verification are
pending, not waived.

Do not use token-event positions or unknown DOM positions for source rewriting.
Only JDT/LTK or original Gradle/Maven/Jackson authorities may approve an edit.

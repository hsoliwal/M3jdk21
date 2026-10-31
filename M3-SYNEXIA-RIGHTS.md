# M3 / Synexia First-Party Rights and Provenance Notice

Copyright © Hitesh Soliwal and the respective contributors to the Synexia/M3
first-party work identified by repository history and provenance records.
Dates of creation, authorship, contribution and publication are evidenced by
the applicable source history, commits, pull requests, design records, hashes,
receipts, specifications and other dated project records.

## First-party work

"M3/Synexia first-party work" means independently authored project material,
including, where applicable, M3, M3Scale, M3 String, M3Index, M3 precomputation,
M3 recipes and convergence machinery, source code, concrete implementations,
tests, benchmarks, specifications, documentation, diagrams, schemas, manifests,
configuration, generated first-party material, and protectable selection,
coordination and arrangement of those materials.

Publication in a public repository does **not** transfer ownership, abandon
authorship, dedicate first-party work to the public domain, or erase provenance.

## No assignment or implied ownership transfer

Except where an express written assignment says otherwise, a copyright or other
right owned by an author or rights holder remains owned by that author or rights
holder.

An applicable open-source license grants the permissions stated in that license.
It is a license, not an assignment of authorship, inventorship, provenance or
ownership.

Nothing in publication, forking, cloning, redistribution, modification,
integration, benchmarking, discussion, citation, or use of M3/Synexia
first-party work authorizes a recipient to represent itself as the original
author, originator, inventor or owner of first-party material that it did not
create or lawfully acquire.

No additional license, waiver, ownership transfer or public-domain dedication
is implied beyond the rights expressly granted by the license governing the
particular material.

## Copyright and other legally available rights

Copyright is asserted over protectable original expression to the fullest extent
recognized by applicable law.

To the extent M3/Synexia technical work also embodies inventions, know-how,
architectures, technical solutions, methods, data structures, combinations or
other subject matter for which separate legal rights may exist, publication of
this notice is not intended to waive, surrender or disclaim those legally
available rights.

This notice does not itself create a patent, trademark, trade-secret or other
statutory right, and it does not enlarge rights beyond applicable law or an
applicable license.

## Open-source grants remain controlling

Where first-party material is distributed under Apache-2.0 or another
open-source license, recipients retain every permission expressly granted by
that license, including any applicable copyright and patent grants.

This reservation therefore must not be interpreted to revoke or narrow an
existing express open-source grant. It reserves ownership and all rights that
were **not** granted.

## Third-party and upstream boundary

This notice claims **no ownership of third-party or upstream material merely
because that material appears in this fork**.

OpenJDK, Eclipse, SWT, Nebula, TornadoVM, OpenRewrite, Gitea, GEF and every
other upstream/donor work remain owned by their respective rights holders and
remain governed by their applicable licenses, notices, exceptions, attribution
requirements and other terms.

A first-party M3/Synexia modification to an upstream-derived file does not
relicense the upstream work and does not convert third-party authorship into
M3/Synexia authorship.

Likewise, third-party material in a combined work does not erase or transfer
ownership of independently authored M3/Synexia additions.

## Provenance reservation

Repository history is evidence of provenance, not the sole possible evidence.
Earlier source history, design notes, specifications, experiments, dated files,
hashes, correspondence, issue/PR records and other records may also establish
conception, authorship, chronology and origin.

Moving, squashing or consolidating unreleased Git history for a clean release
line does not intentionally abandon the underlying authorship or provenance
record. Supporting historical evidence may exist outside the release branch.

## Attribution and representation

Applicable license and NOTICE obligations must be preserved.

Nothing in any M3/Synexia license grant authorizes false attribution, false
claims of original authorship, or false claims that independently authored
first-party M3/Synexia work originated from another person or project.

## JEPs, standards proposals, and technical publications

A JDK Enhancement Proposal (JEP), JSR, standards submission, paper, presentation,
or downstream design that discusses or builds on M3/Synexia work does not, by
its submission, numbering, acceptance, or publication, transfer ownership of
that work or establish that its proposer originated the underlying M3 contribution.
Authorship of a new proposal must be distinguished from authorship of material
and designs on which it relies.

**Project attribution policy:** proposals and publications materially drawing
on M3/Synexia designs should identify the source contributions, cite the relevant
versioned design/source records, distinguish prior M3 work from the proposer's
new contributions, and avoid presenting M3-originated material as independently
originated work. This citation policy expresses the project's expectation for
accurate provenance; enforceable obligations remain those imposed by the
applicable license and law. It adds no restriction to existing license grants.

When copying or adapting protected source, documentation, diagrams, or other
expression, preserve the applicable copyright, attribution, NOTICE, and
modification records as required by the governing license. A citation alone
does not substitute for compliance with those requirements.

### Published design record

The following record provides a concrete reference for the design described
in the paper, without claiming that this is its earliest conception or proving
exclusive invention of every component:

- **Work:** M3: Shared Structure and Reusable Computation in a Java Runtime.
- **Attributed authors:** Hitesh Soliwal and Contributors to the Synexia Project.
- **Version/date:** technical design paper v0.1, 7 October 2026.
- **Publication commit:** `ace350f716614b50cf77d7462b1762b7eb8664ca`.
- **Permanent reference:** [paper at its publication commit](https://github.com/hsoliwal/M3jdk21/blob/ace350f716614b50cf77d7462b1762b7eb8664ca/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md).
- **Design scope:** canonical shared immutable payloads; range/composition
  identity; indexed metadata and reusable String/regex precompute; M3JDK21
  runtime ownership; Synexia recipe convergence; proposed collection and
  SWT/Eclipse integration; compatibility constraints and evaluation programme.

Suggested citation: Hitesh Soliwal and Contributors to the Synexia Project,
*M3: Shared Structure and Reusable Computation in a Java Runtime*, v0.1,
7 October 2026, publication commit `ace350f716614b50cf77d7462b1762b7eb8664ca`.


### Version 0.2 extension

- **Version/date:** technical design paper v0.2, 7 October 2026.
- **Publication source commit:** `b4a6b240cd996e35649d58de1780514108b3fd34`.
- **Permanent reference:** [v0.2 paper at its source commit](https://github.com/hsoliwal/M3jdk21/blob/b4a6b240cd996e35649d58de1780514108b3fd34/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md).
- **Added scope:** source-grounded hierarchical lexicons, separate spelling/
  lexeme/concept identity, exact lexical translation and family bundles,
  multilingual qualification, AST/binding boundaries, reuse across structured
  data, and M3SDK as the wider qualified delivery direction for Synexia, with
  M3JDK21 as its Java runtime subset.
- **Donor inspection revision:** `d9098bb5341a1b95750814044b8bb52616cc2c61`
  in `hsoliwal/com.synexia`; the paper links inspected owners and distinguishes
  historical focused receipts from fresh target qualification.
- **Filing intent:** patent filing is planned. This record establishes no filed
  application, patent-pending status, patent grant or exclusive invention claim.
  Existing licenses and applicable copyright/patent grants remain controlling.

The v0.1 record and earlier provenance remain intact. This extension records
additional expressive design documentation; it does not claim ownership of
abstract methods or third-party work.


Earlier project history and independently preserved records remain relevant to
authorship and chronology. This record does not assert that every general
technique described in the paper is original to M3.

This notice does not prohibit independent proposals, lawful discussion,
independently created implementations, or uses permitted by existing licenses.
It does not establish patent rights or a veto over the OpenJDK process.
The [JEP process](https://openjdk.org/jeps/1) governs proposal handling; the
applicable material's license governs reuse.

## Reservation

**All ownership and all rights not expressly granted by the applicable license
are reserved to their respective authors and rights holders.**

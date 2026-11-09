<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Synexia M3 invariant: validated per-file refinement

M3JDK21 owns the integrated Java, HotSpot and JNI runtime. Synexia is the canonical
convergence workspace for reusable recipes, donor evaluation, source custody and
proof. Receiving generated source does not create a Synexia runtime dependency.

## Required transformation cycle

1. Identify the owning file, its exact revision, and the operation's problem
   category. Search the accumulated catalogue before starting another discovery
   pass. Retain unsuccessful candidates and the reason they were rejected.
2. Write the contract before changing the implementation. Include successful
   results, exception type and observation order, callback/comparator order,
   identity, null handling, liveness, mutation, resource ownership and the native
   ABI where relevant. Distinguish an explicit defect repair from an equivalent
   implementation of behavior that was already correct.
3. Compare the incumbent, suitable existing Synexia/M3 atoms and external donors.
   Record correctness, compatibility, complexity, temporary and retained storage,
   measured performance scope, source revision and applicable rights. A problem
   page is discovery evidence; it does not license a submitted solution or prove
   a target contract. Keep mixed or negative performance observations.
4. Refine serially, file by file, through the canonical recipe. A file may contain
   any useful number of independently replaceable behavioral atoms. Each atom
   must have a meaningful semantic responsibility; decomposition must improve
   replacement, reuse or verification. There is no arbitrary atom-count limit.
5. Seal exact preimages, postimages, modes and source/tool inputs. Run the actual
   recipe SDK, its behavioral tests, fixed-point checks and refusal cases. Source
   drift, representation changes, unsafe paths, changed qualified inputs and
   tampered SDK outputs must be refused before target writes. Preserve existing
   engine and policy gates unless a separately justified correction is itself
   qualified.
6. Receive only the SDK-emitted bytes. Verify replay preserves bytes and file
   modification times. Qualify the owning compiler, Java/JNI behavior, native
   ABI, module packaging and installed product required by the affected contract.
   Source transformation proof and runtime acceptance remain separate records.
7. Append the decision, exact source mapping, result, failed attempts and limits
   to the catalogue and canonical recipe memory. A successor has new explicit
   preimages; it does not rewrite an earlier receipt. Compose an original-to-final
   dataset when overlapping successors need one final replay target.

Public interfaces and established contracts remain unchanged. A declared defect
repair must identify the violated contract, demonstrate the old failure and test
the corrected behavior. It must not describe the defective result as equivalent.
Licenses and attribution follow each source or payload; a recipe's license does
not relicense its output or a donor. Existing promotion gates continue to apply.

## String composition contract

The valuable unit is the shared payload owner plus its range coordinate. A tuple
retains immutable child coordinates and composes their content. Slices preserve
owner lifetime. Consumers should use the existing composed representation when
their contract permits it, and native/mapped ownership must remain live for every
consumer that can still observe the range.

This packet preserves the ordinary String API and existing internal layout
contract. It adds no second retained spelling of a composed String. Tuple and
coordinate metadata still consume space. Mutable Java result arrays and a JNI
request for contiguous characters have their own allocation and lifetime
contracts; they are not assertions that arbitrary Java arrays alias a native
mapping. Budget refusal may retain the established flat String representation.

An identity shortcut must prove identity. Ordered equal child owners and packed
coordinates are sufficient for exact tuple reuse. A hash, route key, class name,
observed element value or equal-looking payload is not sufficient. Different
parenthesizations and collisions retain the exact normalized leaf comparison.

## Collection contract

Dynamic arrays, offsets, physical/logical indexes and state masks remain the
owning representation. Filtering and sorting preserve occurrence identity,
hidden entries, backing liveness, mutation rules, callback order and stable key
association. A wrapper must not silently replace its original backing owner.

Classification of known JDK wrappers may read owner metadata; traversal and
mutation still use the original wrapper so checked, synchronized and unmodifiable
behavior remains observable. Generic proving-ground owners are not guessed to
have JDK identity semantics. JNI kernels retain bounds checks, both state passes
where required, exact output ownership and generated-header/export agreement.

## Enforcement and review entry points

The existing canonical host is
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017` in
`hsoliwal/com.synexia`. Its unchanged `prepare.py`, actual OpenRewrite test harness,
`receive.py`, preparation contracts and receiver/refusal contracts enforce exact
source custody and application. The owning runtime fixtures and installed-image
receipts enforce the bounded behaviors exercised in this packet.

This document records the user's requested invariant in the receiving product;
it is not a new competing recipe engine or a claim that all repository families
have completed their promotion gates. See `SOURCE_RECEIPT.json`,
`FILE_PASSES.tsv`, `OPEN_GATES.md` and the problem-category catalogue for the exact
admitted frontier.

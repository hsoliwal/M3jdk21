<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Independent review of the proposed wrapper postimages

## Outcome and scope

The proposed `M3FilteredLists` cursor mutation guard is rejected. The baseline to
candidate regression and the incomplete JDK subList invalidation are described
and executed in this diagnostic package. The other five com.m3 production
postimages have no additional blocking issue identified in this source review.
The proposed `java.util.CollectionViews` body also has no additional blocker
identified: after package/class normalization it is byte-identical to the
reconciled `M3CollectionViews` body.

The review compared full diffs and the surrounding implementations against
`2779aba3f98f31c324be43f731be7914e1a05935`, with special attention to callbacks,
exception identity, mutation order, backing authority, and physical storage.
The source seals in `diagnostic-manifest.json` identify the reviewed postimages.
These conclusions apply to those bytes. The parent qualification work owns the
ordinary suites, recipe replay, bootstrap/core tests, and native qualification;
this review does not claim those gates were run by these diagnostics.

## Reviewed production atoms

| Target | Independent source assessment |
| --- | --- |
| `M3FilteredViews` | The new sorted reversal retains the existing default backed reversal, then restores the strict read-only mutation boundary. Navigable reversal delegates to the existing descending view. The endpoint removals refuse immediately, including empty views, without running source or predicate operations. Existing navigation exception recovery is unchanged and remains pending. |
| `M3FilteredNavigableSet` | The two new endpoint removal overrides directly refuse mutation. They do not scan the owner or evaluate the predicate first. Existing comparator, range, and split behavior is unchanged. |
| `M3CollectionViews` | The new COW bulk branch delegates one composed predicate to the actual owner's `removeIf`. Visibility precedes the operation predicate with short circuiting. All non-COW owners keep their existing iterator mutation path and partial-write behavior. This explicitly repairs the JDK COW snapshot iterator's unsupported removal rather than claiming equivalence with its previous failure. |
| `M3LongIndexProjection` | Opposite known sort direction reverses the order of equal-value runs while copying each run's physical indices intact. The scan remains inside the current `[from, to)` window. It allocates only the final primitive index array and keeps the shared payload owner and stable occurrence order within ties. Same-direction, singleton/empty, and encounter-order fallback paths are unchanged. |
| `M3PrimitiveFilteredSequences` | Deep list/deque traversal opens only the implementation's own private view chain, stores predicates in inner-to-outer order, and creates one cursor over the opaque physical owner. Predicates are called directly, preserving short circuiting and thrown objects without invoking overridable `LongPredicate.and`. The plan holds predicate metadata, not element results. Depth one/two traversal and writable view authority are unchanged. |
| `java.util.CollectionViews` | The seven proposed factories remain the four Collection/Set filtering overloads and three snapshots. The complete normalized body matches the reviewed M3 collection body. It adds no map/list filtering API, runtime facade, or separate transformation engine. Its actual Apache header and core license review gate are preserved. |

The COW call remains virtual. An arbitrary subclass's override retains its own
`removeIf` behavior; atomic survivor publication is the behavior of the JDK
owner implementation and delegating subclasses, not a guarantee imposed on all
hostile overrides. The branch does not copy an alternative payload store or
reimplement the COW publication protocol.

The deep primitive change bounds the implementation's traversal stack. Methods
such as nested mutation, `contains`, or capacity delegation remain outside that
atom, and caller predicates can recurse independently. No whole-family or
unbounded-depth guarantee is inferred from the iterator refinement.

## Blocking rejection retained as evidence

The `M3FilteredLists` postimage inserted a physical `next()`/`previous()` probe
before indexed mutation. Restoring position does not undo a source read: the
included lazy list's valid `set(8)` changes from tape `[set:1:8]` and success to
tape `[get:2]` and the exact unrelated caller exception. Separately, a real JDK
subList's exhausted `next()` throws before checking its root iterator; catching
that exception as validation lets all four recorded stale mutations proceed.

The package runs both the unchanged required-behavior assertions and explicit
positive defect observations. The required list oracle remains RED for the
rejected write regression and for the stale subList cases. Neither a passing
donor suite nor a successful diagnostic observation overrides those failures.

The executable proof sources now reside in this package's
`src/test/java/com/m3/collections/` source root. Their exact Java bytes and
required-behavior assertions are unchanged by that path correction. The source
mapping is sealed in `diagnostic-manifest.json`; the fresh
`recorded-replay-layout-amended.json` supplements the unchanged original replay.

## Remaining owner boundaries

Comparator-failure classification requires participating range-owner control
flow, because an exception object cannot identify the origin of its current
throw. The integrated collection receiver currently provides no filtered map or
list API for such a new helper to serve. Its present source intake therefore
does not justify adding unused TreeMap helpers or public contracts.

Read-only indexed iteration must retain the frozen predicate demand while
validating the actual physical owner's generation. A generic opaque `List`
exposes no callback-free generation read. The diagnostic README maps the
existing final M3 array/slot list owners and their real epoch fields as a future
resolution boundary; it does not substitute an arbitrary type whitelist or
transport probe for the unresolved contract.

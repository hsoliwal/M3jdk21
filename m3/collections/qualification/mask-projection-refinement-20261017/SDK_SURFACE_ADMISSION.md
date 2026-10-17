# SDK superset surface and M3 backing admission

SPDX-License-Identifier: Apache-2.0
Copyright 2026 Hitesh Soliwal and contributors

The user clarified this is an SDK superset, not a renamed imitation of existing libraries.
Public JDK names and qualified Guava, Eclipse Collections, Spring Framework and Apache
Commons Collections API names retain their original identity and semantics. M3 names identify
backing data structures and target-owned extensions, not forged third-party packages/classes.
Additive SDK capabilities must not remove or weaken the existing API surface.

The current target at d47a4b4fdb8e3794ec56768b06aa9e23730c6b8e already provides
m3/collections/interop M3LibraryCollections. Its source was fully read: exact Git blob
35d419cc78f4fe583ea97cab3026d1dbf037f795. The optional interop POM, blob
f105d56a842c3aa75e9e7a027d4c0d893699d102, pins Guava33.4.8-jre, Eclipse Collections11.1.0,
and Spring Core6.2.0. This inventory is not a fresh dependency-wide build or blanket admission.
No dependency versions, interop factory methods or public interfaces are changed by this task.

Existing mappings include java.util List/Deque to M3LinkedList, Map to M3LinkedHashMap,
NavigableMap/NavigableSet to M3TreeMap/M3TreeSet; Guava ListMultimap/SetMultimap/
SortedSetMultimap/Table through the donor's adapters and M3 owners; Eclipse MutableList/
MutableMap/MutableSortedMap/MutableSet/MutableSortedSet through its actual adapters;
Spring org.springframework.util.MultiValueMap through M3SpringMultiValueMap and existing
M3 owners. Preserve differences such as pair-count versus key-count size, absent versus
empty mappings, supplied-list aliasing, duplicate multiplicity and sorted comparators.
Do not substitute one universal multimap contract for these distinct APIs.

Apache Commons Collections uses org.apache.commons.collections4.MultiValuedMap, not Spring's
MultiValueMap or Guava's Multimap. An M3-backed Apache adapter remains PENDING in this bounded
receiver: do not invent a class or mark it implemented because a naming row exists. The
SDK mapping emitted by this recipe records exact external names, existing backing owners,
source inventory evidence and explicit pending/excluded acceptance dispositions.

Array/index/mask mechanics are shared only below a contract-preserving adapter. Reference
payloads remain GC-traced references; byte flags describe application state, not JVM object
liveness. Detached sorted snapshots, live filtered wrappers and immutable offset views stay
separate contracts over their owning storage. Stable ties preserve occurrence identities.
Synexia and other nonpublic workspaces remain convergence workspaces; qualified target changes
belong in M3JDK21 with reusable recipes and findings returned to Synexia.

A best-candidate selection requires a recorded problem category, donor rights, exact source,
functional/refusal corpus and resource measurements. A regression must remain visible. Complete
Guava/Eclipse/Spring/Apache/JDK replacement, concrete-class serialization compatibility, JDK
runtime promotion and a global performance superiority claim are not established by this task.

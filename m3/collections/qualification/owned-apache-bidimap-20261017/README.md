# Owned M3 BidiMap implementation

M3BidiMap implements the complete Apache BidiMap interface over two existing
M3LinkedHashMap payload/index owners. It owns uniqueness/eviction, inverse,
mutation, views and scalar map-cursor behavior. There is no Apache implementation
superclass or delegate. Apache imports are API interfaces only. No entry node is
retained in map storage; temporary entries are allocated by entry-returning APIs.

Construct it as new com.m3.collections.apache.M3BidiMap<K,V>(). Put replaces the
key mapping and evicts any other key owning its value. Null keys/values, insertion
orders in both directions, cached live inverse and inverse-of-inverse identity
are supported. The API values Set follows forward mapping order. MapIterator
uses the M3 key-slot cursor and scalar key/validity state without per-row entries.

The Apache4.5 protocol body is adapted with its original header, license and
notice retained. Version quirks are intentional: the guarded entry setter returns
the installed value; map cursor setter returns the prior value; duplicate checks
use key identity; the values Set retains provider identity equality/hash. Raw
entries from toArray variants bypass inverse maintenance, as in the pinned
provider; iterator/splitter/stream/forEach entry setters remain guarded. Both
forward and inverse states, including these raw-array effects, were compared.

Executed with actual Commons Collections4.5 and strict Java21:12 oracle groups,
8721 cases,580058 checks and8000 fixed-seed operations;66 additional paired view
traversal/generator/callback cases, all equivalent; two real OpenRewrite recipe
tests across8 subset/order states, marker-free fresh-byte replay and4 refusals.
The receiving templates are byte-exact tested source. Existing M3 public owner
interfaces remain unchanged. No speedup or installed-JDK replacement claim.

This delivers one owned collection implementation, not every class in Apache or
all five libraries. Donor concrete class identity/serialized wire form and stale
entries after external structural changes are not claimed equivalent. Hosted
CI and independent source review remain pending; the PR stays draft.

V2 review refinement: a real values-iterator replacement discriminator exposed a mismatch in V1. V2 retains the delivered value in scalar cursor state and preserves the pinned provider inverse-state quirk. Eight forward/inverse key/value/entry/map-cursor replacement cases now pass, along with the original580058checks and66traversal comparisons. V1 recipe resources remain byte-identical. New recipes receive V2 from absent targets or transform only the exact V1 body; six real recipe tests pass including fresh marker-free byte replay and refusals. Shared core source changes now trigger the oracle workflow. Hosted CI and review recheck remain pending.

V3 lifecycle refinement clears the delivered-value scalar only after successful iterator removal, matching donor release timing. V1 and V2 receiving resources remain immutable; new full/transition V3 recipes pass with ten total tests. Full580058checks,66traversal and8replacement cases rerun and pass. Source lifecycle review is not a garbage-collection performance measurement.

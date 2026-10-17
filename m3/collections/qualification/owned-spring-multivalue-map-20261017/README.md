# Owned M3 Spring map-of-lists implementation

`com.m3.collections.spring.M3MultiValueMap` directly implements Spring6.2.0 `MultiValueMap`. M3LinkedHashMap owns the indexed directory; M3ArrayList owns every mutable list created internally. Caller-supplied lists remain aliased. The independently owned `M3SingleValueMapView` implements the live scalar view and creates its own serializable immutable one-element array lists. No Spring implementation superclass, delegate or helper instance supplies collection behavior.

Includes default/expected-size/Map-copy construction, insertion order, null/empty-list distinctions, full multi-value operations, selective Map dispatch, live Map views, shallow clone, independent deep copies and own object-graph serialization. Existing provider-backed public factories remain unchanged until a separately qualified compatible migration; this PR does not relabel those adapters as owned bodies.

Root executed strictJava21 compilation and real-provider comparisons:142073checks,5234paired operations,5000fixed-seed random operations; alias/callback/view/singleton/copy/serialization cases; six explicit diagnostics. Two concrete LinkedMultiValueMap structural-forEach callback-trace differences are recorded. The selected M3-backed real provider protocol is compared strictly in those cases. Own serialized wire/class identity, concrete ArrayList capacity/revision behavior and blanket donor parity are not promised. Performance has not been benchmarked.

The unchanged canonical Synexia Java snapshot engine receives exact tested source bytes. Two real OpenRewrite tests pass with16subset/order states, fresh marker-free replay, fixed points and6refusals. PlainText receiving mode provides custody; actual strictcompile/runtime evidence is separate. `verify.py` checks source/template custody before and after qualification.

Workflow compiles the current core plus the owned sources and executes the same oracle against the real pinned provider. Changes to shared core owners trigger it. Hosted CI and independent source review remain pending. This is one Spring capability toward the full five-library M3 superset, which remains unfinished.

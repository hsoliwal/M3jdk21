# Full-extent reuse and compact slice admission

SPDX-License-Identifier: Apache-2.0
Copyright 2026 Hitesh Soliwal and contributors

The first qualified candidate shared the complete parent index when re-sorting a same-order
slice. The pass-0004 retention probe showed a 10-element result retaining 100000 index ints,
versus 10 in the target baseline. Both already retained the same 100000-long payload owner.
Those results and that candidate remain historical evidence, not final admission.

Refine the recipe before receiving any target output: reuse an immutable index only for its
complete extent. For a partial same-order view, copy exactly its physical-index range without
running the stable merge. The order certificate still removes all comparator/sort work; the
result does not increase retained index capacity relative to the baseline sorted result.
No arbitrary density/size threshold or adaptive policy is introduced. This is the same exact
full-extent condition used for read-only compactor ingress. Opposite order continues to use
independent mutable scratch; plain slice() remains an unchanged zero-copy offset view.

Public interfaces, order, duplicates, physical positions, result identity and output isolation
are unchanged. The new resource fixture must check both full-range sharing and compact partial
results; original target and functional oracle cases remain unchanged. Revise only templates
and fixtures, regenerate with the qualified recipe, and repeat all ordered Java/JNI gates.

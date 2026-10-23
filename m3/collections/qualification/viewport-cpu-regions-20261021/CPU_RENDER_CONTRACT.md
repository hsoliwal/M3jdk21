# Intrinsic viewport CPU/rendering contract

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

This proposal records the intended invariant; the full renderer integration is not implemented here.

**The viewport is the boundary between canonical CPU state and reconstructible rendering. It is intrinsic to the original widget, not another wrapper/control.**

| Responsibility | Owner and invariant |
| --- | --- |
| Canonical model | CPU-owned dynamic primitive/reference arrays, offsets, physical-slot/index projections, masks and generations; stable logical identity survives scrolling, filtering, sorting and rendering-resource eviction. |
| Viewport | Original widget owns visible interval/spatial region, bounded overscan, clipping, scroll geometry and the logical-ID mapping used for presentation and input. It does not copy the entire model or discard essential offscreen state. |
| Presentation | Theme, locale/language, fonts/shaping, direction, scale and geometry are explicit inputs. Paint-only changes invalidate paint; metric/shaping changes invalidate layout. Locale-sensitive sorting is an explicit query change and may change order. |
| Rendering | GPU/native/software backends consume admitted projections and damage. Buffers, textures, glyph caches and geometry are derived resources, not canonical state. Context loss recreates them from CPU authority. |
| Interaction | Input, selection, expansion, focus, accessibility, IME, event ordering and disposal retain existing widget contracts. Hit-testing must use the corresponding presented projection, not a stale slot reused for another logical item. |

A render submission binds model, projection, layout, presentation and device generations. Obsolete submissions are rejected before publication. GPU buffers must not be overwritten or reclaimed before their consumers finish; CPU ownership and native lifetime remain authoritative. This is a contract requirement, not a claim that fences/generation wiring has been implemented by the current kernel changes.

Share algorithmic atoms; do not force one renderer or one data structure on every widget. Preserve each original public widget and collection contract. For GEF, geometry crossing the viewport remains visible even when endpoints are outside it. Theme/language changes must not silently rebuild or duplicate canonical payload storage.

Mandatory refinement discipline remains catalogue -> meaningful behavioral atom -> source-sealed reusable Synexia Maven/OpenRewrite recipe -> serial per-file application -> compiler/test/native/runtime evidence -> fixed point and refusal -> reviewed promotion. Candidate and historical evidence do not grant completed-product status.

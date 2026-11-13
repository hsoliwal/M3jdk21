# M3SpanIndex receiver and qualification

Canonical OpenRewrite recipe: [Synexia PR #12111](https://github.com/hsoliwal/com.synexia/pull/12111).
M3JDK21 receiving base: 4e29dc9d4141261e8763fbe9ffd6a20e017b1aff.

## Data structure

This first-party Java 21 collection adapts the sparse interval projection
from Nebula GridRowSpanIndex (source blob
1763d24a2c419239f6e600bc4cabec08bdf68f27).
It stores starts, inclusive ends and full-width state masks in aligned primitive
arrays. Stable primitive index sorting produces immutable, mask-filtered
prefix-maximum snapshots. An earliest-covering query takes O(log k) for k
selected spans, without per-element Entry or Span objects.

JNI is optional: a C11 native batch kernel receives primitive arrays only.
No Java predicate, SWT handle, event or object reference crosses the boundary.

## Proof and promotion

Four new Java/native files were copied mechanically from the exact source
images in the canonical recipe. See RECEIVER_SOURCE.tsv for SHA-256 and
Git blob identifiers. No existing production file was modified.

The focused Java 21 strict-compiler oracle passed 128,804 assertions.
The separate native C11 strict-compiler and loaded-JNI oracle passed the
same 128,804 assertions. A 65,536-span descending source case was included.

Maven/OpenRewrite source-generation execution, complete M3 collection build
and hosted CI are pending. This PR is a draft until those gates are green.

The Nebula original owner already answers this interval query. Its open
sort and visibility improvements (#240 and #243) must be preserved.
Any later widget integration requires a separate source-sealed recipe,
a demonstrated additional semantic capability, and native paint, scrollbar,
selection and disposal contract tests. Never add a mandatory M3JDK21 library
dependency to the ordinary Nebula widget.

The donor and algorithm-category comparisons reside in the canonical
Synexia recipe's CATALOGUE.tsv. External coding challenge pages are used
as category evidence only, not copied solutions.

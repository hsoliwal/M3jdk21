# M3 bulk scheduler registration freeze

Source authority is Synexia PR #10475, recipe
`M3Jdk21BulkProviderRegistrationFreezePortRecipe`. The M3JDK receiver uses the
existing `M3Jdk21HashPinnedSnapshotRecipe`; `java.base` acquires no Synexia
or TornadoVM dependency.

Original regression: scheduler copied provider instances but continued to query
potentially mutable `id()` and `priority()`, allowing registration identity and
published ordering to drift after construction.

The corrected target `BulkScheduler` retains immutable registration triples
`(provider, id, priority)` and checks their consistency before/after provider
admission and after execution. It retains all prior public source signatures,
no global provider registry, and no hidden provider fallback.

The jit/jtreg test checks:
- frozen published IDs after caller-side metadata mutation;
- identity drift refusal;
- priority drift refusal;
- mutation from within `supports()` refused;
- mutation from within `execute()` refused;
- unchanged stable provider execution.

This is candidate hardening, **not** promotion into a public JDK API. The
receiving invariant's active String phase remains unchanged. JDK build/jtreg,
provider hardware parity and resource evidence are separate gates.

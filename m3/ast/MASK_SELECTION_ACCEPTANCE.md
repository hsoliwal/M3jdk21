# M3AST L0 collection family and state-mask selector

Receiver based on M3JDK21 #1090 `533085f39e7091b2adf3f33ea145c59b81def6b8`. Canonical recipe lives in the Synexia owner and is pinned by `SOURCE_BORROW_MASK_SELECTION.tsv`. No second canonical implementation, no java.base or HotSpot changes. No JNI symbols are added; the existing mask compaction JNI path remains distinct.

New operations return stable global occurrence coordinates or canonical pool handles, preserving order and duplicates. Every row is selected only if the L0 file-family bit mask AND per-occurrence unsigned state flags satisfy the requested bit sets. The implementation retains zero per-element node objects and allocates one final primitive array per requested materialization.

All collection classes are **not** qualified. This independent AST hierarchy can classify all 13 families but does not implement list/map/set/queue/graph APIs or replace the JDK's java.util implementation.

Use `python3 m3/ast/verify_mask_selection.py`; Maven/JUnit, full JDK image/jtreg, native/GC/perf remain external gates.

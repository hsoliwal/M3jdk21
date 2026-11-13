# JDK backports into M3jdk21 java.base (census2 P2-021/022, P2-063, P2-066), 2026-11-06

Sources are the com.synexia hash-pinned crates under synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/:

| upstream | crate | M3jdk21 file | how applied |
| --- | --- | --- | --- |
| JDK-8325730 (empty StringBuilder/StringBuffer toString allocates) | m3jdk21-empty-builders-8325730-20261028 | java/lang/StringBuffer.java, java/lang/StringBuilder.java | master blob = pinned preimage (sha256 a0b2539b8f6c / a476ed91663c); pinned postimage (e746f79db79e / 9b5cd15b878a) |
| JDK-8372256 (ConcurrentHashMap.equals throws CCE/NPE from the other map's get) | m3jdk21-chm-equals-8372256-20261027 | java/util/concurrent/ConcurrentHashMap.java | master drifted from the pinned preimage (javadoc-only: {@inheritDoc ConcurrentMap}); 3-way merge before->post onto live adds exactly the 15-line fix |
| JDK-8349883 (Locale.LanguageRange.parse AIOOBE on "-") | m3jdk21-languagerange-8349883-currentmaster-20261031 | java/util/Locale.java | master blob = pinned preimage (109224fce3f8); pinned postimage (ec0b5ef389db) |

## Gates

All run with stock JDK 21.0.9.

**jt3_gate.sh** compiles the 4 files with javac --patch-module java.base=<master c46cfa6639aa share/classes>, which also pulls in their implicit closure. Live and backported each compile with 0 errors. errdiff reports nothing NEW and nothing GONE.

**BpProbe.java** loads ConcurrentHashMap and Locale from the patch directory (class+load confirmed) and runs on the stock VM.
- ConcurrentHashMap:
  - live master class: `chm.equals(TreeMap<Integer>)` throws ClassCastException;
  - backported class: it returns false;
  - `equals` of two equal maps is still true.
- Locale:
  - master's Locale needs StaticProperty fields that 21.0.9 lacks, so it cannot be patched in alone. The identical hunk was therefore applied to 21.0.9's own Locale.java.
  - stock: `parse("-")` and `parse("--")` throw ArrayIndexOutOfBoundsException;
  - fixed: both throw IllegalArgumentException;
  - "en-" and "*-" still throw IAE, and "en-US" still parses.

**StringBuilder/StringBuffer: compile only.** Their M3 versions use M3 String internals, and a stock VM cannot boot a patched java.lang. The change is the upstream early return: `length() == 0` yields "" before any M3 composition. jtreg is NOT_RUN on this host.

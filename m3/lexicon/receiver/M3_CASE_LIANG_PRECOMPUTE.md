# Precomputed CAMEL/PASCAL boundaries and opt-in Liang candidate cuts

Pinned original: `M3WindowBoundarySummary.java` Git blob fdce0b7b067f33b447d76559de550d78c37dddca. Preserve its public API, composable atom/seam semantics and all CSV/punctuation and JNI interfaces. A private 8 KiB process-frozen truth table replaces scalar UTF-16 character-type decisions for CAMEL/PASCAL; out-of-range synthetic categories fall back to the exact scalar predicate. No text normalization, language guessing or allocation per boundary.

The separate immutable `M3LiangPatternPrecompute` compiles a **caller-reviewed ASCII-only** versioned language pattern list with explicit left/right minima into flat primitive trie arrays, delivering discretionary cut bits. It does not insert hyphens, tokenize Unicode graphemes, decide UAX14 line breaks, shape glyphs or choose line width. Invalid Unicode/patterns must fail closed. The Synexia visual compiler retains Knuth/Liang/UAX14/Paroksha layout and legal-break authority. Atom-cursor overload does not flatten words.

Test-only C17 JNI validates scalar category transitions and max/odd Liang scoring independently. Product JNI exports remain unchanged. No copied external pattern dataset, competitive challenge solution, or donor code. LeetCode #208/HackerRank Contacts/GeeksforGeeks trie topics classify test shapes only.

Execution: exact source preimage check -> docs -> Java21 strict compile -> GCC C17 strict compile -> independent Java oracle -> JNI checked/interpreted -> SHA-256 postimages -> canonical Synexia recipe fixed-point/refusal -> real Maven/OpenRewrite/JDK/jtreg. The real reactor, JMH, locale-aware Unicode, and full visual-compiler integration remain HOLD.

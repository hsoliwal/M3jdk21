<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Separator lexicons, word-id space and word-id regex rows (leaf LEXICONS, 2026-10-18)

Status: design + scaffold. [V] = verified at a pin (cited blob), [H] = proposal of this leaf.
Pins: Synexia 11c94feb1815, M3jdk21 970a34350eb (live reads at local origin/master 2f2beb2eff7a).
Consumers: REGEX_PRECOMPUTE_DESIGN.md §2b (transfer monoid), §2d (seam rule), §2e (word ids), §2f (token regex).

## W1. Canonical word id space (CWID)

* [V] M3LEX001 rows are strictly ascending in UTF-16 order; `M3StringPool.Lexicon.atom(row)` gives every
  lexicon atom `canonicalId = Long.MIN_VALUE | (generation64 & 0x3fff_ffff_0000_0000L) | (row + 1)`
  (M3StringPool.java blob 0905414556ad L1220-1250), generation64 = first 8 bytes of the image SHA-256.
* [V] SYNARR01 snapshots enter the same Lexicon type: language-0 width-2 scalar records, sorted at load,
  generation64 = namespaceHigh ^ namespaceLow. Duplicate spellings are not rejected (NOTE: two CWIDs possible).
* [H] CWID := `(int) canonicalId` when `canonicalId < 0` and the 30-bit tag `(canonicalId >>> 32) & 0x3fff_ffff`
  equals the open lexicon's tag; CWID 0 = not a lexicon word. CWIDs are dense `1..V`, so every per-word sidecar
  is a flat array indexed by `CWID - 1`. Ids from different generations are never compared (refuse).
* [V] Other id spaces stay distinct identities (invariant M3LEXICONS-RESOURCE-OWNERSHIP-1): SYNDICT1
  `((language+1) << 25) | (word+1)`; MIWROWS1 IndexId (language 16 bits, overlay bit 47, 47-bit local id).
  [H] A mapping between them is a remap sidecar keyed by spelling equality, built and verified, never assumed.
* Owner: `com.m3.lexicon.M3LexiconWordIds` (encode/decode/tag check; pure functions, no allocation).

## W2. Folded variants

* [H] Two fold kinds: ASCII_LOWER (`A-Z -> a-z` only) and UNICODE_SIMPLE (per char
  `Character.toLowerCase(Character.toUpperCase(c))`, the `String.equalsIgnoreCase` rule).
* foldId(CWID) = CWID of the folded spelling when it is itself a row, else `0x8000_0000 | classIndex`
  (dense derived space). Then `equalsIgnoreCase` between lexicon words = one int compare, and a folded literal
  needle is a CWID-sequence search over fold ids. Sidecar M3FOLD01 (LEXICON_FORMATS.tsv). Owner
  `com.m3.lexicon.M3LexiconFoldIds` (fold function + class builder over a sorted word list).

## W3. Word-id regex transfer rows (vocab x |Q|), coordinated with R6-AUTOMATON

* For a minimized DFA (states and dead-state numbering owned by R6), row `T[w][q]` = state after feeding the
  UTF-16 units of word w from q. Matching a word-tokenized String = `q = T[w_k][q]` per token plus the rows of
  separator atoms; an OOV or separator atom uses the §2b lazy per-atom element (same function type
  `Q -> Q`). Composition is function composition, so any split of the token sequence gives the same state.
* Accept bits per (w, q) mark "an accepting state is reached inside w" for find(); optional u16 first-accept
  offsets give match ends without re-scanning. Layout M3RXW001; pages of 4096 words, built lazily, bound to
  (pattern key SHA-256, lexicon generation). Owner `com.m3.lexicon.M3RegexWordTransferRows`.

## W4. Separator masks -> word-id sequences without flattening

* Input: a composite = ordered atoms (CharSequence views of mapped payloads). Each atom has, per separator
  kind, a boundary summary (S below). The root summary yields boundary bits in composite coordinates.
* Tokens are the spans between boundaries; a span equal to a whole lexicon atom takes its CWID directly
  (O(1)); other spans resolve through the lexicon's binary search reading the span through atom slices.
  Output = SoA lanes `int[] wordIds, int[] starts` (DOOP), never a String[] or a joined char[].
* Owner `com.m3.lexicon.M3WordIdSequence` (resolver interface; java.base binding is a later TARGET_OWNED leaf).

## S. Separator lexicons as a boundary monoid

Boundary bit j (0 < j < n) = "a token boundary sits before code unit j". Kinds:

| kind | rule (exact donor semantics) | window | donor oracle |
| --- | --- | --- | --- |
| CAMEL | j where type(j-1) in {LOWERCASE_LETTER, DECIMAL_DIGIT_NUMBER} and type(j) = UPPERCASE_LETTER | 2 units | spec oracle (test) |
| PASCAL | Commons Lang `splitByCharacterTypeCamelCase` as a bit rule: bit j iff (type(j) != type(j-1) and not LOWER after UPPER) or (type(j-1) = type(j) = UPPER and type(j+1) = LOWER) ("HTMLParser" -> HTML, Parser; digit runs are their own type) | units j-1..j+1 | commons-lang3 3.17.0 StringUtils cd1204f32e57 |
| CSV | RFC 4180 with quote state; separator units = delimiter, CR, LF outside quotes | state machine | commons-csv CSVParser a2bc23070638 / Lexer 6d9c8a485006 |
| PUNCT | unit (code point) in a configurable set of Character.getType P* classes; both units of a pair | code point | java.lang.Character.getType |

Types are `Character.getType(char)` per UTF-16 unit for CAMEL/PASCAL (exact parity with Commons Lang, which
classifies chars); PUNCT classifies code points and joins surrogate pairs across seams.

**Window kinds (CAMEL, PASCAL).** Summary `S = (len, first2 types, last2 types, interior bits)`, where interior
bits hold only positions whose whole window lies inside the atom. `combine(L, R)` = `L.bits | R.bits << L.len |
seam(L.last2 ++ R.first2)`, the seam positions being the ones whose window first becomes complete; at the root,
`finish(S)` evaluates the edge positions with start/end sentinels. Every position is decided exactly once by the
same function of the global unit sequence, so combine is associative and split-independent (identity = empty
atom). This is the §2d seam-trigram rule with a 3-unit window.

**State kind (CSV).** Seven states: FIELD_START, UNQUOTED, QUOTED, QUOTE_SEEN (a quote inside a quoted field:
close or escape, decided by the next unit), CLOSED (whitespace after a closing quote), AFTER_CR (a following LF continues the record end), ERROR (absorbing). Per atom, for each entry state e: `exit[e]` and
`bits[e]`. `combine(L, R).exit[e] = R.exit[L.exit[e]]`, `.bits[e] = L.bits[e] | R.bits[L.exit[e]] << L.len` —
function composition, associative, identity = (id, 0). The in-quote state is carried across atom seams; a
delimiter inside a quoted field is never a boundary. ERROR reproduces Commons CSV's refusal ("invalid char
between encapsulated token and delimiter").

**Unit kind (PUNCT).** Bits are per unit; seam work is only the surrogate pair split across L/R.

**Never flatten.** Summaries are computed per atom once (atoms are shared, so the cost is per distinct atom) and
composed on tuple nodes; split/tokenize walks bits and atom slices. Materialization is NONE for split and WINDOW
for span-to-CWID resolution (lexicon compare through slices).

## Differential proofs (scaffold, M3LexiconReceiverProof)

1. PASCAL vs `StringUtils.splitByCharacterTypeCamelCase` over fixed cases and >= 20,000 random strings from a
   mixed alphabet, each evaluated on >= 3 random atom splits (split independence).
2. CAMEL vs the spec oracle; CAMEL bits are a subset of PASCAL bits on letter/digit input.
3. CSV vs `CSVFormat.RFC4180` parse (field values after unquoting) on random valid RFC 4180 records with random
   atom splits; ERROR state on inputs Commons CSV refuses.
4. PUNCT vs `Character.getType` per code point incl. supplementary punctuation split across seams.
5. M3RXW001 rows: for random word sequences, composing rows equals running the DFA over the concatenation.
6. M3LEX001 writer/verifier round trip; corrupted header, checksum, order and Java-hash cases refused.

Not claimed: java.base binding of M3WordIdSequence to M3StringAtom/M3StringTuple, jtreg, data images, R6 DFA
serialization (pattern key is caller-supplied in the scaffold), SYNDICT1/SNXSTR01/SNXLEX01/M3PAIR01 body readers.

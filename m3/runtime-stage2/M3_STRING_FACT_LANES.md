# M3 canonical String fact lanes

M3JDK treats java.lang.String as an immutable logical view over canonical MIndex
storage. Expensive reusable facts therefore belong to the canonical storage entry
or canonical joined composition, not to each Java String wrapper.

## Safety rule

Every accelerated fact is one of:

1. **Exact identity fact** — may prove equality only when canonical identity and
   range semantics are identical.
2. **Necessary-condition fact** — may reject work when missing, but may never
   prove a positive result.
3. **Derived exact metric** — length/hash/code-point count/encoding length that is
   exact for the immutable canonical content.

Hash, Bloom, bit-signal, trigram and character-class collisions must always fall
through to exact UTF-16 verification.

## Tier 0: addressing

Always available:

- pool identity
- pool generation
- canonical entry ID
- UTF-16 offset and length
- byte-lane encoding ID, offset and length when present
- storage kind: mapped leaf, local leaf, joined composition, slice

These values are enough to address content without materializing a Java array.

## Tier 1: always-hot exact facts

Stored once per canonical leaf/composition when inexpensive:

- UTF-16 length
- Java String hashCode
- stable content hash
- coder / ASCII / Latin1 classification
- code-point count
- first and last UTF-16 units
- surrogate-presence flag
- blank/empty flags
- UTF-8 length when a canonical UTF-8 lane exists
- canonical encoded-body IDs

Joined values aggregate these facts from children. Java hash is composable from
child hash and child length. Character-class and presence facts are OR/AND
aggregates as appropriate.

## Tier 2: search / regex necessary-condition facts

Shared with Synexia MIndexString semantics:

- 64-bit case-folded UTF-16 presence signal
- ASCII upper/lower/digit/word/space presence flags
- optional wider presence/Bloom lanes
- required-character masks
- first-character class masks
- short literal prefix/suffix windows
- trigram candidate filter/image IDs
- line-break presence and optional sparse line index
- word-boundary edge facts

These facts can reject impossible Pattern/Matcher and substring work before
touching the character stream. They never turn a possible match into a proven
match.

## Tier 3: optional heavy immutable indexes

Built only for sufficiently hot/large canonical entries or lexicon images:

- trigram/N-gram posting indexes
- literal substring automata
- regex DFA/NFA execution images for repeated known patterns
- page-level character-class masks
- normalized/case-folded canonical entry IDs
- code-point boundary indexes
- grapheme-boundary indexes
- line-start indexes
- pre-encoded representations for explicit charset/error-policy tuples

A pool may load these lanes lazily and independently. Failure or absence must
fall back to exact String semantics without changing results.

## Joined composition

A joined String does not duplicate child metadata. Its canonical composition
stores child identities/ranges plus aggregate facts.

For a join A+B+C:

- length = len(A)+len(B)+len(C)
- Java hash is composed from child hash/length values
- presence and class masks are merged
- first/last facts come from the first/last non-empty child
- prefix/suffix windows reuse children and inspect only join seams
- N-gram facts reuse child facts plus N-grams crossing seams
- surrogate/code-point seam handling inspects only adjacent edge units

This makes richer precompute economically viable because work is proportional to
new seams rather than total concatenated content.

## Regex bridge

java.util.regex remains the semantic oracle.

Pattern-side compilation may precompute required literal/class facts. Matcher
may reject a segmented String only when canonical String facts prove a required
fact is absent. Otherwise the stock Pattern node graph executes against
CharSequence.length/charAt/subSequence, which are segment-aware.

Future MIndex regex DFA/trigram execution images can be attached as optional
accelerators only when their supported-semantics fingerprint exactly matches
the JDK Pattern flags and construct subset. Unsupported constructs always use
the stock JDK engine.

## Materialization

byte[], char[], jchar* and char* are compatibility projections. Creating one
must never mutate canonical storage identity. Normal String/regex/search
execution should operate against the canonical pool or joined descriptor
directly.

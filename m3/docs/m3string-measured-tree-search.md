# M3 String measured-tree literal search

## Invariant

M3 String remains the existing canonical owner + packed coordinate value. `M3StringTuple` remains the
only canonical composition owner. This work adds no rope/list/tree payload to `M3String` and adds no
new String identity domain.

The tuple DAG is already a measured tree because `M3StringFacts.compose(left,right)` supplies
composable length/hash/code-point/prefix/suffix/code-unit/bigram/trigram facts. Literal search should
consume those measurements before descending child text.

## Search law

For a prepared composite source and a prepared needle:

1. inspect the exact logical tuple/range;
2. a child whose prepared `M3StringFacts.mayContain(needleFacts)` is false is proven unable to hold
   a complete match and is skipped;
3. if both children remain possible, immediately delegate the entire range to the existing
   KMP/adaptive-BMH authority rather than recursively multiplying work;
4. if exactly one child remains possible, recurse only there;
5. independently scan the bounded seam window for matches crossing the child boundary;
6. reverse search mirrors the order: right, seam, left;
7. absent/unprepared facts disable the tree optimization and retain the existing exact path.

Fact filters are rejection evidence only. A positive fact result never establishes a match.

## Seam geometry

For needle length N at boundary B, only candidate starts in `[B-N+1, B-1]` can cross the boundary.
The seam therefore needs at most `2*N-2` source units. The existing pattern prefix table supplies
linear-time overlap-safe verification.

## Cost boundary

Tree pruning never forces source facts to be prepared. It activates only when the source/range facts
are already cached. When both children remain possible the implementation falls back immediately to
the existing exact search. Therefore the tree does not replace the current worst-case search
algorithm; it removes subtrees only when M3 precompute has proof.

## Donor/problem catalogue

- Existing `M3MeasuredRope` / `M3RopeMeasure`: REUSE_EXISTING design proof.
- Existing `M3StringTuple` + `M3StringFacts`: runtime owner.
- Existing KMP/adaptive BMH: exact verifier.
- LeetCode 28 / GFG KMP: problem taxonomy only.
- Rope/finger-tree measured sequence literature: representation concept only; no third-party source
  is copied.

## M3FS

The prepared facts/effect images can later be persisted by the existing M3JDK `jdk.m3fs` prepared
precompute plane. M3FS is not reimplemented by this slice.

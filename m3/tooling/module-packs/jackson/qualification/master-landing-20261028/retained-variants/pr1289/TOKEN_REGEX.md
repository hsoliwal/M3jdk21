<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3TokenRegex — structural regular expressions over JSON AST tokens

This is M3JDK21-owned code inside the existing jdk.m3.jackson module. It
does **not** replace java.util.regex.Pattern/Matcher or the java.base M3RX7
composite-code-unit DFA. Use it when the pattern refers to JSON grammatical
tokens, field names, and values rather than character positions.

## Example

    M3AST ast = new M3ObjectMapper().readAST(
        "{\"age\":42,\"comment\":\"age: 42\"}");
    M3TokenRegex expr = M3TokenRegex.compile(
        "[{kind:FIELD}&{name:age}] [{kind:NUMBER_INT}&{value:/[0-9]+/}]");
    M3TokenRegex.Span hit = expr.findFirst(ast, 100_000, 512);

The result is half-open **token occurrence indices**. A STRING containing
the substring age or 42 is not a FIELD/NUMBER_INT event and cannot satisfy
this pattern. Subpattern /[0-9]+/ matches the *entire* token attribute,
because the existing Java Pattern.matches contract is used for attributes.

## Event model

Depth-first traversal of the sealed immutable AST emits:

- OBJECT/ARRAY on entry, then END_OBJECT/END_ARRAY on exit.
- FIELD for each object member occurrence, in original order, including
  duplicate names, then the corresponding JSON value subtree.
- STRING, NUMBER_INT, NUMBER_FLOAT, TRUE, FALSE, NULL for scalar atoms.

Each child-reference occurrence is visited separately even if hash-consing
deduplicated its underlying atom. No Map.Entry or per-event M3ASTAtom wrapper
is ever stored or constructed by this matcher; it uses M3ASTPool's existing
kind/name/value and childOffset/childCount primitive-backed accessors and
a reusable primitive DFS path stack. No JSON subtree serialization is used.

## Bounded grammar

- Bare token types: FIELD, STRING, NUMBER_INT, END_OBJECT, etc.
- [] or . matches any one token event.
- Attribute predicates: [{name:id}], [{kind:FIELD}&{name:/id|age/}],
  [{value:"hello world"}].
- Sequence with whitespace, alternation |, noncapturing ( ... ),
  postfix ?, *, +, and ^/$ at complete stream boundaries.

No captures beyond the overall Span, counted repetitions, negation, arbitrary
attributes, structural parent-axis selection, reluctant quantifiers, or
lookahead/lookbehind in the token grammar yet. Reject these instead of
treating the expression as ordinary character regex. This is a **bounded
T-Rex-inspired subset**, not source/API compatibility with Square T-Rex.

The immutable plan stores NFA opcodes/targets/rules in bounded arrays (up to
512 states and 4096 pattern characters). Each execution owns its temporary
long[] state/start lanes and its mutable attribute Matcher contexts, so
sharing compiled plans between threads is safe. Token events and traversal
depth have explicit caller-supplied limits. Leftmost-longest matching costs
O(tokenEvents * states * predicateCost), O(states + depth) scratch memory;
the exact cost of optional Java regex attribute predicates may be non-linear.

## Limits, security, and future owners

Attribute Pattern syntax is still the full Java regex grammar. Reject unknown
or unsafe user-supplied patterns at a higher boundary; this slice does not
claim a general timeout or linear-time guarantee for arbitrary backreferences.

Span indices are JSON token-event ordinals, not UTF-16, code-point, file-byte,
or AST unique-atom indices. The current generic JSON AST does not retain
parser source spans; exact source replacement must await M3File/M3String
source-position metadata, and transformations must use JDT/LTK authority.

Future work: named capture offset lanes, sequential find/iterator cursor,
matching directly over Java compiler lexical tokens and M3String lexicon IDs,
mask/offset selection prefilter, native transition acceleration and 100 MB
JSON throughput/RSS qualification. Do not merge or claim these already exist.

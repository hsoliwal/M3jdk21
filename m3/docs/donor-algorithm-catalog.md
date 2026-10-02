# Donor and algorithm decisions

The reuse ledgers are the admission gates. This catalogue records inspected
techniques and limitations; public visibility does not authorize copying source.

| Surface | Problem / candidate | Reuse decision and license | Evidence / tradeoff |
| --- | --- | --- | --- |
| Private Synexia at `6df9df8d8f42111239013ee941723ec37f97ba6e` | Immutable byte/UTF16 atoms, joined ranges and canonical live bodies | Selected seven-file Apache-2.0 closure adapted with original names and notices | Raw Git hashes, deterministic adapter patch, replay refusal, four stock-JDK differential modes. Weak body lookup is not a global memory bound. |
| OpenClaw local harvested catalogue | Existing taskflow and skill packaging | Inventoried first; no fit for the Java retained-UTF16 storage contract | No donor implementation copied. Existing owned durable-task and packaging leaves remain tooling capabilities. |
| OpenJDK 21 Pattern / CharsetEncoder | Full engine semantics and whole-input stateful encoding | Runtime wrappers; GPL-2.0 with Classpath exception retained by the JDK, no source copied | Exact view differential corpus; charAt access can cost log(segment count). No alternative regex-engine equivalence claim. |
| [LeetCode first occurrence](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) | Exact literal first-match query | Problem category only; no code or editorial copied, no source license assumed | Published lowercase constraints are narrower than Java UTF16; our stock-JDK oracle adds empty/negative/seam/unpaired cases. |
| [HackerRank string similarity](https://www.hackerrank.com/challenges/string-similarity/problem) | Prefix similarity across suffixes; candidate Z/prefix facts | Problem category only; no submissions copied, no implementation admission | Atom/composition fact candidate remains pending, with construction/memory/amortization gates. Similarity never proves equality or a regex match. |
| [GeeksforGeeks KMP](https://www.geeksforgeeks.org/dsa/kmp-algorithm-for-pattern-searching/) | Prefix-failure literal search | Technique catalogue only; no article code copied, no reusable source license assumed | Existing explicit facade uses cursor KMP, tested against String. Per-call O(pattern length) metadata; no shared pattern cache or general speed claim. |

The HackerRank Z/prefix choice is an inference about a candidate algorithm, not
a claim that its public statement prescribes that implementation. JNI, GPU,
RE2/J, mapped application-resolver sharing and new persistent formats remain
separate capabilities until a donor, exact contract and executed proof fit.

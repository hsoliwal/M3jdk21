<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 meaningful-name contract

M3JDK21 source names state the domain concept or operation. Names do not advertise speed, encode
implementation machinery, preserve migration history, or substitute generic filler for intent.
Documentation explains mechanics; identifiers carry the stable semantic skeleton.

## Admission rules

The `M3JdkStandard` plugin applies these deterministic checks to M3-owned type and method names:

| Identifier | Form | Maximum length | Maximum semantic words |
|---|---|---:|---:|
| Type | UpperCamel | 48 characters | 7 |
| Method | lowerCamel | 40 characters | 6 |

`M3` and numeric width/lane tokens do not count as semantic words. Established technical
abbreviations remain legal; this gate does not maintain a closed acronym dictionary.

A type or method is refused when any of these conditions holds:

1. It is an exact placeholder such as `Foo`, `Helper`, `doIt`, or `processData`.
2. Its camel-case words contain promotional wording such as `Ultra`, `Turbo`, `Fastest`,
   `NextGen`, `HighPerformance`, or `Optimized`. Matching is word-based, so a domain term
   such as `Hyperlink` is not confused with the promotional word `Hyper`.
3. It carries a version or date suffix such as `V2`, `Version3`, or `20261031`. Version
   matching follows camel-case token boundaries, so a domain name such as `IPv6` remains legal.
4. It repeats an adjacent semantic word, such as `loadLoad`.
5. It exceeds the length or semantic-word limit above.

The lexical gate proves only explicit anti-patterns. Review still decides whether an otherwise legal
name accurately identifies its domain concept. A passing result is not a semantic-equivalence claim.

## Compatibility and rollout

The gate never renames source. Existing public and protected names remain compatibility-locked.
Any public rename requires the repository's canonical mapping, a compatibility bridge or documented
break decision, and the normal ABI/runtime gates. Methods marked `@Override` keep the inherited
contract and are exempt from the additional semantic-quality check.

Repository-wide adoption starts in `mode=warning` to establish a census. Error mode applies to new
or explicitly renamed M3-owned APIs after their compatibility and naming records are admitted. No
bulk rename, generated alias, or silent public-surface change follows from this contract.

## Changed-source admission

`M3ChangedNameGate` compares two exact Git commits, parses only M3-owned Java files changed between
them, and refuses findings located on added source lines. Existing names on untouched lines do not
block unrelated maintenance. A rename, a new declaration, or a new file necessarily adds the
relevant declaration line and is therefore checked.

The gate excludes vendored, generated, qualification, retained-variant and archived source trees.
Ordinary OpenJDK owners are not reinterpreted as M3 APIs; the existing public-JDK naming and ABI
gates remain authoritative for them. `scheme=m3` remains a separate opt-in until namespace
migration has an admitted compatibility recipe.

`M3JdkStandardCensus` provides the complementary repository-wide, parse-only warning inventory.
It uses javac parsing without type attribution, so unresolved module or dependency closures do not
hide names. Java syntax errors still fail closed. Reports are sorted by path, numeric line and
numeric column, making repeated runs byte-stable.

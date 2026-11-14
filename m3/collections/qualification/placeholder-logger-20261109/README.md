<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# PlaceholderLogger: the {} logging owner for absorbed code (2026-11-09)

On 2026-11-09 the user chose "Internal {} facade". This lets one new internal production class unblock the absorbed
classes held on lombok `@Slf4j` and slf4j. The census of the current HOLD rows:

| Selection | Held only on slf4j |
|---|---|
| paradigm modules | 85 |
| common.lazy | 6 |
| other synexia-common | 47 |

Those 138 classes use `info`/`error`/`debug`/`warn`/`trace`: 546/68/47/28/3 calls. 111 of them carry `@Slf4j`, and
20 call `LoggerFactory.getLogger(X.class)`.

## Design

`jdk.internal.m3.collections.PlaceholderLogger` is a functional interface over `System.Logger`, in java.base, so no
new module dependency is needed.

**Call sites.** Its methods have exactly the names and descriptors that those call sites already use. A relocated
call stays an `invokeinterface` of the same size and its message string is unchanged. That keeps relocated classes
provable as bytecode-equal to the donor modulo the owner type.

**Formatting.** It follows the reference `{}` rules:
- each `{}` takes the next argument;
- `\{}` is a literal `{}`;
- `\{}` is a backslash followed by a substituted argument;
- arrays print their elements, and a recursive array prints `[...]`;
- a failing `toString()` prints `[FAILED toString()]`;
- a trailing `Throwable` is logged as the throwable, not formatted.

**Laziness.** Arguments are formatted only when the level is enabled.

**Level mapping.** trace, debug, info, warn and error map to TRACE, DEBUG, INFO, WARNING and ERROR.

## Proof

- **Differential** (`tools/PlaceholderFormatDiff.java`, DIFFERENTIAL.txt). Fixed edge cases plus 20,000 random
  patterns per seed were compared with `org.slf4j.helpers.MessageFormatter.arrayFormat` from slf4j-api 2.0.17. The
  oracle is used at qualification time only. Result: 0 mismatches over 3 x 20,238 cases. A mutant with the
  doubled-backslash rule disabled is caught.
- **`PlaceholderLoggerTest`** (5 tests, no slf4j). It covers formatting, escapes, arrays, bounded recursion, the
  failing-toString marker, trailing-throwable routing, level gating (nothing formats while disabled) and naming.
- **Build.** `-Xlint:all -Werror` and `-Xdoclint:all,-missing` both pass. Line 1 of the header is `/* ****`, per the
  release lane's doclint rule.

## Next

Recipe SLF4J_TO_PLACEHOLDER_LOGGER, used in the absorb batches, does three things:
- removes `@Slf4j` together with the `lombok.extern.slf4j` and `org.slf4j` imports;
- inserts `private static final PlaceholderLogger log = PlaceholderLogger.of(Owner.class);` where lombok put the field;
- maps `Logger` to `PlaceholderLogger` and `LoggerFactory.getLogger` to `PlaceholderLogger.of`.

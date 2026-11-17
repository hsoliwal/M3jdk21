# Cross-language string catalogue (owner decisions 25-27, 2026-11-17)

Read from 179 official pages:

- languages: Python 3.12 str/string/textwrap, ECMAScript String and lodash, Ruby String and ActiveSupport, PHP strings
  and mbstring, Kotlin kotlin.text, .NET System.String, Swift String and Foundation, Rust str and String, Go
  strings/utf8/strconv, C++ basic_string and C string.h;
- formats and specs: RFC 4180, RFC 8259, XML 1.0, WHATWG HTML, CommonMark 0.31, YAML 1.2.2, TOML 1.0,
  java.util.Properties, RFC 3986/4648, JLS 21 chapter 3;
- schemas and Unicode: XSD 1.1 part 2, JSON Schema 2020-12, UAX #29/#15, POSIX shell, SQL and C literals.

Three readers wrote one row per documented operation, and a fourth merged the rows into unified operations against
java.lang.String's 113 public methods.

| File | Content |
|---|---|
| `CROSS_LANGUAGE_ROWS.tsv` | 1,024 rows: source, API, semantics with edge rules, unit, status against String, family, proposed Java member, oracle, doc URL |
| `CROSS_LANGUAGE_OPERATIONS.tsv` | 459 unified operations: 292 NEW, 119 PARTIAL, 48 EXISTS; sources, conflicts, best oracle |
| `CROSS_LANGUAGE_PLAN.tsv` | 24 landing batches, from COMPARE through the LEX kernels |
| `CROSS_LANGUAGE_NOTES.txt`, `_SOURCES_READ.txt`, `_GAPS.txt` | naming collisions and misplaced rows, every URL read, what was not read |

Every row is research output. Each operation is proven against its oracle when it lands; the oracles are the
language's own implementation (python, node, go, rustc and dotnet are installed), the donor jar, or the spec table.

# ENCODE family draft (2026-11-17)

Draft only: nothing committed, pushed or opened as a PR. `encode_family.txt` (593 lines) is
inserted in `String.head.java` right after the case family text (leaf31 head 11196 lines + 593 =
11789). No name of the family existed in the head (grep: 0 hits for every public and private name).

## Methods (13 public)

| Method | Donor (oracle) |
|---|---|
| `encodePercent()` | CPython 3.12.6 `urllib.parse.quote(s, safe='')`; RFC 3986 section 2.3 unreserved set |
| `encodePercent(String safe)` | CPython `urllib.parse.quote(s, safe)` |
| `encodePercentPlus()` | CPython `urllib.parse.quote_plus(s)` = Go 1.26.2 `url.QueryEscape` |
| `encodePercentPlus(String safe)` | CPython `urllib.parse.quote_plus(s, safe)` |
| `decodePercent()` | CPython `urllib.parse.unquote(s)` (utf-8, errors='replace') |
| `decodePercentPlus()` | CPython `urllib.parse.unquote_plus(s)` |
| `encodeUriComponent()` | ECMAScript `encodeURIComponent` (node 25.9.0) |
| `encodeUri()` | ECMAScript `encodeURI` |
| `decodeUriComponent()` | ECMAScript `decodeURIComponent` |
| `decodeUri()` | ECMAScript `decodeURI` |
| `toWellFormed()` | ECMAScript `String.prototype.toWellFormed` |
| `utf8Length()` | Guava 33.7.2 `Utf8.encodedLength`; CPython `len(s.encode('utf-8'))` |
| `rot13()` | CPython `codecs.encode(s, 'rot13')` (PHP `str_rot13` is the catalogue row; not run) |

Extra exact oracles run in the proof (in-JVM, every code point 0..10FFFF alone plus every corpus
text, lone surrogates included): Guava `PercentEscaper("-._~", false/true)`,
`PercentEscaper("-_.!~*'()")` (= encodeUriComponent), `PercentEscaper("-_.!~*'();/?:@&=+$,#")`
(= encodeUri), `UrlEscapers.urlPathSegmentEscaper()` = `encodePercent("!$&'()*+,;=:@")`,
`urlFragmentEscaper()` = `encodePercent("!$&'()*+,;=:@/?")`, `Utf8.encodedLength`. On well-formed
text: FreeMarker 2.3.35 `StringUtil.URLEnc(s, "UTF-8")` (`?url`) = encodeUriComponent; unbescape
1.1.8 `escapeUriPathSegment` / `escapeUriPath` / `escapeUriFragmentId` / `escapeUriQueryParam` =
`encodePercent` with `"!$&'()*+,;=:@"`, `+ "/"`, `+ "/?"`, and `"!$'()*,;:@/?"`; Go
`url.PathEscape` = `encodePercent("$&+:=@")`.

## Semantic choices

- **Lone surrogates (encoders, utf8Length).** Python raises UnicodeEncodeError, ECMAScript throws
  URIError, Guava throws IllegalArgumentException: every encoder and `utf8Length()` throws
  `IllegalArgumentException("Unpaired surrogate at index N")` (Guava's message and index; the
  ported Guava tests check it). URLEncoder, FreeMarker and unbescape write `%3F` (their
  `getBytes` replacement); that rule is not taken.
- **Decoders never reject lone surrogates**: they are copied (Python passes non-ASCII through,
  ECMAScript's Decode copies every non-'%' unit).
- **Python safe sets.** A non-ASCII character of `safe` is ignored (`quote_from_bytes` encodes
  `safe` as ASCII with 'ignore'); alphanumerics in `safe` are allowed and change nothing.
  `quote_plus`: a space always becomes '+', also when `safe` holds a space; '+' in `safe` is kept
  as '+' (ambiguous, but Python's rule).
- **decodePercent replacement ("prove which").** Proven: Python's default `errors='replace'`, one
  U+FFFD per maximal ill-formed subpart (Unicode section 3.9). The JDK's own UTF-8 decoder is NOT
  the same: `new String(bytes, UTF_8)` gives one U+FFFD for an encoded surrogate (`ED A0 80` ->
  1 U+FFFD, Python 3) and for a truncated `ED A0` (Python 2); the first proof run caught it
  (`"%00%ED%A0"`, 2970 texts). The method text therefore decodes held bytes with a private
  maximal-subpart decoder (`heldBytesDecoded`). Python decodes per ASCII run; the method decodes
  each run of adjacent escapes, which is the same because an ASCII byte never continues a UTF-8
  sequence and a non-ASCII character ends Python's run.
- **ECMAScript errors** (`URIError`) become `IllegalArgumentException("Malformed percent escape at
  index N")`, N = the index of the escape that starts the bad sequence. `decodeUri()` keeps a
  reserved escape exactly as written (`%2f` stays lower case).
- **Unchanged results return this** (checked for 12 methods on every corpus text in the proof and
  with assertSame in JUnit).
- **One method per rule; no nested enum.** The TEMPLATE_ENGINES catalogue proposes
  `encodeUri(UriComponent, Charset)` with a public `UriComponent` enum. Not taken: a new public
  type needs owner admission (no-public-sprawl; TEMPLATE_ENGINES_NOTES fallback = zero-arg
  members), and `encodePercent(String safe)` already gives every component, each composition
  proven against Guava/unbescape/Go and named in the javadoc. Naming question for the owner: the
  zero-arg `encodeUri()` is ECMAScript's encodeURI; an admitted `encodeUri(UriComponent)` overload
  would sit next to it with a different rule.

## Not added (and why)

- **application/x-www-form-urlencoded**: `java.net.URLEncoder` / `URLDecoder` own it (JDK owns fast
  implementations). Not identical to any member: proven `encodePercentPlus("*")` =
  `URLEncoder.encode(s, UTF_8)` on well-formed text without '~' (URLEncoder encodes '~', keeps '*',
  writes `%3F` for a lone surrogate). `decodePercentPlus()` = `URLDecoder.decode(s, UTF_8)` on every
  encoder output; they differ where URLDecoder throws on a '%' without two hex digits, where
  URLDecoder accepts `"%+1"` (U+0001) and full-width digits (Integer.parseInt), and on encoded
  surrogates (JDK decoder rule above).
- **Go** `PathEscape` / `QueryEscape`: compositions (above), no new names.
  `PathUnescape`/`QueryUnescape`: Go strings are bytes (no UTF-8 check) and a malformed '%' is an
  error; no exact Java rule, not added.
- **unbescape lenient decoders** (invalid hex digit read as 0, '%' within the last two characters
  throws): engine quirks the catalogue says not to replicate.
- **hexCodec** (`java.util.HexFormat`), **base64/base32/uu/quoted-printable/MIME header**
  (catalogue: not on String), **punycode/IDNA** (`java.net.IDN`; not named in the ENCODE rows),
  **canEncode(Charset)** (composes: `cs.newEncoder().canEncode(s)`), **toUriFragment** (composes:
  `"#" + encodePercent("/!$&'()*+,;=:@?")`), **decodeUtf8Strict / decodeUtf16Strict /
  encodeUtf8(int)** (byte[] or static codecs; CharsetDecoder REPORT composes),
  **percentDecode() as Optional with errors='strict'** (Python strict keeps a stray '%' but rejects
  bad UTF-8, a third rule; ECMAScript strict decoding is `decodeUriComponent()`),
  **toWellFormed(String replacement)** (Go's rule replaces runs of bad UTF-8 bytes; no UTF-16
  oracle), **static utf8Length(int)** (Go answers -1 for a surrogate, Rust has no surrogates:
  donors disagree), **isWellFormed()** (VALIDATE family in the catalogue rows; left to it).

## M3 paths

- Encoders: prepared M3StringFacts answer in O(1) when the text is ASCII and every unit is a word
  character (`regexAllClasses & REGEX_WORD`), which every set keeps; otherwise the first unit to
  encode is found on the bytes of a flat Latin-1 string (`m3() == null && isLatin1()`) or in bulk
  windows of SCAN_WINDOW units via getChars; none -> this; else the clean prefix is copied as one
  range and the rest read in windows, a surrogate pair never split, kept runs copied as ranges.
- Decoders: `mayContainUnit('%')` (and '+') facts gate, then `indexOf` jumps; `charAt` only at a
  '%' and its two digits; the text between escapes is copied as one range.
- toWellFormed: `facts.latin1` or a flat Latin-1 string -> this; windows otherwise.
  utf8Length: `facts.ascii` -> length(); Latin-1 bytes; windows. rot13: `asciiWithout(REGEX_ALPHA)`
  gate, `firstUnitIn` scan, one bulk `toCharArray`.
- Reused head helpers: `asciiMask`, `inMask` (search family), `mayContainUnit` (replace family),
  `asciiWithout` (case family), `SCAN_WINDOW`.
- String storage invariant (the relayed request on `value` / `chValue`): the family reads `value`
  only inside java.lang.String on the flat Latin-1 path, never writes it, never hands it or a view
  of it out, and adds no field. The head has no `chValue` field (grep: 0); this draft does not add
  one (out of scope for an ENCODE draft; no edits outside the draft folder).

## Proof and tests

- `proof/run_proof.sh <draft> <work> <donor-cp>`: eleven texts verbatim in the head, stock JDK 21
  String + texts compiled with `-Xlint:all,-this-escape -Werror` (no warnings), corpus 109,548
  encoder texts (every BMP unit, 3,847 supplementary code points, every ordered ASCII pair, every
  string of 0..4 units over a 12-unit alphabet with 0xD835 0xDD1E 0xDD1F, seams at 505..520 and
  1018..1030), 22,807 texts x 11 safe sets, 112,035 decoder texts (every '%' + two ASCII units,
  escaped byte sequences of length 1..4 over 23 bytes, token sequences, seams); 71 checks per run;
  six runs (flat, M3 stub with facts, without facts; compact strings on and off): see PROOF.txt.
- JUnit: StringEncodeFamilyTest (1,885 parameterized texts x 2 storage forms, 5 rule tests) and
  StringEncodeFamilyDonorTest (Guava): see JUNIT.txt.
- Donor tests: Guava 33.7.2 Utf8Test, UrlEscapersTest, UrlEscaperTesting fetched from the v33.7.2
  tag (raw.githubusercontent.com) into `donor-src/` (Apache-2.0, 4 files, kept for re-runs),
  rewritten by `tools/port_guava_donor.py` and ported by port_donor_tests.py with
  `encode.spec.tsv`: 80 assertions ported in 10 tests, 3 skipped (`encode.skipped.tsv`: a random
  loop, two null receivers). Not ported, by rule (no exact member): Guava PercentEscaperTest
  (its `PercentEscaper("", ...)` keeps only alphanumerics), UrlEscapersTest's form-parameter test
  (URLEncoder's rule). Commons-lang3 / commons-text have no percent-encoding tests.
- `REGEX_SPEC.tsv`: 41 rows (product: none; proof 5, oracle-internal 1, port tool 9, shared porter
  25), generated and checked by `tools/regex_spec.py` (every positive matches, every negative does
  not; porter rows verbatim in port_donor_tests.py).

## Not proven / open

- `utf8Length()` overflow branch (UTF-8 length above Integer.MAX_VALUE, a UTF-16 string of more
  than 715 M units) is not exercised.
- M3 storage is the proof stub (M3StringStub.java.txt), as for the earlier families; the real
  M3String/M3StringFacts paths run only once the text lands in M3jdk21.
- `-Xdoclint` was not run (the proof uses -Xlint); the javadoc links `java.net.URLEncoder`,
  `java.net.URLDecoder` and family members only.
- PHP `str_rot13` and Rust `String::from_utf16_lossy` (toWellFormed) were not run (CPython and node
  are the oracles).

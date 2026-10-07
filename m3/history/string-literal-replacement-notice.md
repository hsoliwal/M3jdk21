# Indexed literal regex replacement provenance

The receiving `java.lang.M3String` route adapts the literal admission and first/all
replacement mechanism from Synexia `MIndexString`, by Hitesh Soliwal, licensed
Apache-2.0. Exact donor commits, blob and SHA-256 are in
`string-literal-replacement-lineage.tsv`; the donor license is retained verbatim
in `licenses/Synexia-Apache-2.0.txt`.

The receiving implementation composes the existing M3 bounded search precompute,
nonoverlapping replacement, owner slices and balanced joins. It does not copy the
donor's temporary spelling/failure arrays or introduce another payload owner.
All existing OpenJDK and Synexia source copyright and license headers remain.
The test uses the existing OpenJDK InMemoryJavaCompiler and ByteCodeLoader unchanged.

The admitted fast-path syntax is a nonempty BMP literal regex and a non-null
replacement without dollar or backslash. Any surrogate in the regex stays with
Pattern/Matcher: stock JDK21 execution proves that code-unit substring search can
match half of a pair where regex cannot. Sources and replacements may still
contain arbitrary UTF-16. Regex syntax, empty patterns, captures, escapes and
exception ordering keep the existing Pattern/Matcher route.

Local source compilation and structural gates cannot establish receiving-runtime
correctness. Matching M3JDK image build and jtreg are required. The historical
Linux image with MIndexString is not evidence for this current M3String receiver.
This atom is not the complete static regex Program/image/GPU engine port.

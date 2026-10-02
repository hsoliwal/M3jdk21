# Java 21 String acceptance coverage

Generated from the installed stock Java 21 `javap -public java.lang.String` surface. The receipt records the exact tool/version. Per-method categories describe the explicit facade only; a matching method name does not establish all overload or exception contracts.

| Actual Java 21 signature | Route A disposition |
| --- | --- |
| `public java.lang.String();` | pending |
| `public java.lang.String(java.lang.String);` | pending |
| `public java.lang.String(char[]);` | pending |
| `public java.lang.String(char[], int, int);` | pending |
| `public java.lang.String(int[], int, int);` | pending |
| `public java.lang.String(byte[], int, int, int);` | pending |
| `public java.lang.String(byte[], int);` | pending |
| `public java.lang.String(byte[], int, int, java.lang.String) throws java.io.UnsupportedEncodingException;` | pending |
| `public java.lang.String(byte[], int, int, java.nio.charset.Charset);` | pending |
| `public java.lang.String(byte[], java.lang.String) throws java.io.UnsupportedEncodingException;` | pending |
| `public java.lang.String(byte[], java.nio.charset.Charset);` | pending |
| `public java.lang.String(byte[], int, int);` | pending |
| `public java.lang.String(byte[]);` | pending |
| `public java.lang.String(java.lang.StringBuffer);` | pending |
| `public java.lang.String(java.lang.StringBuilder);` | pending |
| `public int length();` | implemented_tested_route_a_subset |
| `public boolean isEmpty();` | implemented_tested_route_a_subset |
| `public char charAt(int);` | implemented_tested_route_a_subset |
| `public int codePointAt(int);` | implemented_tested_route_a_subset |
| `public int codePointBefore(int);` | pending |
| `public int codePointCount(int, int);` | implemented_tested_route_a_subset |
| `public int offsetByCodePoints(int, int);` | pending |
| `public void getChars(int, int, char[], int);` | implemented_tested_route_a_subset |
| `public void getBytes(int, int, byte[], int);` | pending_exact_overload_gate |
| `public byte[] getBytes(java.lang.String) throws java.io.UnsupportedEncodingException;` | pending_exact_overload_gate |
| `public byte[] getBytes(java.nio.charset.Charset);` | partial_route_a |
| `public byte[] getBytes();` | pending_exact_overload_gate |
| `public boolean equals(java.lang.Object);` | implemented_tested_route_a_subset |
| `public boolean contentEquals(java.lang.StringBuffer);` | pending_exact_overload_gate |
| `public boolean contentEquals(java.lang.CharSequence);` | implemented_tested_route_a_subset |
| `public boolean equalsIgnoreCase(java.lang.String);` | pending |
| `public int compareTo(java.lang.String);` | partial_route_a |
| `public int compareToIgnoreCase(java.lang.String);` | pending |
| `public boolean regionMatches(int, java.lang.String, int, int);` | pending |
| `public boolean regionMatches(boolean, int, java.lang.String, int, int);` | pending |
| `public boolean startsWith(java.lang.String, int);` | pending |
| `public boolean startsWith(java.lang.String);` | pending |
| `public boolean endsWith(java.lang.String);` | pending |
| `public int hashCode();` | implemented_tested_route_a_subset |
| `public int indexOf(int);` | pending_exact_overload_gate |
| `public int indexOf(int, int);` | pending_exact_overload_gate |
| `public int indexOf(int, int, int);` | pending_exact_overload_gate |
| `public int lastIndexOf(int);` | pending_exact_overload_gate |
| `public int lastIndexOf(int, int);` | pending_exact_overload_gate |
| `public int indexOf(java.lang.String);` | partial_route_a |
| `public int indexOf(java.lang.String, int);` | partial_route_a |
| `public int indexOf(java.lang.String, int, int);` | pending_exact_overload_gate |
| `public int lastIndexOf(java.lang.String);` | partial_route_a |
| `public int lastIndexOf(java.lang.String, int);` | partial_route_a |
| `public java.lang.String substring(int);` | implemented_tested_route_a_subset |
| `public java.lang.String substring(int, int);` | implemented_tested_route_a_subset |
| `public java.lang.CharSequence subSequence(int, int);` | implemented_tested_route_a_subset |
| `public java.lang.String concat(java.lang.String);` | partial_route_a |
| `public java.lang.String replace(char, char);` | pending |
| `public boolean matches(java.lang.String);` | partial_route_a |
| `public boolean contains(java.lang.CharSequence);` | partial_route_a |
| `public java.lang.String replaceFirst(java.lang.String, java.lang.String);` | pending |
| `public java.lang.String replaceAll(java.lang.String, java.lang.String);` | pending |
| `public java.lang.String replace(java.lang.CharSequence, java.lang.CharSequence);` | pending |
| `public java.lang.String[] split(java.lang.String, int);` | pending |
| `public java.lang.String[] splitWithDelimiters(java.lang.String, int);` | pending |
| `public java.lang.String[] split(java.lang.String);` | pending |
| `public static java.lang.String join(java.lang.CharSequence, java.lang.CharSequence...);` | pending |
| `public static java.lang.String join(java.lang.CharSequence, java.lang.Iterable<? extends java.lang.CharSequence>);` | pending |
| `public java.lang.String toLowerCase(java.util.Locale);` | pending |
| `public java.lang.String toLowerCase();` | pending |
| `public java.lang.String toUpperCase(java.util.Locale);` | pending |
| `public java.lang.String toUpperCase();` | pending |
| `public java.lang.String trim();` | pending |
| `public java.lang.String strip();` | pending |
| `public java.lang.String stripLeading();` | pending |
| `public java.lang.String stripTrailing();` | pending |
| `public boolean isBlank();` | pending |
| `public java.util.stream.Stream<java.lang.String> lines();` | pending |
| `public java.lang.String indent(int);` | pending |
| `public java.lang.String stripIndent();` | pending |
| `public java.lang.String translateEscapes();` | pending |
| `public <R> R transform(java.util.function.Function<? super java.lang.String, ? extends R>);` | pending |
| `public java.lang.String toString();` | implemented_tested_route_a_subset |
| `public java.util.stream.IntStream chars();` | implemented_tested_route_a_subset |
| `public java.util.stream.IntStream codePoints();` | implemented_tested_route_a_subset |
| `public char[] toCharArray();` | implemented_tested_route_a_subset |
| `public static java.lang.String format(java.lang.String, java.lang.Object...);` | pending |
| `public static java.lang.String format(java.util.Locale, java.lang.String, java.lang.Object...);` | pending |
| `public java.lang.String formatted(java.lang.Object...);` | pending |
| `public static java.lang.String valueOf(java.lang.Object);` | pending |
| `public static java.lang.String valueOf(char[]);` | pending |
| `public static java.lang.String valueOf(char[], int, int);` | pending |
| `public static java.lang.String copyValueOf(char[], int, int);` | pending |
| `public static java.lang.String copyValueOf(char[]);` | pending |
| `public static java.lang.String valueOf(boolean);` | pending |
| `public static java.lang.String valueOf(char);` | pending |
| `public static java.lang.String valueOf(int);` | pending |
| `public static java.lang.String valueOf(long);` | pending |
| `public static java.lang.String valueOf(float);` | pending |
| `public static java.lang.String valueOf(double);` | pending |
| `public native java.lang.String intern();` | pending |
| `public java.lang.String repeat(int);` | pending |
| `public java.util.Optional<java.lang.String> describeConstable();` | pending |
| `public java.lang.String resolveConstantDesc(java.lang.invoke.MethodHandles$Lookup);` | pending |
| `public int compareTo(java.lang.Object);` | partial_route_a |
| `public java.lang.Object resolveConstantDesc(java.lang.invoke.MethodHandles$Lookup) throws java.lang.ReflectiveOperationException;` | pending |

Route B remains scoped to an opt-in explicit admission rewrite; `invokedynamic`, constants, escaping values and mixed callers retain ordinary Java fallback. Route C needs distinct exact-candidate VM/native gates. Historical StringJoiner expectation failures remain failures.

Regex uses stock Pattern against the retained view. Tested flags/captures/lookarounds/backreferences/replacements and Unicode seams do not stand for every Matcher option. Transparent/anchoring bounds, all region combinations and exhaustive replacement grammar remain coverage gaps.

## FastDoubleParser 1.0.90

### Origin

Thirteen classes of FastDoubleParser 1.0.90 (https://github.com/wrandelshofer/FastDoubleParser, MIT; the vendored
source inclusion m3/vendor/third_party/fastdoubleparser-1.0.90) are part of java.base as
jdk.internal.math.M3* (the Java-grammar double/float parsers for CharSequence and byte[], their templates and the
FastDoubleMath, FastFloatMath, FastDoubleSwar, FastIntegerMath and FftMultiplier kernels).

### Changes made by Synexia (2026-10-31)

Package ch.randelshofer.fastdoubleparser -> jdk.internal.math and type T -> M3T; the slow-path calls to
Double.parseDouble / Float.parseFloat now call FloatingDecimal.readJavaFormatString (the JDK converter they fall back
to), because java.base routes those methods through this code; M3FftMultiplier.squareFft squares the magnitude (the
donor squared toByteArray() of a negative value, its two's complement; found by the per-atom JUnit test, unreachable
from the parsers, which square only powers of ten); LITERAL_CONSTANTS v1 (no magic numbers): 488 literals in 8 of
the 13 source files became 205 private static final constants named by role, each with the literal's own type and
spelling and a one-line Javadoc giving the role, under one //FLLDCIM anchor comment in each of the 10 classes that
declare them (nested classes included); javac folds the constants back, so the method bytecode is unchanged
(m3/tooling/jdk-superset/number-parse/CONSTANTS.tsv, BYTECODE_IDENTITY.tsv); HEX_ROUTE v1: the hexadecimal kernels
M3FastDoubleMath.tryHexFloatToDoubleTruncated and M3FastFloatMath.tryHexFloatToFloatTruncated decline every literal
(private static final boolean HEX_LITERALS_TO_FLOATING_DECIMAL = true), so every hexadecimal literal takes the
FloatingDecimal path; the donor code stays behind the constant, which javac drops (the donor's hex path rounds an
unsigned significand of 2^63 or more twice, keeps no sticky bit for the digits its re-parse drops, and clamps the
exponent number before it subtracts the fraction digits; HEX_ATOMS.tsv, BYTECODE_METHOD_DELTA.tsv);
DEC_EXPONENT_CLAMP v1: parseDecFloatLiteral of both Java-grammar templates passes DECLINED_EXPONENT (private static
final int, Integer.MIN_VALUE) as both exponents when the exponent number reaches MAX_EXPONENT_NUMBER, so the decimal
kernels decline and the literal takes the FloatingDecimal path (the donor stops accumulating the exponent number at
the clamp and adds the digit counts afterwards, so "0." + 723 zeros + "1e10240" gave 1e300 instead of Infinity;
DEC_ATOMS.tsv, DEC_BYTECODE_METHOD_DELTA.tsv). Nothing else was changed.

### LICENSE

```
MIT License

Copyright (c) 2024 Werner Randelshofer, Switzerland.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### NOTICE

```
# FastDoubleParser

This is a Java port of Daniel Lemire's fast_float project.
This project provides parsers for double, float, BigDecimal and BigInteger values.

## Copyright

Copyright © 2024 Werner Randelshofer, Switzerland.

## Licensing

This code is licensed under MIT License.
https://github.com/wrandelshofer/FastDoubleParser/blob/522be16e145f43308c43b23094e31d5efcaa580e/LICENSE
(The file 'LICENSE' is included in the sources and classes Jar files that are released by this project
- as is required by that license.)

Some portions of the code have been derived from other projects.
All these projects require that we include a copyright notice, and some require that we also include some text of their
license file.

fast_double_parser, Copyright (c) 2022 Daniel Lemire. BSL License.
https://github.com/lemire/fast_double_parser
https://github.com/lemire/fast_double_parser/blob/07d9189a8fb815fe800cb15ca022e7a07093236e/LICENSE.BSL
(The file 'thirdparty-LICENSE' is included in the sources and classes Jar files that are released by this project
- as is required by that license.)

fast_float, Copyright (c) 2021 The fast_float authors. MIT License.
https://github.com/fastfloat/fast_float
https://github.com/fastfloat/fast_float/blob/cc1e01e9eee74128e48d51488a6b1df4a767a810/LICENSE-MIT
(The file 'thirdparty-LICENSE' is included in the sources and classes Jar files that are released by this project
- as is required by that license.)

bigint, Copyright 2020 Tim Buktu. 2-clause BSD License.
https://github.com/tbuktu/bigint/tree/floatfft
https://github.com/tbuktu/bigint/blob/617c8cd8a7c5e4fb4d919c6a4d11e2586107f029/LICENSE
https://github.com/wrandelshofer/FastDoubleParser/blob/39e123b15b71f29a38a087d16a0bc620fc879aa6/bigint-LICENSE
(We only use those portions of the bigint project that can be licensed under 2-clause BSD License.)
(The file 'thirdparty-LICENSE' is included in the sources and classes Jar files that are released by this project
- as is required by that license.)
```

### Third-party licenses (fast_float, bigint)

```
------
Third-party license for
fast_float, Copyright (c) 2021 The fast_float authors. MIT License.
https://github.com/fastfloat/fast_float
https://github.com/fastfloat/fast_float/blob/35d523195bf7d57aba0e735ad6eba1e6f71ba8d6/LICENSE-MIT

MIT License

Copyright (c) 2021 The fast_float authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
------
Third-party license for
bigint, Copyright 2020 Tim Buktu. 2-clause BSD License.
https://github.com/tbuktu/bigint/tree/floatfft
https://github.com/tbuktu/bigint/blob/617c8cd8a7c5e4fb4d919c6a4d11e2586107f029/LICENSE
https://github.com/wrandelshofer/FastDoubleParser/blob/39e123b15b71f29a38a087d16a0bc620fc879aa6/bigint-LICENSE
(We only use those portions of the bigint project that can be licensed under 2-clause BSD License.)

2-clause BSD License

Copyright 2022 Tim Buktu

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

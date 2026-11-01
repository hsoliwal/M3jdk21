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
(m3/tooling/jdk-superset/number-parse/CONSTANTS.tsv, BYTECODE_IDENTITY.tsv); JIT_ATOMIZE v1: the parse-path
methods above the HotSpot inlining limit FreqInlineSize (325 bytes) were split, in the same 8 files, into private
methods with the same behaviour (parseExponentNumber and parseLiteralTail in both grammar templates, returning one
packed long, with packedIndex; valueOfTruncatedDecFloatLiteral and valueOfTruncatedHexLiteral; isClingerFastPath,
clingerFastPath and tryDecToDoubleWithEiselLemire / tryDecToFloatWithEiselLemire; jdkConverterValue in the four
parsers), so parseDecFloatLiteral and parseHexFloatLiteral went from 466-515 to 261-316 bytes and
tryDecToDoubleWithFastAlgorithm / tryDecToFloatWithFastAlgorithm from 279 / 278 to 35 (JIT_SIZE_PRE.tsv,
JIT_SIZE.tsv); HEX_ROUNDING v1: a hex significand at or above 2^63 converts through unsignedToDouble /
unsignedToFloat, rounded once as by the JDK (the donor added 2^64 to the signed conversion and rounded twice), and a
truncated hex significand takes the fast path only when significand and significand + 1 give the same value, else
the JDK converter decides (the donor dropped the sticky tail); SWAR_HEX_BORROW v1:
M3FastDoubleSwar.tryToParseEightHexDigitsUtf8 tests for letters at or below f without a borrow between byte lanes
(the donor took g and G for hex digits, so Double.parseDouble("0x1.79G60184p0") returned a value where the JDK throws
NumberFormatException); NUMBER-PARSE-PATTERN v1: packedIndex and its three constants, identical in both grammar
templates, have one owner, M3AbstractFloatValueParser; CNPCDA v1: the four //FLLDCIM anchor comments removed with
that move are back in the two template files, each with an anchor naming the new owner (gates: GATES.tsv and
RECIPE_MEMORY.tsv in m3/tooling/jdk-superset/number-parse). Nothing else was changed.

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

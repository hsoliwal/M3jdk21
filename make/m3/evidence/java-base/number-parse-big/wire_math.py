"""wire_math.py <java.base classes dir> : NUMBER_PARSE_ABSORB v2 wiring - java.math.BigInteger(String, int) and
java.math.BigDecimal(char[], int, int, MathContext) ask jdk.internal.math.M3BigNumbers first. Each public constructor
becomes a delegation to a private constructor that copies the fast result's final fields, or runs the original body,
unchanged, when the answer is null (short literal, any rejection, a non-decimal radix, a limited MathContext)."""
import sys
from pathlib import Path

root = Path(sys.argv[1]) / "java/math"


def edit(name, old, new, imp_after, imp):
    p = root / name
    s = p.read_bytes().decode("utf-8")
    assert s.count(old) == 1, (name, old[:50])
    s = s.replace(old, new, 1)
    if imp not in s:
        assert s.count(imp_after) >= 1, (name, "import anchor")
        s = s.replace(imp_after, imp_after + imp, 1)
    p.write_bytes(s.encode("utf-8"))
    print("WIRED", name)


edit("BigInteger.java",
     "    public BigInteger(String val, int radix) {\n",
     "    public BigInteger(String val, int radix) {\n"
     "        //FLLDCIM: M3 - one fast big-number parser in the JDK (FastDoubleParser 1.0.90 absorbed as jdk.internal.math.M3*,\n"
     "        // NUMBER_PARSE_ABSORB v2): a long decimal literal it accepts takes the fast path; every other literal, and any\n"
     "        // literal it rejects, the original parser below, so results and exceptions stay exactly the JDK's\n"
     "        this(val, radix, radix == M3BigNumbers.DECIMAL_RADIX ? M3BigNumbers.decimalBigIntegerOrNull(val) : null);\n"
     "    }\n"
     "\n"
     "    /**\n"
     "     * The parser of {@link #BigInteger(String, int)}: {@code m3} is the value the JDK's fast big-number parser\n"
     "     * accepted, or {@code null} for the original parser below.\n"
     "     */\n"
     "    private BigInteger(String val, int radix, BigInteger m3) {\n"
     "        if (m3 != null) {\n"
     "            signum = m3.signum;\n"
     "            mag = m3.mag;\n"
     "            return;\n"
     "        }\n",
     "package java.math;\n", "\nimport jdk.internal.math.M3BigNumbers;\n")

edit("BigDecimal.java",
     "    public BigDecimal(char[] in, int offset, int len, MathContext mc) {\n",
     "    public BigDecimal(char[] in, int offset, int len, MathContext mc) {\n"
     "        //FLLDCIM: M3 - one fast big-number parser in the JDK (FastDoubleParser 1.0.90 absorbed as jdk.internal.math.M3*,\n"
     "        // NUMBER_PARSE_ABSORB v2): a long literal it accepts takes the fast path when the precision is unlimited; every\n"
     "        // other literal, and any literal it rejects, the original parser below, so results and exceptions stay the JDK's\n"
     "        this(in, offset, len, mc,\n"
     "             mc != null && mc.precision == 0 ? M3BigNumbers.bigDecimalOrNull(in, offset, len) : null);\n"
     "    }\n"
     "\n"
     "    /**\n"
     "     * The parser of {@link #BigDecimal(char[], int, int, MathContext)}: {@code m3} is the value the JDK's fast\n"
     "     * big-number parser accepted, or {@code null} for the original parser below.\n"
     "     */\n"
     "    private BigDecimal(char[] in, int offset, int len, MathContext mc, BigDecimal m3) {\n"
     "        if (m3 != null) {\n"
     "            this.intVal = m3.intVal;\n"
     "            this.intCompact = m3.intCompact;\n"
     "            this.scale = m3.scale;\n"
     "            this.precision = m3.precision;\n"
     "            return;\n"
     "        }\n",
     "package java.math;\n", "\nimport jdk.internal.math.M3BigNumbers;\n")

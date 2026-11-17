/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of CPython 3.12's own test_html.py (html.escape, html.unescape),
 *          ported to java.lang.String by port_python_donor.py
 * @run junit StringEscapeMarkupFamilyPythonDonorTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from CPython 3.12.6 Lib/test/test_html.py (PSF-2.0): each html.escape and
 * html.unescape call the passing donor test made, with its answer.
 */
public class StringEscapeMarkupFamilyPythonDonorTest {

    @Test
    void htmlTests_test_escape() {
        assertEquals("&#x27;&lt;script&gt;&quot;&amp;foo;&quot;&lt;/script&gt;&#x27;",
                ("'<script>\"&foo;\"</script>'").escapeHtml(true));
        assertEquals("'&lt;script&gt;\"&amp;foo;\"&lt;/script&gt;'",
                ("'<script>\"&foo;\"</script>'").escapeHtml(false));
    }

    @Test
    void htmlTests_test_unescape() {
        assertEquals("no character references",
                ("no character references").unescapeHtml5Python());
        assertEquals("&\n&\t& &&",
                ("&\n&\t& &&").unescapeHtml5Python());
        assertEquals("&0 &9 &a &0; &9; &a;",
                ("&0 &9 &a &0; &9; &a;").unescapeHtml5Python());
        assertEquals("&",
                ("&").unescapeHtml5Python());
        assertEquals("&;",
                ("&;").unescapeHtml5Python());
        assertEquals("&#",
                ("&#").unescapeHtml5Python());
        assertEquals("&#;",
                ("&#;").unescapeHtml5Python());
        assertEquals("&#x",
                ("&#x").unescapeHtml5Python());
        assertEquals("&#x;",
                ("&#x;").unescapeHtml5Python());
        assertEquals("&#X",
                ("&#X").unescapeHtml5Python());
        assertEquals("&#X;",
                ("&#X;").unescapeHtml5Python());
        assertEquals("&#y",
                ("&#y").unescapeHtml5Python());
        assertEquals("&#y;",
                ("&#y;").unescapeHtml5Python());
        assertEquals("&#xy",
                ("&#xy").unescapeHtml5Python());
        assertEquals("&#xy;",
                ("&#xy;").unescapeHtml5Python());
        assertEquals("&#Xy",
                ("&#Xy").unescapeHtml5Python());
        assertEquals("&#Xy;",
                ("&#Xy;").unescapeHtml5Python());
        assertEquals("A",
                ("&#65").unescapeHtml5Python());
        assertEquals("A ",
                ("&#65 ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#65X").unescapeHtml5Python());
        assertEquals("A",
                ("&#0000065").unescapeHtml5Python());
        assertEquals("A ",
                ("&#0000065 ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#0000065X").unescapeHtml5Python());
        assertEquals("A",
                ("&#65;").unescapeHtml5Python());
        assertEquals("A ",
                ("&#65; ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#65;X").unescapeHtml5Python());
        assertEquals("A",
                ("&#0000065;").unescapeHtml5Python());
        assertEquals("A ",
                ("&#0000065; ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#0000065;X").unescapeHtml5Python());
        assertEquals("A",
                ("&#x41").unescapeHtml5Python());
        assertEquals("A ",
                ("&#x41 ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#x41X").unescapeHtml5Python());
        assertEquals("A",
                ("&#x000041").unescapeHtml5Python());
        assertEquals("A ",
                ("&#x000041 ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#x000041X").unescapeHtml5Python());
        assertEquals("A",
                ("&#x41;").unescapeHtml5Python());
        assertEquals("A ",
                ("&#x41; ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#x41;X").unescapeHtml5Python());
        assertEquals("A",
                ("&#x000041;").unescapeHtml5Python());
        assertEquals("A ",
                ("&#x000041; ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#x000041;X").unescapeHtml5Python());
        assertEquals("A",
                ("&#X41;").unescapeHtml5Python());
        assertEquals("A ",
                ("&#X41; ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#X41;X").unescapeHtml5Python());
        assertEquals("A",
                ("&#X000041;").unescapeHtml5Python());
        assertEquals("A ",
                ("&#X000041; ").unescapeHtml5Python());
        assertEquals("AX",
                ("&#X000041;X").unescapeHtml5Python());
        assertEquals("a",
                ("&#97").unescapeHtml5Python());
        assertEquals("a ",
                ("&#97 ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#97X").unescapeHtml5Python());
        assertEquals("a",
                ("&#0000097").unescapeHtml5Python());
        assertEquals("a ",
                ("&#0000097 ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#0000097X").unescapeHtml5Python());
        assertEquals("a",
                ("&#97;").unescapeHtml5Python());
        assertEquals("a ",
                ("&#97; ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#97;X").unescapeHtml5Python());
        assertEquals("a",
                ("&#0000097;").unescapeHtml5Python());
        assertEquals("a ",
                ("&#0000097; ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#0000097;X").unescapeHtml5Python());
        assertEquals("a",
                ("&#x61").unescapeHtml5Python());
        assertEquals("a ",
                ("&#x61 ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#x61X").unescapeHtml5Python());
        assertEquals("a",
                ("&#x000061").unescapeHtml5Python());
        assertEquals("a ",
                ("&#x000061 ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#x000061X").unescapeHtml5Python());
        assertEquals("a",
                ("&#x61;").unescapeHtml5Python());
        assertEquals("a ",
                ("&#x61; ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#x61;X").unescapeHtml5Python());
        assertEquals("a",
                ("&#x000061;").unescapeHtml5Python());
        assertEquals("a ",
                ("&#x000061; ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#x000061;X").unescapeHtml5Python());
        assertEquals("a",
                ("&#X61;").unescapeHtml5Python());
        assertEquals("a ",
                ("&#X61; ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#X61;X").unescapeHtml5Python());
        assertEquals("a",
                ("&#X000061;").unescapeHtml5Python());
        assertEquals("a ",
                ("&#X000061; ").unescapeHtml5Python());
        assertEquals("aX",
                ("&#X000061;X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#34").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#34 ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#34X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#0000034").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#0000034 ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#0000034X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#34;").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#34; ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#34;X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#0000034;").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#0000034; ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#0000034;X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#x22").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#x22 ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#x22X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#x000022").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#x000022 ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#x000022X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#x22;").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#x22; ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#x22;X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#x000022;").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#x000022; ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#x000022;X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#X22;").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#X22; ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#X22;X").unescapeHtml5Python());
        assertEquals("\"",
                ("&#X000022;").unescapeHtml5Python());
        assertEquals("\" ",
                ("&#X000022; ").unescapeHtml5Python());
        assertEquals("\"X",
                ("&#X000022;X").unescapeHtml5Python());
        assertEquals("&",
                ("&#38").unescapeHtml5Python());
        assertEquals("& ",
                ("&#38 ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#38X").unescapeHtml5Python());
        assertEquals("&",
                ("&#0000038").unescapeHtml5Python());
        assertEquals("& ",
                ("&#0000038 ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#0000038X").unescapeHtml5Python());
        assertEquals("&",
                ("&#38;").unescapeHtml5Python());
        assertEquals("& ",
                ("&#38; ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#38;X").unescapeHtml5Python());
        assertEquals("&",
                ("&#0000038;").unescapeHtml5Python());
        assertEquals("& ",
                ("&#0000038; ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#0000038;X").unescapeHtml5Python());
        assertEquals("&",
                ("&#x26").unescapeHtml5Python());
        assertEquals("& ",
                ("&#x26 ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#x26X").unescapeHtml5Python());
        assertEquals("&",
                ("&#x000026").unescapeHtml5Python());
        assertEquals("& ",
                ("&#x000026 ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#x000026X").unescapeHtml5Python());
        assertEquals("&",
                ("&#x26;").unescapeHtml5Python());
        assertEquals("& ",
                ("&#x26; ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#x26;X").unescapeHtml5Python());
        assertEquals("&",
                ("&#x000026;").unescapeHtml5Python());
        assertEquals("& ",
                ("&#x000026; ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#x000026;X").unescapeHtml5Python());
        assertEquals("&",
                ("&#X26;").unescapeHtml5Python());
        assertEquals("& ",
                ("&#X26; ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#X26;X").unescapeHtml5Python());
        assertEquals("&",
                ("&#X000026;").unescapeHtml5Python());
        assertEquals("& ",
                ("&#X000026; ").unescapeHtml5Python());
        assertEquals("&X",
                ("&#X000026;X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#9731").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#9731 ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#9731X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#0009731").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#0009731 ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#0009731X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#9731;").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#9731; ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#9731;X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#0009731;").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#0009731; ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#0009731;X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#x2603").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#x2603 ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#x2603X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#x002603").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#x002603 ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#x002603X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#x2603;").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#x2603; ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#x2603;X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#x002603;").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#x002603; ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#x002603;X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#X2603;").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#X2603; ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#X2603;X").unescapeHtml5Python());
        assertEquals("\u2603",
                ("&#X002603;").unescapeHtml5Python());
        assertEquals("\u2603 ",
                ("&#X002603; ").unescapeHtml5Python());
        assertEquals("\u2603X",
                ("&#X002603;X").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34",
                ("&#1053236").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34 ",
                ("&#1053236 ").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34X",
                ("&#1053236X").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34",
                ("&#1053236;").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34 ",
                ("&#1053236; ").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34X",
                ("&#1053236;X").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34",
                ("&#x101234").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34 ",
                ("&#x101234 ").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34X",
                ("&#x101234X").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34",
                ("&#x101234;").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34 ",
                ("&#x101234; ").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34X",
                ("&#x101234;X").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34",
                ("&#X101234;").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34 ",
                ("&#X101234; ").unescapeHtml5Python());
        assertEquals("\uDBC4\uDE34X",
                ("&#X101234;X").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#55296").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#55296;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xd800").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xd800;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#56064").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#56064;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xdb00").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xdb00;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#56320").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#56320;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xdc00").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xdc00;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#57343").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#57343;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xdfff").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xdfff;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#1114112").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#1114112;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#x110000").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#x110000;").unescapeHtml5Python());
        assertEquals("",
                ("&#1").unescapeHtml5Python());
        assertEquals("",
                ("&#1;").unescapeHtml5Python());
        assertEquals("",
                ("&#x1").unescapeHtml5Python());
        assertEquals("",
                ("&#x1;").unescapeHtml5Python());
        assertEquals("",
                ("&#11").unescapeHtml5Python());
        assertEquals("",
                ("&#11;").unescapeHtml5Python());
        assertEquals("",
                ("&#xb").unescapeHtml5Python());
        assertEquals("",
                ("&#xb;").unescapeHtml5Python());
        assertEquals("",
                ("&#14").unescapeHtml5Python());
        assertEquals("",
                ("&#14;").unescapeHtml5Python());
        assertEquals("",
                ("&#xe").unescapeHtml5Python());
        assertEquals("",
                ("&#xe;").unescapeHtml5Python());
        assertEquals("",
                ("&#127").unescapeHtml5Python());
        assertEquals("",
                ("&#127;").unescapeHtml5Python());
        assertEquals("",
                ("&#x7f").unescapeHtml5Python());
        assertEquals("",
                ("&#x7f;").unescapeHtml5Python());
        assertEquals("",
                ("&#65534").unescapeHtml5Python());
        assertEquals("",
                ("&#65534;").unescapeHtml5Python());
        assertEquals("",
                ("&#xfffe").unescapeHtml5Python());
        assertEquals("",
                ("&#xfffe;").unescapeHtml5Python());
        assertEquals("",
                ("&#65535").unescapeHtml5Python());
        assertEquals("",
                ("&#65535;").unescapeHtml5Python());
        assertEquals("",
                ("&#xffff").unescapeHtml5Python());
        assertEquals("",
                ("&#xffff;").unescapeHtml5Python());
        assertEquals("",
                ("&#1114110").unescapeHtml5Python());
        assertEquals("",
                ("&#1114110;").unescapeHtml5Python());
        assertEquals("",
                ("&#x10fffe").unescapeHtml5Python());
        assertEquals("",
                ("&#x10fffe;").unescapeHtml5Python());
        assertEquals("",
                ("&#1114111").unescapeHtml5Python());
        assertEquals("",
                ("&#1114111;").unescapeHtml5Python());
        assertEquals("",
                ("&#x10ffff").unescapeHtml5Python());
        assertEquals("",
                ("&#x10ffff;").unescapeHtml5Python());
        assertEquals("\r",
                ("&#13").unescapeHtml5Python());
        assertEquals("\r",
                ("&#13;").unescapeHtml5Python());
        assertEquals("\r",
                ("&#xd").unescapeHtml5Python());
        assertEquals("\r",
                ("&#xd;").unescapeHtml5Python());
        assertEquals("\u20AC",
                ("&#128").unescapeHtml5Python());
        assertEquals("\u20AC",
                ("&#128;").unescapeHtml5Python());
        assertEquals("\u20AC",
                ("&#x80").unescapeHtml5Python());
        assertEquals("\u20AC",
                ("&#x80;").unescapeHtml5Python());
        assertEquals("\u2022",
                ("&#149").unescapeHtml5Python());
        assertEquals("\u2022",
                ("&#149;").unescapeHtml5Python());
        assertEquals("\u2022",
                ("&#x95").unescapeHtml5Python());
        assertEquals("\u2022",
                ("&#x95;").unescapeHtml5Python());
        assertEquals("\u009D",
                ("&#157").unescapeHtml5Python());
        assertEquals("\u009D",
                ("&#157;").unescapeHtml5Python());
        assertEquals("\u009D",
                ("&#x9d").unescapeHtml5Python());
        assertEquals("\u009D",
                ("&#x9d;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#0").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#0;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#x0").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#x0;").unescapeHtml5Python());
        assertEquals("\t",
                ("&#9").unescapeHtml5Python());
        assertEquals("\t",
                ("&#9;").unescapeHtml5Python());
        assertEquals("\t",
                ("&#x9").unescapeHtml5Python());
        assertEquals("\t",
                ("&#x9;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#1000000000000000000").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#1000000000000000000;").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xde0b6b3a7640000").unescapeHtml5Python());
        assertEquals("\uFFFD",
                ("&#xde0b6b3a7640000;").unescapeHtml5Python());
        assertEquals("\";",
                ("&quot;;").unescapeHtml5Python());
        assertEquals("\";",
                ("&#34;;").unescapeHtml5Python());
        assertEquals("\";",
                ("&#x22;;").unescapeHtml5Python());
        assertEquals("\";",
                ("&#X22;;").unescapeHtml5Python());
        assertEquals("\"quot;",
                ("&quot;quot;").unescapeHtml5Python());
        assertEquals("\"quot;",
                ("&#34;quot;").unescapeHtml5Python());
        assertEquals("\"quot;",
                ("&#x22;quot;").unescapeHtml5Python());
        assertEquals("\"quot;",
                ("&#X22;quot;").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&quot&quot&quot").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&quot;&quot;&quot;").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&#34&#34&#34").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&#34;&#34;&#34;").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&#x22&#x22&#x22").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&#x22;&#x22;&#x22;").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&#X22&#X22&#X22").unescapeHtml5Python());
        assertEquals("\"\"\"",
                ("&#X22;&#X22;&#X22;").unescapeHtml5Python());
        assertEquals("&",
                ("&amp").unescapeHtml5Python());
        assertEquals("&",
                ("&amp;").unescapeHtml5Python());
        assertEquals("&",
                ("&AMP").unescapeHtml5Python());
        assertEquals("&",
                ("&AMP;").unescapeHtml5Python());
        assertEquals("&Amp",
                ("&Amp").unescapeHtml5Python());
        assertEquals("&Amp;",
                ("&Amp;").unescapeHtml5Python());
        assertEquals("&svadilfari;",
                ("&svadilfari;").unescapeHtml5Python());
        assertEquals("\u00ACit",
                ("&notit").unescapeHtml5Python());
        assertEquals("\u00ACit;",
                ("&notit;").unescapeHtml5Python());
        assertEquals("\u00ACin",
                ("&notin").unescapeHtml5Python());
        assertEquals("\u2209",
                ("&notin;").unescapeHtml5Python());
        assertEquals("\u00ACReallyAnExistingNamedCharacterReference;",
                ("&notReallyAnExistingNamedCharacterReference;").unescapeHtml5Python());
        assertEquals("\u2233",
                ("&CounterClockwiseContourIntegral;").unescapeHtml5Python());
        assertEquals("\u223E\u0333",
                ("&acE;").unescapeHtml5Python());
        assertEquals("&acE",
                ("&acE").unescapeHtml5Python());
        assertEquals("{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { " +
                "{ { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { { ",
                ("&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; &#123; &#123; &#123; &#123; &#123; &#123; " +
                "&#123; &#123; ").unescapeHtml5Python());
        assertEquals("\u00C9ric\u00C9ric&alphacentauri\u03B1centauri",
                ("&Eacuteric&Eacute;ric&alphacentauri&alpha;centauri").unescapeHtml5Python());
        assertEquals("&co;",
                ("&co;").unescapeHtml5Python());
    }
}

/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String escape-markup family (escapeHtml3 ... unescapeHtml5Python)
 * @run junit StringEscapeMarkupFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringEscapeMarkupFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringEscapeMarkupFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every escape-markup method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String, and
 * returns the receiver itself when nothing changes; fixed cases pin the documented rules. The
 * flat answers equal those of commons-text 1.15.0, unbescape 1.1.8 and CPython 3.12 html on
 * every text of the donor-oracle proof (string-lane record).
 */
public class StringEscapeMarkupFamilyTest {
    private static final char HIGH = (char) 0xD835;
    private static final char LOW = (char) 0xDD1E;
    private static final char LOW2 = (char) 0xDD1F;
    private static final String[] TOKENS = {"&", "#", "x", ";", "amp", "not", "65", "1114112", "\"",
        "'", "<", "\u0000", "\u00E9", "\u0085", String.valueOf(HIGH), String.valueOf(LOW),
        String.valueOf(LOW2)};

    static Stream<String> texts() {
        List<String> texts = new ArrayList<>();
        for (int length = 0; length <= 3; length++) {
            int total = (int) Math.pow(TOKENS.length, length);
            for (int code = 0; code < total; code++) {
                StringBuilder text = new StringBuilder();
                int rest = code;
                for (int i = 0; i < length; i++) {
                    text.append(TOKENS[rest % TOKENS.length]);
                    rest /= TOKENS.length;
                }
                texts.add(text.toString());
            }
        }
        return texts.stream();
    }

    private static Object answer(Supplier<String> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            return e.getClass();
        }
    }

    private static List<Object> answers(String s) {
        List<Object> out = new ArrayList<>();
        out.add(s.escapeHtml3());
        out.add(s.escapeHtml4());
        out.add(s.escapeXml10());
        out.add(s.escapeXml11());
        out.add(s.escapeHtml4Xml());
        out.add(s.escapeHtml5());
        out.add(s.escapeHtml5Xml());
        out.add(s.escapeHtml(true));
        out.add(s.escapeHtml(false));
        for (String.HtmlEscapeType type : String.HtmlEscapeType.values()) {
            for (String.HtmlEscapeLevel level : String.HtmlEscapeLevel.values()) {
                out.add(s.escapeHtml(type, level));
            }
        }
        for (String.XmlVersion version : String.XmlVersion.values()) {
            for (String.XmlEscapeType type : String.XmlEscapeType.values()) {
                for (String.XmlEscapeLevel level : String.XmlEscapeLevel.values()) {
                    out.add(s.escapeXml(version, type, level, false));
                    out.add(s.escapeXml(version, type, level, true));
                }
            }
        }
        out.add(answer(s::unescapeHtml3));
        out.add(answer(s::unescapeHtml4));
        out.add(answer(s::unescapeXml));
        out.add(answer(s::unescapeHtml));
        out.add(answer(s::unescapeXmlStrict));
        out.add(answer(s::unescapeHtml5Python));
        return out;
    }

    private static List<Function<String, String>> forms() {
        return List.of(
            text -> new String(("<<" + text + ">>").toCharArray()).substring(2, 2 + text.length()),
            text -> new String(text.toCharArray(), 0, text.length() / 2)
                    .concat(new String(text.toCharArray(), text.length() / 2,
                            text.length() - text.length() / 2)));
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("texts")
    void everyFormAnswersAsTheFlatString(String text) {
        List<Object> expected = answers(new String(text.toCharArray()));
        for (Function<String, String> form : forms()) {
            assertEquals(expected, answers(form.apply(text)));
        }
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("texts")
    void unchangedAnswerIsTheReceiver(String text) {
        for (Function<String, String> form : forms()) {
            String s = form.apply(text);
            List<Object> results = answers(s);
            for (Object result : results) {
                if (s.equals(result)) assertSame(s, result);
            }
        }
    }

    @Test
    void commonsHtmlRules() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;caf&eacute; &euro;&lt;/a&gt;",
                "<a href=\"x\">caf\u00E9 \u20AC</a>".escapeHtml4());
        assertEquals("&lt;p&gt;caf&eacute; \u20AC", "<p>caf\u00E9 \u20AC".escapeHtml3(),
                "HTML 3.2 names stop at U+00FF");
        assertEquals("it's &lang;", "it's \u2329".escapeHtml4(), "the apostrophe is kept");
        assertEquals("" + HIGH + LOW, ("" + HIGH + LOW).escapeHtml4());
        assertEquals("\u2329\u00E9\u20AC", "&lang;&eacute;&euro;".unescapeHtml4());
        assertEquals("&euro;\u00E9", "&euro;&eacute;".unescapeHtml3());
        assertEquals("ABC", "&#x41;&#X42;&#67;".unescapeHtml4());
        assertEquals("&#65", "&#65".unescapeHtml4(), "the semicolon is required");
        assertEquals("&#1a;", "&#1a;".unescapeHtml4());
        assertEquals("\uD83D", "&#55357;".unescapeHtml4(), "a surrogate value gives that char");
        assertEquals("\uD83D\uDE00", "&#128512;".unescapeHtml4());
        assertEquals("&#2147483648;", "&#2147483648;".unescapeHtml4(), "not an int: kept");
        assertThrows(IllegalArgumentException.class, () -> "&#1114112;".unescapeHtml4());
        assertThrows(IllegalArgumentException.class, () -> "&#x7FFFFFFF;".unescapeXml());
    }

    @Test
    void commonsXmlRules() {
        assertEquals("a&lt;b&gt;c&quot;d&apos;e&amp;f", "a<b>c\"d'e&f".escapeXml10());
        assertEquals("ab", ("a\u0000\u0001\u000B\uFFFEb" + HIGH).escapeXml10());
        assertEquals("a&#1;&#11;b", "a\u0000\u0001\u000Bb".escapeXml11());
        assertEquals("&#127;\u0085&#134;\u00A0", "\u007F\u0085\u0086\u00A0".escapeXml10());
        assertEquals("" + HIGH + LOW, ("" + HIGH + LOW).escapeXml11());
        assertEquals("'''&eacute;", "&apos;&#39;&#x27;&eacute;".unescapeXml());
    }

    @Test
    void unbescapeHtmlRules() {
        assertEquals("&#39;&amp;&lt;&gt;&quot;&#0;&#55349;",
                ("'&<>\"\u0000" + HIGH).escapeHtml4Xml());
        assertEquals("&apos;&amp;\u00E9", "'&\u00E9".escapeHtml5Xml());
        assertEquals("&eacute;&#20013;&afr;&amp;",
                ("\u00E9\u4E2D" + HIGH + LOW + "&").escapeHtml5());
        assertEquals("&#xe9;&#x27;", "\u00E9'".escapeHtml(
                String.HtmlEscapeType.HEXADECIMAL_REFERENCES,
                String.HtmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT));
        assertEquals("'&amp;", "'&".escapeHtml(
                String.HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
                String.HtmlEscapeLevel.LEVEL_0_ONLY_MARKUP_SIGNIFICANT_EXCEPT_APOS));
        assertEquals("a&#45;b", "a-b".escapeHtml(String.HtmlEscapeType.DECIMAL_REFERENCES,
                String.HtmlEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC));
        assertEquals("&#x61;", "a".escapeHtml(String.HtmlEscapeType.HEXADECIMAL_REFERENCES,
                String.HtmlEscapeLevel.LEVEL_4_ALL_CHARACTERS));
        assertEquals("\u00ACin", "&notin".unescapeHtml());
        assertEquals("&notit;", "&notit;".unescapeHtml(), "unbescape's search misses &not here");
        assertEquals("&centes", "&centes".unescapeHtml(), "unbescape's search misses &cent here");
        assertEquals("&\u223E\u0333\uFFFD\u20AC\uFFFD",
                "&amp&acE;&#0;&#x80&#xD800;".unescapeHtml());
        assertEquals("& amp;&<", "& amp;&<".unescapeHtml());
    }

    @Test
    void unbescapeXmlRules() {
        String.XmlEscapeType hexa =
                String.XmlEscapeType.CHARACTER_ENTITY_REFERENCES_DEFAULT_TO_HEXA;
        String.XmlEscapeLevel one = String.XmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT;
        assertEquals("&#x7f;&apos;\t", "\u007F'\t".escapeXml(String.XmlVersion.XML_1_0, hexa, one,
                false));
        assertEquals("&#x9;&#x1;", "\t\u0001\u0000".escapeXml(String.XmlVersion.XML_1_1, hexa, one,
                true));
        assertEquals("&#233;", "\u00E9".escapeXml(String.XmlVersion.XML_1_0,
                String.XmlEscapeType.DECIMAL_REFERENCES,
                String.XmlEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_MARKUP_SIGNIFICANT, false));
        assertEquals("A&#X41;&amp", "&#x41;&#X41;&amp".unescapeXmlStrict());
        assertThrows(IllegalArgumentException.class, () -> "&#0;".unescapeXmlStrict());
        assertThrows(IllegalArgumentException.class, () -> "&#xD800;".unescapeXmlStrict());
        assertThrows(IllegalArgumentException.class, () -> "&#99999999999;".unescapeXmlStrict());
        assertEquals("\u0000", "&#0;".unescapeXml(), "the lenient rule decodes it");
    }

    @Test
    void pythonRules() {
        assertEquals("&#x27;&lt;s&gt;&quot;&amp;", "'<s>\"&".escapeHtml(true));
        assertEquals("'&lt;s&gt;\"&amp;", "'<s>\"&".escapeHtml(false));
        assertEquals("\u00ACit", "&notit".unescapeHtml5Python());
        assertEquals("\u00A2es", "&centes".unescapeHtml5Python(), "longest prefix, as HTML5");
        assertEquals("a\u20AC\r\uFFFDb", "a&#1;&#x80;&#13&#0;&#x7f;b".unescapeHtml5Python());
        assertEquals("\u0081\uFFFD", "&#129;&#x110000;".unescapeHtml5Python());
        assertEquals("&co;&Amp;", "&co;&Amp;".unescapeHtml5Python());
    }

    @Test
    void nullArguments() {
        assertThrows(NullPointerException.class, () -> "a".escapeHtml(null,
                String.HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT));
        assertThrows(NullPointerException.class, () -> "a".escapeHtml(
                String.HtmlEscapeType.DECIMAL_REFERENCES, null));
        assertThrows(NullPointerException.class, () -> "a".escapeXml(null,
                String.XmlEscapeType.DECIMAL_REFERENCES,
                String.XmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT, false));
        assertThrows(NullPointerException.class, () -> "a".escapeXml(String.XmlVersion.XML_1_0,
                null, String.XmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT, false));
        assertThrows(NullPointerException.class, () -> "a".escapeXml(String.XmlVersion.XML_1_0,
                String.XmlEscapeType.DECIMAL_REFERENCES, null, false));
    }

    @Test
    void unchangedIsSame() {
        String plain = "plain text 123";
        assertSame(plain, plain.escapeHtml4());
        assertSame(plain, plain.escapeHtml5());
        assertSame(plain, plain.escapeXml10());
        assertSame(plain, plain.escapeHtml(true));
        assertSame(plain, plain.unescapeHtml());
        assertSame(plain, plain.unescapeHtml5Python());
        String amp = "a & b";
        assertSame(amp, amp.unescapeHtml4());
        assertSame(amp, amp.unescapeXmlStrict());
    }

    @Test
    void longTextsAcrossWindows() {
        String pair = "" + HIGH + LOW;
        assertEquals("a".repeat(511) + "&afr;&amp;", ("a".repeat(511) + pair + "&").escapeHtml5());
        assertEquals("a".repeat(511) + pair + "&amp;",
                ("a".repeat(511) + pair + "&").escapeHtml4());
        assertEquals("a".repeat(1023) + "&#55349;", ("a".repeat(1023) + HIGH).escapeHtml4Xml());
        assertEquals("x".repeat(1023), ("x".repeat(1023) + HIGH).escapeXml10());
        assertEquals("x".repeat(600) + "<" + "y".repeat(600),
                ("x".repeat(600) + "&lt;" + "y".repeat(600)).unescapeHtml());
        assertEquals("&amp;".repeat(700), "&".repeat(700).escapeHtml(false));
    }
}

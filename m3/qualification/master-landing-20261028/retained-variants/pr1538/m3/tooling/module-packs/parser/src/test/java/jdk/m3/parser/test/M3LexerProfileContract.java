/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.security.MessageDigest;
import java.util.HexFormat;
import jdk.m3.parser.grammar.M3GrammarException;
import jdk.m3.parser.grammar.M3GrammarLexer;
import jdk.m3.parser.grammar.M3GrammarLimits;
import jdk.m3.parser.grammar.M3LexerProfile;
import jdk.m3.parser.grammar.M3TokenStream;

/** Public behavior oracle, frozen against the complete original boolean policy. */
public final class M3LexerProfileContract {
    private static final int FLAGS = 19;
    private static final String IDENTITY = "profile-\u03b1-\ud800";
    private static final String[] OPERATORS = {">", "==", "Aa", "BB", ">", "<=", "=", ">>>"};
    // Deliberately explicit; this oracle does not invoke the production comparator.
    private static final String[] SORTED = {">>>", "<=", "==", "Aa", "BB", "=", ">", ">"};
    private static final String TEXT = "\u03b1_1 true FALSE null NaN Infinity 0xF_F 1e+ .5 "
            + "$id #id :id ?12 @id [a]]b] 'a''b' \"q\" `null` <= == >>> > + /**/ z "
            + "-- line\r\nend // line\nx \ud83d\ude00 \ud800 \udfff";
    private static final String EXPECTED_POLICIES = "cafb4192a3ad8dca39e175883b04a9ec4e221b5e0bfcaf93fa20a494539fc8c2";
    private static final String EXPECTED_TOKENS = "cd3d6b33746613861441507afe0bf1d174145723924b9011d7432f97187ead71";
    private static long checks;

    private M3LexerProfileContract() { }

    public static void main(String[] arguments) throws Exception {
        String[] roots = observe();
        check(roots[0].equals(EXPECTED_POLICIES), "frozen policy/fingerprint/refusal transcript");
        check(roots[1].equals(EXPECTED_TOKENS), "frozen lexical transcript for every accepted policy");
        System.out.println("M3_LEXER_PROFILE_PASS states=524288 accepted=221184 refused=303104 checks="
                + checks + " policies=" + roots[0] + " tokens=" + roots[1]);
    }

    /** Baseline recorder uses this exact observation body before the candidate exists. */
    static String[] observe() throws Exception {
        Transcript policies = new Transcript();
        Transcript tokens = new Transcript();
        int accepted = 0;
        int refused = 0;
        long emptyFingerprint = expectedFingerprint(0, IDENTITY, SORTED);
        for (int vector = 0; vector < 1 << FLAGS; vector++) {
            M3LexerProfile.Builder builder = set(M3LexerProfile.builder(IDENTITY).operators(OPERATORS), vector);
            String refusal = conflict(vector);
            policies.integer(vector);
            if (refusal != null) {
                IllegalStateException failure = expect(IllegalStateException.class, builder::build);
                check(refusal.equals(failure.getMessage()), "role conflict precedence at " + vector);
                policies.integer(0);
                policies.text(failure.getClass().getName());
                policies.text(failure.getMessage());
                refused++;
                continue;
            }
            M3LexerProfile profile = builder.build();
            check(profile.identity() == IDENTITY, "identity object retained");
            int actual = flags(profile);
            check(actual == vector, "all nineteen getters at " + vector);
            long fingerprint = profile.fingerprint();
            check(fingerprint == expectedFingerprint(vector, IDENTITY, SORTED), "ordered fingerprint at " + vector);
            policies.integer(1);
            policies.integer(actual);
            policies.wide(fingerprint);

            M3TokenStream stream = new M3GrammarLexer(profile).lex(TEXT);
            check(stream.source() == TEXT, "immutable source object retained");
            tokens.integer(vector);
            tokenTranscript(tokens, stream);

            M3LexerProfile reset = set(builder, 0).build();
            check(reset != profile && flags(reset) == 0 && reset.fingerprint() == emptyFingerprint,
                    "false overwrites every flag in reused builder");
            check(flags(profile) == vector && profile.fingerprint() == fingerprint,
                    "built profile isolated from later builder mutation");
            accepted++;
        }
        check(accepted == 221184 && refused == 303104, "complete finite boolean domain");
        operatorAndBuilderContracts();
        lexicalEdges(tokens);
        return new String[] {policies.finish(), tokens.finish()};
    }

    private static M3LexerProfile.Builder set(M3LexerProfile.Builder b, int v) {
        return b.caseInsensitiveLiterals(on(v, 0)).singleQuoteString(on(v, 1))
                .doubleQuoteString(on(v, 2)).dollarInIdentifier(on(v, 3))
                .doubleQuoteIdentifier(on(v, 4)).backtickIdentifier(on(v, 5))
                .backtickJsonLiteral(on(v, 6)).lineCommentDashDash(on(v, 7))
                .lineCommentSlashSlash(on(v, 8)).blockComments(on(v, 9))
                .hashNamePlaceholder(on(v, 10)).colonValuePlaceholder(on(v, 11))
                .questionParameter(on(v, 12)).bracketQuotedIdentifier(on(v, 13))
                .colonParameter(on(v, 14)).atParameter(on(v, 15))
                .dollarParameter(on(v, 16)).hexNumbers(on(v, 17))
                .specialNumberLiterals(on(v, 18));
    }

    private static boolean on(int vector, int position) { return (vector & (1 << position)) != 0; }

    private static int flags(M3LexerProfile p) {
        boolean[] observations = {p.caseInsensitiveLiterals(), p.singleQuoteString(), p.doubleQuoteString(),
                p.dollarInIdentifier(), p.doubleQuoteIdentifier(), p.backtickIdentifier(), p.backtickJsonLiteral(),
                p.lineCommentDashDash(), p.lineCommentSlashSlash(), p.blockComments(), p.hashNamePlaceholder(),
                p.colonValuePlaceholder(), p.questionParameter(), p.bracketQuotedIdentifier(), p.colonParameter(),
                p.atParameter(), p.dollarParameter(), p.hexNumbers(), p.specialNumberLiterals()};
        int result = 0;
        for (int i = 0; i < observations.length; i++) if (observations[i]) result += 1 << i;
        return result;
    }

    private static String conflict(int vector) {
        if (on(vector, 5) && on(vector, 6)) return "backtick role conflict";
        if (on(vector, 11) && on(vector, 14)) return "colon token role conflict";
        if (on(vector, 3) && on(vector, 16)) return "dollar token role conflict";
        return null;
    }

    // Frozen incumbent fingerprint contract, with explicit order independent of new storage.
    private static long expectedFingerprint(int vector, String identity, String[] sorted) {
        long result = mix(0x4d494c4558455231L, identity.hashCode());
        for (int bit = 0; bit < FLAGS; bit++) result = mix(result, on(vector, bit) ? 1 : 0);
        for (String operator : sorted) result = mix(result, operator.hashCode());
        return result;
    }

    private static long mix(long left, long right) {
        long z = left ^ Long.rotateLeft(right + 0x9e3779b97f4a7c15L, 29);
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static void operatorAndBuilderContracts() {
        check("identity".equals(expect(NullPointerException.class,
                () -> M3LexerProfile.builder(null)).getMessage()), "identity null diagnostic");
        M3LexerProfile.Builder b = M3LexerProfile.builder(IDENTITY);
        for (int bit = 0; bit < FLAGS; bit++) {
            check(setOne(b, bit, true) == b, "true setter returns original builder");
            check(flags(b.build()) == 1 << bit, "setter changes exactly its own policy");
            check(setOne(b, bit, false) == b && flags(b.build()) == 0,
                    "false setter returns original builder and clears only its policy");
        }
        String[] aliased = OPERATORS.clone();
        check(b.operators(aliased) == b, "operators returns original builder");
        aliased[0] = "!";
        M3LexerProfile frozen = b.build();
        long before = expectedFingerprint(0, IDENTITY, SORTED);
        check(frozen.fingerprint() == before, "operator input snapshot");
        b.operators("!");
        check(frozen.fingerprint() == before, "built operator snapshot survives builder reuse");
        check(b.build().fingerprint() == expectedFingerprint(0, IDENTITY, new String[] {"!"}),
                "new operator snapshot installed");
        check("values".equals(expect(NullPointerException.class,
                () -> b.operators((String[]) null)).getMessage()), "null operator-array diagnostic");
        check(b.build().fingerprint() == expectedFingerprint(0, IDENTITY, new String[] {"!"}),
                "null operator-array failure preserves prior builder state");
        check("empty operator".equals(expect(IllegalArgumentException.class,
                () -> b.operators("")).getMessage()), "empty operator diagnostic");
        // The original setter installs the clone before element validation; keep that observable state.
        check(b.build().fingerprint() == expectedFingerprint(0, IDENTITY, new String[] {""}),
                "refused empty element retains incumbent builder state");
        check("empty operator".equals(expect(IllegalArgumentException.class,
                () -> b.operators(new String[] {null})).getMessage()), "null element diagnostic");
        M3LexerProfile invalid = b.build();
        expect(NullPointerException.class, invalid::fingerprint);
        b.operators(OPERATORS);
        check(b.build().fingerprint() == before, "builder recovers after explicit valid replacement");
        M3LexerProfile duplicates = M3LexerProfile.builder(IDENTITY).operators(">", ">").build();
        check(duplicates.fingerprint() == expectedFingerprint(0, IDENTITY, new String[] {">", ">"}),
                "duplicate operator multiplicity preserved");
        check(M3LexerProfile.builder("Aa").build().fingerprint()
                == M3LexerProfile.builder("BB").build().fingerprint(),
                "existing String-hash collision behavior retained");
    }

    private static M3LexerProfile.Builder setOne(M3LexerProfile.Builder b, int bit, boolean value) {
        return switch (bit) {
            case 0 -> b.caseInsensitiveLiterals(value);
            case 1 -> b.singleQuoteString(value);
            case 2 -> b.doubleQuoteString(value);
            case 3 -> b.dollarInIdentifier(value);
            case 4 -> b.doubleQuoteIdentifier(value);
            case 5 -> b.backtickIdentifier(value);
            case 6 -> b.backtickJsonLiteral(value);
            case 7 -> b.lineCommentDashDash(value);
            case 8 -> b.lineCommentSlashSlash(value);
            case 9 -> b.blockComments(value);
            case 10 -> b.hashNamePlaceholder(value);
            case 11 -> b.colonValuePlaceholder(value);
            case 12 -> b.questionParameter(value);
            case 13 -> b.bracketQuotedIdentifier(value);
            case 14 -> b.colonParameter(value);
            case 15 -> b.atParameter(value);
            case 16 -> b.dollarParameter(value);
            case 17 -> b.hexNumbers(value);
            case 18 -> b.specialNumberLiterals(value);
            default -> throw new AssertionError("unknown flag");
        };
    }

    private static void lexicalEdges(Transcript out) {
        int rich = ((1 << FLAGS) - 1) & ~(1 << 3) & ~(1 << 5) & ~(1 << 11);
        String[] texts = {"", " \t\r\n", "/*", "'", "\"", "`", "[", "[a]]", "'a\\'b'", "\"a\"\"b\"",
                "// trailing", "--\r\nnext", "a/*text*/b", "1e+ 0x 0xF_ 0X\u0661 .2e-", "TRUE False NuLl Infinity NaN",
                "\ud800\udc00\ud800\udfff\udfff", "# $ : @ ?", "#x :x @x $x ?001", "===>><=", "\"/* not comment */\""};
        for (int vector : new int[] {0, 1 << 1, 1 << 4, 1 << 5, 1 << 6, 1 << 9, 1 << 13, rich}) {
            M3LexerProfile profile = set(M3LexerProfile.builder(IDENTITY).operators(OPERATORS), vector).build();
            M3GrammarLexer lexer = new M3GrammarLexer(profile);
            check(lexer.profile() == profile, "lexer retains immutable policy");
            for (String text : texts) {
                out.integer(vector);
                out.text(text);
                try {
                    M3TokenStream stream = lexer.lex(text);
                    out.integer(1);
                    tokenTranscript(out, stream);
                } catch (M3GrammarException failure) {
                    out.integer(0);
                    out.text(failure.getClass().getName());
                    out.text(failure.getMessage());
                }
            }
        }
        M3LexerProfile plain = M3LexerProfile.builder("bounded").build();
        expect(NullPointerException.class, () -> new M3GrammarLexer(plain).lex(null));
        expect(M3GrammarException.class, () -> new M3GrammarLexer(plain,
                new M3GrammarLimits(2, 8, 32, 32, 4, 64)).lex("abc"));
        expect(M3GrammarException.class, () -> new M3GrammarLexer(plain,
                new M3GrammarLimits(16, 1, 32, 32, 4, 64)).lex("a b"));
        expect(M3GrammarException.class, () -> new M3GrammarLexer(plain,
                new M3GrammarLimits(16, 8, 32, 32, 4, 1)).lex("abc"));
        StringBuilder mutable = new StringBuilder("before");
        M3TokenStream frozen = new M3GrammarLexer(plain).lex(mutable);
        mutable.setCharAt(0, 'B');
        check(frozen.source().equals("before") && frozen.textAt(0).equals("before"),
                "foreign mutable source remains a captured snapshot");
    }

    private static void tokenTranscript(Transcript output, M3TokenStream stream) {
        output.integer(stream.size());
        for (int index = 0; index < stream.size(); index++) {
            output.integer(stream.start(index));
            output.integer(stream.end(index));
            output.integer(stream.tokenClass(index).ordinal());
            output.text(stream.textAt(index));
        }
    }

    private static <T extends Throwable> T expect(Class<T> type, Operation action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }

    private static final class Transcript {
        private final MessageDigest digest;
        private final byte[] buffer = new byte[8192];
        private int used;

        Transcript() throws java.security.NoSuchAlgorithmException { digest = MessageDigest.getInstance("SHA-256"); }

        void integer(int value) {
            if (used == buffer.length) flush();
            buffer[used++] = (byte) (value >>> 24);
            buffer[used++] = (byte) (value >>> 16);
            buffer[used++] = (byte) (value >>> 8);
            buffer[used++] = (byte) value;
        }

        void wide(long value) { integer((int) (value >>> 32)); integer((int) value); }

        void text(String value) {
            integer(value.length());
            for (int i = 0; i < value.length(); i++) integer(value.charAt(i));
        }

        private void flush() { digest.update(buffer, 0, used); used = 0; }
        String finish() { flush(); return HexFormat.of().formatHex(digest.digest()); }
    }
}

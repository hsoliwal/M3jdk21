// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.io.NumberInput;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;

/**
 * JACKSON_NUMBERS_TO_JDK v1 differential. Run it once with master's jackson-core 2.19.2 and its FastDoubleParser
 * 1.0.90 on the class path, once with the overlaid jackson-core alone, on the stock JDK and on M3JDK's java.base.
 * Every number of a seeded corpus goes through Jackson's char and byte parsers (the fast double and big-number
 * features on, each getter in a fresh parser) and through the NumberInput fast-parser methods. One line per result:
 * the value (double/float raw bits, BigDecimal unscaled value and scale, BigInteger) or the exception class.
 * Exception messages go to a second stream (args[2]) so values and messages compare separately.
 */
public final class JacksonNumbersDifferential {
    private static PrintStream messages;

    public static void main(String[] args) throws Exception {
        long seed = Long.parseLong(args[0]);
        int count = Integer.parseInt(args[1]);
        messages = new PrintStream(args[2], StandardCharsets.UTF_8);
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
                .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(100_000).build())
                .build();
        SplittableRandom random = new SplittableRandom(seed);
        StringBuilder out = new StringBuilder(1 << 16);
        for (int i = 0; i < count; i++) {
            String json = jsonNumber(random);
            out.append(i).append(" json ").append(json.length() > 60 ? json.length() + "c" : json);
            for (int getter = 0; getter < 4; getter++) {
                out.append(" | ").append(viaParser(factory, json, getter, false));
                out.append(" ").append(viaParser(factory, json, getter, true));
            }
            String literal = (i & 3) == 0 ? javaLiteral(random) : json;
            out.append(" || ").append(direct(literal));
            System.out.println(out);
            out.setLength(0);
        }
        messages.close();
    }

    private static String jsonNumber(SplittableRandom r) {
        StringBuilder b = new StringBuilder();
        if (r.nextInt(4) == 0) b.append('-');
        int digits = r.nextInt(8) == 0 ? 400 + r.nextInt(1200) : 1 + r.nextInt(r.nextBoolean() ? 20 : 120);
        b.append((char) ('1' + r.nextInt(9)));
        for (int k = 1; k < digits; k++) b.append((char) ('0' + r.nextInt(10)));
        if (r.nextInt(16) == 0) return b.length() > 1 && b.charAt(0) == '-' ? "-0" : "0";
        if (r.nextBoolean()) {
            b.append('.');
            int fraction = 1 + r.nextInt(r.nextInt(8) == 0 ? 900 : 30);
            for (int k = 0; k < fraction; k++) b.append((char) ('0' + r.nextInt(10)));
        }
        if (r.nextInt(3) == 0) {
            b.append(r.nextBoolean() ? 'e' : 'E');
            int kind = r.nextInt(10);
            if (kind == 0) b.append(r.nextBoolean() ? "+" : "-");
            long exponent = kind < 7 ? r.nextInt(30) : kind < 9 ? r.nextInt(700) : 2_147_483_000L + r.nextInt(2000);
            b.append(r.nextBoolean() ? exponent : -exponent);
        }
        return b.toString();
    }

    private static String javaLiteral(SplittableRandom r) {
        String[] fixed = {"NaN", "-Infinity", "+Infinity", "  1.5  ", "1d", "2.5f", "0x1p3", "0x1.8p-2", "-0x0.0p0",
            "0x8Db1Ffd473B83a5fp196", "0X1.FFFFFFFFFFFFFP1023", "1e", "abc", "", "--1", "1_000", ".5", "5.", "1e+",
            "0x", "1.0e2147483648", "4.9e-324", "2.4703282292062327e-324", "1.7976931348623159e308", "+7", "0005"};
        if (r.nextBoolean()) return fixed[r.nextInt(fixed.length)];
        StringBuilder b = new StringBuilder("0x");
        int digits = 1 + r.nextInt(24);
        for (int k = 0; k < digits; k++) b.append("0123456789abcdefABCDEF".charAt(r.nextInt(22)));
        if (r.nextBoolean()) b.append('.').append(Integer.toHexString(r.nextInt(1 << 20)));
        return b.append('p').append(r.nextInt(2200) - 1100).toString();
    }

    private static String viaParser(JsonFactory factory, String json, int getter, boolean bytes) {
        return attempt(() -> {
            try (JsonParser p = bytes ? factory.createParser(("[" + json + "]").getBytes(StandardCharsets.UTF_8))
                                      : factory.createParser("[" + json + "]")) {
                p.nextToken();
                JsonToken token = p.nextToken();
                return switch (getter) {
                    case 0 -> token + ":" + Long.toHexString(Double.doubleToRawLongBits(p.getDoubleValue()));
                    case 1 -> Integer.toHexString(Float.floatToRawIntBits(p.getFloatValue()));
                    case 2 -> big(p.getDecimalValue());
                    default -> token == JsonToken.VALUE_NUMBER_INT ? p.getNumberType() + ":" + p.getBigIntegerValue()
                                                                    : p.getNumberType().toString();
                };
            }
        });
    }

    private static String direct(String s) {
        char[] chars = (" " + s + " ").toCharArray();
        return attempt(() -> Long.toHexString(Double.doubleToRawLongBits(NumberInput.parseDouble(s, true))))
                + " " + attempt(() -> Long.toHexString(Double.doubleToRawLongBits(
                        NumberInput.parseDouble(chars, 1, s.length(), true))))
                + " " + attempt(() -> Integer.toHexString(Float.floatToRawIntBits(NumberInput.parseFloat(s, true))))
                + " " + attempt(() -> Integer.toHexString(Float.floatToRawIntBits(
                        NumberInput.parseFloat(chars, 1, s.length(), true))))
                + " " + attempt(() -> big(NumberInput.parseBigDecimal(s, true)))
                + " " + attempt(() -> big(NumberInput.parseBigDecimal(chars, 1, s.length(), true)))
                + " " + attempt(() -> String.valueOf(NumberInput.parseBigInteger(s, true)))
                + " " + attempt(() -> String.valueOf(NumberInput.parseBigIntegerWithRadix(s, 16, true)));
    }

    private static String big(BigDecimal d) {
        return d.unscaledValue().toString(36) + "e" + d.scale();
    }

    private static String attempt(Callable<String> call) {
        try {
            return call.call();
        } catch (Throwable t) {
            String message = String.valueOf(t.getMessage());
            messages.println(t.getClass().getName() + ": " + (message.length() > 300 ? message.substring(0, 300) : message));
            return "!" + t.getClass().getSimpleName();
        }
    }
}

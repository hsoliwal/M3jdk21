// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.core.io.NumberOutput;
import com.fasterxml.jackson.core.io.schubfach.DoubleToDecimal;
import com.fasterxml.jackson.core.io.schubfach.FloatToDecimal;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.SplittableRandom;

/**
 * JACKSON_NUMBERS_TO_JDK v2 differential: run with the jackson-core before v2 (its own Schubfach copy) and after
 * (java.base). Each sample double and float goes through the char and byte generators with USE_FAST_DOUBLE_WRITER on,
 * NumberOutput.toString(v, true) and the io.schubfach facades. One line per sample; the two runs must be identical.
 */
public final class JacksonNumberWriteDifferential {
    public static void main(String[] args) throws Exception {
        long count = Long.parseLong(args[1]);
        SplittableRandom random = new SplittableRandom(Long.parseLong(args[0]));
        JsonFactory factory = JsonFactory.builder().enable(StreamWriteFeature.USE_FAST_DOUBLE_WRITER).build();
        StringBuilder out = new StringBuilder(1 << 16);
        for (long i = 0; i < count; i++) {
            double d = switch ((int) (i % 3)) {
                case 0 -> Double.longBitsToDouble(random.nextLong());
                case 1 -> random.nextInt(1_000_000_000) / Math.pow(10, random.nextInt(25));
                default -> random.nextDouble() * Math.pow(10, random.nextInt(600) - 300);
            };
            float f = (i & 1) == 0 ? Float.intBitsToFloat(random.nextInt()) : (float) d;
            StringWriter chars = new StringWriter();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (JsonGenerator c = factory.createGenerator(chars); JsonGenerator b = factory.createGenerator(bytes)) {
                for (JsonGenerator g : new JsonGenerator[] {c, b}) {
                    g.writeStartArray();
                    g.writeNumber(d);
                    g.writeNumber(f);
                    g.writeEndArray();
                }
            }
            out.append(chars).append(' ').append(bytes.toString(StandardCharsets.UTF_8)).append(' ')
                    .append(NumberOutput.toString(d, true)).append(' ').append(NumberOutput.toString(f, true)).append(' ')
                    .append(DoubleToDecimal.toString(d)).append(' ').append(FloatToDecimal.toString(f));
            System.out.println(out);
            out.setLength(0);
        }
    }
}

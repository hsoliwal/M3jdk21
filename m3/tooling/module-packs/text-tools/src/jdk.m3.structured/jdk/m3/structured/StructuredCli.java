/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Pipe-friendly bounded structured text CLI; no filesystem write, reflection, or process spawn. */
public final class StructuredCli {
    private static final Limits LIMITS = Limits.standard();
    private StructuredCli() { }

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, System.err));
    }

    /** Returns 0 on success, 2 for invalid input/arguments, 1 for I/O failures. */
    public static int run(String[] args, InputStream input, OutputStream output, PrintStream errors) {
        Objects.requireNonNull(args, "args");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(errors, "errors");
        try {
            if (args.length < 2 || args.length > 3) throw new IllegalArgumentException("invalid arguments");
            String command = args[0];
            if (!command.equals("csv-cut") && !command.equals("tsv-cut")
                    && !command.equals("jsonl-pointer") && !command.equals("ini-get")
                    && !command.equals("env-get")) {
                throw new IllegalArgumentException("unknown command");
            }
            if ((command.equals("ini-get") ? args.length != 3 : args.length != 2)) {
                throw new IllegalArgumentException("invalid command arity");
            }
            int max = Math.toIntExact(LIMITS.maxBytes());
            byte[] bytes = input.readNBytes(max + 1);
            if (bytes.length > max) throw new IllegalArgumentException("input byte budget");
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            TextFrame frame = TextFrame.fromString(decoded);
            LIMITS.check(frame);
            String result = switch (command) {
                case "csv-cut", "tsv-cut" -> StructuredTextTools.selectDelimited(frame,
                        command.equals("csv-cut") ? ',' : '\t', columns(args[1]), LIMITS).text();
                case "jsonl-pointer" -> JsonLinesTools.select(frame, args[1], LIMITS).text();
                case "ini-get" -> {
                    var values = StructuredTextTools.ini(frame, LIMITS).get(args[1]);
                    if (values == null || !values.containsKey(args[2])) {
                        throw new IllegalArgumentException("missing INI key");
                    }
                    yield values.get(args[2]) + "\n";
                }
                case "env-get" -> {
                    var values = StructuredTextTools.dotenv(frame, LIMITS);
                    if (!values.containsKey(args[1])) throw new IllegalArgumentException("missing .env key");
                    yield values.get(args[1]) + "\n";
                }
                default -> throw new IllegalArgumentException("unsupported command");
            };
            byte[] produced = result.getBytes(StandardCharsets.UTF_8);
            if (produced.length > max) throw new IllegalArgumentException("output byte budget");
            output.write(produced);
            output.flush();
            return 0;
        } catch (CharacterCodingException | IllegalArgumentException e) {
            errors.println("m3-structured: " + e.getMessage());
            return 2;
        } catch (IOException e) {
            errors.println("m3-structured: I/O failure");
            return 1;
        }
    }

    private static int[] columns(String value) {
        if (value.isEmpty() || value.length() > 2048) throw new IllegalArgumentException("invalid columns");
        String[] parts = value.split(",", -1);
        if (parts.length > LIMITS.maxNodes()) throw new IllegalArgumentException("column budget");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("invalid column");
            try { result[i] = Integer.parseInt(parts[i]); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("column overflow", e); }
        }
        return result;
    }
}


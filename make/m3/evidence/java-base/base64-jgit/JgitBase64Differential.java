// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.zip.CRC32;

import org.eclipse.jgit.util.Base64;

/**
 * JGIT_BASE64_TO_JDK v1 differential: one line per call of JGit's util.Base64 on seeded inputs. Each line is the case
 * kind and the outcome: the result (hex up to 24 bytes, else length and CRC-32) or the exception class and message.
 * Run on the master jar and on the branch jar and compare line by line.
 *
 * Decode cases: canonical base64 of random bytes, and the same with white space inserted, padding stripped, high bits
 * set, data after the padding, '=' inside, the URL-safe alphabet, random characters, inside a larger array (sub-range),
 * invalid ranges, null, and decode(String) of Latin-1 text. Encode cases: random bytes and sub-ranges, invalid ranges,
 * null.
 */
public final class JgitBase64Differential {
    private static final byte[] NOISE = " \t\r\n=+/-_.:\u0000AZaz09".getBytes(StandardCharsets.ISO_8859_1);
    private static final String[] KINDS = {"canonical", "whitespace", "unpadded", "highbit", "trailing", "equals-inside",
        "urlsafe", "random", "subrange", "string"};

    public static void main(String[] args) {
        SplittableRandom random = new SplittableRandom(Long.parseLong(args[0]));
        int count = Integer.parseInt(args[1]);
        StringBuilder out = new StringBuilder(1 << 16);
        for (int i = 0; i < count; i++) {
            String kind = KINDS[i % KINDS.length];
            byte[] input = decodeInput(kind, random);
            out.append("dec-").append(kind).append(' ');
            if (kind.equals("string")) {
                String text = new String(input, StandardCharsets.ISO_8859_1);
                outcome(out, () -> Base64.decode(text));
            } else if (kind.equals("subrange")) {
                int start = random.nextInt(9);
                byte[] big = embed(input, start, random);
                outcome(out, () -> Base64.decode(big, start, input.length));
            } else {
                outcome(out, () -> Base64.decode(input, 0, input.length));
            }
            byte[] data = new byte[random.nextInt(300)];
            random.nextBytes(data);
            int off = data.length == 0 ? 0 : random.nextInt(data.length);
            int len = random.nextInt(data.length - off + 1);
            out.append("enc-full ");
            outcome(out, () -> Base64.encodeBytes(data).getBytes(StandardCharsets.ISO_8859_1));
            out.append("enc-range ");
            outcome(out, () -> Base64.encodeBytes(data, off, len).getBytes(StandardCharsets.ISO_8859_1));
            if (out.length() > 1 << 15) {
                System.out.print(out);
                out.setLength(0);
            }
        }
        invalid(out);
        System.out.print(out);
    }

    private static byte[] decodeInput(String kind, SplittableRandom random) {
        byte[] data = new byte[random.nextInt(200)];
        random.nextBytes(data);
        byte[] text = java.util.Base64.getEncoder().encode(data);
        switch (kind) {
            case "whitespace":
                return insert(text, random, " \t\r\n".getBytes(StandardCharsets.ISO_8859_1));
            case "unpadded":
                return java.util.Base64.getEncoder().withoutPadding().encode(data);
            case "highbit":
                for (int k = 0, n = 1 + random.nextInt(3); k < n && text.length > 0; k++) {
                    text[random.nextInt(text.length)] |= (byte) 0x80;
                }
                return text;
            case "trailing":
                byte[] more = java.util.Base64.getEncoder().encode(new byte[1 + random.nextInt(9)]);
                byte[] joined = Arrays.copyOf(text, text.length + more.length);
                System.arraycopy(more, 0, joined, text.length, more.length);
                return joined;
            case "equals-inside":
                if (text.length > 0) {
                    text[random.nextInt(text.length)] = '=';
                }
                return text;
            case "urlsafe":
                return java.util.Base64.getUrlEncoder().encode(data);
            case "random":
                byte[] chars = new byte[random.nextInt(41)];
                for (int k = 0; k < chars.length; k++) {
                    chars[k] = random.nextInt(4) == 0 ? NOISE[random.nextInt(NOISE.length)]
                            : text.length == 0 ? (byte) 'Q' : text[random.nextInt(text.length)];
                }
                return chars;
            case "string":
                return random.nextBoolean() ? text : insert(text, random, new byte[] {(byte) 0xE9, (byte) 0xC1});
            default:
                return text;
        }
    }

    private static byte[] insert(byte[] text, SplittableRandom random, byte[] pool) {
        byte[] out = new byte[text.length * 2 + 4];
        int n = 0;
        for (byte b : text) {
            if (random.nextInt(8) == 0) {
                out[n++] = pool[random.nextInt(pool.length)];
            }
            out[n++] = b;
        }
        return Arrays.copyOf(out, n);
    }

    /** input at start inside random bytes from "AQ=!", which the decoder must not read. */
    private static byte[] embed(byte[] input, int start, SplittableRandom random) {
        byte[] big = new byte[start + input.length + random.nextInt(8)];
        for (int k = 0; k < big.length; k++) {
            big[k] = (byte) "AQ=!".charAt(random.nextInt(4));
        }
        System.arraycopy(input, 0, big, start, input.length);
        return big;
    }

    private static void invalid(StringBuilder out) {
        byte[] text = "QUJD".getBytes(StandardCharsets.ISO_8859_1);
        int[][] ranges = {{-1, 4}, {0, -1}, {0, -4}, {0, 5}, {4, 0}, {5, 0}, {-1, 0}, {1, 4}, {2, 2}, {Integer.MAX_VALUE, 4}};
        for (int[] r : ranges) {
            out.append("dec-invalid ").append(r[0]).append(',').append(r[1]).append(' ');
            outcome(out, () -> Base64.decode(text, r[0], r[1]));
            out.append("enc-invalid ").append(r[0]).append(',').append(r[1]).append(' ');
            outcome(out, () -> Base64.encodeBytes(text, r[0], r[1]).getBytes(StandardCharsets.ISO_8859_1));
        }
        out.append("dec-null 0 ");
        outcome(out, () -> Base64.decode(null, 0, 0));
        out.append("dec-null 4 ");
        outcome(out, () -> Base64.decode(null, 0, 4));
        out.append("enc-null ");
        outcome(out, () -> Base64.encodeBytes(null).getBytes(StandardCharsets.ISO_8859_1));
    }

    interface Call {
        byte[] run();
    }

    private static void outcome(StringBuilder out, Call call) {
        try {
            byte[] result = call.run();
            if (result.length <= 24) {
                out.append("ok hex=");
                for (byte b : result) {
                    out.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
                }
            } else {
                CRC32 crc = new CRC32();
                crc.update(result, 0, result.length);
                out.append("ok len=").append(result.length).append(" crc=").append(Long.toHexString(crc.getValue()));
            }
        } catch (RuntimeException e) {
            out.append("ex ").append(e.getClass().getName()).append(": ").append(e.getMessage());
        }
        out.append('\n');
    }
}

// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package census3;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * Seeded differential of census3 P3-012 (m3/file): the donor MIndexFile family against the owner jdk.m3.file family
 * with the restored members. Usage: java census3.Census3FileDifferential donor|owner episodes [--trace]. Prints one
 * SHA-256 over the canonical op log: per episode the tokenized text (token count / UTF-16 length), the UTF-8 channel
 * facades of the file, the semantic projection, a kind projection, a span and the snapshot (byte count, bytes written,
 * digest of the written bytes, through a partial channel of a seeded chunk size), the channel writer against the
 * OutputStream writer (event count, equality, finished flags), and the validated-pass readers (readIndexedText /
 * readProjectionText: length + digest of the recovered text; read(InputStream) summary counts) on the intact stream, on
 * truncated copies and on copies with one bit of the terminal content digest flipped (refusal classes). The two sides
 * must print byte-identical output. Random-position tampering is not compared: the owner's frame validation (not a
 * census3 member) refuses streams the donor accepts, or refuses with IOException where the donor throws EOFException
 * (README.md). A zero-progress
 * channel is not probed: the owner's M3FileText.writeUtf8To (not a census3 member) spins on it where the donor
 * MIndexString.writeUtf8To throws IOException (recorded in README.md as an owner observation).
 */
public final class Census3FileDifferential {
    private static final long SEED = 0x43334649304cL; // "C3FI0L"
    private static final String[] ATOMS = {"alpha", "beta", "gamma", "x1", "42", "3.14", "0x1F", " ", "  ", "\t", "\n",
        "\r\n", "+", "-", "==", "(", ")", ";", ",", ".", "//", "/*", "*/", "\"quoted\"", "'c'", "\"\"", "é",
        "हि", "🙂", "\ud800", "\udc00", "note", "// note", "\u0000", "_id", "$v", "あ"};

    /** One implementation side. */
    public interface Side {
        void reset();
        String tokenize(String text, boolean code);
        String writeFile(int chunk);
        String writeSemantic(int chunk);
        String writeKinds(int chunk, int kindMask);
        String writeSpan(int chunk, int start, int end);
        String writeSnapshot(int chunk);
        String events(String text, int chunk) throws Exception;
        String readIndexed(int truncate, int tamperAt);
        String readProjection(int kindMask, int truncate, int tamperAt);
        String readSummary(int truncate, int tamperAt);
    }

    public static void main(String[] args) throws Exception {
        Side side = (Side) Class.forName(args[0].equals("donor") ? "census3.DonorFile" : "census3.OwnerFile")
                .getDeclaredConstructor().newInstance();
        int episodes = Integer.parseInt(args[1]);
        boolean trace = args.length > 2;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder log = new StringBuilder(1 << 16);
        SplittableRandom random = new SplittableRandom(SEED);
        for (int episode = 0; episode < episodes; episode++) {
            SplittableRandom r = random.split();
            side.reset();
            String text = text(r, r.nextInt(12) == 0 ? 0 : 1 + r.nextInt(40), false);
            boolean code = r.nextBoolean();
            int chunk = 1 + r.nextInt(7);
            int kinds = r.nextInt(256);
            log.append("E ").append(episode).append(' ').append(code).append(' ').append(chunk).append(' ')
                    .append(side.tokenize(text, code)).append('\n');
            log.append(" f ").append(side.writeFile(chunk)).append('\n');
            log.append(" p ").append(side.writeSemantic(chunk)).append('\n');
            log.append(" k ").append(kinds).append(' ').append(side.writeKinds(chunk, kinds)).append('\n');
            int start = r.nextInt(-1, 6);
            int end = start + r.nextInt(-1, 8);
            log.append(" s ").append(start).append(' ').append(end).append(' ').append(side.writeSpan(chunk, start, end))
                    .append('\n');
            log.append(" n ").append(side.writeSnapshot(chunk)).append('\n');
            if (episode % 4 == 0) {
                // the event round trip goes through a UTF-8 file: well-formed text only (no lone surrogates)
                String fileText = text(r, 1 + r.nextInt(40), true);
                log.append(" e ").append(side.events(fileText, chunk)).append('\n');
                log.append(" ri ").append(side.readIndexed(0, -1)).append('\n');
                log.append(" rp ").append(kinds).append(' ').append(side.readProjection(kinds, 0, -1)).append('\n');
                log.append(" rs ").append(side.readSummary(0, -1)).append('\n');
                int truncate = 1 + r.nextInt(40);
                int tamper = r.nextInt(32);
                log.append(" rt ").append(truncate).append(' ').append(side.readIndexed(truncate, -1)).append('\n');
                log.append(" rx ").append(tamper).append(' ').append(side.readProjection(kinds, 0, tamper)).append('\n');
                log.append(" ry ").append(tamper).append(' ').append(side.readSummary(0, tamper)).append('\n');
            }
            if (log.length() > (1 << 15)) {
                digest.update(log.toString().getBytes(StandardCharsets.UTF_8));
                if (trace) System.out.print(log);
                log.setLength(0);
            }
        }
        digest.update(log.toString().getBytes(StandardCharsets.UTF_8));
        if (trace) System.out.print(log);
        System.out.println("census3-file episodes=" + episodes + " sha256=" + HexFormat.of().formatHex(digest.digest()));
    }

    private static String text(SplittableRandom random, int atoms, boolean wellFormed) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < atoms; i++) {
            String atom = ATOMS[random.nextInt(ATOMS.length)];
            if (wellFormed && atom.length() == 1 && Character.isSurrogate(atom.charAt(0))) atom = "s";
            out.append(atom);
        }
        return out.toString();
    }
}

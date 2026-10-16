/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Qualified regex effect images preserve UTF-16 seams without retaining mappings
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3StringRegexImageTest
 */
import jdk.internal.mindex.M3RegexDpPlan;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.regex.Pattern;

public final class M3StringRegexImageTest {
    private static long checks;
    private static void check(boolean value) { if (!value) throw new AssertionError("regex image"); checks++; }
    public static void main(String[] args) throws Exception {
        String pattern = "a*".repeat(30) + ".*";
        M3RegexDpPlan producer = M3RegexDpPlan.prepare(pattern);
        M3RegexDpPlan consumer = M3RegexDpPlan.prepare(pattern);
        Path temp = Files.createTempFile("m3-regex-image-", ".bin");
        try {
            for (String s : new String[]{"", "a", "\ud800", "\udc00", "\udc00a\ud800", "\ud800\udc00"}) {
                // Test identity only; production must use its qualified canonical-source receipt.
                byte[] units = new byte[s.length() * 2];
                for (int i = 0; i < s.length(); i++) {
                    units[2*i] = (byte)s.charAt(i); units[2*i+1] = (byte)(s.charAt(i) >>> 8);
                }
                byte[] source = MessageDigest.getInstance("SHA-256").digest(units);
                byte[] image = producer.summarizeUtf16(s, null).toImage(source, null);
                byte[] trustedImage = MessageDigest.getInstance("SHA-256").digest(image);
                Files.write(temp, image);
                M3RegexDpPlan.Effect loaded;
                try (FileChannel file = FileChannel.open(temp, StandardOpenOption.READ)) {
                    loaded = consumer.restoreEffect(file.map(FileChannel.MapMode.READ_ONLY, 0, file.size()), source, trustedImage, null);
                }
                check(loaded.length() == s.length());
                check(Arrays.equals(image, loaded.toImage(source, null)));
                for (int n : new int[]{0, 1, 2, 17, 256}) {
                    check(consumer.accepts(loaded.repeated(n, null).apply(consumer.initialState()))
                            == Pattern.matches(pattern, s.repeat(n)));
                }
                image[image.length-1] ^= 1;
                try { consumer.restoreEffect(ByteBuffer.wrap(image), source, trustedImage, null); throw new AssertionError("digest not enforced"); }
                catch (IllegalArgumentException expected) { checks++; }
            }
        } finally { Files.deleteIfExists(temp); }
        System.out.println("M3_REGEX_IMAGE_PUBLIC_CHECKS=" + checks);
    }
}

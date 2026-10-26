// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Random;
import jdk.m3.parser.M3File;

public final class M3FileLineMapContract {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private static void reject(Runnable code) {
        checks++;
        try { code.run(); }
        catch (IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("invalid coordinate accepted");
    }
    private static void trial(String sourceText) {
        M3File source = M3File.of(URI.create("memory:/source"), sourceText);
        int lines = 1;
        for (int i = 0; i < sourceText.length(); i++) {
            char c = sourceText.charAt(i);
            if (c == '\r') {
                lines++;
                if (i + 1 < sourceText.length() && sourceText.charAt(i + 1) == '\n') i++;
            } else if (c == '\n') lines++;
        }
        check(source.lineCount() == lines, "CRLF/CR/LF exact logical lines");
        for (int offset = 0; offset <= sourceText.length(); offset++) {
            M3File.LinePosition p = source.linePosition(offset);
            check(p.line() >= 1 && p.line() <= lines && p.column() >= 0, "valid position");
            check(source.offsetAt(p.line(), p.column()) == offset, "every UTF-16 offset roundtrips");
        }
        check(source.text().equals(sourceText), "source content never modified");
        int badLine = lines + 1;
        reject(() -> source.linePosition(-1));
        reject(() -> source.linePosition(sourceText.length() + 1));
        reject(() -> source.offsetAt(0, 0));
        reject(() -> source.offsetAt(badLine, 0));
        reject(() -> source.offsetAt(1, 100_000));
    }
    public static void main(String[] args) {
        for (String s : new String[] {"", "a", "\r", "\n", "\r\n", "\r\n\r\n",
                "a\r\nb\rc\nd\ud83d\ude00\r\n", "\ud800\n\udc00"}) trial(s);
        Random random = new Random(0x3a5700ef);
        String[] parts = {"a", "é", "\ud83d\ude00", "\n", "\r", "\r\n", "\ud800"};
        for (int i = 0; i < 400; i++) {
            StringBuilder text = new StringBuilder();
            int length = random.nextInt(64);
            for (int j = 0; j < length; j++) text.append(parts[random.nextInt(parts.length)]);
            trial(text.toString());
        }
        System.out.println("M3_FILE_LINE_MAP_PASS checks=" + checks);
    }
}

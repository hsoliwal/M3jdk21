// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;
import jdk.m3.text.TextTools;
import jdk.m3.io.FileTools;
import jdk.m3.structured.JsonLinesTools;
import jdk.m3.structured.StructuredTextTools;
import jdk.m3.structured.StructuredCli;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class M3TextSmoke {
    private M3TextSmoke() { }
    private static final Limits LIMITS = Limits.standard();
    private static TextFrame text(String value) { return TextFrame.fromString(value); }
    private static void same(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) throws Exception {
        same(true, TextTools.nativeTextAvailable()); // fail if the linked image silently uses Java fallback
        same("a\nb\n", TextTools.sort(text("b\na"), false, LIMITS).text());
        same("a\nb\n", TextTools.uniq(text("a\na\nb\n"), LIMITS).text());
        same("A\né\n😀\n", TextTools.sort(text("😀\né\nA\n"), false, LIMITS).text());
        same("😀\n😀\né\nA\n", TextTools.sort(text("A\n😀\n😀\né\n"), true, LIMITS).text());
        same("😀\r\nx\n", TextTools.uniq(text("😀\r\n😀\nx\n"), LIMITS).text());
        StringBuilder nativeUniqueInput = new StringBuilder();
        for (int i = 0; i < 130; i++) nativeUnqiueInput.append(i / 2).append('\n');
        same(65, TextTools.uniq(text(nativeUnqiueInput.toString()), LIMITS).lines().size());
        same("a\n", TextTools.comm(text("a\nb\n"), text("a\nc\n"), LIMITS).common().text());
        same("a\tleft\tright\n", TextTools.join(text("a\tleft\n"), text("a\tright\n"), LIMITS).text());
        same("😀\n", TextTools.cut(text("a😀b\n"), 1, 2, LIMITS).text());
        same("a|b\n", TextTools.paste(List.of(text("a\n"), text("b\n")), "|", LIMITS).text());
        same("z😀\n", TextTools.tr(text("a😀\n"), "a", "z", LIMITS).text());
        List<List<String>> calls = new ArrayList<>();
        TextTools.xargs(text("a b\nc\n"), List.of("tool"), 1, calls::add, LIMITS);
        same(List.of(List.of("tool", "a b"), List.of("tool", "c")), calls);
        var change = TextTools.diff(text("a\n"), text("b"), LIMITS);
        same(true, change.text().contains("@@ -1,1 +1,1 @@"));
        same(text("b"), TextTools.patch(text("a\n"), change, LIMITS));
        same(2, TextTools.csplit(text("a\n#b\n"), Pattern.compile("^#"), LIMITS).size());
        same(2, TextTools.split(text("a\nb\nc\n"), 2, LIMITS).size());
        same("a\n😀\nb\n", TextTools.fold(text("a😀b\n"), 1, LIMITS).text());
        same("2\ta\n", TextTools.nl(text("a\n"), 2, 1, LIMITS).text());
        same(TextTools.shuf(text("a\nb\nc\n"), 7, LIMITS), TextTools.shuf(text("a\nb\nc\n"), 7, LIMITS));
        same("a\nb\nc\n", TextTools.tsort(text("b c\na b\n"), LIMITS).text());
        Path directory = Path.of(args[0]); Files.createDirectory(directory);
        Path file = directory.resolve("data.txt");
        var input = text("😀\r\na"); same(input, FileTools.tee(input, file, false, LIMITS));
        same(input.text(), Files.readString(file));
        same("data.txt\n", FileTools.find(directory, 2, Pattern.compile(".*\\.txt"), LIMITS).text());
        Path fifo = directory.resolve("fifo"); FileTools.mkfifo(fifo, 0600);
        same(0010000, (Integer) Files.getAttribute(fifo, "unix:mode") & 0170000);
// Structured-format atoms are built into a third opt-in JMOD, not java.base.
        same("a\n", StructuredTextTools.selectDelimited(text("\"a\",b\n"),
                ',', new int[]{0}, LIMITS).text());
        same("x,y", StructuredTextTools.delimited(text("\"x,y\",z\n"), ',', LIMITS)
                .get(0).get(0));
        same("b\ta\n", StructuredTextTools.selectDelimited(text("a\tb\n"),
                '\t', new int[]{1, 0}, LIMITS).text());
        same("v", StructuredTextTools.ini(text("[s]\nk=v\n"), LIMITS).get("s").get("k"));
        same("v", StructuredTextTools.dotenv(text("KEY=v\n"), LIMITS).get("KEY"));
        same("42\n", JsonLinesTools.select(text("{\"value\":42}\n"), "/value", LIMITS).text());
        var pipeOut = new ByteArrayOutputStream();
        var pipeErr = new ByteArrayOutputStream();
        same(0, StructuredCli.run(new String[]{"csv-cut", "1"},
                new ByteArrayInputStream("a,b\n".getBytes(StandardCharsets.UTF_8)),
                pipeOut, new PrintStream(pipeErr, true, StandardCharsets.UTF_8)));
        same("b\n", pipeOut.toString(StandardCharsets.UTF_8));
        same("", pipeErr.toString(StandardCharsets.UTF_8));
        System.out.println("M3_TEXT_IMAGE_PASS tools=24 java=" + Runtime.version());
    }
}

/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.LexiconTool;
import jdk.m3.text.SharedLexiconImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** Command-line contract of the lexicon image tool: usage refusals, build and map modes. */
public final class LexiconToolContractTest {
    private static final String USAGE = "build INPUT OUTPUT | map IMAGE HOLD_MILLIS";
    private static int checks;

    private LexiconToolContractTest() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private interface Action { void run() throws Exception; }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && (reason == null || reason.equals(thrown.getMessage()))) return;
            throw new AssertionError("expected " + type.getSimpleName() + " '" + reason + "'", thrown);
        }
        throw new AssertionError("accepted, expected refusal: " + reason);
    }

    private static String mapOutput(Path image) throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            LexiconTool.main(new String[] {"map", image.toString(), "0"});
        } finally {
            System.setOut(original);
        }
        return captured.toString(StandardCharsets.UTF_8).strip();
    }

    private static void delete(Path file) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                Files.deleteIfExists(file);
                return;
            } catch (IOException stillMappedOnWindows) {
                if (attempt == 100) throw stillMappedOnWindows;
                System.gc();
                Thread.sleep(20);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        refused(IllegalArgumentException.class, USAGE, () -> LexiconTool.main(new String[0]));
        refused(IllegalArgumentException.class, USAGE, () -> LexiconTool.main(new String[] {"build", "in"}));
        refused(IllegalArgumentException.class, USAGE, () -> LexiconTool.main(new String[] {"map", "img"}));
        refused(IllegalArgumentException.class, USAGE,
                () -> LexiconTool.main(new String[] {"verify", "in", "out"}));
        refused(IllegalArgumentException.class, USAGE,
                () -> LexiconTool.main(new String[] {"build", "in", "out", "extra"}));
        Path dir = Files.createTempDirectory("m3-lexicon-tool-");
        Path image = dir.resolve("english-demo.m3lex");
        try {
            refused(NoSuchFileException.class, null, () -> LexiconTool.main(
                    new String[] {"build", dir.resolve("absent.txt").toString(), image.toString()}));
            check(Files.notExists(image), "failed build publishes nothing");
            LexiconTool.main(new String[] {"build", "lexicon/english-demo.txt", image.toString()});
            SharedLexiconImage opened = SharedLexiconImage.open(image);
            String expected = "pid=" + ProcessHandle.current().pid() + " image=" + opened.imageIdentity()
                    + " records=" + opened.size() + " bytes=" + opened.mappedBytes();
            check(opened.size() > 0, "built image has records");
            check(expected.equals(mapOutput(image)), "map mode reports pid, identity, records and bytes");
            refused(NoSuchFileException.class, null,
                    () -> LexiconTool.main(new String[] {"map", dir.resolve("absent.m3lex").toString(), "0"}));
        } finally {
            delete(image);
            delete(dir);
        }
        System.out.println("M3_LEXICON_TOOL_CONTRACT_PASS checks=" + checks);
    }
}

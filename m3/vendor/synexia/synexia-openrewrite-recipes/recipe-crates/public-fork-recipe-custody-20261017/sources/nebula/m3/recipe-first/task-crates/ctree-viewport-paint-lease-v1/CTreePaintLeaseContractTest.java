// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.m3.proof;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Fail-closed source and call-order proof of the original EPL-2.0 CTree.
 * Source identity is pinned; no SWT source is copied or relicensed here.
 * Real SWT GUI/JNI/platform differential tests remain a separate gate.
 */
public final class CTreePaintLeaseContractTest {
    public static final String POST_SHA256 =
            "19c1196540552a095b648a52706c87dea66f465953fa9e24f45f1eb950486e6b";
    private static int checks;

    private CTreePaintLeaseContractTest() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("expected CTree source path");
        String source = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        require(sha256(source).equals(POST_SHA256), "source hash drift: review widget contract");
        verifyStructure(source);
        requireReject(source.replace("gc2.dispose();", "gc2.getClass();"),
                "missing scratch GC cleanup");
        requireReject(source.replace("if (paintedItems.isEmpty()) return;", ""),
                "missing viewport-empty allocation guard");
        requireReject(source.replace("image.dispose();", "image.getBounds();"),
                "missing image cleanup");
        System.out.println("PASS CTreePaintLeaseContractTest: " + checks
                + " source-shape and mutation-refusal checks");
    }

    private static void verifyStructure(String whole) {
        String body = section(whole, "protected void paintBody(Event e) {",
                "protected void paintColumns(").replaceAll("\\s+", " ");
        String cells = section(whole, "protected void paintItems(GC gc, Rectangle ebounds) {",
                "protected void paintSelectionIndicators(").replaceAll("\\s+", " ");
        require(body.contains("Image image = new Image(e.display, ebounds); try {")
                && body.contains("GC gc = new GC(image); try {"), "nested paint leases");
        require(body.contains("} finally { gc.dispose(); } } finally { image.dispose(); }"),
                "outer image and paint GC released on failure");
        int cursor = 0;
        for (String operation : new String[]{
                "paintBackground(gc, ebounds);",
                "paintItemBackgrounds(gc, ebounds);",
                "paintSelectionIndicators(gc, ebounds);",
                "paintColumns(gc, ebounds);",
                "paintItems(gc, ebounds);",
                "paintViewport(gc, ebounds);",
                "paintFocus(gc, ebounds);",
                "e.gc.drawImage(image, ebounds.x, ebounds.y);"
        }) {
            cursor = body.indexOf(operation, cursor);
            require(cursor >= 0, "painter order: " + operation);
            cursor += operation.length();
        }
        int empty = cells.indexOf("if (paintedItems.isEmpty()) return;");
        int image = cells.indexOf("Image image = new Image(gc.getDevice(), ebounds);");
        require(empty >= 0 && image > empty, "no scratch image for empty paint window");
        require(cells.contains("GC gc2 = new GC(image); try {"),
                "scratch GC scope");
        require(cells.contains("} finally { gc2.dispose(); } } finally { image.dispose(); }"),
                "scratch image and GC released even if cell callback throws");
        require(cells.contains("cell.paint(gc, ebounds);")
                && cells.contains("cell.paintClientArea(gc2,"),
                "same existing cell callback authority");
    }

    private static void requireReject(String damaged, String context) {
        boolean rejected = false;
        try { verifyStructure(damaged); }
        catch (IllegalStateException expected) { rejected = true; }
        require(rejected, "unrejected mutation: " + context);
    }

    private static String section(String source, String start, String end) {
        int left = source.indexOf(start);
        int right = source.indexOf(end, left);
        if (left < 0 || right <= left)
            throw new IllegalStateException("source method boundary drift");
        return source.substring(left, right);
    }

    private static String sha256(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new IllegalStateException(message);
    }
}

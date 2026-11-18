/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/** JDK-only headless preview with actual Pageable/Printable controls. */
public final class M3PrintPreviewSmoke {
    private M3PrintPreviewSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3PrintPreviewSmoke::exercise);
        System.out.println("M3_NATIVE_PRINT_PREVIEW_PASS");
    }

    private static void exercise() {
        M3PrintPreview preview = new M3PrintPreview();
        check(preview.getPageCount() == 0 && !preview.isPageCached(), "no print job");
        AtomicInteger prints = new AtomicInteger();
        Book book = new Book();
        PageFormat page = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(200, 300);
        paper.setImageableArea(10, 10, 180, 280);
        page.setPaper(paper);
        Printable writer = (g, pf, index) -> {
            prints.incrementAndGet();
            g.setColor(index == 0 ? Color.RED : Color.BLUE);
            g.fillRect(10, 10, 100, 100);
            return Printable.PAGE_EXISTS;
        };
        book.append(writer, page, 2);
        preview.setPageable(book);
        check(preview.getPageCount() == 2 && preview.getPageIndex() == 0, "known page count");
        check(preview.getPreferredSize().width == 224 && preview.getPreferredSize().height == 324, "zoom geometry");
        preview.setSize(224, 324);
        BufferedImage pageZero = paint(preview);
        check((pageZero.getRGB(62, 62) & 0x00ffffff) == (Color.RED.getRGB() & 0x00ffffff), "page 0 renders red");
        check(prints.get() == 1 && preview.isPageCached(), "first page rendered only once");
        paint(preview);
        check(prints.get() == 1, "second paint hits same page cache");
        preview.setPageIndex(1);
        check(!preview.isPageCached(), "page change invalidates cache");
        BufferedImage pageOne = paint(preview);
        check((pageOne.getRGB(62, 62) & 0x00ffffff) == (Color.BLUE.getRGB() & 0x00ffffff), "page 1 renders blue");
        check(prints.get() == 2, "second page rendered on demand");
        preview.fitToWidth(124);
        check(Math.abs(preview.getZoom() - 0.5) < 0.00001, "fit width respects gutter");
        preview.setSize(preview.getPreferredSize());
        paint(preview);
        check(prints.get() == 3, "zoom invalidates cache");
        preview.invalidatePreview();
        paint(preview);
        check(prints.get() == 4, "explicit invalidation redraws print source");
        reject(() -> preview.setPageIndex(2), IndexOutOfBoundsException.class);
        reject(() -> preview.setZoom(0), IllegalArgumentException.class);
        reject(() -> preview.setZoom(Double.NaN), IllegalArgumentException.class);
        preview.setPageable(null);
        check(preview.getPageIndex() == 0 && preview.getPageCount() == 0 && !preview.isPageCached(), "reset");
    }
    private static BufferedImage paint(M3PrintPreview preview) {
        BufferedImage image = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try { preview.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }
    private static void reject(Runnable action, Class<? extends Throwable> expected) {
        try { action.run(); } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw failure;
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}

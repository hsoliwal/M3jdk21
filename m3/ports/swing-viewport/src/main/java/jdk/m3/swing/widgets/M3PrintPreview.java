/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Native Swing counterpart of Nebula PaperClips' PrintPreview control.
 * It accepts Java's original Pageable/Printable contract, not SWT PrintJob,
 * and renders a single requested page rather than materializing all pages.
 * A bounded 4M-pixel cache is optional and is invalidated on page/job/zoom edits.
 * EDT-only, component-owned, with no native peer or custom printing engine.
 */
public final class M3PrintPreview extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int GUTTER = 12;
    private static final int MAX_CACHE_PIXELS = 4_000_000;
    private Pageable pageable;
    private int pageIndex;
    private double zoom = 1;
    private transient BufferedImage cached;
    private int cachedPage = -1;
    private PrinterException lastFailure;
    private int printCalls;

    public M3PrintPreview() {
        this(null);
    }

    public M3PrintPreview(Pageable pageable) {
        setOpaque(true);
        Color bg = UIManager.getColor("Panel.background");
        setBackground(bg == null ? Color.LIGHT_GRAY : bg);
        setFocusable(true);
        this.pageable = pageable;
        getAccessibleContext().setAccessibleName("Print preview");
    }

    public Pageable getPageable() { return pageable; }
    public int getPageIndex() { return pageIndex; }
    public int getPageCount() { return pageable == null ? 0 : pageable.getNumberOfPages(); }
    public double getZoom() { return zoom; }
    public PrinterException getLastFailure() { return lastFailure; }
    public int getPrintCallCount() { return printCalls; }
    public boolean isPageCached() { return cached != null && cachedPage == pageIndex; }

    public void setPageable(Pageable value) {
        requireEdt();
        pageable = value;
        pageIndex = 0;
        invalidatePreview();
        revalidate();
        repaint();
    }

    /** A page index in a known-length book must exist. Unknown-length books allow nonnegative indices. */
    public void setPageIndex(int index) {
        requireEdt();
        checkIndex(index);
        if (pageIndex != index) {
            pageIndex = index;
            invalidatePreview();
            revalidate();
            repaint();
        }
    }

    public void setZoom(double zoom) {
        requireEdt();
        if (!Double.isFinite(zoom) || zoom < 0.1 || zoom > 4) {
            throw new IllegalArgumentException("zoom must be finite and in [0.1,4]");
        }
        if (this.zoom != zoom) {
            this.zoom = zoom;
            invalidatePreview();
            revalidate();
            repaint();
        }
    }

    /** Fit the current page's paper width to a viewport, without editing the print job. */
    public void fitToWidth(int viewportWidth) {
        requireEdt();
        if (viewportWidth <= 2 * GUTTER) throw new IllegalArgumentException("viewport width too small");
        PageFormat page = currentFormat();
        if (page == null) return;
        setZoom(Math.max(0.1, Math.min(4.0, (viewportWidth - 2.0 * GUTTER) / page.getWidth())));
    }

    /** Call when the underlying Printable's mutable contents change. */
    public void invalidatePreview() {
        requireEdt();
        cached = null;
        cachedPage = -1;
        lastFailure = null;
        repaint();
    }

    @Override public Dimension getPreferredSize() {
        PageFormat page = currentFormat();
        if (page == null) return new Dimension(320, 450);
        return new Dimension(pixelExtent(page.getWidth()), pixelExtent(page.getHeight()));
    }

    private int pixelExtent(double points) {
        if (!Double.isFinite(points) || points <= 0) return 2 * GUTTER + 1;
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(points * zoom + GUTTER * 2.0));
    }

    private PageFormat currentFormat() {
        if (pageable == null) return null;
        int count = pageable.getNumberOfPages();
        if (count == 0 || (count != Pageable.UNKNOWN_NUMBER_OF_PAGES && pageIndex >= count)) return null;
        return Objects.requireNonNull(pageable.getPageFormat(pageIndex), "page format");
    }

    private void checkIndex(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("page index");
        int count = getPageCount();
        if (count == 0 || (count != Pageable.UNKNOWN_NUMBER_OF_PAGES && index >= count)) {
            throw new IndexOutOfBoundsException("page index");
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            PageFormat format = currentFormat();
            if (format == null) return;
            int paperWidth = (int) Math.min(Integer.MAX_VALUE - 2L, Math.max(1, Math.ceil(format.getWidth() * zoom)));
            int paperHeight = (int) Math.min(Integer.MAX_VALUE - 2L, Math.max(1, Math.ceil(format.getHeight() * zoom)));
            g.setColor(Color.DARK_GRAY);
            g.drawRect(GUTTER - 1, GUTTER - 1, paperWidth + 1, paperHeight + 1);
            if (cached == null || cachedPage != pageIndex) {
                renderCache(format, paperWidth, paperHeight);
            }
            if (cached != null) {
                g.drawImage(cached, GUTTER, GUTTER, null);
            } else if (lastFailure == null) {
                Graphics2D page = (Graphics2D) g.create();
                try {
                    page.translate(GUTTER, GUTTER);
                    drawPage(page, format, paperWidth, paperHeight);
                } finally {
                    page.dispose();
                }
            }
            if (lastFailure != null) {
                g.setColor(Color.RED);
                g.drawString("Preview failed: " + lastFailure.getClass().getSimpleName(), GUTTER + 8, GUTTER + 24);
            }
        } finally {
            g.dispose();
        }
    }

    private void renderCache(PageFormat format, int width, int height) {
        if (width <= 0 || height <= 0 || width > 4096 || height > 4096
                || (long) width * height > MAX_CACHE_PIXELS) return;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            drawPage(g, format, width, height);
            if (lastFailure == null) {
                cached = image;
                cachedPage = pageIndex;
            }
        } finally {
            g.dispose();
        }
    }

    private void drawPage(Graphics2D graphics, PageFormat format, int width, int height) {
        Graphics2D page = (Graphics2D) graphics.create();
        try {
            page.setColor(Color.WHITE);
            page.fillRect(0, 0, width, height);
            page.setClip(0, 0, width, height);
            page.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            page.scale(zoom, zoom);
            printCalls++;
            int answer = pageable.getPrintable(pageIndex).print(page, format, pageIndex);
            if (answer == Printable.NO_SUCH_PAGE) {
                // An UNKNOWN_NUMBER_OF_PAGES book may end before the requested index.
                lastFailure = new PrinterException("No such page: " + pageIndex);
            }
        } catch (PrinterException exception) {
            lastFailure = exception;
        } finally {
            page.dispose();
        }
    }

    @Override public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {
            private static final long serialVersionUID = 1L;
        };
        return accessibleContext;
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}

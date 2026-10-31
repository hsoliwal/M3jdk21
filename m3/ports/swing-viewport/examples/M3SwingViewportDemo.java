/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.plaf.LayerUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * M3JDK Swing viewport examples: a JTree of 1,001,001 nodes with every child of the root expanded
 * (variable row height, or fixed row height with {@code largeModel}), and a JList of 10^6 rows, each
 * with a frame-time overlay that shows whether {@code -Dm3.swing.viewport} is on.
 * <p>
 * The property is read once when the {@code java.desktop} classes load, so a JVM cannot switch it.
 * The window's "Relaunch" button starts the same demo in a new JVM with the property flipped (same
 * {@code --patch-module} and other JVM options); {@code run_examples.sh} runs every demo headless with
 * the property off and on and writes the screenshots and timings.
 * <p>
 * Usage: {@code java [--patch-module java.desktop=<patch>] [-Dm3.swing.viewport=true]
 * M3SwingViewportDemo tree-variable|tree-fixed|list [--capture <dir>]}. With {@code --capture} it
 * builds the view, scrolls 60 frames offscreen from the middle, writes {@code <mode>-<on|off>.png}
 * with the overlay and appends a row to {@code examples.tsv} in {@code <dir>}; it works headless.
 */
public final class M3SwingViewportDemo {

    private static final System.Logger LOG = System.getLogger("m3.swing.viewport.demo");
    private static final boolean M3 = Boolean.getBoolean("m3.swing.viewport");
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    /** The view and what the overlay reports about it. */
    record Built(JComponent view, String what, long items, long setupMs, int rowStep) {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "tree-variable";
        Path capture = args.length > 2 && "--capture".equals(args[1]) ? Path.of(args[2]) : null;
        if (capture == null && GraphicsEnvironment.isHeadless()) {
            LOG.log(System.Logger.Level.ERROR, "headless: pass --capture <dir>");
            return;
        }
        Built[] built = new Built[1];
        SwingUtilities.invokeAndWait(() -> built[0] = build(mode));
        if (capture != null) {
            SwingUtilities.invokeAndWait(() -> capture(mode, built[0], capture));
        } else {
            SwingUtilities.invokeLater(() -> show(mode, built[0], args));
        }
    }

    // ---- the three views ------------------------------------------------------------------------

    static Built build(String mode) {
        long t0 = System.nanoTime();
        return switch (mode) {
            case "tree-variable" -> tree(false, t0);
            case "tree-fixed" -> tree(true, t0);
            case "list" -> list(t0);
            default -> throw new IllegalArgumentException("mode: tree-variable, tree-fixed or list");
        };
    }

    /** Root, 1,000 children, 1,000 leaves each; every 97th node a two-line HTML label. */
    static Built tree(boolean fixed, long t0) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        int id = 0;
        for (int i = 0; i < 1000; i++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(label(++id, "group " + i));
            root.add(child);
            for (int j = 0; j < 1000; j++) {
                child.add(new DefaultMutableTreeNode(label(++id, "item " + i + "." + j)));
            }
        }
        JTree tree = new JTree(new DefaultTreeModel(root));
        if (fixed) {
            tree.setRowHeight(18);
            tree.setLargeModel(true);
        } else {
            tree.setRowHeight(0);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            tree.expandPath(new TreePath(new Object[] {root, root.getChildAt(i)}));
        }
        tree.getPreferredSize();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return new Built(tree, fixed ? "JTree fixed height, largeModel" : "JTree variable height", id + 1L, ms,
                fixed ? 18 : 17);
    }

    /** 10^6 rows; every 50th a two-line HTML row, so row heights vary. */
    static Built list(long t0) {
        DefaultListModel<String> model = new DefaultListModel<>();
        List<String> rows = new ArrayList<>(1_000_000);
        for (int i = 0; i < 1_000_000; i++) {
            rows.add(i % 50 == 0 ? "<html><b>row " + i + "</b><br>second line</html>" : "row " + i);
        }
        model.addAll(rows);
        JList<String> list = new JList<>(model);
        list.getPreferredSize();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return new Built(list, "JList variable height", 1_000_000L, ms, 17);
    }

    static String label(int id, String text) {
        return id % 97 == 0 ? "<html><b>" + text + "</b><br>two-line HTML label</html>" : text;
    }

    // ---- the frame-time overlay -----------------------------------------------------------------

    /** The last 120 frame times, in nanoseconds. */
    static final class Frames {
        private final long[] ns = new long[120];
        private int count;

        void add(long d) {
            ns[count++ % ns.length] = d;
        }

        String summary() {
            int n = Math.min(count, ns.length);
            if (n == 0) {
                return "frame -";
            }
            long[] s = Arrays.copyOf(ns, n);
            Arrays.sort(s);
            return String.format("frame p50 %.2f ms  p95 %.2f ms  (%d frames)", s[n / 2] / 1e6,
                    s[Math.min(n - 1, (int) (n * 0.95))] / 1e6, count);
        }
    }

    static void paintOverlay(Graphics g, int width, Built b, Frames frames) {
        Graphics2D g2 = (Graphics2D) g.create();
        String[] lines = {
            "-Dm3.swing.viewport=" + M3 + (M3 ? "  (M3 paths)" : "  (stock paths)"),
            b.what() + ": " + String.format("%,d", b.items()) + (b.view() instanceof JTree ? " nodes" : " rows"),
            "setup " + String.format("%,d", b.setupMs()) + " ms (build, expand, first layout)",
            frames.summary(),
        };
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int w = 0;
        for (String s : lines) {
            w = Math.max(w, fm.stringWidth(s));
        }
        int h = lines.length * fm.getHeight() + 10;
        int x = width - w - 22;
        g2.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g2.setColor(M3 ? new Color(0x14532d) : new Color(0x7c2d12));
        g2.fillRoundRect(x, 8, w + 14, h, 10, 10);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(Color.WHITE);
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], x + 7, 8 + 5 + fm.getAscent() + i * fm.getHeight());
        }
        g2.dispose();
    }

    // ---- headless capture -----------------------------------------------------------------------

    static void capture(String mode, Built b, Path dir) {
        JScrollPane sp = new JScrollPane(b.view());
        sp.setSize(WIDTH, HEIGHT);
        sp.doLayout();
        JViewport vp = sp.getViewport();
        vp.doLayout();
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Frames frames = new Frames();
        int top = Math.max(0, b.view().getPreferredSize().height / 2);
        for (int f = 0; f < 70; f++) {
            Graphics2D g = img.createGraphics();
            long t0 = System.nanoTime();
            vp.setViewPosition(new Point(0, top + f * 3 * b.rowStep()));
            sp.paint(g);
            long d = System.nanoTime() - t0;
            g.dispose();
            if (f >= 10) {
                frames.add(d);
            }
        }
        Graphics2D g = img.createGraphics();
        paintOverlay(g, WIDTH, b, frames);
        g.dispose();
        String name = mode + "-" + (M3 ? "on" : "off");
        try {
            Files.createDirectories(dir);
            ImageIO.write(img, "png", dir.resolve(name + ".png").toFile());
            String row = String.join("\t", mode, String.valueOf(M3), String.valueOf(b.items()),
                    String.valueOf(b.setupMs()), frames.summary(), name + ".png") + "\n";
            Files.writeString(dir.resolve("examples.tsv"), row, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "capture failed: " + dir, e);
            return;
        }
        LOG.log(System.Logger.Level.INFO, () -> name + ": setup " + b.setupMs() + " ms, " + frames.summary());
    }

    // ---- interactive window ---------------------------------------------------------------------

    static void show(String mode, Built b, String[] args) {
        Frames frames = new Frames();
        JScrollPane sp = new JScrollPane(b.view());
        LayerUI<JComponent> hud = new LayerUI<>() {
            @Override
            public void paint(Graphics g, JComponent c) {
                long t0 = System.nanoTime();
                super.paint(g, c);
                frames.add(System.nanoTime() - t0);
                paintOverlay(g, c.getWidth(), b, frames);
            }
        };
        JLayer<JComponent> layer = new JLayer<>(sp, hud);
        JViewport vp = sp.getViewport();
        Timer scroll = new Timer(16, e -> {
            Point p = vp.getViewPosition();
            int max = b.view().getHeight() - vp.getHeight();
            vp.setViewPosition(new Point(0, p.y + 7 * b.rowStep() > max ? 0 : p.y + 7 * b.rowStep()));
        });
        JButton auto = new JButton("Auto-scroll");
        auto.addActionListener(e -> {
            if (scroll.isRunning()) {
                scroll.stop();
            } else {
                scroll.start();
            }
        });
        JButton middle = new JButton("Jump to the middle");
        middle.addActionListener(e -> vp.setViewPosition(new Point(0, b.view().getHeight() / 2)));
        JButton relaunch = new JButton("Relaunch with -Dm3.swing.viewport=" + !M3);
        JFrame frame = new JFrame("M3JDK Swing viewport: " + b.what() + (M3 ? " (M3)" : " (stock)"));
        relaunch.addActionListener(e -> {
            if (relaunch(args)) {
                frame.dispose();
            }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(auto);
        buttons.add(middle);
        buttons.add(relaunch);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(layer, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);
        frame.setSize(new Dimension(WIDTH + 40, HEIGHT + 80));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    /** Starts this demo in a new JVM with the same options and the property flipped. */
    static boolean relaunch(String[] args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ProcessHandle.current().info().command().orElse("java"));
        for (String a : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!a.startsWith("-Dm3.swing.viewport=")) {
                cmd.add(a);
            }
        }
        cmd.add("-Dm3.swing.viewport=" + !M3);
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(M3SwingViewportDemo.class.getName());
        cmd.addAll(Arrays.asList(args));
        try {
            new ProcessBuilder(cmd).inheritIO().start();
            return true;
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "relaunch failed: " + cmd, e);
            return false;
        }
    }

    private M3SwingViewportDemo() {
    }
}

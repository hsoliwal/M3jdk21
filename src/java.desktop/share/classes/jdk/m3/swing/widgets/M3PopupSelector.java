/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * A native Swing text/arrow selector with a lazily created table or tree popup.
 * It does not own or materialize any row: the caller's existing Swing model,
 * renderer and view continue to own row data and selection.
 *
 * <p>Derived from the observable widget requirements of Nebula TableCombo and
 * CTreeCombo, not from their EPL-2.0 source. This first-party Java implementation
 * uses Swing's stock LAF, accessibility and event dispatch.</p>
 *
 * @param <V> existing Swing popup view, usually JTable or JTree
 */
public abstract class M3PopupSelector<V extends JComponent> extends JPanel {
    private static final long serialVersionUID = 1L;
    private final V view;
    private final JTextField text = new JTextField();
    private final JButton arrow = new JButton("\u25BE");
    private final EventListenerList listeners = new EventListenerList();
    private JPopupMenu popup;
    private int visibleRowCount = 7;
    private boolean closeOnSelection = true;

    protected M3PopupSelector(V view) {
        super(new BorderLayout(0, 0));
        this.view = Objects.requireNonNull(view, "view");
        text.setEditable(false);
        text.getAccessibleContext().setAccessibleName("Selected item");
        arrow.setFocusable(false);
        arrow.getAccessibleContext().setAccessibleName("Show choices");
        arrow.addActionListener(event -> setPopupVisible(!isPopupVisible()));
        add(text, BorderLayout.CENTER);
        add(arrow, BorderLayout.LINE_END);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "m3.open");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "m3.open");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "m3.close");
        getActionMap().put("m3.open", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { setPopupVisible(!isPopupVisible()); }
        });
        getActionMap().put("m3.close", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent e) { setPopupVisible(false); }
        });
    }

    protected final V view() { return view; }
    public final JTextField getTextField() { return text; }
    public final String getText() { return text.getText(); }
    public final void setEditable(boolean editable) { text.setEditable(editable); }
    public final boolean isEditable() { return text.isEditable(); }
    protected final void display(String value) { text.setText(value == null ? "" : value); }

    public final void addActionListener(ActionListener listener) {
        listeners.add(ActionListener.class, Objects.requireNonNull(listener, "listener"));
    }
    public final void removeActionListener(ActionListener listener) {
        listeners.remove(ActionListener.class, listener);
    }
    protected final void fireSelection() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selection");
        for (ActionListener listener : listeners.getListeners(ActionListener.class)) {
            listener.actionPerformed(event);
        }
        if (closeOnSelection) setPopupVisible(false);
    }

    public final int getVisibleRowCount() { return visibleRowCount; }
    public final void setVisibleRowCount(int count) {
        if (count <= 0) throw new IllegalArgumentException("visibleRowCount must be positive");
        visibleRowCount = count;
        if (popup != null) {
            if (popup.isVisible()) {
                updatePopupPreferredSize();
            }
            popup.pack();
        }
    }
    public final boolean isCloseOnSelection() { return closeOnSelection; }
    public final void setCloseOnSelection(boolean close) { closeOnSelection = close; }
    public final boolean isPopupCreated() { return popup != null; }
    public final boolean isPopupVisible() { return popup != null && popup.isVisible(); }

    /** Popup creation is deferred to first display; hidden widgets create no popup. */
    public final void setPopupVisible(boolean visible) {
        requireEdt();
        if (!visible) {
            if (popup != null) popup.setVisible(false);
            return;
        }
        if (!isEnabled()) return;
        if (!isShowing()) throw new IllegalComponentStateException("selector must be showing");
        if (popup == null) {
            popup = new JPopupMenu();
            popup.setLayout(new BorderLayout());
            popup.add(new JScrollPane(view), BorderLayout.CENTER);
        }
        updatePopupPreferredSize();
        popup.show(this, 0, getHeight());
        view.requestFocusInWindow();
    }

    /**
     * Size the original Swing viewport from its last visible native row and
     * actual table-header preferred height. Never materialize backing rows.
     * The same calculation is used when a visible selector changes its limit.
     */
    private void updatePopupPreferredSize() {
        int visibleRows = Math.max(1, Math.min(visibleRowCount, Math.max(1, rowCount())));
        long pixelHeight = (long) visibleRows * Math.max(16, rowHeight());
        if (view instanceof javax.swing.JTree tree) {
            Rectangle last = tree.getRowBounds(visibleRows - 1);
            if (last != null) pixelHeight = Math.max(pixelHeight, (long) last.y + last.height);
        } else if (view instanceof javax.swing.JTable table) {
            Rectangle last = table.getCellRect(visibleRows - 1, 0, true);
            if (last != null) pixelHeight = Math.max(pixelHeight, (long) last.y + last.height);
        }
        int header = view instanceof javax.swing.JTable ? 28 : 0;
        if (view instanceof javax.swing.JTable table && table.getTableHeader() != null) {
            Dimension size = table.getTableHeader().getPreferredSize();
            if (size != null) {
                header = Math.max(header, Math.max(0, size.height));
            }
        }
        JScrollPane scroll = (JScrollPane) popup.getComponent(0);
        Insets chrome = scroll.getInsets();
        Insets viewportChrome = scroll.getViewportBorder() == null
                ? new Insets(0, 0, 0, 0)
                : scroll.getViewportBorder().getBorderInsets(scroll);
        int width = Math.max(getWidth(), 240);
        long availableWidth = (long) width - Math.max(0, chrome.left)
                - Math.max(0, chrome.right) - Math.max(0, viewportChrome.left)
                - Math.max(0, viewportChrome.right);
        boolean vertical = scroll.getVerticalScrollBarPolicy()
                == JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                || scroll.getVerticalScrollBarPolicy() != JScrollPane.VERTICAL_SCROLLBAR_NEVER
                && rowCount() > visibleRows;
        if (vertical) {
            availableWidth -= Math.max(0,
                    scroll.getVerticalScrollBar().getPreferredSize().width);
        }
        boolean tracksWidth = view instanceof javax.swing.Scrollable scrollable
                && scrollable.getScrollableTracksViewportWidth();
        int horizontalPolicy = scroll.getHorizontalScrollBarPolicy();
        boolean horizontal = horizontalPolicy == JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS;
        if (!horizontal && horizontalPolicy != JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                && !tracksWidth) {
            int viewWidth = view.getPreferredSize().width;
            horizontal = viewWidth > Math.max(0L, availableWidth);
        }
        int horizontalHeight = horizontal ? Math.max(0,
                scroll.getHorizontalScrollBar().getPreferredSize().height) : 0;
        long preferredHeight = pixelHeight + header + Math.max(0, chrome.top)
                + Math.max(0, chrome.bottom) + Math.max(0, viewportChrome.top)
                + Math.max(0, viewportChrome.bottom) + horizontalHeight;
        int height = (int) Math.min(Integer.MAX_VALUE, preferredHeight);
        scroll.setPreferredSize(new Dimension(width, height));
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        // May be called by a superclass constructor before child fields initialize.
        if (text != null) text.setEnabled(enabled);
        if (arrow != null) arrow.setEnabled(enabled);
        if (!enabled && popup != null) popup.setVisible(false);
    }

    @Override public void removeNotify() {
        if (popup != null) popup.setVisible(false);
        super.removeNotify();
    }

    protected abstract int rowCount();
    protected abstract int rowHeight();

    protected static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing EDT required");
        }
    }
}

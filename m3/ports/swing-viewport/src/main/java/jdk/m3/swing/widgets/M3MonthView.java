/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Native Swing 6x7 month calendar with one JTable, no day widgets or per-cell
 * state. Date ordinals are derived from one epoch-day offset. Month navigation
 * changes the view, and a user commit fires an ActionEvent; programmatic date
 * selection changes a bound property, but does not synthesize user action.
 *
 * <p>This is an independent Apache-2.0 Swing widget with no SwingX or SWT
 * dependency. SwingX JXMonthView can remain an optional higher-level donor.</p>
 */
public final class M3MonthView extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final String DATE_COMMITTED = "dateCommitted";
    private final MonthModel model = new MonthModel();
    private final JTable table;
    private final JButton previous = new JButton("‹");
    private final JButton next = new JButton("›");
    private final JLabel heading = new JLabel("", SwingConstants.CENTER);
    private Locale calendarLocale;
    private YearMonth month;
    private LocalDate selected;
    private LocalDate minimum;
    private LocalDate maximum;
    private long gridStart;

    public M3MonthView() { this(Locale.getDefault()); }

    public M3MonthView(Locale locale) {
        super(new BorderLayout(0, 2));
        calendarLocale = Objects.requireNonNull(locale, "locale");
        selected = LocalDate.now();
        month = YearMonth.from(selected);
        table = new JTable(model);
        JPanel toolbar = new JPanel(new BorderLayout());
        previous.getAccessibleContext().setAccessibleName("Previous month");
        next.getAccessibleContext().setAccessibleName("Next month");
        previous.addActionListener(e -> shiftMonth(-1));
        next.addActionListener(e -> shiftMonth(1));
        toolbar.add(previous, BorderLayout.WEST);
        toolbar.add(heading, BorderLayout.CENTER);
        toolbar.add(next, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setRowHeight(Math.max(22, table.getRowHeight()));
        table.setDefaultEditor(Object.class, null);
        table.setDefaultRenderer(Object.class, new DayRenderer());
        table.getAccessibleContext().setAccessibleName("Calendar days");
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    commitCell(table.rowAtPoint(event.getPoint()), table.columnAtPoint(event.getPoint()));
                }
            }
        });
        table.getInputMap(WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "m3.commitDay");
        table.getActionMap().put("m3.commitDay", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(ActionEvent event) {
                commitCell(table.getSelectedRow(), table.getSelectedColumn());
            }
        });
        add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName("Month calendar");
        updateMonth();
    }

    public JTable getTable() { return table; }
    public YearMonth getMonth() { return month; }
    public LocalDate getSelectedDate() { return selected; }
    public Locale getCalendarLocale() { return calendarLocale; }
    public LocalDate getMinimum() { return minimum; }
    public LocalDate getMaximum() { return maximum; }

    public void setCalendarLocale(Locale locale) {
        requireEdt();
        Locale value = Objects.requireNonNull(locale, "locale");
        if (!calendarLocale.equals(value)) {
            calendarLocale = value;
            updateMonth();
        }
    }

    public void setMonth(YearMonth target) {
        requireEdt();
        YearMonth value = Objects.requireNonNull(target, "month");
        if (!month.equals(value)) {
            month = value;
            updateMonth();
        }
    }

    public void shiftMonth(int delta) {
        requireEdt();
        final YearMonth target;
        try { target = month.plusMonths(delta); }
        catch (DateTimeException outOfRange) { return; }
        if (minimum != null && target.isBefore(YearMonth.from(minimum))) return;
        if (maximum != null && target.isAfter(YearMonth.from(maximum))) return;
        setMonth(target);
    }

    public void setSelectedDate(LocalDate date) {
        requireEdt();
        Objects.requireNonNull(date, "date");
        if (!isSelectable(date)) throw new IllegalArgumentException("date outside bounds");
        LocalDate old = selected;
        selected = date;
        if (!month.equals(YearMonth.from(date))) {
            month = YearMonth.from(date);
            updateMonth();
        } else {
            model.fireTableDataChanged();
        }
        firePropertyChange("selectedDate", old, selected);
    }

    /** Null bounds are unbounded; a displaced selection is cleared, not clamped. */
    public void setDateRange(LocalDate min, LocalDate max) {
        requireEdt();
        if (min != null && max != null && min.isAfter(max)) throw new IllegalArgumentException("reversed date range");
        minimum = min;
        maximum = max;
        if (selected != null && !isSelectable(selected)) {
            LocalDate old = selected;
            selected = null;
            firePropertyChange("selectedDate", old, null);
        }
        updateMonth();
    }

    public boolean isSelectable(LocalDate date) {
        return date != null && (minimum == null || !date.isBefore(minimum))
                && (maximum == null || !date.isAfter(maximum));
    }

    /** Returns the date occupying this view cell; no per-cell model allocation. */
    public LocalDate dateAt(int row, int column) {
        if (row < 0 || row >= 6 || column < 0 || column >= 7) throw new IndexOutOfBoundsException("calendar cell");
        return LocalDate.ofEpochDay(gridStart + row * 7L + column);
    }

    /** Only user activation fires an ActionEvent; hidden/disabled dates never commit. */
    public void commitCell(int row, int column) {
        requireEdt();
        if (row < 0 || row >= 6 || column < 0 || column >= 7 || !isEnabled()) return;
        LocalDate date = dateAt(row, column);
        if (!isSelectable(date)) return;
        setSelectedDate(date);
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, DATE_COMMITTED);
        for (ActionListener listener : listenerList.getListeners(ActionListener.class)) listener.actionPerformed(event);
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, Objects.requireNonNull(listener, "listener"));
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (table != null) table.setEnabled(enabled);
        if (previous != null) previous.setEnabled(enabled);
        if (next != null) next.setEnabled(enabled);
        if (month != null) updateNavigation();
    }

    private void updateMonth() {
        DayOfWeek first = WeekFields.of(calendarLocale).getFirstDayOfWeek();
        LocalDate firstDate = month.atDay(1);
        int before = (firstDate.getDayOfWeek().getValue() - first.getValue() + 7) % 7;
        gridStart = firstDate.toEpochDay() - before;
        heading.setText(DateTimeFormatter.ofPattern("LLLL uuuu", calendarLocale).format(firstDate));
        model.fireTableStructureChanged();
        updateNavigation();
        repaint();
    }

    private void updateNavigation() {
        previous.setEnabled(isEnabled() && !month.equals(YearMonth.of(-999_999_999, 1))
                && (minimum == null || month.isAfter(YearMonth.from(minimum))));
        next.setEnabled(isEnabled() && !month.equals(YearMonth.of(999_999_999, 12))
                && (maximum == null || month.isBefore(YearMonth.from(maximum))));
    }

    private final class MonthModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        @Override public int getRowCount() { return 6; }
        @Override public int getColumnCount() { return 7; }
        @Override public Class<?> getColumnClass(int column) { return Object.class; }
        @Override public String getColumnName(int column) {
            int start = WeekFields.of(calendarLocale).getFirstDayOfWeek().getValue();
            DayOfWeek day = DayOfWeek.of((start - 1 + column) % 7 + 1);
            return day.getDisplayName(TextStyle.SHORT, calendarLocale);
        }
        @Override public Object getValueAt(int row, int column) { return dateAt(row, column).getDayOfMonth(); }
    }

    private final class DayRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable owner, Object value, boolean selectedCell,
                boolean focused, int row, int column) {
            super.getTableCellRendererComponent(owner, value, selectedCell, focused, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            LocalDate date = dateAt(row, column);
            boolean allowed = isSelectable(date);
            boolean currentMonth = YearMonth.from(date).equals(month);
            if (date.equals(selected)) {
                setBackground(owner.getSelectionBackground());
                setForeground(owner.getSelectionForeground());
            } else if (!allowed || !currentMonth) {
                setForeground(UIManager.getColor("Label.disabledForeground") == null
                        ? Color.GRAY : UIManager.getColor("Label.disabledForeground"));
            }
            return this;
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}

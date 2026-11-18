/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.event.ActionEvent;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

/** No native peer: real JTable month selection and a CDateTime popup owner. */
public final class M3MonthViewCalendarSmoke {
    private M3MonthViewCalendarSmoke() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3MonthViewCalendarSmoke::exercise);
        System.out.println("M3_NATIVE_CALENDAR_POPUP_PASS");
    }

    private static void exercise() {
        M3MonthView calendar = new M3MonthView(Locale.US);
        calendar.setMonth(YearMonth.of(2026, 11));
        JTable days = calendar.getTable();
        TableModel model = days.getModel();
        check(model.getRowCount() == 6 && model.getColumnCount() == 7, "one 6x7 table");
        check(calendar.dateAt(0, 0).equals(LocalDate.of(2026, 11, 1)), "Sunday first in US");
        check(model.getValueAt(0, 0).equals(1), "date values remain primitive-backed");
        check(days.getCellEditor(0, 0) == null, "calendar cells are read only");
        AtomicInteger actions = new AtomicInteger();
        calendar.addActionListener(e -> {
            if (!M3MonthView.DATE_COMMITTED.equals(e.getActionCommand())) throw new AssertionError("action key");
            actions.incrementAndGet();
        });
        calendar.setSelectedDate(LocalDate.of(2026, 11, 18));
        check(actions.get() == 0, "programmatic selection is not user action");
        calendar.commitCell(2, 3); // Nov 18 2026, Wednesday
        check(calendar.getSelectedDate().equals(LocalDate.of(2026, 11, 18)), "committed Wednesday");
        check(actions.get() == 1, "exactly one action per user commit");
        calendar.setDateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20));
        calendar.commitCell(0, 0);
        check(actions.get() == 1 && calendar.getSelectedDate().getDayOfMonth() == 18, "out-of-range date ignored");
        reject(() -> calendar.setSelectedDate(LocalDate.of(2026, 11, 21)), IllegalArgumentException.class);
        check(calendar.isSelectable(LocalDate.of(2026, 11, 20)), "upper date inclusive");
        calendar.setCalendarLocale(Locale.FRANCE);
        check(calendar.dateAt(0, 0).equals(LocalDate.of(2026, 10, 26)), "locale changes first weekday");
        check(days == calendar.getTable() && days.getModel() == model, "existing JTable model owner preserved");
        calendar.setDateRange(null, null);
        calendar.shiftMonth(1);
        check(calendar.getMonth().equals(YearMonth.of(2026, 12)), "next month button logic");
        calendar.shiftMonth(-1);
        check(calendar.getMonth().equals(YearMonth.of(2026, 11)), "previous month button logic");
        days.changeSelection(2, 2, false, false);
        days.getActionMap().get("m3.commitDay").actionPerformed(new ActionEvent(days, 0, "Enter"));
        check(actions.get() == 2, "keyboard commit uses same action channel");
        for (int i = 0; i < 2_000; i++) calendar.shiftMonth(i % 2 == 0 ? 1 : -1);
        check(calendar.getMonth().equals(YearMonth.of(2026, 11)), "repeated month navigation retains model");
        check(days == calendar.getTable() && model == days.getModel(), "no materialized day controls");
        calendar.setEnabled(false);
        int before = actions.get();
        calendar.commitCell(2, 3);
        check(actions.get() == before, "disabled month grid cannot commit");

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        M3DateTime timestamp = new M3DateTime(zone, "yyyy-MM-dd HH:mm:ss");
        timestamp.setLocalDateTime(LocalDateTime.of(2026, 11, 18, 9, 56, 12));
        Instant current = timestamp.getInstant();
        check(!timestamp.isCalendarPopupEnabled() && timestamp.getCalendarButton() == null,
                "existing spinner-only style preserved by default");
        timestamp.setCalendarPopupEnabled(true);
        check(timestamp.isCalendarPopupEnabled() && timestamp.getCalendarButton() != null,
                "calendar enabled explicitly");
        check(timestamp.getMonthView().getSelectedDate().equals(LocalDate.of(2026, 11, 18)),
                "popup selected date tracks spinner");
        timestamp.getMonthView().setSelectedDate(LocalDate.of(2026, 11, 19));
        check(timestamp.getInstant().equals(current), "draft day selection never writes instant");
        timestamp.getMonthView().setMonth(YearMonth.of(2026, 11));
        // US locale is not required for this: find the day by the ordinal API.
        commitDate(timestamp.getMonthView(), LocalDate.of(2026, 11, 20));
        check(timestamp.getLocalDateTime().equals(LocalDateTime.of(2026, 11, 20, 9, 56, 12)),
                "calendar commit preserves wall-clock time");
        check(timestamp.getInstant().equals(current.plusSeconds(2 * 86400L)), "instant updated atomically");
        timestamp.getSpinner().setValue(Date.from(current.plusSeconds(3 * 86400L)));
        check(timestamp.getMonthView().getSelectedDate().equals(LocalDate.of(2026, 11, 21)),
                "direct native spinner edits synchronize existing month view");
        timestamp.setInstant(current.plusSeconds(2 * 86400L));
        timestamp.setRange(current.minusSeconds(10), current.plusSeconds(5 * 86400L));
        check(timestamp.getMonthView().getMinimum().equals(LocalDate.of(2026, 11, 18)),
                "date range projects to local calendar");
        check(timestamp.getMonthView().getMaximum().equals(LocalDate.of(2026, 11, 23)),
                "upper bound projects to local calendar");
        reject(() -> timestamp.getMonthView().setSelectedDate(LocalDate.of(2026, 11, 24)), IllegalArgumentException.class);
        timestamp.setZone(ZoneId.of("UTC"));
        check(timestamp.getInstant().equals(current.plusSeconds(2 * 86400L)), "timezone change preserves instant");
        timestamp.setCalendarPopupEnabled(false);
        check(!timestamp.isCalendarPopupEnabled() && !timestamp.isCalendarPopupVisible(),
                "disabling popup restores spinner-only presentation");
        timestamp.setCalendarPopupEnabled(true);
        check(timestamp.getMonthView() != null, "same native calendar reused after toggle");
        timestamp.setEnabled(false);
        check(!timestamp.getCalendarButton().isEnabled() && !timestamp.getMonthView().isEnabled(),
                "disabled state propagates to calendar");
        timestamp.setEnabled(true);
        reject(timestamp::showCalendarPopup, IllegalStateException.class);

        timestamp.setZone(ZoneId.of("America/New_York"));
        timestamp.setRange(null, null);
        timestamp.setLocalDateTime(LocalDateTime.of(2026, 3, 7, 2, 30));
        Instant beforeGap = timestamp.getInstant();
        commitDate(timestamp.getMonthView(), LocalDate.of(2026, 3, 8));
        check(timestamp.getInstant().equals(beforeGap), "DST gap rejected without altering instant");
        check(timestamp.getMonthView().getSelectedDate().equals(LocalDate.of(2026, 3, 7)),
                "failed commit resets calendar selection");
        timestamp.setInstant(Instant.parse("2026-11-01T05:30:00Z"));
        Instant beforeOverlap = timestamp.getInstant();
        commitDate(timestamp.getMonthView(), LocalDate.of(2026, 11, 1));
        check(timestamp.getInstant().equals(beforeOverlap), "DST overlap rejected without arbitrary resolution");
        check(timestamp.getMonthView().getSelectedDate().equals(LocalDate.of(2026, 11, 1)),
                "explicitly resolved spinner instant retained");
    }

    private static void commitDate(M3MonthView view, LocalDate target) {
        view.setMonth(YearMonth.from(target));
        for (int row = 0; row < 6; row++) for (int column = 0; column < 7; column++) {
            if (view.dateAt(row, column).equals(target)) {
                view.commitCell(row, column);
                return;
            }
        }
        throw new AssertionError("target date missing from month");
    }

    private static void reject(Runnable action, Class<? extends Throwable> expected) {
        try { action.run(); } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw failure;
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/** Xvfb/desktop exercise of popup ownership, click activation and disposal. */
public final class M3MonthViewHeadfulSmoke {
    private M3MonthViewHeadfulSmoke() { }
    public static void main(String[] args) throws Exception {
        if (java.awt.GraphicsEnvironment.isHeadless()) throw new AssertionError("headless environment");
        // XToolkit may hold AWT_LOCK while awaiting MouseEvent class init.
        // Initialize on main before dispatching the first graphical event.
        MouseEvent.getMouseModifiersText(0);
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Native CDateTime popup");
            try {
                M3DateTime picker = new M3DateTime(ZoneId.of("Asia/Kolkata"), "yyyy-MM-dd HH:mm:ss");
                picker.setLocalDateTime(LocalDateTime.of(2026, 11, 18, 9, 56, 12));
                picker.setCalendarPopupEnabled(true);
                frame.setLayout(new BorderLayout());
                frame.add(picker, BorderLayout.NORTH);
                frame.setSize(470, 480);
                frame.setLocation(100, 100);
                frame.setVisible(true);
                if (!picker.getCalendarButton().isShowing()) throw new AssertionError("calendar button not shown");
                picker.showCalendarPopup();
                if (!picker.isCalendarPopupVisible()) throw new AssertionError("calendar popup hidden");
                M3MonthView monthView = picker.getMonthView();
                JTable table = monthView.getTable();
                LocalDate target = LocalDate.of(2026, 11, 20);
                boolean clicked = false;
                for (int row = 0; row < 6 && !clicked; row++) for (int col = 0; col < 7 && !clicked; col++) {
                    if (!monthView.dateAt(row, col).equals(target)) continue;
                    Rectangle r = table.getCellRect(row, col, false);
                    MouseEvent release = new MouseEvent(table, MouseEvent.MOUSE_RELEASED,
                            System.currentTimeMillis(), 0, r.x + r.width / 2, r.y + r.height / 2,
                            1, false, MouseEvent.BUTTON1);
                    table.dispatchEvent(release);
                    clicked = true;
                }
                if (!clicked) throw new AssertionError("target day is missing");
                if (!picker.getLocalDateTime().equals(LocalDateTime.of(2026, 11, 20, 9, 56, 12))) {
                    throw new AssertionError("mouse commit did not preserve spinner wall-clock time");
                }
                if (picker.isCalendarPopupVisible()) throw new AssertionError("popup should close after valid selection");
                picker.setCalendarPopupEnabled(false);
                if (picker.getCalendarButton().getParent() != null) throw new AssertionError("button not detached");
            } finally {
                frame.dispose();
            }
        });
        System.out.println("M3_CALENDAR_POPUP_HEADFUL_PASS");
    }
}

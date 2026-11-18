/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.time.DateTimeException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.JSpinner;

/** JDK-only headless CDateTime text/spinner mode differential. */
public final class M3DateTimeSmoke {
    private M3DateTimeSmoke() { }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SwingUtilities.invokeAndWait(M3DateTimeSmoke::exercise);
        System.out.println("M3_NATIVE_DATE_TIME_PASS");
    }
    private static void exercise() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        M3DateTime picker = new M3DateTime(kolkata, "yyyy-MM-dd HH:mm:ss");
        AtomicInteger changes = new AtomicInteger();
        picker.addChangeListener(e -> changes.incrementAndGet());
        LocalDateTime local = LocalDateTime.of(2026, 11, 18, 9, 56);
        picker.setLocalDateTime(local);
        check(picker.getLocalDateTime().equals(local), "local text selection");
        Instant canonical = picker.getInstant();
        check(canonical.equals(local.toInstant(ZoneOffset.ofHoursMinutes(5, 30))), "instant normalization");
        picker.setZone(ZoneId.of("UTC"));
        check(picker.getInstant().equals(canonical), "timezone changes only display");
        check(picker.getLocalDateTime().equals(LocalDateTime.of(2026, 11, 18, 4, 26)), "UTC wall time");
        picker.setZone(kolkata);
        picker.setRange(canonical.minusSeconds(60), canonical.plusSeconds(60));
        reject(() -> picker.setInstant(canonical.plusSeconds(120)), IllegalArgumentException.class);
        picker.setInstant(canonical.minusSeconds(60));
        check(picker.getInstant().equals(canonical.minusSeconds(60)), "inclusive range minimum");
        picker.setRange(canonical.minusSeconds(15), canonical.plusSeconds(15));
        check(picker.getInstant().equals(canonical.minusSeconds(15)), "bound tightening clamps");
        reject(() -> picker.setRange(canonical.plusSeconds(1), canonical.minusSeconds(1)), IllegalArgumentException.class);
        reject(() -> picker.setInstant(canonical.plusNanos(123)), IllegalArgumentException.class);
        reject(() -> picker.setRange(canonical.plusNanos(123), null), IllegalArgumentException.class);
        check(changes.get() >= 2, "spinner model change events");
        picker.setRange(null, null);
        picker.setZone(ZoneId.of("America/New_York"));
        reject(() -> picker.setLocalDateTime(LocalDateTime.of(2026, 3, 8, 2, 30)), DateTimeException.class);
        reject(() -> picker.setLocalDateTime(LocalDateTime.of(2026, 11, 1, 1, 30)), DateTimeException.class);
        picker.setInstant(Instant.parse("2026-11-01T05:30:00Z"));
        check(picker.getLocalDateTime().equals(LocalDateTime.of(2026, 11, 1, 1, 30)), "explicit overlap instant");
        Instant beforeCommit = picker.getInstant();
        ((JSpinner.DateEditor)picker.getSpinner().getEditor()).getTextField().setText("2026-11-01 01:30:00");
        try { picker.commitEdit(); throw new AssertionError("overlap text accepted"); }
        catch (ParseException expected) { check(beforeCommit.equals(picker.getInstant()), "rejected edit is atomic"); }
        picker.setZone(kolkata);
        picker.setInstant(canonical);
        ((JSpinner.DateEditor)picker.getSpinner().getEditor()).getTextField().setText("2026-11-18 09:57:00");
        try { picker.commitEdit(); }
        catch (ParseException unexpected) { throw new AssertionError(unexpected); }
        check(picker.getLocalDateTime().equals(LocalDateTime.of(2026, 11, 18, 9, 57)), "text commit");
        picker.setEditable(false);
        check(!picker.isEditable(), "read-only mode");
        picker.setEditable(true);
        picker.setEnabled(false);
        check(!picker.getSpinner().isEnabled(), "disabled model remains owned");
    }
    private static void reject(Runnable action, Class<? extends Throwable> type) {
        try { action.run(); } catch (Throwable e) { if (type.isInstance(e)) return; throw e; }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }
}

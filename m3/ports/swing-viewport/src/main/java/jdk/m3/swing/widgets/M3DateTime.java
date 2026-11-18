/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import java.awt.BorderLayout;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

/**
 * Native Swing date-and-time selector for Nebula CDateTime's text/spinner mode.
 * Uses JSpinner, SpinnerDateModel, the stock DateEditor and the Swing EDT.
 * Its canonical value is an Instant at millisecond precision; ZoneId changes
 * only display formatting.
 *
 * <p>Local timestamps at daylight-saving gaps or overlaps are rejected, not
 * silently shifted or assigned an arbitrary offset. Callers may setInstant
 * for an explicitly resolved instant. Graphical calendar popup parity with
 * Nebula/SwingX is outside this initial native control.</p>
 */
public final class M3DateTime extends JPanel {
    private static final long serialVersionUID = 1L;
    private final SpinnerDateModel model;
    private final JSpinner spinner;
    private final JSpinner.DateEditor editor;
    private ZoneId zone;
    private Instant lower;
    private Instant upper;

    public M3DateTime() {
        this(ZoneId.systemDefault(), "yyyy-MM-dd HH:mm:ss");
    }

    public M3DateTime(ZoneId zone, String pattern) {
        super(new BorderLayout());
        this.zone = Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(pattern, "pattern");
        model = new SpinnerDateModel(new Date(0), null, null, Calendar.MINUTE);
        spinner = new JSpinner(model);
        editor = new JSpinner.DateEditor(spinner, pattern);
        editor.getFormat().setTimeZone(TimeZone.getTimeZone(zone));
        editor.getFormat().setLenient(false);
        spinner.setEditor(editor);
        editor.getTextField().getAccessibleContext().setAccessibleName("Date and time");
        editor.getTextField().getAccessibleContext().setAccessibleDescription("Calendar date and wall-clock time");
        add(spinner, BorderLayout.CENTER);
    }

    public JSpinner getSpinner() { return spinner; }
    public ZoneId getZone() { return zone; }
    public Instant getInstant() { return ((Date) model.getValue()).toInstant(); }
    public LocalDateTime getLocalDateTime() { return LocalDateTime.ofInstant(getInstant(), zone); }
    public Instant getMinimum() { return lower; }
    public Instant getMaximum() { return upper; }

    /** Changes only the display timezone, not the canonical timestamp. */
    public void setZone(ZoneId zone) {
        requireEdt();
        ZoneId value = Objects.requireNonNull(zone, "zone");
        if (!this.zone.equals(value)) {
            ZoneId previous = this.zone;
            this.zone = value;
            editor.getFormat().setTimeZone(TimeZone.getTimeZone(value));
            editor.getTextField().setValue(model.getValue());
            firePropertyChange("zone", previous, value);
        }
    }

    public void setInstant(Instant instant) {
        requireEdt();
        Instant value = Objects.requireNonNull(instant, "instant");
        if (value.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException("spinner dates have millisecond precision");
        }
        if ((lower != null && value.isBefore(lower)) || (upper != null && value.isAfter(upper))) {
            throw new IllegalArgumentException("instant outside allowed bounds");
        }
        model.setValue(Date.from(value));
    }

    /** Rejects ambiguous and nonexistent local times; use setInstant to resolve them explicitly. */
    public void setLocalDateTime(LocalDateTime local) {
        requireEdt();
        Objects.requireNonNull(local, "local");
        List<ZoneOffset> offsets = zone.getRules().getValidOffsets(local);
        if (offsets.size() != 1) {
            throw new DateTimeException("nonunique local date/time in " + zone);
        }
        setInstant(local.toInstant(offsets.get(0)));
    }

    /** Inclusive lower and upper bounds; null clears a bound. */
    public void setRange(Instant lower, Instant upper) {
        requireEdt();
        if (lower != null && upper != null && lower.isAfter(upper)) {
            throw new IllegalArgumentException("reversed date range");
        }
        if ((lower != null && lower.getNano() % 1_000_000 != 0)
                || (upper != null && upper.getNano() % 1_000_000 != 0)) {
            throw new IllegalArgumentException("range must have millisecond precision");
        }
        this.lower = lower;
        this.upper = upper;
        // Date is the single value authority for Swing's native SpinnerDateModel.
        model.setStart(lower == null ? null : Date.from(lower));
        model.setEnd(upper == null ? null : Date.from(upper));
        if (lower != null && getInstant().isBefore(lower)) model.setValue(Date.from(lower));
        else if (upper != null && getInstant().isAfter(upper)) model.setValue(Date.from(upper));
    }

    public void addChangeListener(ChangeListener listener) {
        model.addChangeListener(Objects.requireNonNull(listener, "listener"));
    }

    public void removeChangeListener(ChangeListener listener) {
        model.removeChangeListener(listener);
    }

    public boolean isEditable() { return editor.getTextField().isEditable(); }

    public void setEditable(boolean editable) {
        requireEdt();
        editor.getTextField().setEditable(editable);
    }

    /** Parses and validates before changing SpinnerDateModel or firing listeners. */
    public void commitEdit() throws ParseException {
        requireEdt();
        String raw = editor.getTextField().getText();
        ParsePosition position = new ParsePosition(0);
        Date parsed = editor.getFormat().parse(raw, position);
        if (parsed == null || position.getIndex() != raw.length()) {
            throw new ParseException("invalid date/time", Math.max(0, position.getErrorIndex()));
        }
        Instant value = parsed.toInstant();
        LocalDateTime local = LocalDateTime.ofInstant(value, zone);
        if (zone.getRules().getValidOffsets(local).size() != 1) {
            throw new ParseException("ambiguous local date/time", 0);
        }
        if ((lower != null && value.isBefore(lower)) || (upper != null && value.isAfter(upper))) {
            throw new ParseException("date/time outside allowed bounds", 0);
        }
        setInstant(value);
        editor.getTextField().setValue(model.getValue());
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (spinner != null) spinner.setEnabled(enabled);
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Swing EDT required");
    }
}

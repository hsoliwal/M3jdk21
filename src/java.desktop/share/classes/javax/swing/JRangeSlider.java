/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The copyright holders designate this
 * particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package javax.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.util.Objects;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * A slider with two ordered thumbs selecting a closed integer interval.
 * The existing {@link BoundedRangeModel} is the sole value owner: its value
 * is the lower endpoint and its extent is the upper endpoint minus the lower.
 * Consequently, a selected interval cannot span more than
 * {@link Integer#MAX_VALUE}, even when the model's minimum and maximum span
 * a larger range. No second endpoint model or copied data store is used.
 *
 * <p>{@link #setRange(int, int)} changes both endpoints atomically. Changing
 * the lower endpoint with {@link #setValue(int)} preserves the upper endpoint;
 * changing the upper endpoint preserves the lower. These individual setters
 * clamp to the representable interval. Inherited minimum, maximum and extent
 * setters retain the {@code BoundedRangeModel} contract.
 *
 * <p>Drag either thumb, or control-drag between the thumbs to move the interval.
 * Space or F6 changes the active thumb; the usual slider arrow, page, home and
 * end keys operate on that thumb. Tab retains ordinary focus traversal.
 * The adjusting flag is true during a drag; applications that need final-only
 * notifications can ignore change events while {@link #getValueIsAdjusting()}
 * is true. Tick snapping applies to pointer and keyboard changes; moving the
 * whole interval snaps its lower endpoint while preserving the selected span.
 *
 * <p>The supplied two-thumb delegate uses the active look and feel's basic
 * slider colors, ticks and labels. {@link #updateUI()} restores this delegate.
 * A custom {@code SliderUI} must understand the model's two endpoints.
 * As with other Swing components, access this component and its model on the
 * event dispatch thread unless a method explicitly documents otherwise.
 *
 * @since 21
 */
@SuppressWarnings("serial")
public class JRangeSlider extends JSlider {
    private boolean upperThumbSelected;

    /** Creates a horizontal slider selecting 25 through 75 within 0 through 100. */
    public JRangeSlider() {
        this(HORIZONTAL, 0, 100, 25, 75);
    }

    /**
     * Creates a horizontal slider selecting the given interval.
     * @param minimum the minimum value
     * @param maximum the maximum value
     * @param lower the lower selected value
     * @param upper the upper selected value
     * @throws IllegalArgumentException if the interval is invalid or unrepresentable
     */
    public JRangeSlider(int minimum, int maximum, int lower, int upper) {
        this(HORIZONTAL, minimum, maximum, lower, upper);
    }

    /**
     * Creates a slider with the given orientation and interval.
     * @param orientation {@code HORIZONTAL} or {@code VERTICAL}
     * @param minimum the minimum value
     * @param maximum the maximum value
     * @param lower the lower selected value
     * @param upper the upper selected value
     * @throws IllegalArgumentException if the orientation or interval is invalid
     */
    @SuppressWarnings("this-escape") // Follows JSlider's constructor initialization of its UI properties.
    public JRangeSlider(int orientation, int minimum, int maximum,
                        int lower, int upper) {
        this(newModel(minimum, maximum, lower, upper));
        setOrientation(orientation);
    }

    /**
     * Creates a horizontal slider using the supplied model without copying it.
     * @param model the non-null value and extent model
     * @throws NullPointerException if {@code model} is null
     */
    public JRangeSlider(BoundedRangeModel model) {
        super(Objects.requireNonNull(model, "model"));
    }

    /**
     * Replaces the sole interval model and updates both accessible endpoints.
     * @param model the new bounded range model
     */
    @Override
    public void setModel(BoundedRangeModel model) {
        super.setModel(model);
        if (accessibleContext instanceof AccessibleJRangeSlider context) {
            context.endpointsChanged();
        }
    }

    private static BoundedRangeModel newModel(int min, int max, int low, int high) {
        checkRange(min, max, low, high);
        return new DefaultBoundedRangeModel(low, (int) ((long) high - low), min, max);
    }

    private static void checkRange(int min, int max, int low, int high) {
        if (min > max || low < min || high > max || low > high
                || (long) high - low > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unrepresentable selected interval");
        }
    }

    /** Returns the lower selected value. @return the model value */
    public int getLowerValue() {
        return getValue();
    }

    /** Returns the upper selected value. @return the model value plus extent */
    public int getUpperValue() {
        return (int) ((long) getValue() + getExtent());
    }

    /**
     * Sets the lower endpoint, preserving the upper endpoint.
     * @param value the requested lower value, clamped to the valid interval
     */
    public void setLowerValue(int value) {
        setValue(value);
    }

    /**
     * Sets the lower endpoint, preserving the upper endpoint.
     * Unlike a single-thumb slider, this changes the extent as necessary.
     * @param value the requested lower value, clamped to the valid interval
     */
    @Override
    public void setValue(int value) {
        BoundedRangeModel model = getModel();
        long upper = (long) model.getValue() + model.getExtent();
        long minimum = Math.max((long) model.getMinimum(), upper - Integer.MAX_VALUE);
        int lower = (int) Math.max(minimum, Math.min(upper, value));
        model.setRangeProperties(lower, (int) (upper - lower),
                model.getMinimum(), model.getMaximum(), model.getValueIsAdjusting());
    }

    /**
     * Sets the upper endpoint, preserving the lower endpoint.
     * @param value the requested upper value, clamped to the valid interval
     */
    public void setUpperValue(int value) {
        BoundedRangeModel model = getModel();
        long lower = model.getValue();
        long maximum = Math.min((long) model.getMaximum(), lower + Integer.MAX_VALUE);
        long upper = Math.max(lower, Math.min(maximum, value));
        model.setRangeProperties((int) lower, (int) (upper - lower),
                model.getMinimum(), model.getMaximum(), model.getValueIsAdjusting());
    }

    /**
     * Sets both endpoints with a single model update.
     * @param lower the lower endpoint
     * @param upper the upper endpoint
     * @throws IllegalArgumentException if the interval is outside the model's
     *         bounds, is reversed, or has a span greater than {@code Integer.MAX_VALUE}
     */
    public void setRange(int lower, int upper) {
        BoundedRangeModel model = getModel();
        checkRange(model.getMinimum(), model.getMaximum(), lower, upper);
        model.setRangeProperties(lower, (int) ((long) upper - lower),
                model.getMinimum(), model.getMaximum(), model.getValueIsAdjusting());
    }

    /**
     * Moves the interval without changing its span, stopping at either bound.
     * @param delta the requested movement
     */
    public void shiftRange(int delta) {
        moveRange(getLowerValue(), getUpperValue(), delta);
    }

    private void moveRange(int lower, int upper, long delta) {
        long movement = Math.max((long) getMinimum() - lower,
                Math.min((long) getMaximum() - upper, delta));
        setRange((int) (lower + movement), (int) (upper + movement));
    }

    /**
     * Returns whether keyboard input operates on the upper thumb.
     * @return true for the upper thumb, false for the lower thumb
     */
    public boolean isUpperThumbSelected() {
        return upperThumbSelected;
    }

    /**
     * Chooses the thumb operated by keyboard input.
     * @param selected true for the upper thumb, false for the lower thumb
     */
    public void setUpperThumbSelected(boolean selected) {
        boolean old = upperThumbSelected;
        if (old != selected) {
            upperThumbSelected = selected;
            firePropertyChange("upperThumbSelected", old, selected);
            if (accessibleContext instanceof AccessibleJRangeSlider context) {
                context.firePropertyChange(AccessibleContext.ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY,
                        context.getAccessibleChild(old ? 1 : 0),
                        context.getAccessibleChild(selected ? 1 : 0));
            }
            repaint();
        }
    }

    /** Restores the two-thumb basic delegate using the current look and feel. */
    @Override
    public void updateUI() {
        setUI(new RangeSliderUI(this));
        updateLabelUIs();
    }

    /**
     * Installs a delegate, completing any pointer adjustment before teardown.
     * This ordering also permits a final-change listener to replace the UI.
     * @param ui the new slider delegate
     */
    @Override
    public void setUI(SliderUI ui) {
        if (getUI() instanceof RangeSliderUI current) {
            current.rangeTrack.finish();
        }
        super.setUI(ui);
    }

    /**
     * Returns an accessible slider with a value child for each endpoint.
     * @return this component's accessible context
     */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJRangeSlider();
        }
        return accessibleContext;
    }

    /** Accessibility implementation exposing both endpoint values. */
    protected class AccessibleJRangeSlider extends AccessibleJSlider {
        private final Endpoint lower = new Endpoint(false);
        private final Endpoint upper = new Endpoint(true);
        private int lastLower = getLowerValue();
        private int lastUpper = getUpperValue();

        /** Creates an accessible interval slider. */
        protected AccessibleJRangeSlider() { }

        @Override
        public int getAccessibleChildrenCount() { return 2; }

        @Override
        public Accessible getAccessibleChild(int index) {
            return index == 0 ? lower : index == 1 ? upper : null;
        }

        @Override
        public void stateChanged(ChangeEvent event) {
            super.stateChanged(event);
            endpointsChanged();
        }

        private void endpointsChanged() {
            int previousLower = lastLower;
            int previousUpper = lastUpper;
            int nextLower = getLowerValue();
            int nextUpper = getUpperValue();
            lastLower = nextLower;
            lastUpper = nextUpper;
            if (lower != null) {
                lower.firePropertyChange(ACCESSIBLE_VALUE_PROPERTY, previousLower, nextLower);
                upper.firePropertyChange(ACCESSIBLE_VALUE_PROPERTY, previousUpper, nextUpper);
            }
        }

        @Override
        public boolean setCurrentAccessibleValue(Number value) {
            return lower.setCurrentAccessibleValue(value);
        }

        @Override
        public Number getMinimumAccessibleValue() { return lower.getMinimumAccessibleValue(); }

        @Override
        public Number getMaximumAccessibleValue() { return lower.getMaximumAccessibleValue(); }

        @Override
        public boolean doAccessibleAction(int index) { return lower.doAccessibleAction(index); }

        private final class Endpoint extends AccessibleContext
                implements Accessible, AccessibleValue, AccessibleAction, java.io.Serializable {
            private final boolean high;

            Endpoint(boolean high) {
                this.high = high;
                setAccessibleParent(JRangeSlider.this);
                setAccessibleName(high ? "Upper value" : "Lower value");
            }

            @java.io.Serial
            private void writeObject(java.io.ObjectOutputStream output) throws java.io.IOException {
                output.defaultWriteObject();
                output.writeObject(accessibleName);
                output.writeObject(accessibleDescription);
            }

            @java.io.Serial
            private void readObject(java.io.ObjectInputStream input)
                    throws java.io.IOException, ClassNotFoundException {
                input.defaultReadObject();
                accessibleName = (String) input.readObject();
                accessibleDescription = (String) input.readObject();
                input.registerValidation(() -> setAccessibleParent(JRangeSlider.this), 0);
            }

            @Override public AccessibleContext getAccessibleContext() { return this; }
            @Override public AccessibleRole getAccessibleRole() { return AccessibleRole.SLIDER; }
            @Override public int getAccessibleIndexInParent() { return high ? 1 : 0; }
            @Override public int getAccessibleChildrenCount() { return 0; }
            @Override public Accessible getAccessibleChild(int index) { return null; }
            @Override public Locale getLocale() { return JRangeSlider.this.getLocale(); }
            @Override public AccessibleValue getAccessibleValue() { return this; }
            @Override public AccessibleAction getAccessibleAction() { return this; }

            @Override
            public AccessibleStateSet getAccessibleStateSet() {
                AccessibleStateSet states = new AccessibleStateSet();
                if (isEnabled()) { states.add(AccessibleState.ENABLED); }
                if (isVisible()) { states.add(AccessibleState.VISIBLE); }
                if (isShowing()) { states.add(AccessibleState.SHOWING); }
                if (isFocusable()) { states.add(AccessibleState.FOCUSABLE); }
                if (hasFocus() && isUpperThumbSelected() == high) {
                    states.add(AccessibleState.FOCUSED);
                }
                if (getValueIsAdjusting()) { states.add(AccessibleState.BUSY); }
                states.add(getOrientation() == HORIZONTAL
                        ? AccessibleState.HORIZONTAL : AccessibleState.VERTICAL);
                return states;
            }

            @Override
            public Number getCurrentAccessibleValue() {
                return high ? getUpperValue() : getLowerValue();
            }

            @Override
            public Number getMinimumAccessibleValue() {
                return high ? getLowerValue()
                        : Math.max((long) getMinimum(), (long) getUpperValue() - Integer.MAX_VALUE);
            }

            @Override
            public Number getMaximumAccessibleValue() {
                return high ? Math.min((long) getMaximum(), (long) getLowerValue() + Integer.MAX_VALUE)
                        : getUpperValue();
            }

            @Override
            public boolean setCurrentAccessibleValue(Number value) {
                if (value == null || !isEnabled()) { return false; }
                int requested = (int) Math.max(Integer.MIN_VALUE,
                        Math.min(Integer.MAX_VALUE, value.longValue()));
                if (high) { setUpperValue(requested); } else { setLowerValue(requested); }
                return true;
            }

            @Override public int getAccessibleActionCount() { return 2; }

            @Override
            public String getAccessibleActionDescription(int index) {
                return index == 0 ? AccessibleAction.INCREMENT
                        : index == 1 ? AccessibleAction.DECREMENT : null;
            }

            @Override
            public boolean doAccessibleAction(int index) {
                return index >= 0 && index < 2 && setCurrentAccessibleValue(
                        getCurrentAccessibleValue().longValue() + (index == 0 ? 1 : -1));
            }
        }
    }

    private static final class RangeSliderUI extends BasicSliderUI {
        private final Rectangle upperThumb = new Rectangle();
        private InputMapUIResource rangeInputMap;
        private RangeTrackListener rangeTrack;

        RangeSliderUI(JRangeSlider slider) { super(slider); }

        private JRangeSlider range() { return (JRangeSlider) slider; }

        @Override
        protected TrackListener createTrackListener(JSlider slider) {
            rangeTrack = new RangeTrackListener();
            return rangeTrack;
        }

        @Override
        protected PropertyChangeListener createPropertyChangeListener(JSlider slider) {
            PropertyChangeListener original = super.createPropertyChangeListener(slider);
            return event -> {
                original.propertyChange(event);
                if ("model".equals(event.getPropertyName())
                        || ("enabled".equals(event.getPropertyName()) && !slider.isEnabled())) {
                    rangeTrack.finish();
                }
                if ("componentOrientation".equals(event.getPropertyName())) {
                    installRangeInputMap();
                }
            };
        }

        @Override
        protected void installKeyboardActions(JSlider slider) {
            super.installKeyboardActions(slider);
            installRangeInputMap();
            ActionMapUIResource actions = new ActionMapUIResource();
            actions.setParent(SwingUtilities.getUIActionMap(slider));
            actions.put("selectOtherThumb", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {
                    JRangeSlider component = (JRangeSlider) event.getSource();
                    if (component.isEnabled()) {
                        component.setUpperThumbSelected(!component.isUpperThumbSelected());
                    }
                }
            });
            actions.put("minScroll", endpointAction(false));
            actions.put("maxScroll", endpointAction(true));
            SwingUtilities.replaceUIActionMap(slider, actions);
        }

        private void installRangeInputMap() {
            InputMap parent = SwingUtilities.getUIInputMap(slider, JComponent.WHEN_FOCUSED);
            if (parent == rangeInputMap) { return; }
            rangeInputMap = new InputMapUIResource();
            rangeInputMap.setParent(parent);
            rangeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "selectOtherThumb");
            rangeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "selectOtherThumb");
            SwingUtilities.replaceUIInputMap(slider, JComponent.WHEN_FOCUSED, rangeInputMap);
        }

        private Action endpointAction(boolean maximum) {
            return new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {
                    JRangeSlider component = (JRangeSlider) event.getSource();
                    if (component.isEnabled()) {
                        int value = (maximum != component.getInverted())
                                ? component.getMaximum() : component.getMinimum();
                        if (component.isUpperThumbSelected()) { component.setUpperValue(value); }
                        else { component.setLowerValue(value); }
                    }
                }
            };
        }

        @Override
        protected void calculateThumbLocation() {
            if (slider.getOrientation() == HORIZONTAL) {
                thumbRect.setLocation(xPositionForValue(slider.getValue()) - thumbRect.width / 2,
                        trackRect.y);
                upperThumb.setBounds(xPositionForValue(range().getUpperValue()) - thumbRect.width / 2,
                        trackRect.y, thumbRect.width, thumbRect.height);
            } else {
                thumbRect.setLocation(trackRect.x,
                        yPositionForValue(slider.getValue()) - thumbRect.height / 2);
                upperThumb.setBounds(trackRect.x,
                        yPositionForValue(range().getUpperValue()) - thumbRect.height / 2,
                        thumbRect.width, thumbRect.height);
            }
        }

        @Override protected int xPositionForValue(int value) { return position(value, true); }
        @Override protected int yPositionForValue(int value) { return position(value, false); }

        private int position(int value, boolean horizontal) {
            int start = horizontal ? trackRect.x : trackRect.y;
            int length = horizontal ? trackRect.width : trackRect.height;
            long span = (long) slider.getMaximum() - slider.getMinimum();
            if (length <= 1 || span == 0) { return start; }
            double fraction = ((long) value - slider.getMinimum()) / (double) span;
            if (horizontal ? drawInverted() : !drawInverted()) { fraction = 1 - fraction; }
            return start + (int) Math.round(Math.max(0, Math.min(1, fraction)) * (length - 1L));
        }

        @Override public int valueForXPosition(int x) { return value(x, true); }
        @Override public int valueForYPosition(int y) { return value(y, false); }

        private int value(int coordinate, boolean horizontal) {
            int start = horizontal ? trackRect.x : trackRect.y;
            int length = horizontal ? trackRect.width : trackRect.height;
            if (length <= 1) { return slider.getMinimum(); }
            double fraction = Math.max(0, Math.min(1, ((long) coordinate - start) / (double) (length - 1)));
            if (horizontal ? drawInverted() : !drawInverted()) { fraction = 1 - fraction; }
            return (int) (slider.getMinimum()
                    + Math.round(fraction * ((long) slider.getMaximum() - slider.getMinimum())));
        }

        private int snapped(int value) {
            int spacing = slider.getMinorTickSpacing() > 0
                    ? slider.getMinorTickSpacing() : slider.getMajorTickSpacing();
            if (!slider.getSnapToTicks() || spacing <= 0) { return value; }
            long offset = (long) value - slider.getMinimum();
            long result = slider.getMinimum() + Math.round(offset / (double) spacing) * (long) spacing;
            return (int) Math.max(slider.getMinimum(), Math.min(slider.getMaximum(), result));
        }

        private void setActive(long requested) {
            int next = snapped((int) Math.max(slider.getMinimum(), Math.min(slider.getMaximum(), requested)));
            if (range().isUpperThumbSelected()) { range().setUpperValue(next); }
            else { range().setLowerValue(next); }
        }

        @Override
        public void scrollByUnit(int direction) {
            if (!slider.isEnabled()) { return; }
            int spacing = slider.getMinorTickSpacing() > 0
                    ? slider.getMinorTickSpacing() : slider.getMajorTickSpacing();
            long step = slider.getSnapToTicks() ? Math.max(1, spacing) : 1;
            setActive((range().isUpperThumbSelected() ? range().getUpperValue() : range().getLowerValue())
                    + (direction > 0 ? step : -step));
        }

        @Override
        public void scrollByBlock(int direction) {
            if (!slider.isEnabled()) { return; }
            long step = Math.max(1, ((long) slider.getMaximum() - slider.getMinimum()) / 10);
            if (slider.getSnapToTicks()) {
                int spacing = slider.getMinorTickSpacing() > 0
                        ? slider.getMinorTickSpacing() : slider.getMajorTickSpacing();
                step = Math.max(step, spacing);
            }
            setActive((range().isUpperThumbSelected() ? range().getUpperValue() : range().getLowerValue())
                    + (direction > 0 ? step : -step));
        }

        @Override
        public void paintTrack(Graphics graphics) {
            super.paintTrack(graphics);
            Graphics g = graphics.create();
            try {
                Color color = UIManager.getColor(slider.isEnabled() ? "Slider.focus" : "Slider.shadow");
                g.setColor(color != null ? color : slider.getForeground());
                if (slider.getOrientation() == HORIZONTAL) {
                    int low = xPositionForValue(range().getLowerValue());
                    int high = xPositionForValue(range().getUpperValue());
                    g.fillRect(Math.min(low, high), trackRect.y + trackRect.height / 2 - 1,
                            Math.abs(high - low), 3);
                } else {
                    int low = yPositionForValue(range().getLowerValue());
                    int high = yPositionForValue(range().getUpperValue());
                    g.fillRect(trackRect.x + trackRect.width / 2 - 1, Math.min(low, high),
                            3, Math.abs(high - low));
                }
            } finally { g.dispose(); }
        }

        @Override public void paintThumb(Graphics graphics) { /* painted together below */ }

        @Override
        public void paint(Graphics graphics, JComponent component) {
            super.paint(graphics, component);
            boolean upperActive = range().isUpperThumbSelected();
            paintOneThumb(graphics, upperActive ? thumbRect : upperThumb, false);
            paintOneThumb(graphics, upperActive ? upperThumb : thumbRect, true);
        }

        private void paintOneThumb(Graphics graphics, Rectangle bounds, boolean active) {
            Rectangle clip = graphics.getClipBounds();
            if (clip != null && !clip.intersects(bounds)) { return; }
            Rectangle original = thumbRect;
            Graphics g = graphics.create();
            try {
                thumbRect = bounds;
                super.paintThumb(g);
                if (active && slider.hasFocus()) {
                    Color focus = UIManager.getColor("Slider.focus");
                    g.setColor(focus != null ? focus : slider.getForeground());
                    g.drawRect(bounds.x + 2, bounds.y + 2,
                            Math.max(0, bounds.width - 5), Math.max(0, bounds.height - 5));
                }
            } finally {
                thumbRect = original;
                g.dispose();
            }
        }

        private final class RangeTrackListener extends TrackListener {
            private BoundedRangeModel dragModel;
            private boolean interval;
            private int startValue;
            private int startLower;
            private int startUpper;
            private int startMinimum;
            private int startMaximum;
            private int centerOffset;

            private int coordinate(MouseEvent event) {
                return slider.getOrientation() == HORIZONTAL ? event.getX() : event.getY();
            }

            private int valueAt(int coordinate) {
                return slider.getOrientation() == HORIZONTAL
                        ? valueForXPosition(coordinate) : valueForYPosition(coordinate);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (!slider.isEnabled() || !SwingUtilities.isLeftMouseButton(event)) { return; }
                JRangeSlider component = range();
                finish();
                if (slider == null || component.getUI() != RangeSliderUI.this) { return; }
                calculateGeometry();
                if (slider.isRequestFocusEnabled()) { slider.requestFocusInWindow(); }
                Point point = event.getPoint();
                boolean lowHit = thumbRect.contains(point);
                boolean highHit = upperThumb.contains(point);
                int pressed = valueAt(coordinate(event));
                interval = event.isControlDown() && pressed >= range().getLowerValue()
                        && pressed <= range().getUpperValue();
                if (!interval) {
                    boolean high = highHit && (!lowHit || range().isUpperThumbSelected());
                    if (!lowHit && !highHit) {
                        high = Math.abs((long) pressed - range().getUpperValue())
                                < Math.abs((long) pressed - range().getLowerValue());
                    }
                    range().setUpperThumbSelected(high);
                    if (slider == null || component.getUI() != RangeSliderUI.this) { return; }
                    Rectangle thumb = high ? upperThumb : thumbRect;
                    centerOffset = (lowHit || highHit) ? coordinate(event)
                            - (slider.getOrientation() == HORIZONTAL
                            ? thumb.x + thumb.width / 2 : thumb.y + thumb.height / 2) : 0;
                }
                startValue = pressed;
                startLower = range().getLowerValue();
                startUpper = range().getUpperValue();
                startMinimum = slider.getMinimum();
                startMaximum = slider.getMaximum();
                dragModel = slider.getModel();
                dragModel.setValueIsAdjusting(true);
                if (validDrag() && !interval && !lowHit && !highHit) { setActive(pressed); }
            }

            private boolean validDrag() {
                if (dragModel == null) { return false; }
                if (slider == null || !slider.isEnabled() || slider.getModel() != dragModel
                        || slider.getUI() != RangeSliderUI.this
                        || slider.getMinimum() != startMinimum || slider.getMaximum() != startMaximum
                        || (interval && (long) slider.getExtent() != (long) startUpper - startLower)) {
                    finish();
                    return false;
                }
                return true;
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (!validDrag()) { return; }
                if (interval) {
                    long delta = (long) valueAt(coordinate(event)) - startValue;
                    if (slider.getSnapToTicks()) {
                        int lower = (int) Math.max(slider.getMinimum(),
                                Math.min(slider.getMaximum(), startLower + delta));
                        delta = (long) snapped(lower) - startLower;
                    }
                    range().moveRange(startLower, startUpper, delta);
                } else {
                    setActive(valueAt(coordinate(event) - centerOffset));
                }
            }

            @Override public void mouseReleased(MouseEvent event) { finish(); }

            private void finish() {
                BoundedRangeModel previous = dragModel;
                dragModel = null;
                if (previous != null) { previous.setValueIsAdjusting(false); }
            }
        }
    }
}

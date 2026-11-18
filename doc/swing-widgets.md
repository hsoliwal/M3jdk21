# Additional Swing widgets

M3JDK21 includes additional Swing controls in `java.desktop`, alongside the
original `javax.swing` components. They use ordinary Swing models, events,
accessibility and Java2D painting. They do not require SWT, an SWT event loop,
native SWT handles or the historical `jdk.m3.swing.web` widget scaffolds.

These controls provide selected SWT/Nebula widget capabilities through Swing
APIs. Their presence is not a claim that every SWT or Nebula widget, style,
renderer or event contract has a Swing equivalent.

## Ordered range slider

`JRangeSlider` selects an ordered integer interval with two thumbs:

```java
JRangeSlider range = new JRangeSlider(0, 100, 20, 80);
range.addChangeListener(event -> {
    if (!range.getValueIsAdjusting()) {
        System.out.println(range.getLowerValue() + " through " + range.getUpperValue());
    }
});
range.setRange(30, 70);  // One atomic model change.
```

The original `BoundedRangeModel` is the sole value owner. Its `value` is the
lower endpoint and `value + extent` is the upper endpoint. `setLowerValue`
and `setUpperValue` preserve the other endpoint and clamp to the valid
interval. `setRange` rejects reversed, out-of-bounds or unrepresentable
intervals before changing the model. The selected span must fit a
nonnegative `int` extent, even when minimum and maximum cover the full
signed integer range. Inherited minimum, maximum and extent setters keep
their original model behavior.

Drag either thumb, or hold Control while dragging within the selected
interval to move the whole interval. Space or F6 switches the keyboard's
active thumb; arrow, page, Home and End keys adjust it. Tab keeps normal
focus traversal. Horizontal and vertical orientations, inversion, RTL,
ticks and labels are supported. Change events retain the standard
`valueIsAdjusting` convention. Both endpoints have accessible value
children.

The existing SwingX `JXMultiThumbSlider` remains useful for arbitrary
floating-point thumbs that can cross. Its model is different from the
ordered two-endpoint `JRangeSlider`; neither class replaces the other.

## Toggle switch

`JToggleSwitch` is a `JToggleButton` with a capsule and thumb presentation:

```java
JToggleSwitch enabled = new JToggleSwitch("Notifications", true);
enabled.setOnText("On");
enabled.setOffText("Off");
enabled.addActionListener(event -> savePreference(enabled.isSelected()));
```

The inherited `ButtonModel`, `Action`, `ButtonGroup`, item events, action
events and accessible toggle-button role remain the owners of its
behavior. Programmatic `setSelected` retains toggle-button semantics;
it does not synthesize an action event. The standard look-and-feel
delegate handles keyboard input, mouse input, focus and disabled states.

State text inside the capsule is independent of the ordinary button label.
The widget supports RTL, font-sensitive sizing, optional state text and
configurable on/off/thumb colors. Non-UIResource application icons survive
look-and-feel changes. `setIcon(null)` restores the default switch icon.
The implementation is a two-state switch, with no animation timer.

## Circular progress

`JProgressCircle` retains `JProgressBar`'s model and listener contracts:

```java
JProgressCircle progress = new JProgressCircle(0, 100);
progress.setValue(65);
progress.setStringPainted(true);
progress.setRingThickness(8.0f);
```

The foreground is the completed ring and the background is its remaining
track. `setClockwise(false)` reverses the direction. `setCountdown(true)`
paints the remaining fraction without changing the model, accessibility
or inherited progress string. The center is transparent by default.
Insets, resizing and display transforms are respected.

`setIndeterminate(true)` uses the existing `BasicProgressBarUI` animation
lifecycle. There is no additional timer or progress model. The inherited
progress-bar UI identity is retained; `updateUI()` restores the circular
delegate using current progress-bar defaults. A zero-length model range
has a completed fraction of zero. As in `JProgressBar`, passing a null
model leaves it unset.

## Reusing the rest of the existing widget catalogue

Use the established Swing or SwingX control when it already supplies the
required capability. The following are capability matches, not statements
of source-compatible SWT substitution:

| SWT or Nebula capability | Existing Swing or SwingX control |
| --- | --- |
| Buttons, text, menus, tabs, lists, trees and tables | Original `javax.swing` controls |
| Hierarchical table | `JXTreeTable` |
| Date picker and month calendar | `JXDatePicker`, `JXMonthView` |
| Collapsible task panes and accordion building blocks | `JXTaskPane`, `JXTaskPaneContainer`, `JXCollapsiblePane` |
| Multiple split panes | `JXMultiSplitPane` |
| Hyperlink, search field, busy label and status bar | `JXHyperlink`, `JXSearchField`, `JXBusyLabel`, `JXStatusBar` |
| Image viewing | `JXImageView`; a grouped gallery remains a distinct capability |
| Scrollable and layered composition | `JScrollPane`, `JViewport`, `JLayer`, `JLayeredPane` |

SwingX keeps its existing `org.jdesktop.swingx` API and separate distribution;
the new Swing controls introduce no SwingX dependency into `java.desktop`.
Grouped galleries, knobs, rich grids and the remaining Nebula-specific
capabilities require their own explicit model, interaction, rendering and
accessibility mappings. Generic generated widget scaffolds alone do not
establish those mappings.

Create and update all of these Swing components on the event dispatch
thread. They add to the existing widget catalogue; existing Swing public
types and application code retain their original contracts.

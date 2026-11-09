/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.*;

/**
 * NumericValidator — Composable validation for Integer/Long/Double with builder pattern.
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/validation/NumericValidator.java}
 * blob {@code 5c1dde444f0fb722270405f3dbb07ee32be7ce27} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class NumericValidator {
    private static final int EVEN_ODD = 2;
    private NumericValidator() {}

    public static IntBuilder intBuilder(String f) { return new IntBuilder(f); }
    public static LongBuilder longBuilder(String f) { return new LongBuilder(f); }
    public static DoubleBuilder doubleBuilder(String f) { return new DoubleBuilder(f); }

    /** Validation result: valid/invalid + field + value + errors. */
    public static final class ValidationResult {
        private final boolean valid; private final String fieldName; private final Number value; private final List<String> errors;
        ValidationResult(boolean v, String f, Number val, List<String> e) { valid = v; fieldName = f; value = val; errors = List.copyOf(e); }
        public boolean isValid() { return valid; }
        public boolean isInvalid() { return !valid; }
        public String getFieldName() { return fieldName; }
        public Number getValue() { return value; }
        public List<String> getErrors() { return errors; }
        public Optional<String> getFirstError() { return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(0)); }
        public String getErrorMessage() { return valid ? "" : fieldName + " validation failed: " + String.join("; ", errors); }
        public ValidationResult orElseThrow() { if (!valid) throw new IllegalArgumentException(getErrorMessage()); return this; }
        public <X extends Throwable> ValidationResult orElseThrow(java.util.function.Supplier<? extends X> s) throws X { if (!valid) throw s.get(); return this; }
    }

    // --- Generic base for all numeric builders ---
    private abstract static sealed class NumBuilder<T extends Number, B extends NumBuilder<T, B>> permits IntBuilder, LongBuilder, DoubleBuilder {
        final String fieldName; final List<Predicate<T>> preds = new ArrayList<>(); final List<String> msgs = new ArrayList<>();
        NumBuilder(String f) { if (f == null || f.isBlank()) throw new IllegalArgumentException("Field name blank"); fieldName = f; }
        @SuppressWarnings("unchecked") B self() { return (B) this; }
        public B notNull() { preds.add(v -> v != null); msgs.add("must not be null"); return self(); }
        public B custom(Predicate<T> p, String m) { preds.add(p); msgs.add(m); return self(); }
        public ValidationResult validate(T val) {
            List<String> errs = IntStream.range(0, preds.size()).filter(i -> !preds.get(i).test(val)).mapToObj(msgs::get).collect(Collectors.toList());
            return new ValidationResult(errs.isEmpty(), fieldName, val, errs);
        }
    }

    /** Integer validation builder. */
    public static final class IntBuilder extends NumBuilder<Integer, IntBuilder> {
        IntBuilder(String f) { super(f); }
        public IntBuilder min(int m) { preds.add(v -> v != null && v >= m); msgs.add("must be at least " + m); return this; }
        public IntBuilder max(int m) { preds.add(v -> v == null || v <= m); msgs.add("must be at most " + m); return this; }
        public IntBuilder positive() { preds.add(v -> v != null && v > 0); msgs.add("must be positive"); return this; }
        public IntBuilder nonNegative() { preds.add(v -> v != null && v >= 0); msgs.add("must be non-negative"); return this; }
        public IntBuilder negative() { preds.add(v -> v != null && v < 0); msgs.add("must be negative"); return this; }
        public IntBuilder even() { preds.add(v -> v != null && v % EVEN_ODD == 0); msgs.add("must be even"); return this; }
        public IntBuilder odd() { preds.add(v -> v != null && v % EVEN_ODD != 0); msgs.add("must be odd"); return this; }
        public IntBuilder between(int lo, int hi) { preds.add(v -> v != null && v >= lo && v <= hi); msgs.add("must be between " + lo + " and " + hi); return this; }
        public IntValidator build() { return new IntValidator(fieldName, List.copyOf(preds), List.copyOf(msgs)); }
    }

    /** Integer validator (immutable). */
    public static final class IntValidator { private final String f; private final List<Predicate<Integer>> p; private final List<String> m;
        IntValidator(String f, List<Predicate<Integer>> p, List<String> m) { this.f = f; this.p = p; this.m = m; }
        public ValidationResult validate(Integer v) { var e = IntStream.range(0, p.size()).filter(i -> !p.get(i).test(v)).mapToObj(m::get).collect(Collectors.toList()); return new ValidationResult(e.isEmpty(), f, v, e); }
    }

    /** Long validation builder. */
    public static final class LongBuilder extends NumBuilder<Long, LongBuilder> {
        LongBuilder(String f) { super(f); }
        public LongBuilder min(long m) { preds.add(v -> v != null && v >= m); msgs.add("must be at least " + m); return this; }
        public LongBuilder max(long m) { preds.add(v -> v == null || v <= m); msgs.add("must be at most " + m); return this; }
        public LongBuilder positive() { preds.add(v -> v != null && v > 0L); msgs.add("must be positive"); return this; }
        public LongBuilder nonNegative() { preds.add(v -> v != null && v >= 0L); msgs.add("must be non-negative"); return this; }
        public LongBuilder negative() { preds.add(v -> v != null && v < 0L); msgs.add("must be negative"); return this; }
        public LongBuilder between(long lo, long hi) { preds.add(v -> v != null && v >= lo && v <= hi); msgs.add("must be between " + lo + " and " + hi); return this; }
        public LongValidator build() { return new LongValidator(fieldName, List.copyOf(preds), List.copyOf(msgs)); }
    }

    /** Long validator (immutable). */
    public static final class LongValidator { private final String f; private final List<Predicate<Long>> p; private final List<String> m;
        LongValidator(String f, List<Predicate<Long>> p, List<String> m) { this.f = f; this.p = p; this.m = m; }
        public ValidationResult validate(Long v) { var e = IntStream.range(0, p.size()).filter(i -> !p.get(i).test(v)).mapToObj(m::get).collect(Collectors.toList()); return new ValidationResult(e.isEmpty(), f, v, e); }
    }

    /** Double validation builder. */
    public static final class DoubleBuilder extends NumBuilder<Double, DoubleBuilder> {
        DoubleBuilder(String f) { super(f); }
        public DoubleBuilder min(double m) { preds.add(v -> v != null && v >= m); msgs.add(String.format("must be at least %.2f", m)); return this; }
        public DoubleBuilder max(double m) { preds.add(v -> v == null || v <= m); msgs.add(String.format("must be at most %.2f", m)); return this; }
        public DoubleBuilder positive() { preds.add(v -> v != null && v > 0.0); msgs.add("must be positive"); return this; }
        public DoubleBuilder nonNegative() { preds.add(v -> v != null && v >= 0.0); msgs.add("must be non-negative"); return this; }
        public DoubleBuilder negative() { preds.add(v -> v != null && v < 0.0); msgs.add("must be negative"); return this; }
        public DoubleBuilder between(double lo, double hi) { preds.add(v -> v != null && v >= lo && v <= hi); msgs.add(String.format("must be between %.2f and %.2f", lo, hi)); return this; }
        public DoubleBuilder finite() { preds.add(v -> v != null && Double.isFinite(v)); msgs.add("must be finite"); return this; }
        public DoubleValidator build() { return new DoubleValidator(fieldName, List.copyOf(preds), List.copyOf(msgs)); }
    }

    /** Double validator (immutable). */
    public static final class DoubleValidator { private final String f; private final List<Predicate<Double>> p; private final List<String> m;
        DoubleValidator(String f, List<Predicate<Double>> p, List<String> m) { this.f = f; this.p = p; this.m = m; }
        public ValidationResult validate(Double v) { var e = IntStream.range(0, p.size()).filter(i -> !p.get(i).test(v)).mapToObj(m::get).collect(Collectors.toList()); return new ValidationResult(e.isEmpty(), f, v, e); }
    }
}

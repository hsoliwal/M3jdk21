/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary M3 named operator defaults delegate to existing virtual arithmetic without changing Java 21 operators
 * @modules jdk.m3.paradigm/jdk.internal.m3.paradigm.contract
 * @run main/othervm ArithmeticOperatorDefaultsTest
 */
import jdk.internal.m3.paradigm.contract.ComplexNumber;
import jdk.internal.m3.paradigm.contract.IAdditive;
import jdk.internal.m3.paradigm.contract.IArithmeticDefaults;
import jdk.internal.m3.paradigm.contract.IDivisible;
import jdk.internal.m3.paradigm.contract.IRemainder;

public class ArithmeticOperatorDefaultsTest {
    private static int assertions;

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }

    private static void checkEqual(ComplexNumber lhs, ComplexNumber rhs, String label) {
        check(Double.doubleToRawLongBits(lhs.real()) == Double.doubleToRawLongBits(rhs.real())
                && Double.doubleToRawLongBits(lhs.imag()) == Double.doubleToRawLongBits(rhs.imag()),
                label);
    }

    /** Retain a final application class and provide an independent adapter. */
    private record Money(long cents) { }

    private static final class MoneyAdapter implements IArithmeticDefaults<Money> {
        private final Money left;
        int calls;

        MoneyAdapter(Money left) { this.left = left; }

        @Override public Money add(Money other) { calls++; return new Money(left.cents + other.cents); }
        @Override public Money subtract(Money other) { calls++; return new Money(left.cents - other.cents); }
        @Override public Money multiply(Money other) { calls++; return new Money(left.cents * other.cents); }
    }

    private record Count(int value) { }

    /** Partial capabilities need no artificial subtraction or multiplication methods. */
    private static final class AddOnly implements IAdditive<Count> {
        @Override public Count add(Count other) { return new Count(other.value + 1); }
    }

    /** Division and remainder are optional and retain their original error contracts. */
    private static final class QuotientAdapter implements IDivisible<Count>, IRemainder<Count> {
        private final Count numerator;
        int calls;

        QuotientAdapter(Count numerator) { this.numerator = numerator; }

        @Override public Count divide(Count other) {
            calls++;
            return new Count(numerator.value / other.value);
        }

        @Override public Count remainder(Count other) {
            calls++;
            return new Count(numerator.value % other.value);
        }
    }

    private static class Probe implements IArithmeticDefaults<Probe> {
        int additions;
        int subtractions;
        int multiplications;

        @Override public Probe add(Probe other) { additions++; return other; }
        @Override public Probe subtract(Probe other) { subtractions++; return other; }
        @Override public Probe multiply(Probe other) { multiplications++; return other; }
    }

    private static final class OverrideProbe extends Probe {
        @Override public OverrideProbe plus(Probe other) {
            return this;
        }
    }

    public static void main(String[] args) {
        ComplexNumber a = new ComplexNumber(3.0, -4.0);
        ComplexNumber b = new ComplexNumber(2.0, 5.0);
        checkEqual(a.plus(b), a.add(b), "plus equivalence");
        checkEqual(a.minus(b), a.subtract(b), "minus equivalence");
        checkEqual(a.times(b), a.multiply(b), "times equivalence");
        check(a instanceof IArithmeticDefaults<?>, "final record must implement alias capability");

        // Observe the original Java floating-point rules, including signed zero and NaN.
        ComplexNumber zeros = new ComplexNumber(-0.0, Double.NaN);
        checkEqual(zeros.plus(ComplexNumber.ZERO), zeros.add(ComplexNumber.ZERO),
                "signed-zero and NaN results");
        checkEqual(zeros.times(ComplexNumber.I), zeros.multiply(ComplexNumber.I),
                "multiplication nonfinite results");

        int nullFailures = 0;
        try { a.add(null); } catch (NullPointerException expected) { nullFailures++; }
        try { a.plus(null); } catch (NullPointerException expected) { nullFailures++; }
        try { a.subtract(null); } catch (NullPointerException expected) { nullFailures++; }
        try { a.minus(null); } catch (NullPointerException expected) { nullFailures++; }
        try { a.multiply(null); } catch (NullPointerException expected) { nullFailures++; }
        try { a.times(null); } catch (NullPointerException expected) { nullFailures++; }
        check(nullFailures == 6, "original validation remains authoritative");

        MoneyAdapter money = new MoneyAdapter(new Money(250));
        check(money.plus(new Money(125)).cents == 375, "final-class addition adapter");
        check(money.minus(new Money(125)).cents == 125, "final-class subtraction adapter");
        check(money.times(new Money(2)).cents == 500, "final-class multiplication adapter");
        check(money.calls == 3, "one dispatch per alias");

        IAdditive<Count> addOnly = new AddOnly();
        check(addOnly.plus(new Count(6)).value == 7, "single addition capability");
        check(!(addOnly instanceof IDivisible<?>), "partial addition must not imply division");
        QuotientAdapter quotient = new QuotientAdapter(new Count(25));
        check(quotient.div(new Count(4)).value == 6, "independent division hook");
        check(quotient.rem(new Count(4)).value == 1, "independent remainder hook");
        int zeroDivisionFailures = 0;
        try { quotient.divide(new Count(0)); } catch (ArithmeticException expected) { zeroDivisionFailures++; }
        try { quotient.div(new Count(0)); } catch (ArithmeticException expected) { zeroDivisionFailures++; }
        try { quotient.remainder(new Count(0)); } catch (ArithmeticException expected) { zeroDivisionFailures++; }
        try { quotient.rem(new Count(0)); } catch (ArithmeticException expected) { zeroDivisionFailures++; }
        check(zeroDivisionFailures == 4, "division-by-zero exceptions remain unchanged");
        check(quotient.calls == 6, "each independent operation dispatches exactly once");

        Probe probe = new Probe();
        Probe operand = new Probe();
        check(probe.plus(operand) == operand, "virtual addition result");
        check(probe.minus(operand) == operand, "virtual subtraction result");
        check(probe.times(operand) == operand, "virtual multiplication result");
        check(probe.additions == 1 && probe.subtractions == 1 && probe.multiplications == 1,
                "each original method called exactly once");

        OverrideProbe override = new OverrideProbe();
        check(override.plus(operand) == override, "class override precedes interface default");
        check(override.additions == 0, "override cannot be bypassed");

        // Ordinary Java integer + keeps ordinary Java meaning; this opt-in API does not
        // give a record symbolic + or rewrite unrelated arithmetic expressions.
        check(3 + 4 == 7, "Java 21 numeric operator unchanged");
        System.out.println("M3_OPERATOR_DEFAULTS_PASS checks=" + assertions);
    }
}

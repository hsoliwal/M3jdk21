// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime.fixture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** One method per escape kind; method names are asserted by the test. */
public class ItemUsers {
    FakeItem kept;
    final List<FakeItem> list = new ArrayList<>();

    public String behaviour(FakeTable t) {
        FakeItem item = t.getItem(3);
        item.setChecked(true);
        return "x" + item.getText() + t.indexOfItem(item);
    }

    public int behaviourLoop(FakeTable t) {
        int n = 0;
        for (FakeItem item : t.getItems()) n += item.getText().length();
        return n;
    }

    public void field(FakeTable t) {
        kept = t.getItem(1);
    }

    public void collection(FakeTable t) {
        list.add(t.getItem(2));
    }

    public FakeItem returned(FakeTable t) {
        return t.getItem(4);
    }

    public boolean identity(FakeTable t, FakeItem other) {
        return t.getItem(5) == other;
    }

    public Supplier<String> lambda(FakeTable t) {
        FakeItem item = t.getItem(6);
        return () -> item.getText();
    }

    public void retainsCallee(FakeTable t) {
        t.remember(t.getItem(7));
    }

    public void unknownArg(FakeTable t) {
        Object o = t.getItem(8);
        consume(o);
    }

    public void arrayStore(FakeTable t, FakeItem[] out) {
        out[0] = t.getItem(9);
    }

    public int hashed(FakeTable t) {
        return t.getItem(10).hashCode();
    }

    public String constructed() {
        return new FakeItem().getText();
    }

    static void consume(Object o) {
    }

    Object keptObject;

    public void requireAlias(FakeTable t) {
        keptObject = java.util.Objects.requireNonNull(t.getItem(1));
    }

    public void requireMessageAlias(FakeTable t) {
        keptObject = java.util.Objects.requireNonNull(t.getItem(1), "message");
    }

    public void requireSupplierAlias(FakeTable t) {
        keptObject = java.util.Objects.requireNonNull(t.getItem(1), () -> "message");
    }

    public Object requireReturnAlias(FakeTable t) {
        return java.util.Objects.requireNonNull(t.getItem(1));
    }

    public void requireCastAlias(FakeTable t) {
        Object alias = java.util.Objects.requireNonNull(t.getItem(1));
        FakeItem local = (FakeItem) alias;
        kept = local;
    }

    public String requireBehaviour(FakeTable t) {
        return java.util.Objects.requireNonNull(t.getItem(1)).getText();
    }

    @com.m3.residency.M3BehaviourOnly
    static void overloaded(String text) {
    }

    static void overloaded(FakeItem item) {
        external = item;
    }

    @com.m3.residency.M3Retains
    static void retainingOverload(String text) {
    }

    static void retainingOverload(FakeItem item) {
    }

    static Object external;

    public void overloadCall(FakeTable t) {
        overloaded(t.getItem(1));
    }

    public void retainingOverloadCall(FakeTable t) {
        retainingOverload(t.getItem(1));
    }

    public void differentNullHelper(FakeTable t) {
        keptObject = java.util.Objects.requireNonNullElse(t.getItem(1), new Object());
    }
}

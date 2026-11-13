/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Runs against the actual M3 image; no patched runtime, WhiteBox, or assertions flag. */
public final class M3ObjectBaseVtableContract {
    private M3ObjectBaseVtableContract() { }
    @FunctionalInterface private interface Action { void run() throws Throwable; }
    private interface Adapter { <T> T toAdapter(Class<T> type); }
    private interface ChildAdapter extends Adapter { }
    private interface Bare { }
    private static final class BareReceiver implements Bare { }
    private static final class Token { }

    private static class Base implements Cloneable, ChildAdapter {
        @Override public Object clone() throws CloneNotSupportedException { return super.clone(); }
        @SuppressWarnings("removal")
        final void finalizeForTest() throws Throwable { finalize(); }
    }
    private static final class Receiver extends Base {
        final Token token = new Token();
        int equalsCalls, cloneCalls, finalizeCalls;
        @Override public int hashCode() { return 173; }
        @Override public boolean equals(Object other) { equalsCalls++; return this == other; }
        @Override public String toString() { return "receiver"; }
        @Override public Object clone() throws CloneNotSupportedException {
            cloneCalls++; return super.clone();
        }
        @Override @SuppressWarnings("removal")
        protected void finalize() { finalizeCalls++; }
        @Override public <T> T toAdapter(Class<T> type) {
            return type == Token.class ? type.cast(token) : super.toAdapter(type);
        }
    }
    private static final class Unclonable {
        Object cloneObject() throws CloneNotSupportedException { return super.clone(); }
    }
    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
    private static void fails(Class<? extends Throwable> type, Action action, String label) throws Throwable {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(label + ": unexpected exception", failure);
        }
        throw new AssertionError(label + ": missing " + type.getName());
    }

    private static void adaptersAndVirtuals() throws Throwable {
        Object plain = new Object();
        require(plain.toAdapter(Object.class) == plain, "default identity");
        require(plain.toAdapter(String.class) == null, "default wrong type");
        require(plain.toAdapter(int.class) == null && plain.toAdapter(void.class) == null, "primitive types");
        require(Integer.valueOf(7).toAdapter(int.class) == null, "primitive is not wrapper");
        fails(NullPointerException.class, () -> plain.toAdapter(null), "null adapter class");
        require(plain.hashCode() == System.identityHashCode(plain), "native identity hash");
        require(plain.equals(plain) && !plain.equals(new Object()) && !plain.equals(null), "Object equality");
        require(plain.toString().equals("java.lang.Object@" + Integer.toHexString(plain.hashCode())), "Object text");

        Receiver receiver = new Receiver();
        Object erased = receiver;
        require(erased.toAdapter(Token.class) == receiver.token, "Object receiver override dispatch");
        require(erased.toAdapter(Object.class) == receiver, "override super fallback");
        fails(NullPointerException.class, () -> erased.toAdapter(null), "override null fallback");
        ChildAdapter overridden = receiver;
        require(overridden.toAdapter(Token.class) == receiver.token, "redeclared interface override dispatch");
        ChildAdapter inherited = new Base();
        require(inherited.toAdapter(ChildAdapter.class) == inherited, "interface uses inherited Object method");
        Bare bare = new BareReceiver();
        require(bare.toAdapter(Bare.class) == bare, "implicit Object method on interface");
        require(erased.hashCode() == 173 && erased.toString().equals("receiver"), "hash and text dispatch");
        require(erased.equals(receiver) && receiver.equalsCalls == 1, "equals dispatch");
        Base base = receiver;
        Receiver copy = (Receiver) base.clone();
        require(copy != receiver && copy.getClass() == Receiver.class, "clone type and identity");
        require(receiver.cloneCalls == 1 && copy.token == receiver.token, "clone dispatch and shallow payload");
        base.finalizeForTest();
        require(receiver.finalizeCalls == 1, "explicit finalize dispatch");
        fails(CloneNotSupportedException.class, () -> new Unclonable().cloneObject(), "non-Cloneable rejection");
    }

    private static void arrays() {
        Object[] references = {new Object(), null};
        int[] integers = {1, 2};
        Object[] kinds = {new boolean[1], new byte[1], new char[1], new short[1], integers,
                new long[1], new float[1], new double[1], references, new Object[1][1]};
        for (Object array : kinds) {
            require(array.toAdapter(Object.class) == array, "array Object adapter");
            require(array.toAdapter(array.getClass()) == array, "array exact type adapter");
            require(array.toAdapter(Cloneable.class) == array, "array Cloneable adapter");
            require(array.toAdapter(Serializable.class) == array, "array Serializable adapter");
            require(array.toAdapter(String.class) == null, "array wrong type");
            require(array.hashCode() == System.identityHashCode(array) && array.equals(array), "array Object dispatch");
        }
        require(references.clone() != references && references.clone()[0] == references[0], "reference array clone");
        require(integers.clone() != integers && integers.clone()[1] == 2, "primitive array clone");
    }

    private static void timeoutsAndMonitorChecks() throws Throwable {
        Object monitor = new Object();
        fails(IllegalMonitorStateException.class, () -> monitor.wait(0L), "wait requires monitor");
        fails(IllegalMonitorStateException.class, monitor::notify, "notify requires monitor");
        fails(IllegalMonitorStateException.class, monitor::notifyAll, "notifyAll requires monitor");
        synchronized (monitor) {
            fails(IllegalArgumentException.class, () -> monitor.wait(-1L), "negative milliseconds");
            fails(IllegalArgumentException.class, () -> monitor.wait(0L, -1), "negative nanoseconds");
            fails(IllegalArgumentException.class, () -> monitor.wait(0L, 1_000_000), "excess nanoseconds");
            monitor.wait(1L);
            monitor.wait(0L, 1);
            monitor.notify();
            monitor.notifyAll();
        }
    }

    private static void zeroWaitAndInterrupt(boolean virtual) throws InterruptedException {
        Object monitor = new Object();
        boolean[] released = {false};
        CountDownLatch entered = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicReference<Throwable> problem = new AtomicReference<>();
        Runnable action = () -> {
            try {
                synchronized (monitor) {
                    entered.countDown();
                    while (!released[0]) monitor.wait(0L);
                    Thread.currentThread().interrupt();
                    fails(InterruptedException.class, monitor::wait, "interrupted zero wait");
                    require(!Thread.currentThread().isInterrupted(), "interrupt status cleared");
                }
            } catch (Throwable failure) { problem.set(failure); }
            finally { done.countDown(); }
        };
        Thread waiter = (virtual ? Thread.ofVirtual() : Thread.ofPlatform().daemon()).unstarted(action);
        waiter.start();
        try {
            require(entered.await(5, TimeUnit.SECONDS), "waiter entered, virtual=" + virtual);
            Thread.ofPlatform().daemon().start(() -> {
                synchronized (monitor) { released[0] = true; monitor.notify(); monitor.notifyAll(); }
            });
            require(done.await(5, TimeUnit.SECONDS), "waiter completed, virtual=" + virtual);
            if (problem.get() != null) throw new AssertionError("waiter failed, virtual=" + virtual, problem.get());
        } finally { waiter.interrupt(); }
    }

    public static void main(String[] args) throws Throwable {
        adaptersAndVirtuals();
        arrays();
        timeoutsAndMonitorChecks();
        zeroWaitAndInterrupt(false);
        zeroWaitAndInterrupt(true);
        System.out.println("PASS M3ObjectBaseVtableContract adapters arrays virtuals waits platform virtual");
    }
}

// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3Class direct getters preserve Optional payload and frozen-row contracts
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xint M3ClassOptionalTest
 * @run main/othervm M3ClassOptionalTest
 */

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jdk.internal.mindex.M3CI;
import jdk.internal.mindex.M3Class;
import jdk.internal.mindex.M3VI;

/** Contract oracle using the real frozen metadata owner and unchanged Optional APIs. */
public final class M3ClassOptionalTest {
    private static long checks;

    private M3ClassOptionalTest() { }

    interface Marker { }
    static class Base { }
    static final class Child extends Base implements Marker { }
    static final class Container {
        final class Inner implements Marker { }
    }
    enum Tone { DARK, LIGHT }
    record Pair(int id, String label) implements Marker { }
    sealed interface Shape permits Circle, Polygon { }
    record Circle(int radius) implements Shape { }
    static non-sealed class Polygon implements Shape { }
    static final class Square extends Polygon { }

    private record Snapshot(M3Class view, M3CI owner, int row, String binaryName,
            String canonicalName, Optional<M3Class> superclass,
            Optional<M3Class> componentType, Optional<M3Class> nestHost,
            Optional<M3Class> declaringClass, Optional<M3Class> enclosingClass) {
        static Snapshot capture(M3Class view) {
            return new Snapshot(view, view.owner(), view.row(), view.name(),
                    view.canonicalName().orElse(null), view.superclass(),
                    view.componentType(), view.nestHost(), view.declaringClass(),
                    view.enclosingClass());
        }
    }

    public static void main(String[] args) {
        class Local extends Base implements Marker { }
        Marker anonymous = new Marker() { };
        List<Class<?>> roots = List.of(M3ClassOptionalTest.class, Base.class,
                Child.class, Container.Inner.class, Local.class,
                anonymous.getClass(), Marker.class, Pair.class, Tone.class,
                Shape.class, Circle.class, Square.class, int.class, long.class,
                boolean.class, void.class, int[].class, Child[].class,
                String[][].class, Optional.class);

        M3CI standard = M3CI.compile(roots, M3CI.Options.standard(), M3VI.Progress.none());
        M3CI compact = M3CI.compile(roots, M3CI.Options.compact(), M3VI.Progress.none());
        check(standard != compact, "separate immutable owners");
        check(standard.size() == compact.size(), "boundary policy preserves class rows");
        check(standard.boundary().retained(), "standard boundary retained");
        check(!compact.boundary().retained(), "compact boundary omitted");
        check(compact.boundary().retainedReferenceCount() == 0,
                "compact snapshot has no WeakReference objects");

        List<Snapshot> retainedStandard = verifyAllRows(standard, true);
        List<Snapshot> retainedCompact = verifyAllRows(compact, false);
        // Exercise different rows and owners before rereading previously returned views.
        // A mutable recycled facade or cross-owner cache would corrupt these observations.
        for (int pass = 0; pass < 4; pass++) {
            verifyRetained(retainedCompact);
            verifyRetained(retainedStandard);
        }
        Reference.reachabilityFence(roots);
        Reference.reachabilityFence(anonymous);
        System.out.println("PASS M3ClassOptionalTest checks=" + checks
                + " rowsPerSnapshot=" + standard.size() + " snapshots=2");
    }

    private static List<Snapshot> verifyAllRows(M3CI index, boolean retainedBoundary) {
        List<Snapshot> snapshots = new ArrayList<>();
        boolean absentCanonical = false;
        boolean presentCanonical = false;
        boolean absentSuperclass = false;
        boolean presentSuperclass = false;
        boolean absentComponent = false;
        boolean presentComponent = false;
        boolean absentDeclaring = false;
        boolean presentDeclaring = false;
        boolean absentEnclosing = false;
        boolean presentEnclosing = false;
        boolean local = false;
        boolean anonymous = false;
        boolean record = false;
        boolean sealed = false;
        boolean primitive = false;
        boolean array = false;
        boolean iface = false;

        for (int row = 0; row < index.size(); row++) {
            M3Class view = index.classAt(row);
            Snapshot saved = Snapshot.capture(view);
            snapshots.add(saved);
            verifyGetters(saved);
            absentCanonical |= saved.canonicalName() == null;
            presentCanonical |= saved.canonicalName() != null;
            absentSuperclass |= saved.superclass().isEmpty();
            presentSuperclass |= saved.superclass().isPresent();
            absentComponent |= saved.componentType().isEmpty();
            presentComponent |= saved.componentType().isPresent();
            absentDeclaring |= saved.declaringClass().isEmpty();
            presentDeclaring |= saved.declaringClass().isPresent();
            absentEnclosing |= saved.enclosingClass().isEmpty();
            presentEnclosing |= saved.enclosingClass().isPresent();
            local |= view.isLocalClass();
            anonymous |= view.isAnonymousClass();
            record |= view.isRecord();
            sealed |= view.isSealed();
            primitive |= view.isPrimitive();
            array |= view.isArray();
            iface |= view.isInterface();

            if (retainedBoundary) {
                Class<?> type = view.tryClass().orElseThrow();
                check(Objects.equals(view.getCanonicalName(), type.getCanonicalName()),
                        "JVM canonical name: " + view.name());
                sameClass(view.getSuperclass(), type.getSuperclass(), "JVM superclass");
                sameClass(view.getComponentType(), type.getComponentType(), "JVM component");
                sameClass(view.getNestHost(), type.getNestHost(), "JVM nest host");
                sameClass(view.getDeclaringClass(), type.getDeclaringClass(), "JVM declaring");
                sameClass(view.getEnclosingClass(), type.getEnclosingClass(), "JVM enclosing");
            } else {
                check(view.tryClass().isEmpty(), "compact getters do not need JVM boundary");
            }
        }
        check(absentCanonical && presentCanonical, "canonical present/absent coverage");
        check(absentSuperclass && presentSuperclass, "superclass present/absent coverage");
        check(absentComponent && presentComponent, "component present/absent coverage");
        check(absentDeclaring && presentDeclaring, "declaring present/absent coverage");
        check(absentEnclosing && presentEnclosing, "enclosing present/absent coverage");
        check(local && anonymous && record && sealed && primitive && array && iface,
                "real JVM class-shape coverage");
        return snapshots;
    }

    private static void verifyRetained(List<Snapshot> snapshots) {
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            Snapshot saved = snapshots.get(i);
            check(saved.view().owner() == saved.owner(), "retained owner identity");
            check(saved.view().row() == saved.row(), "retained row identity");
            check(saved.view().name() == saved.binaryName(), "retained canonical String reference");
            verifyGetters(saved);
            sameRow(saved.superclass().orElse(null), saved.view().superclass().orElse(null),
                    saved.owner(), "retained Optional superclass");
            sameRow(saved.componentType().orElse(null), saved.view().componentType().orElse(null),
                    saved.owner(), "retained Optional component");
            sameRow(saved.nestHost().orElse(saved.view()), saved.view().nestHost().orElse(saved.view()),
                    saved.owner(), "retained Optional nest host");
            sameRow(saved.declaringClass().orElse(null), saved.view().declaringClass().orElse(null),
                    saved.owner(), "retained Optional declaring");
            sameRow(saved.enclosingClass().orElse(null), saved.view().enclosingClass().orElse(null),
                    saved.owner(), "retained Optional enclosing");
        }
    }

    private static void verifyGetters(Snapshot saved) {
        M3Class view = saved.view();
        check(view.getCanonicalName() == saved.canonicalName(),
                "canonical Optional payload identity: " + view.name());
        sameRow(view.getSuperclass(), saved.superclass().orElse(null),
                saved.owner(), "superclass");
        sameRow(view.getComponentType(), saved.componentType().orElse(null),
                saved.owner(), "component type");
        M3Class host = view.getNestHost();
        sameRow(host, saved.nestHost().orElse(view), saved.owner(), "nest host");
        if (saved.nestHost().isEmpty()) {
            check(host == view, "absent nest host returns exact receiver");
        }
        sameRow(view.getDeclaringClass(), saved.declaringClass().orElse(null),
                saved.owner(), "declaring class");
        sameRow(view.getEnclosingClass(), saved.enclosingClass().orElse(null),
                saved.owner(), "enclosing class");
    }

    private static void sameRow(M3Class actual, M3Class expected, M3CI owner, String label) {
        if (expected == null) {
            check(actual == null, label + " absent");
        } else {
            check(actual != null, label + " present");
            check(actual.owner() == owner && expected.owner() == owner,
                    label + " exact parent owner");
            check(actual.row() == expected.row(), label + " exact row");
            check(actual.name() == expected.name(), label + " canonical payload reference");
        }
    }

    private static void sameClass(M3Class actual, Class<?> expected, String label) {
        check((actual == null ? null : actual.tryClass().orElseThrow()) == expected, label);
    }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
}

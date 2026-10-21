/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Observable composition contract; no alternative scheduler or provider model. */
class MFilesFileTaskCompositionTest {
    @TempDir Path temp;

    @Test void compositionIsLazyAndRunsEachStageOnceInOrderPerApply() throws IOException {
        List<String> calls = new ArrayList<>();
        MFiles.FileTask<Integer> first = file -> {
            calls.add("first");
            return 7;
        };
        MFiles.FileTask<String> composed = first.andThen(value -> {
            calls.add("after:" + value);
            return "result:" + value;
        });
        assertTrue(calls.isEmpty());
        assertEquals("result:7", composed.apply(null));
        assertEquals(List.of("first", "after:7"), calls);
        assertEquals("result:7", composed.apply(null));
        assertEquals(List.of("first", "after:7", "first", "after:7"), calls);
    }

    @Test void nullAfterIsRejectedImmediatelyWithoutCallingFirst() {
        AtomicInteger calls = new AtomicInteger();
        MFiles.FileTask<String> first = file -> {
            calls.incrementAndGet();
            return "first";
        };
        assertThrows(NullPointerException.class, () -> first.andThen(null));
        assertEquals(0, calls.get());
    }

    @Test void nullFirstResultIsPassedToAfter() throws IOException {
        AtomicInteger afterCalls = new AtomicInteger();
        MFiles.FileTask<String> first = file -> null;
        MFiles.FileTask<Integer> composed = first.andThen(value -> {
            assertNull(value);
            afterCalls.incrementAndGet();
            return 12;
        });
        assertEquals(12, composed.apply(null));
        assertEquals(1, afterCalls.get());
    }

    @Test void nullFinalResultIsAllowed() throws IOException {
        MFiles.FileTask<String> first = file -> "first";
        MFiles.FileTask<Object> composed = first.andThen(value -> null);
        assertNull(composed.apply(null));
    }

    @Test void originalInputIsPassedUnchangedToFirst() throws IOException {
        Path path = Files.writeString(temp.resolve("identity.txt"), "content");
        try (Stream<MFile> entries = MFiles.from(path).stream()) {
            MFile input = entries.findFirst().orElseThrow();
            MFiles.FileTask<MFile> first = file -> {
                assertSame(input, file);
                return file;
            };
            assertSame(input, first.andThen(Function.identity()).apply(input));
        }
    }

    @Test void checkedFirstFailureRetainsIdentityAndSkipsAfter() {
        IOException expected = new IOException("original checked failure");
        AtomicInteger afterCalls = new AtomicInteger();
        MFiles.FileTask<String> first = file -> { throw expected; };
        MFiles.FileTask<Integer> composed = first.andThen(value -> afterCalls.incrementAndGet());
        assertSame(expected, assertThrows(IOException.class, () -> composed.apply(null)));
        assertEquals(0, afterCalls.get());
    }

    @Test void runtimeFirstFailureRetainsIdentityAndSkipsAfter() {
        IllegalStateException expected = new IllegalStateException("original runtime failure");
        AtomicInteger afterCalls = new AtomicInteger();
        MFiles.FileTask<String> first = file -> { throw expected; };
        MFiles.FileTask<Integer> composed = first.andThen(value -> afterCalls.incrementAndGet());
        assertSame(expected, assertThrows(IllegalStateException.class, () -> composed.apply(null)));
        assertEquals(0, afterCalls.get());
    }

    @Test void runtimeAfterFailureRetainsIdentityAndDoesNotRepeatFirst() {
        IllegalArgumentException expected = new IllegalArgumentException("after failure");
        AtomicInteger firstCalls = new AtomicInteger();
        MFiles.FileTask<Integer> first = file -> firstCalls.incrementAndGet();
        MFiles.FileTask<String> composed = first.andThen(value -> { throw expected; });
        assertSame(expected, assertThrows(IllegalArgumentException.class, () -> composed.apply(null)));
        assertEquals(1, firstCalls.get());
    }

    @Test void errorFromAfterRetainsIdentity() {
        AssertionError expected = new AssertionError("after error");
        MFiles.FileTask<String> first = file -> "first";
        MFiles.FileTask<Object> composed = first.andThen(value -> { throw expected; });
        assertSame(expected, assertThrows(AssertionError.class, () -> composed.apply(null)));
    }

    @Test void chainedCompositionPreservesDeclarationOrder() throws IOException {
        List<String> calls = new ArrayList<>();
        MFiles.FileTask<Integer> first = file -> { calls.add("first"); return 2; };
        MFiles.FileTask<String> composed = first.andThen(value -> {
            calls.add("middle");
            return value + 3;
        }).andThen(value -> {
            calls.add("last");
            return Integer.toString(value);
        });
        assertEquals("5", composed.apply(null));
        assertEquals(List.of("first", "middle", "last"), calls);
    }

    @Test void functionInputAndOutputVarianceAreAccepted() throws IOException {
        MFiles.FileTask<Integer> first = file -> 42;
        Function<Number, StringBuilder> after = value -> new StringBuilder(value.toString());
        MFiles.FileTask<CharSequence> composed = first.andThen(after);
        assertEquals("42", composed.apply(null).toString());
        MFiles.FileTask<Integer> methodReference = file -> 17;
        assertEquals("17", methodReference.andThen(Object::toString).apply(null));
    }

    @Test void publicSurfaceRetainsTheSameSingleAbstractMethod() throws ReflectiveOperationException {
        assertTrue(MFiles.FileTask.class.isAnnotationPresent(FunctionalInterface.class));
        var abstracts = Arrays.stream(MFiles.FileTask.class.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers())).toList();
        assertEquals(1, abstracts.size());
        var apply = abstracts.getFirst();
        assertEquals("apply", apply.getName());
        assertArrayEquals(new Class<?>[] {MFile.class}, apply.getParameterTypes());
        assertArrayEquals(new Class<?>[] {IOException.class}, apply.getExceptionTypes());
        var andThen = MFiles.FileTask.class.getMethod("andThen", Function.class);
        assertTrue(andThen.isDefault());
        assertEquals(MFiles.FileTask.class, andThen.getReturnType());
        assertEquals(1, andThen.getTypeParameters().length);
    }

    @Test void composedStagesStayOnTheAdmittedWorkerWithOneWorkerAndOneCredit() throws Exception {
        Path path = Files.writeString(temp.resolve("worker.txt"), "content");
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> firstThread = new AtomicReference<>();
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        MFiles.FileTask<String> first = file -> {
            assertNotSame(caller, Thread.currentThread());
            firstThread.set(Thread.currentThread());
            firstCalls.incrementAndGet();
            return file.name();
        };
        MFiles.FileTask<Integer> composed = first.andThen(name -> {
            assertSame(firstThread.get(), Thread.currentThread());
            afterCalls.incrementAndGet();
            return name.length();
        });
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            try (Stream<Integer> results = MFiles.from(path).mapParallel(executor, 1, composed)) {
                assertEquals(List.of("worker.txt".length()), results.toList());
            }
            assertEquals(7, executor.submit(() -> 7).get());
        }
        assertEquals(1, firstCalls.get());
        assertEquals(1, afterCalls.get());
    }
}

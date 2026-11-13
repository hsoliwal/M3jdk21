/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Extracts class and method hotness from a JFR recording. */
public final class M3JfrProfileReader {
    private M3JfrProfileReader() {
    }

    /** Reads supported JFR events; unknown events are ignored. */
    public static M3Profile read(Path recording) throws IOException {
        if (!Files.isRegularFile(recording) || Files.size(recording) == 0) {
            return M3Profile.empty();
        }
        Map<String, Long> loadRanks = new LinkedHashMap<>();
        Map<String, Long> classSamples = new LinkedHashMap<>();
        Map<String, Long> methodSamples = new LinkedHashMap<>();
        long ordinal = 0;
        try (RecordingFile file = new RecordingFile(recording)) {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                String eventName = event.getEventType().getName();
                if (eventName.equals("jdk.ClassLoad") || eventName.equals("jdk.ClassDefine")) {
                    RecordedClass loaded = firstClassField(event, "loadedClass", "definedClass");
                    if (loaded != null && loaded.getName() != null) {
                        loadRanks.putIfAbsent(loaded.getName(), ordinal++);
                    }
                }
                if (eventName.equals("jdk.ExecutionSample") || eventName.equals("jdk.NativeMethodSample")) {
                    sampleStack(event.getStackTrace(), classSamples, methodSamples);
                }
            }
        }
        return new M3Profile(loadRanks, classSamples, methodSamples);
    }

    private static RecordedClass firstClassField(RecordedEvent event, String... names) {
        for (String name : names) {
            if (event.hasField(name)) {
                try {
                    return event.getClass(name);
                } catch (RuntimeException ignored) {
                    // A vendor event with a differently typed field is ignored.
                }
            }
        }
        return null;
    }

    private static void sampleStack(RecordedStackTrace trace, Map<String, Long> classSamples,
                                    Map<String, Long> methodSamples) {
        if (trace == null) {
            return;
        }
        for (RecordedFrame frame : trace.getFrames()) {
            if (!frame.isJavaFrame()) {
                continue;
            }
            RecordedMethod method = frame.getMethod();
            if (method == null || method.getType() == null || method.getType().getName() == null) {
                continue;
            }
            String className = method.getType().getName();
            String symbol = className + "#" + method.getName() + method.getDescriptor();
            classSamples.merge(className, 1L, Math::addExact);
            methodSamples.merge(symbol, 1L, Math::addExact);
        }
    }
}

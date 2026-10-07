// SPDX-License-Identifier: Apache-2.0
package com.m3.pack.diagnostics;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;

/** Runtime proof of JFR recording/readback and the linked native diagnostic launcher. */
public final class M3DiagnosticsSmoke {
    private M3DiagnosticsSmoke() {}

    @Name("com.m3pack.Probe")
    static final class Probe extends Event {
        int marker = 21;
    }

    /** Write only the caller-provided recording and invoke jcmd against this process. */
    public static void main(String[] args) throws Exception {
        Path recording = Path.of(args[0]);
        try (Recording session = new Recording()) {
            session.enable("com.m3pack.Probe");
            session.start();
            new Probe().commit();
            session.stop();
            session.dump(recording);
        }
        long matches = RecordingFile.readAllEvents(recording).stream()
                .filter(event -> event.getEventType().getName().equals("com.m3pack.Probe"))
                .filter(event -> event.getInt("marker") == 21).count();
        if (matches != 1) throw new IllegalStateException("JFR readback mismatch: " + matches);
        Path jcmd = Path.of(System.getProperty("java.home"), "bin", "jcmd");
        Process nativeCommand = new ProcessBuilder(jcmd.toString(),
                Long.toString(ProcessHandle.current().pid()), "VM.version").inheritIO().start();
        if (!nativeCommand.waitFor(30, TimeUnit.SECONDS)) {
            nativeCommand.destroyForcibly();
            throw new IllegalStateException("jcmd timeout");
        }
        if (nativeCommand.exitValue() != 0) throw new IllegalStateException("jcmd failed");
        System.out.println("M3_DIAGNOSTICS_PASS events=1 native-jcmd=PASS");
    }
}

// SPDX-License-Identifier: Apache-2.0
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;

/** Exercises the selected image's JFR recording and reading path, not just module enumeration. */
public final class DiagnosticsSmoke {
    static final class Probe extends Event { int value = 21; }
    public static void main(String[] args) throws Exception {
        Path recording = Path.of(args[0]);
        try (Recording session = new Recording()) {
            session.enable(Probe.class);
            session.start();
            new Probe().commit();
            session.stop();
            session.dump(recording);
        }
        if (!Files.isRegularFile(recording)
                || RecordingFile.readAllEvents(recording).stream().noneMatch(event -> event.getInt("value") == 21)) {
            throw new AssertionError("JFR recording/readback failed");
        }
        System.out.println("PASS: linked diagnostics image recorded and read an event");
    }
}

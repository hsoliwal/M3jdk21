// SPDX-License-Identifier: Apache-2.0
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/** Real desktop/native launch proof under Xvfb; not a mock JavaFX facade. */
public final class JavaFxSmoke {
    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> Platform.setImplicitExit(false));
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                Button button = new Button("M3 Java 21");
                stage.setScene(new Scene(button, 320, 120));
                stage.show();
                if (!stage.isShowing() || button.snapshot(null, null).getWidth() <= 0) {
                    throw new AssertionError("JavaFX native scene was not materialized");
                }
                stage.close();
            } catch (Throwable problem) {
                failure.set(problem);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) throw new AssertionError("JavaFX launch timeout");
        Platform.exit();
        if (failure.get() != null) throw new AssertionError("JavaFX native launch failed", failure.get());
        String version = System.getProperty("javafx.runtime.version");
        if (version == null || !version.startsWith("21.")) throw new AssertionError("Unexpected JavaFX: " + version);
        System.out.println("PASS: JavaFX " + version + " control, snapshot and native toolkit launch");
    }
}

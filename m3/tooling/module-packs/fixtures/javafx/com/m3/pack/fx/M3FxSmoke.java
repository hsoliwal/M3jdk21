// SPDX-License-Identifier: Apache-2.0
package com.m3.pack.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/** Toolkit/native launch proof. A display or Xvfb is required; no fake headless success. */
public final class M3FxSmoke extends Application {
    /** JavaFX constructs the application. */
    public M3FxSmoke() {}

    @Override
    public void start(Stage stage) {
        Label label = new Label("M3 JavaFX image");
        stage.setScene(new Scene(label, 320, 100));
        stage.show();
        Platform.runLater(() -> {
            if (!stage.isShowing() || !label.getText().equals("M3 JavaFX image")) {
                throw new IllegalStateException("JavaFX render proof failed");
            }
            System.out.println("M3_JAVAFX_PASS toolkit=started control=shown");
            stage.close();
            Platform.exit();
        });
    }

    /** Launch the actual JavaFX toolkit. */
    public static void main(String[] args) {
        launch(args);
    }
}

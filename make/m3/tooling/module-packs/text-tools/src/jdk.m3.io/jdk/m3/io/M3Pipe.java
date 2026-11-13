// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.io;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/** Small argv-only entry point. Quote each | so that the caller's shell passes it to this program. */
public final class M3Pipe {
    private M3Pipe() { }

    /**
     * Syntax: --timeout-ms positive-integer -- command arguments '|' command arguments.
     * No glob, variable, quote, redirection or shell-script parsing is performed here.
     * A literal | command argument is available through M3ProcessPipeline's Java API, not this CLI.
     */
    public static M3ProcessPipeline.Request parse(String... args) {
        if (args.length < 4 || !args[0].equals("--timeout-ms") || !args[2].equals("--")) {
            throw new IllegalArgumentException("usage: --timeout-ms N -- command args '|' command args");
        }
        long millis;
        try { millis = Long.parseLong(args[1]); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("invalid timeout", e); }
        List<List<String>> commands = new ArrayList<>();
        List<String> stage = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            if (args[i].equals("|")) {
                if (stage.isEmpty()) throw new IllegalArgumentException("empty pipeline stage");
                commands.add(List.copyOf(stage));
                stage.clear();
            } else stage.add(args[i]);
        }
        if (stage.isEmpty()) throw new IllegalArgumentException("empty pipeline stage");
        commands.add(List.copyOf(stage));
        return M3ProcessPipeline.Request.inherited(commands, Duration.ofMillis(millis));
    }

    public static void main(String[] args) {
        int status;
        try { status = M3ProcessPipeline.run(parse(args)).pipefailExitCode(); }
        catch (IllegalArgumentException e) { System.err.println(e.getMessage()); status = 2; }
        catch (IOException e) { System.err.println("pipeline launch failed: " + e.getMessage()); status = 125; }
        catch (TimeoutException e) { System.err.println(e.getMessage()); status = 124; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); status = 130; }
        System.exit(status);
    }
}

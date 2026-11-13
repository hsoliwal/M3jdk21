/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * REACH gate, test side: with the JaCoCo agent attached ({@code output=none}), resets the execution data when a test
 * starts and writes what it collected to {@code <reach.dir>/<TestClass>__<display name>.exec} when it ends, so every
 * dynamic test has its own coverage. Registered through META-INF/services; talks to the agent through its public
 * {@code org.jacoco.agent.rt.RT} API by reflection, so the tests themselves need no JaCoCo.
 */
public final class ReachListener implements TestExecutionListener {

    private static final String DIR = "reach.dir";
    private Object agent;
    private Method data;

    @Override
    public void testPlanExecutionStarted(TestPlan plan) {
        try {
            agent = Class.forName("org.jacoco.agent.rt.RT").getMethod("getAgent").invoke(null);
            data = Class.forName("org.jacoco.agent.rt.IAgent").getMethod("getExecutionData", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("run with -javaagent:org.jacoco.agent-<v>-runtime.jar", e);
        }
    }

    @Override
    public void executionStarted(TestIdentifier id) {
        if (id.isTest()) {
            pull();
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (!id.isTest()) {
            return;
        }
        byte[] exec = pull();
        String uid = id.getUniqueId();
        int at = uid.indexOf("[class:");
        String cls = at < 0 ? "unknown" : uid.substring(at + "[class:".length(), uid.indexOf(']', at));
        String name = (cls + "__" + id.getDisplayName()).replaceAll("[^A-Za-z0-9_.()#,$-]", "_");
        try {
            Files.write(Path.of(System.getProperty(DIR)).resolve(name + ".exec"), exec);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] pull() {
        try {
            return (byte[]) data.invoke(agent, true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

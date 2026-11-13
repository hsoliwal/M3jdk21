/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * LIB_INTAKE generic smoke: loads every class of the intake module inside the linked image.
 *
 * <p>Classes are loaded without static initialization, so a relocated reference to a class the
 * image lacks fails here. Prints the failures first, then {@code loaded=N failed=F}. Usage:
 * {@code image/bin/java --add-modules <module> LoadAll.java <class list>}.
 */
public final class LoadAll {
    private LoadAll() {
    }

    /**
     * Loads every class named in the list file and prints the counts.
     *
     * @param args one argument: the file listing one binary class name per line
     * @throws Exception if the class list cannot be read
     */
    public static void main(String[] args) throws Exception {
        List<String> names = Files.readAllLines(Path.of(args[0]));
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        long failed = names.stream().filter(n -> !loads(n, loader)).count();
        System.out.println("loaded=" + (names.size() - failed) + " failed=" + failed);
    }

    private static boolean loads(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader).getDeclaredMethods();
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            System.out.println("FAILED " + name + " " + e);
            return false;
        }
    }
}

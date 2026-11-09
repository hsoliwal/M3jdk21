/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Qualification-only use of the installed OpenJDK compiler's JNIWriter.
 * The compiler implementation is used through its actual API, not copied here.
 */
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.jvm.JNIWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public final class HeaderFromInstalledClasses {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, null)) {
            Path output = Path.of(args[0]);
            files.setLocationFromPaths(StandardLocation.NATIVE_HEADER_OUTPUT, List.of(output));
            JavaFileObject seed = new SimpleJavaFileObject(URI.create("string:///HeaderSeed.java"),
                    JavaFileObject.Kind.SOURCE) {
                public CharSequence getCharContent(boolean ignored) { return "class HeaderSeed {}"; }
            };
            JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, files, null,
                    List.of("-proc:none"), null, List.of(seed));
            task.parse();
            task.analyze();
            var elements = task.getElements();
            var module = elements.getModuleElement("java.base");
            JNIWriter writer = JNIWriter.instance(task.getContext());
            int written = 0;
            for (String header : Files.readAllLines(Path.of(args[1]))) {
                String name = header.substring(0, header.length() - 2).replace('_', '.');
                var type = elements.getTypeElement(module, name);
                if (type == null) throw new IllegalArgumentException("missing installed class " + name);
                writer.write((ClassSymbol) type);
                written++;
            }
            System.out.println("GENERATED_INSTALLED_HEADERS count=" + written);
        }
    }
}

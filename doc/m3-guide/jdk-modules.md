<!-- SPDX-License-Identifier: Apache-2.0 -->

# JDK modules

[Guide](README.md) · [Projects](projects.md) · [Coverage](coverage.md)

All 70 direct java.*/jdk.* source modules at the pinned snapshot have an inspected module-info.java. Declared exports, service wiring and requires are static observations. A declaration does not establish runtime compatibility or which platform builds an optional module. Package-directory counts retain platform-specific source roots and are not distinct namespace counts. Empty compiler placeholder modules and the native JDWP module legitimately have no package-directory row.

| Module / descriptor | Declared role | Requires | Package directories / existing package docs | Export clauses |
| --- | --- | --- | ---: | ---: |
| [java.base](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.base/share/classes/module-info.java) | Defines the foundational APIs of the Java SE Platform. | none (base module) | 237 / 59 | 121 |
| [java.compiler](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.compiler/share/classes/module-info.java) | Defines the "Language Model", "Annotation Processing", and "Java Compiler" APIs. | java.base (implicit) | 7 / 6 | 6 |
| [java.datatransfer](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.datatransfer/share/classes/module-info.java) | Defines the API for transferring data between and within applications. | java.base (implicit) | 2 / 1 | 2 |
| [java.desktop](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.desktop/share/classes/module-info.java) | Defines the AWT and Swing user interface toolkits, plus APIs for accessibility, audio, imaging, printing, and JavaBeans. | java.prefs; transitive java.datatransfer; transitive java.xml; java.base (implicit) | 137 / 55 | 54 |
| [java.instrument](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.instrument/share/classes/module-info.java) | Defines services that allow agents to instrument programs running on the JVM. | java.base (implicit) | 2 / 1 | 2 |
| [java.logging](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.logging/share/classes/module-info.java) | Defines the Java Logging API. | java.base (implicit) | 3 / 2 | 1 |
| [java.management.rmi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.management.rmi/share/classes/module-info.java) | Defines the connector for the Java Management Extensions (JMX) Remote API. | java.naming; transitive java.management; transitive java.rmi; java.base (implicit) | 3 / 1 | 3 |
| [java.management](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.management/share/classes/module-info.java) | Defines the Java Management Extensions (JMX) API. | java.base (implicit) | 19 / 13 | 16 |
| [java.naming](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.naming/share/classes/module-info.java) | Defines the Java Naming and Directory Interface (JNDI) API. | java.security.sasl; java.base (implicit) | 17 / 5 | 9 |
| [java.net.http](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.net.http/share/classes/module-info.java) | Defines the HTTP Client and WebSocket APIs. | java.base (implicit) | 6 / 2 | 1 |
| [java.prefs](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.prefs/share/classes/module-info.java) | Defines the Preferences API. | java.xml; java.base (implicit) | 4 / 1 | 1 |
| [java.rmi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.rmi/share/classes/module-info.java) | Defines the Remote Method Invocation (RMI) API. | java.logging; java.base (implicit) | 11 / 5 | 8 |
| [java.scripting](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.scripting/share/classes/module-info.java) | Defines the Scripting API. | java.base (implicit) | 2 / 1 | 1 |
| [java.se](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.se/share/classes/module-info.java) | Defines the API of the Java SE Platform. | transitive java.compiler; transitive java.datatransfer; transitive java.desktop; transitive java.instrument; transitive java.logging; transitive java.management; transitive java.management.rmi; transitive java.naming; transitive java.net.http; transitive java.prefs; transitive java.rmi; transitive java.scripting; transitive java.security.jgss; transitive java.security.sasl; transitive java.sql; transitive java.sql.rowset; transitive java.transaction.xa; transitive java.xml; transitive java.xml.crypto; java.base (implicit) | 0 / 0 | 0 |
| [java.security.jgss](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.security.jgss/share/classes/module-info.java) | Defines the Java binding of the IETF Generic Security Services API (GSS-API). | java.naming; java.base (implicit) | 18 / 2 | 8 |
| [java.security.sasl](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.security.sasl/share/classes/module-info.java) | Defines Java support for the IETF Simple Authentication and Security Layer (SASL). | java.logging; java.base (implicit) | 5 / 1 | 2 |
| [java.smartcardio](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.smartcardio/share/classes/module-info.java) | Defines the Java Smart Card I/O API. | java.base (implicit) | 4 / 1 | 1 |
| [java.sql.rowset](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.sql.rowset/share/classes/module-info.java) | Defines the JDBC RowSet API. | transitive java.logging; transitive java.naming; transitive java.sql; java.base (implicit) | 6 / 5 | 3 |
| [java.sql](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.sql/share/classes/module-info.java) | Defines the JDBC API. | transitive java.logging; transitive java.transaction.xa; transitive java.xml; java.base (implicit) | 2 / 2 | 2 |
| [java.transaction.xa](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.transaction.xa/share/classes/module-info.java) | Defines an API for supporting distributed transactions in JDBC. | java.base (implicit) | 1 / 1 | 1 |
| [java.xml.crypto](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.xml.crypto/share/classes/module-info.java) | Defines the API for XML cryptography. | java.logging; transitive java.xml; java.base (implicit) | 33 / 6 | 6 |
| [java.xml](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.xml/share/classes/module-info.java) | Defines the Java APIs for XML Processing (JAXP). | java.base (implicit) | 103 / 44 | 32 |
| [jdk.accessibility](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.accessibility/share/classes/module-info.java) | Defines JDK utility classes used by implementors of Assistive Technologies. | transitive java.desktop; java.base (implicit) | 3 / 1 | 1 |
| [jdk.attach](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.attach/share/classes/module-info.java) | Defines the attach API. | jdk.internal.jvmstat; java.base (implicit) | 7 / 2 | 3 |
| [jdk.charsets](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.charsets/share/classes/module-info.java) | Provides charsets that are not in java.base (mostly double byte and IBM charsets). | java.base (implicit) | 1 / 0 | 0 |
| [jdk.compiler](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.compiler/share/classes/module-info.java) | Defines the implementation of the compiler and its command line equivalent, javac javac tool . | transitive java.compiler; jdk.internal.opt; jdk.zipfs; java.base (implicit) | 20 / 5 | 18 |
| [jdk.crypto.cryptoki](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.crypto.cryptoki/share/classes/module-info.java) | Provides the implementation of the SunPKCS11 security provider. | jdk.crypto.ec; java.base (implicit) | 2 / 0 | 0 |
| [jdk.crypto.ec](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.crypto.ec/share/classes/module-info.java) | Provides the implementation of the SunEC security provider. | java.base (implicit) | 3 / 0 | 0 |
| [jdk.crypto.mscapi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.crypto.mscapi/windows/classes/module-info.java) | Provides the implementation of the SunMSCAPI security provider. | java.base (implicit) | 1 / 0 | 0 |
| [jdk.dynalink](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.dynalink/share/classes/module-info.java) | Defines the API for dynamic linking of high-level operations on objects. | java.logging; java.base (implicit) | 6 / 5 | 5 |
| [jdk.editpad](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.editpad/share/classes/module-info.java) | Provides the implementation of the edit pad service used by jdk.jshell. | java.desktop; jdk.internal.ed; java.base (implicit) | 1 / 0 | 0 |
| [jdk.hotspot.agent](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.hotspot.agent/share/classes/module-info.java) | Defines the implementation of the HotSpot Serviceability Agent. | java.datatransfer; java.desktop; java.rmi; java.scripting; java.base (implicit) | 89 / 0 | 1 |
| [jdk.httpserver](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.httpserver/share/classes/module-info.java) | Defines the JDK-specific HTTP server API, and provides the jwebserver tool for running a minimal HTTP server. | java.base (implicit) | 6 / 2 | 2 |
| [jdk.incubator.vector](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.incubator.vector/share/classes/module-info.java) | Defines an API for expressing computations that can be reliably compiled at runtime into SIMD instructions, such as AVX instructions on x64, and NEON instructions on AArch64. | java.base (implicit) | 1 / 1 | 1 |
| [jdk.internal.ed](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.ed/share/classes/module-info.java) | Internal editor support for JDK tools. | java.base (implicit) | 2 / 0 | 2 |
| [jdk.internal.jvmstat](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.jvmstat/share/classes/module-info.java) | Defines an internal API for monitoring a HotSpot Java Virtual Machine using its shared memory instrumentation buffer. | java.base (implicit) | 9 / 5 | 3 |
| [jdk.internal.le](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.le/share/classes/module-info.java) | Internal API for line editing. | java.base (implicit) | 20 / 5 | 9 |
| [jdk.internal.opt](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.opt/share/classes/module-info.java) | Internal option processing API. | java.base (implicit) | 4 / 0 | 2 |
| [jdk.internal.vm.ci](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.vm.ci/share/classes/module-info.java) | Internal JVM compiler interface and compiler/runtime support, as exposed by the qualified exports in its descriptor. | java.base (implicit) | 14 / 14 | 5 |
| [jdk.internal.vm.compiler.management](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.vm.compiler.management/share/classes/module-info.java) | Registers JVMCI compiler specific management interfaces for the JVM. | jdk.internal.vm.ci; java.base (implicit) | 0 / 0 | 0 |
| [jdk.internal.vm.compiler](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.vm.compiler/share/classes/module-info.java) | JVMCI compiler implementation for the JVM. | jdk.internal.vm.ci; java.base (implicit) | 0 / 0 | 0 |
| [jdk.jartool](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jartool/share/classes/module-info.java) | Defines tools for manipulating Java Archive (JAR) files, including the jar jar tool and jarsigner jarsigner tool tools. | jdk.internal.opt; java.base (implicit) | 3 / 1 | 1 |
| [jdk.javadoc](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.javadoc/share/classes/module-info.java) | Defines the implementation of the tool and its command-line equivalent, javadoc javadoc tool . | java.xml; transitive java.compiler; transitive jdk.compiler; jdk.internal.opt; java.base (implicit) | 14 / 12 | 1 |
| [jdk.jcmd](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jcmd/share/classes/module-info.java) | Defines tools for diagnostics and troubleshooting a JVM such as the jcmd jcmd tool , jps jps tool , jstat jstat tool tools. | jdk.attach; jdk.internal.jvmstat; java.base (implicit) | 8 / 0 | 0 |
| [jdk.jconsole](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jconsole/share/classes/module-info.java) | Defines the JMX graphical tool, jconsole jconsole , for monitoring and managing a running application. | java.management.rmi; java.rmi; jdk.attach; jdk.internal.jvmstat; jdk.management; jdk.management.agent; transitive java.desktop; transitive java.management; java.base (implicit) | 3 / 1 | 1 |
| [jdk.jdeps](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jdeps/share/classes/module-info.java) | Defines tools for analysing dependencies in Java libraries and programs, including the jdeps jdeps tool , javap javap tool , and jdeprscan jdeprscan tool tools. | java.compiler; jdk.compiler; java.base (implicit) | 5 / 2 | 1 |
| [jdk.jdi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jdi/share/classes/module-info.java) | Defines the Java Debug Interface. | jdk.attach; jdk.jdwp.agent; java.base (implicit) | 9 / 5 | 5 |
| [jdk.jdwp.agent](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jdwp.agent/share/classes/module-info.java) | Provides the implementation of the Java Debug Wire Protocol (JDWP) agent. | java.base (implicit) | 0 / 0 | 0 |
| [jdk.jfr](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jfr/share/classes/module-info.java) | Defines the API for JDK Flight Recorder. | java.base (implicit) | 20 / 3 | 3 |
| [jdk.jlink](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jlink/share/classes/module-info.java) | Defines the jlink jlink tool tool for creating run-time images, the jmod jmod tool tool for creating and manipulating JMOD files, and the jimage jimage tool tool for inspecting the JDK implementation-specific container file for classes and resources. | jdk.internal.opt; jdk.jdeps; java.base (implicit) | 7 / 0 | 0 |
| [jdk.jpackage](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jpackage/share/classes/module-info.java) | Defines the Java Packaging tool, jpackage. | jdk.internal.opt; jdk.jlink; java.desktop; java.base (implicit) | 7 / 0 | 0 |
| [jdk.jshell](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jshell/share/classes/module-info.java) | Provides the jshell jshell tool tool for evaluating snippets of Java code, and defines a JDK-specific API for modeling and executing snippets. | java.logging; jdk.compiler; jdk.internal.ed; jdk.internal.le; jdk.internal.opt; transitive java.compiler; transitive java.prefs; transitive jdk.jdi; java.base (implicit) | 7 / 4 | 4 |
| [jdk.jsobject](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jsobject/share/classes/module-info.java) | Defines the API for the JavaScript Object. | java.base (implicit) | 1 / 1 | 1 |
| [jdk.jstatd](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jstatd/share/classes/module-info.java) | Defines the jstatd jstatd tool tool for starting a daemon for the jstat tool to monitor JVM statistics remotely. | java.rmi; jdk.internal.jvmstat; java.base (implicit) | 3 / 2 | 1 |
| [jdk.localedata](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.localedata/share/classes/module-info.java) | Provides the locale data for locales other than locale. | java.base (implicit) | 3 / 0 | 0 |
| [jdk.management.agent](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.management.agent/share/classes/module-info.java) | Defines the JMX management agent. | java.management; java.management.rmi; java.base (implicit) | 5 / 2 | 1 |
| [jdk.management.jfr](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.management.jfr/share/classes/module-info.java) | Defines the Management Interface for JDK Flight Recorder. | jdk.management; transitive java.management; transitive jdk.jfr; java.base (implicit) | 2 / 1 | 1 |
| [jdk.management](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.management/share/classes/module-info.java) | Defines JDK-specific management interfaces for the JVM. | transitive java.management; java.base (implicit) | 4 / 1 | 1 |
| [jdk.naming.dns](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.naming.dns/share/classes/module-info.java) | Provides the implementation of the DNS Java Naming provider. | java.naming; java.base (implicit) | 2 / 0 | 1 |
| [jdk.naming.rmi](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.naming.rmi/share/classes/module-info.java) | Provides the implementation of the RMI Java Naming provider. | java.naming; java.rmi; java.base (implicit) | 2 / 0 | 2 |
| [jdk.net](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.net/share/classes/module-info.java) | Defines the JDK-specific Networking API. | java.base (implicit) | 6 / 2 | 2 |
| [jdk.nio.mapmode](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.nio.mapmode/share/classes/module-info.java) | Defines JDK-specific file mapping modes. | java.base (implicit) | 1 / 0 | 1 |
| [jdk.random](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.random/share/classes/module-info.java) | Defines implementations of the Interface. | java.base (implicit) | 1 / 0 | 1 |
| [jdk.sctp](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.sctp/share/classes/module-info.java) | Defines the JDK-specific API for SCTP. | java.base (implicit) | 3 / 1 | 1 |
| [jdk.security.auth](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.security.auth/share/classes/module-info.java) | Provides implementations of the javax.security.auth.* interfaces and various authentication modules. | java.security.jgss; transitive java.naming; java.base (implicit) | 4 / 4 | 4 |
| [jdk.security.jgss](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.security.jgss/share/classes/module-info.java) | Defines JDK extensions to the GSS-API and an implementation of the SASL GSSAPI mechanism. | java.logging; java.security.sasl; transitive java.security.jgss; java.base (implicit) | 2 / 1 | 1 |
| [jdk.unsupported.desktop](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.unsupported.desktop/share/classes/module-info.java) | This module is intended for use only by the javafx.swing module for the purpose of FX/Swing interop. | transitive java.desktop; java.base (implicit) | 2 / 0 | 1 |
| [jdk.unsupported](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.unsupported/share/classes/module-info.java) | Compatibility access to the unsupported sun.misc, sun.reflect and com.sun.nio.file packages; inspect the exports and opens before use. | java.base (implicit) | 3 / 0 | 3 |
| [jdk.xml.dom](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.xml.dom/share/classes/module-info.java) | Defines the subset of the W3C Document Object Model (DOM) API that is not part of the Java SE API. | transitive java.xml; java.base (implicit) | 4 / 4 | 4 |
| [jdk.zipfs](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.zipfs/share/classes/module-info.java) | Provides the implementation of the Zip file system provider. | java.base (implicit) | 1 / 0 | 0 |

## Descriptor details

### java.base

Defines the foundational APIs of the Java SE Platform.

- Source and declaration: [src/java.base/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.base/share/classes/module-info.java); Git blob `5488cfd8ba37d96ffef7be0b313b4e91a0cb063d`
- Explicit requires: none
- Exports: `java.io`; `java.lang`; `java.lang.annotation`; `java.lang.constant`; `java.lang.foreign`; `java.lang.invoke`; `java.lang.module`; `java.lang.ref`; `java.lang.reflect`; `java.lang.runtime`; `java.math`; `java.net`; `java.net.spi`; `java.nio`; `java.nio.channels`; `java.nio.channels.spi`; `java.nio.charset`; `java.nio.charset.spi`; `java.nio.file`; `java.nio.file.attribute`; `java.nio.file.spi`; `java.security`; `java.security.cert`; `java.security.interfaces`; `java.security.spec`; `java.text`; `java.text.spi`; `java.time`; `java.time.chrono`; `java.time.format`; `java.time.temporal`; `java.time.zone`; `java.util`; `java.util.concurrent`; `java.util.concurrent.atomic`; `java.util.concurrent.locks`; `java.util.function`; `java.util.jar`; `java.util.random`; `java.util.regex`; `java.util.spi`; `java.util.stream`; `java.util.zip`; `javax.crypto`; `javax.crypto.interfaces`; `javax.crypto.spec`; `javax.net`; `javax.net.ssl`; `javax.security.auth`; `javax.security.auth.callback`; `javax.security.auth.login`; `javax.security.auth.spi`; `javax.security.auth.x500`; `javax.security.cert`; `com.sun.crypto.provider` (qualified to jdk.crypto.cryptoki); `sun.invoke.util` (qualified to jdk.compiler); `com.sun.security.ntlm` (qualified to java.security.sasl); `jdk.internal.javac` (qualified to java.compiler, jdk.compiler, jdk.incubator.vector, jdk.jshell); `jdk.internal.access` (qualified to java.desktop, java.logging, java.management, java.naming, java.rmi, jdk.charsets, jdk.jartool, jdk.jlink, jdk.jfr, jdk.net, jdk.sctp, jdk.crypto.cryptoki); `jdk.internal.foreign` (qualified to jdk.incubator.vector); `jdk.internal.event` (qualified to jdk.jfr); `jdk.internal.io` (qualified to jdk.internal.le, jdk.jshell); `jdk.internal.jimage` (qualified to jdk.jlink); `jdk.internal.jimage.decompressor` (qualified to jdk.jlink); `jdk.internal.loader` (qualified to java.instrument, java.logging, java.naming); `jdk.internal.jmod` (qualified to jdk.compiler, jdk.jlink); `jdk.internal.logger` (qualified to java.logging); `jdk.internal.classfile` (qualified to jdk.jartool, jdk.jlink, jdk.jshell); `jdk.internal.classfile.attribute` (qualified to jdk.jartool, jdk.jlink); `jdk.internal.classfile.constantpool` (qualified to jdk.jartool, jdk.jlink); `jdk.internal.classfile.instruction` (qualified to jdk.jlink, jdk.jshell); `jdk.internal.org.objectweb.asm` (qualified to jdk.jfr); `jdk.internal.org.objectweb.asm.tree` (qualified to jdk.jfr); `jdk.internal.org.objectweb.asm.util` (qualified to jdk.jfr); `jdk.internal.org.objectweb.asm.commons` (qualified to jdk.jfr); `jdk.internal.org.xml.sax` (qualified to jdk.jfr); `jdk.internal.org.xml.sax.helpers` (qualified to jdk.jfr); `jdk.internal.misc` (qualified to java.desktop, java.logging, java.management, java.naming, java.net.http, java.rmi, java.security.jgss, jdk.attach, jdk.charsets, jdk.compiler, jdk.crypto.cryptoki, jdk.incubator.vector, jdk.jfr, jdk.jshell, jdk.nio.mapmode, jdk.unsupported, jdk.internal.vm.ci); `jdk.internal.module` (qualified to java.instrument, java.management.rmi, jdk.jartool, jdk.compiler, jdk.jfr, jdk.jlink, jdk.jpackage); `jdk.internal.perf` (qualified to java.management, jdk.management.agent, jdk.internal.jvmstat); `jdk.internal.platform` (qualified to jdk.management, jdk.jfr); `jdk.internal.ref` (qualified to java.desktop, java.net.http, jdk.naming.dns); `jdk.internal.reflect` (qualified to java.logging, java.sql, java.sql.rowset, jdk.dynalink, jdk.internal.vm.ci, jdk.unsupported); `jdk.internal.vm` (qualified to java.management, jdk.internal.jvmstat, jdk.management, jdk.management.agent, jdk.internal.vm.ci); `jdk.internal.vm.annotation` (qualified to java.instrument, jdk.internal.vm.ci, jdk.incubator.vector, jdk.jfr, jdk.unsupported); `jdk.internal.vm.vector` (qualified to jdk.incubator.vector); `jdk.internal.util.xml` (qualified to jdk.jfr); `jdk.internal.util.xml.impl` (qualified to jdk.jfr); `jdk.internal.util.random` (qualified to jdk.random); `jdk.internal.util` (qualified to java.desktop, java.prefs, java.security.jgss, java.smartcardio, jdk.charsets, jdk.jlink, jdk.jpackage, jdk.net); `sun.net` (qualified to java.net.http, jdk.naming.dns); `sun.net.ext` (qualified to jdk.net); `sun.net.dns` (qualified to java.security.jgss, jdk.naming.dns); `sun.net.util` (qualified to java.desktop, java.net.http, jdk.jconsole, jdk.sctp); `sun.net.www` (qualified to java.net.http, jdk.jartool); `sun.net.www.protocol.http` (qualified to java.security.jgss); `sun.nio.ch` (qualified to java.management, jdk.crypto.cryptoki, jdk.net, jdk.sctp); `sun.nio.cs` (qualified to jdk.charsets); `sun.nio.fs` (qualified to jdk.net); `sun.reflect.annotation` (qualified to jdk.compiler); `sun.reflect.generics.reflectiveObjects` (qualified to java.desktop); `sun.reflect.misc` (qualified to java.desktop, java.datatransfer, java.management, java.management.rmi, java.rmi, java.sql.rowset); `sun.security.action` (qualified to java.desktop, java.security.jgss, jdk.crypto.ec); `sun.security.internal.interfaces` (qualified to jdk.crypto.cryptoki); `sun.security.internal.spec` (qualified to jdk.crypto.cryptoki); `sun.security.jca` (qualified to java.smartcardio, jdk.crypto.ec, jdk.crypto.cryptoki, jdk.naming.dns); `sun.security.pkcs` (qualified to jdk.crypto.ec, jdk.jartool); `sun.security.provider` (qualified to java.rmi, java.security.jgss, jdk.crypto.cryptoki, jdk.crypto.ec, jdk.security.auth); `sun.security.provider.certpath` (qualified to java.naming, jdk.jartool); `sun.security.rsa` (qualified to jdk.crypto.cryptoki); `sun.security.timestamp` (qualified to jdk.jartool); `sun.security.tools` (qualified to jdk.jartool); `sun.security.util` (qualified to java.desktop, java.naming, java.rmi, java.security.jgss, java.security.sasl, java.smartcardio, java.xml.crypto, jdk.crypto.ec, jdk.crypto.cryptoki, jdk.jartool, jdk.security.auth, jdk.security.jgss); `sun.security.util.math` (qualified to jdk.crypto.ec); `sun.security.util.math.intpoly` (qualified to jdk.crypto.ec); `sun.security.x509` (qualified to jdk.crypto.ec, jdk.crypto.cryptoki, jdk.jartool); `sun.security.validator` (qualified to jdk.jartool); `sun.util.cldr` (qualified to jdk.jlink); `sun.util.locale.provider` (qualified to java.desktop, jdk.jlink, jdk.localedata); `sun.util.logging` (qualified to java.desktop, java.logging, java.prefs); `sun.util.resources` (qualified to jdk.localedata)
- Service consumers: `java.lang.System.LoggerFinder`, `java.net.ContentHandlerFactory`, `java.net.spi.InetAddressResolverProvider`, `java.net.spi.URLStreamHandlerProvider`, `java.nio.channels.spi.AsynchronousChannelProvider`, `java.nio.channels.spi.SelectorProvider`, `java.nio.charset.spi.CharsetProvider`, `java.nio.file.spi.FileSystemProvider`, `java.nio.file.spi.FileTypeDetector`, `java.security.Provider`, `java.text.spi.BreakIteratorProvider`, `java.text.spi.CollatorProvider`, `java.text.spi.DateFormatProvider`, `java.text.spi.DateFormatSymbolsProvider`, `java.text.spi.DecimalFormatSymbolsProvider`, `java.text.spi.NumberFormatProvider`, `java.time.chrono.AbstractChronology`, `java.time.chrono.Chronology`, `java.time.zone.ZoneRulesProvider`, `java.util.random.RandomGenerator`, `java.util.spi.CalendarDataProvider`, `java.util.spi.CalendarNameProvider`, `java.util.spi.CurrencyNameProvider`, `java.util.spi.LocaleNameProvider`, `java.util.spi.ResourceBundleControlProvider`, `java.util.spi.ResourceBundleProvider`, `java.util.spi.TimeZoneNameProvider`, `java.util.spi.ToolProvider`, `javax.security.auth.spi.LoginModule`, `jdk.internal.io.JdkConsoleProvider`, `jdk.internal.logger.DefaultLoggerFinder`, `sun.text.spi.JavaTimeDateTimePatternProvider`, `sun.util.spi.CalendarProvider`, `sun.util.locale.provider.LocaleDataMetaInfo`, `sun.util.resources.LocaleData.CommonResourceBundleProvider`, `sun.util.resources.LocaleData.SupplementaryResourceBundleProvider`
- Service implementations: `java.nio.file.spi.FileSystemProvider` → jdk.internal.jrtfs.JrtFileSystemProvider; `java.util.random.RandomGenerator` → java.security.SecureRandom, java.util.Random, java.util.SplittableRandom
- Package navigation: 237 source directories, 59 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.compiler

Defines the "Language Model", "Annotation Processing", and "Java Compiler" APIs.

- Source and declaration: [src/java.compiler/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.compiler/share/classes/module-info.java); Git blob `0793855d1dc6efb9846d77c5219be91b8a25ce84`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `javax.annotation.processing`; `javax.lang.model`; `javax.lang.model.element`; `javax.lang.model.type`; `javax.lang.model.util`; `javax.tools`
- Service consumers: `javax.tools.DocumentationTool`, `javax.tools.JavaCompiler`
- Service implementations: none declared
- Package navigation: 7 source directories, 6 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/langtools](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/langtools); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.datatransfer

Defines the API for transferring data between and within applications.

- Source and declaration: [src/java.datatransfer/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.datatransfer/share/classes/module-info.java); Git blob `67d982b596a208edd5418b4ef3175829bec1bf23`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `java.awt.datatransfer`; `sun.datatransfer` (qualified to java.desktop)
- Service consumers: `sun.datatransfer.DesktopDatatransferService`
- Service implementations: none declared
- Package navigation: 2 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.desktop

Defines the AWT and Swing user interface toolkits, plus APIs for accessibility, audio, imaging, printing, and JavaBeans.

- Source and declaration: [src/java.desktop/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.desktop/share/classes/module-info.java); Git blob `897647ee3689a5f54827803c780c81ec22fb70d1`
- Explicit requires: `java.prefs`; `transitive java.datatransfer`; `transitive java.xml`; `java.base` is implicitly readable
- Exports: `java.applet`; `java.awt`; `java.awt.color`; `java.awt.desktop`; `java.awt.dnd`; `java.awt.event`; `java.awt.font`; `java.awt.geom`; `java.awt.im`; `java.awt.im.spi`; `java.awt.image`; `java.awt.image.renderable`; `java.awt.print`; `java.beans`; `java.beans.beancontext`; `javax.accessibility`; `javax.imageio`; `javax.imageio.event`; `javax.imageio.metadata`; `javax.imageio.plugins.bmp`; `javax.imageio.plugins.jpeg`; `javax.imageio.plugins.tiff`; `javax.imageio.spi`; `javax.imageio.stream`; `javax.print`; `javax.print.attribute`; `javax.print.attribute.standard`; `javax.print.event`; `javax.sound.midi`; `javax.sound.midi.spi`; `javax.sound.sampled`; `javax.sound.sampled.spi`; `javax.swing`; `javax.swing.border`; `javax.swing.colorchooser`; `javax.swing.event`; `javax.swing.filechooser`; `javax.swing.plaf`; `javax.swing.plaf.basic`; `javax.swing.plaf.metal`; `javax.swing.plaf.multi`; `javax.swing.plaf.nimbus`; `javax.swing.plaf.synth`; `javax.swing.table`; `javax.swing.text`; `javax.swing.text.html`; `javax.swing.text.html.parser`; `javax.swing.text.rtf`; `javax.swing.tree`; `javax.swing.undo`; `sun.awt` (qualified to jdk.accessibility, jdk.unsupported.desktop); `java.awt.dnd.peer` (qualified to jdk.unsupported.desktop); `sun.awt.dnd` (qualified to jdk.unsupported.desktop); `sun.swing` (qualified to jdk.unsupported.desktop)
- Service consumers: `java.awt.im.spi.InputMethodDescriptor`, `javax.accessibility.AccessibilityProvider`, `javax.imageio.spi.ImageInputStreamSpi`, `javax.imageio.spi.ImageOutputStreamSpi`, `javax.imageio.spi.ImageReaderSpi`, `javax.imageio.spi.ImageTranscoderSpi`, `javax.imageio.spi.ImageWriterSpi`, `javax.print.PrintServiceLookup`, `javax.print.StreamPrintServiceFactory`, `javax.sound.midi.spi.MidiDeviceProvider`, `javax.sound.midi.spi.MidiFileReader`, `javax.sound.midi.spi.MidiFileWriter`, `javax.sound.midi.spi.SoundbankReader`, `javax.sound.sampled.spi.AudioFileReader`, `javax.sound.sampled.spi.AudioFileWriter`, `javax.sound.sampled.spi.FormatConversionProvider`, `javax.sound.sampled.spi.MixerProvider`, `sun.swing.InteropProvider`
- Service implementations: `sun.datatransfer.DesktopDatatransferService` → sun.awt.datatransfer.DesktopDatatransferServiceImpl; `java.net.ContentHandlerFactory` → sun.awt.www.content.MultimediaContentHandlers; `javax.print.PrintServiceLookup` → sun.print.PrintServiceLookupProvider; `javax.print.StreamPrintServiceFactory` → sun.print.PSStreamPrinterFactory; `javax.sound.midi.spi.MidiDeviceProvider` → com.sun.media.sound.MidiInDeviceProvider, com.sun.media.sound.MidiOutDeviceProvider, com.sun.media.sound.RealTimeSequencerProvider, com.sun.media.sound.SoftProvider; `javax.sound.midi.spi.MidiFileReader` → com.sun.media.sound.StandardMidiFileReader; `javax.sound.midi.spi.MidiFileWriter` → com.sun.media.sound.StandardMidiFileWriter; `javax.sound.midi.spi.SoundbankReader` → com.sun.media.sound.AudioFileSoundbankReader, com.sun.media.sound.DLSSoundbankReader, com.sun.media.sound.JARSoundbankReader, com.sun.media.sound.SF2SoundbankReader; `javax.sound.sampled.spi.AudioFileReader` → com.sun.media.sound.AiffFileReader, com.sun.media.sound.AuFileReader, com.sun.media.sound.SoftMidiAudioFileReader, com.sun.media.sound.WaveFileReader, com.sun.media.sound.WaveFloatFileReader, com.sun.media.sound.WaveExtensibleFileReader; `javax.sound.sampled.spi.AudioFileWriter` → com.sun.media.sound.AiffFileWriter, com.sun.media.sound.AuFileWriter, com.sun.media.sound.WaveFileWriter, com.sun.media.sound.WaveFloatFileWriter; `javax.sound.sampled.spi.FormatConversionProvider` → com.sun.media.sound.AlawCodec, com.sun.media.sound.AudioFloatFormatConverter, com.sun.media.sound.PCMtoPCMCodec, com.sun.media.sound.UlawCodec; `javax.sound.sampled.spi.MixerProvider` → com.sun.media.sound.DirectAudioDeviceProvider, com.sun.media.sound.PortMixerProvider
- Package navigation: 137 source directories, 55 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.instrument

Defines services that allow agents to instrument programs running on the JVM.

- Source and declaration: [src/java.instrument/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.instrument/share/classes/module-info.java); Git blob `526aed6bd4209516d62bbdef7ed41fcb6faf7212`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `java.lang.instrument`; `sun.instrument` (qualified to java.base)
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 2 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.logging

Defines the Java Logging API.

- Source and declaration: [src/java.logging/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.logging/share/classes/module-info.java); Git blob `272d561291a30ba5bea7d7460e0eb3f0d4209ead`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `java.util.logging`
- Service consumers: none declared
- Service implementations: `jdk.internal.logger.DefaultLoggerFinder` → sun.util.logging.internal.LoggingProviderImpl
- Package navigation: 3 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.management.rmi

Defines the connector for the Java Management Extensions (JMX) Remote API.

- Source and declaration: [src/java.management.rmi/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.management.rmi/share/classes/module-info.java); Git blob `f6d652a22c46aa1504155c04f67c5c33e19087ff`
- Explicit requires: `java.naming`; `transitive java.management`; `transitive java.rmi`; `java.base` is implicitly readable
- Exports: `javax.management.remote.rmi`; `com.sun.jmx.remote.protocol.rmi` (qualified to java.management); `com.sun.jmx.remote.internal.rmi` (qualified to jdk.management.agent)
- Service consumers: none declared
- Service implementations: `javax.management.remote.JMXConnectorProvider` → com.sun.jmx.remote.protocol.rmi.ClientProvider; `javax.management.remote.JMXConnectorServerProvider` → com.sun.jmx.remote.protocol.rmi.ServerProvider
- Package navigation: 3 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.management

Defines the Java Management Extensions (JMX) API.

- Source and declaration: [src/java.management/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.management/share/classes/module-info.java); Git blob `8544d858ac57b5fc7f35ce39ccdc0c4ebca38339`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `java.lang.management`; `javax.management`; `javax.management.loading`; `javax.management.modelmbean`; `javax.management.monitor`; `javax.management.openmbean`; `javax.management.relation`; `javax.management.remote`; `javax.management.timer`; `com.sun.jmx.remote.internal` (qualified to java.management.rmi, jdk.management.agent); `com.sun.jmx.remote.security` (qualified to java.management.rmi, jdk.management.agent); `com.sun.jmx.remote.util` (qualified to java.management.rmi); `sun.management` (qualified to jdk.jconsole, jdk.management, jdk.management.agent); `sun.management.counter` (qualified to jdk.management.agent); `sun.management.counter.perf` (qualified to jdk.management.agent); `sun.management.spi` (qualified to jdk.management, jdk.management.jfr)
- Service consumers: `javax.management.remote.JMXConnectorProvider`, `javax.management.remote.JMXConnectorServerProvider`, `sun.management.spi.PlatformMBeanProvider`
- Service implementations: `javax.security.auth.spi.LoginModule` → com.sun.jmx.remote.security.FileLoginModule
- Package navigation: 19 source directories, 13 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.naming

Defines the Java Naming and Directory Interface (JNDI) API.

- Source and declaration: [src/java.naming/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.naming/share/classes/module-info.java); Git blob `c4c7a606c6ca5e8904903313dad7ab64e4228a90`
- Explicit requires: `java.security.sasl`; `java.base` is implicitly readable
- Exports: `javax.naming`; `javax.naming.directory`; `javax.naming.event`; `javax.naming.ldap`; `javax.naming.spi`; `javax.naming.ldap.spi`; `com.sun.jndi.toolkit.ctx` (qualified to jdk.naming.dns); `com.sun.jndi.toolkit.url` (qualified to jdk.naming.dns, jdk.naming.rmi); `com.sun.naming.internal` (qualified to jdk.naming.rmi)
- Service consumers: `javax.naming.ldap.StartTlsResponse`, `javax.naming.spi.InitialContextFactory`, `javax.naming.ldap.spi.LdapDnsProvider`
- Service implementations: `java.security.Provider` → sun.security.provider.certpath.ldap.JdkLDAP
- Package navigation: 17 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.net.http

Defines the HTTP Client and WebSocket APIs.

- Source and declaration: [src/java.net.http/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.net.http/share/classes/module-info.java); Git blob `cf9d07bdf328f77f9760381dca55f794ea0f3a52`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `java.net.http`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 6 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.prefs

Defines the Preferences API.

- Source and declaration: [src/java.prefs/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.prefs/share/classes/module-info.java); Git blob `170317902cff6d8c8e204922c1eebd8abe919cff`
- Explicit requires: `java.xml`; `java.base` is implicitly readable
- Exports: `java.util.prefs`
- Service consumers: `java.util.prefs.PreferencesFactory`
- Service implementations: none declared
- Package navigation: 4 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.rmi

Defines the Remote Method Invocation (RMI) API.

- Source and declaration: [src/java.rmi/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.rmi/share/classes/module-info.java); Git blob `96b61a172ff05f030eb8afb507e3dabdc5aee3b3`
- Explicit requires: `java.logging`; `java.base` is implicitly readable
- Exports: `java.rmi`; `java.rmi.dgc`; `java.rmi.registry`; `java.rmi.server`; `javax.rmi.ssl`; `sun.rmi.registry` (qualified to jdk.management.agent); `sun.rmi.server` (qualified to java.management.rmi, jdk.management.agent, jdk.jconsole); `sun.rmi.transport` (qualified to java.management.rmi, jdk.management.agent, jdk.jconsole)
- Service consumers: `java.rmi.server.RMIClassLoaderSpi`
- Service implementations: none declared
- Package navigation: 11 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.scripting

Defines the Scripting API.

- Source and declaration: [src/java.scripting/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.scripting/share/classes/module-info.java); Git blob `4367a55a5a1e16bb7bee4dd604b027d0f3a8be5c`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `javax.script`
- Service consumers: `javax.script.ScriptEngineFactory`
- Service implementations: none declared
- Package navigation: 2 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.se

Defines the API of the Java SE Platform.

- Source and declaration: [src/java.se/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.se/share/classes/module-info.java); Git blob `81b1bd3cb8aa9977ff9a66ed9c8ffeba6eb14472`
- Explicit requires: `transitive java.compiler`; `transitive java.datatransfer`; `transitive java.desktop`; `transitive java.instrument`; `transitive java.logging`; `transitive java.management`; `transitive java.management.rmi`; `transitive java.naming`; `transitive java.net.http`; `transitive java.prefs`; `transitive java.rmi`; `transitive java.scripting`; `transitive java.security.jgss`; `transitive java.security.sasl`; `transitive java.sql`; `transitive java.sql.rowset`; `transitive java.transaction.xa`; `transitive java.xml`; `transitive java.xml.crypto`; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 0 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.security.jgss

Defines the Java binding of the IETF Generic Security Services API (GSS-API).

- Source and declaration: [src/java.security.jgss/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.security.jgss/share/classes/module-info.java); Git blob `d9ad7ad4919ed8b21e6809366603116868ed2ba3`
- Explicit requires: `java.naming`; `java.base` is implicitly readable
- Exports: `javax.security.auth.kerberos`; `org.ietf.jgss`; `sun.security.jgss` (qualified to jdk.security.jgss); `sun.security.jgss.krb5` (qualified to jdk.security.auth); `sun.security.jgss.krb5.internal` (qualified to jdk.security.jgss); `sun.security.krb5` (qualified to jdk.security.auth); `sun.security.krb5.internal` (qualified to jdk.security.jgss); `sun.security.krb5.internal.ktab` (qualified to jdk.security.auth)
- Service consumers: none declared
- Service implementations: `java.security.Provider` → sun.security.jgss.SunProvider
- Package navigation: 18 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.security.sasl

Defines Java support for the IETF Simple Authentication and Security Layer (SASL).

- Source and declaration: [src/java.security.sasl/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.security.sasl/share/classes/module-info.java); Git blob `8671c20af913a1ee34e85383f1772462d965bb16`
- Explicit requires: `java.logging`; `java.base` is implicitly readable
- Exports: `javax.security.sasl`; `com.sun.security.sasl.util` (qualified to jdk.security.jgss)
- Service consumers: none declared
- Service implementations: `java.security.Provider` → com.sun.security.sasl.Provider
- Package navigation: 5 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.smartcardio

Defines the Java Smart Card I/O API.

- Source and declaration: [src/java.smartcardio/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.smartcardio/share/classes/module-info.java); Git blob `60ac96c2b42de5212dcad452423b0fe60bf7c788`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `javax.smartcardio`
- Service consumers: none declared
- Service implementations: `java.security.Provider` → sun.security.smartcardio.SunPCSC
- Package navigation: 4 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.sql.rowset

Defines the JDBC RowSet API.

- Source and declaration: [src/java.sql.rowset/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.sql.rowset/share/classes/module-info.java); Git blob `6c09c98cca52f259050024b5308c76ada3ffd0a1`
- Explicit requires: `transitive java.logging`; `transitive java.naming`; `transitive java.sql`; `java.base` is implicitly readable
- Exports: `javax.sql.rowset`; `javax.sql.rowset.serial`; `javax.sql.rowset.spi`
- Service consumers: `javax.sql.rowset.RowSetFactory`
- Service implementations: none declared
- Package navigation: 6 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.sql

Defines the JDBC API.

- Source and declaration: [src/java.sql/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.sql/share/classes/module-info.java); Git blob `0ae1131297c373f197e675d5a679f441e9ae6f70`
- Explicit requires: `transitive java.logging`; `transitive java.transaction.xa`; `transitive java.xml`; `java.base` is implicitly readable
- Exports: `java.sql`; `javax.sql`
- Service consumers: `java.sql.Driver`
- Service implementations: none declared
- Package navigation: 2 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.transaction.xa

Defines an API for supporting distributed transactions in JDBC.

- Source and declaration: [src/java.transaction.xa/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.transaction.xa/share/classes/module-info.java); Git blob `f3a3072e84fa81ab79fdd160cc463a4177559bad`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `javax.transaction.xa`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 1 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.xml.crypto

Defines the API for XML cryptography.

- Source and declaration: [src/java.xml.crypto/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.xml.crypto/share/classes/module-info.java); Git blob `e6c0bb6aa37f955b5254402be9689a77d895b64c`
- Explicit requires: `java.logging`; `transitive java.xml`; `java.base` is implicitly readable
- Exports: `javax.xml.crypto`; `javax.xml.crypto.dom`; `javax.xml.crypto.dsig`; `javax.xml.crypto.dsig.dom`; `javax.xml.crypto.dsig.keyinfo`; `javax.xml.crypto.dsig.spec`
- Service consumers: none declared
- Service implementations: `java.security.Provider` → org.jcp.xml.dsig.internal.dom.XMLDSigRI
- Package navigation: 33 source directories, 6 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jaxp](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jaxp); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### java.xml

Defines the Java APIs for XML Processing (JAXP).

- Source and declaration: [src/java.xml/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/java.xml/share/classes/module-info.java); Git blob `a39b490a9cb8467fd0f1bdfec7b3433ad4549d07`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `javax.xml`; `javax.xml.catalog`; `javax.xml.datatype`; `javax.xml.namespace`; `javax.xml.parsers`; `javax.xml.stream`; `javax.xml.stream.events`; `javax.xml.stream.util`; `javax.xml.transform`; `javax.xml.transform.dom`; `javax.xml.transform.sax`; `javax.xml.transform.stax`; `javax.xml.transform.stream`; `javax.xml.validation`; `javax.xml.xpath`; `org.w3c.dom`; `org.w3c.dom.bootstrap`; `org.w3c.dom.events`; `org.w3c.dom.ls`; `org.w3c.dom.ranges`; `org.w3c.dom.traversal`; `org.w3c.dom.views`; `org.xml.sax`; `org.xml.sax.ext`; `org.xml.sax.helpers`; `com.sun.org.apache.xml.internal.dtm` (qualified to java.xml.crypto); `com.sun.org.apache.xml.internal.utils` (qualified to java.xml.crypto); `com.sun.org.apache.xpath.internal` (qualified to java.xml.crypto); `com.sun.org.apache.xpath.internal.compiler` (qualified to java.xml.crypto); `com.sun.org.apache.xpath.internal.functions` (qualified to java.xml.crypto); `com.sun.org.apache.xpath.internal.objects` (qualified to java.xml.crypto); `com.sun.org.apache.xpath.internal.res` (qualified to java.xml.crypto)
- Service consumers: `javax.xml.datatype.DatatypeFactory`, `javax.xml.parsers.DocumentBuilderFactory`, `javax.xml.parsers.SAXParserFactory`, `javax.xml.stream.XMLEventFactory`, `javax.xml.stream.XMLInputFactory`, `javax.xml.stream.XMLOutputFactory`, `javax.xml.transform.TransformerFactory`, `javax.xml.validation.SchemaFactory`, `javax.xml.xpath.XPathFactory`, `org.xml.sax.XMLReader`
- Service implementations: none declared
- Package navigation: 103 source directories, 44 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jaxp](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jaxp); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.accessibility

Defines JDK utility classes used by implementors of Assistive Technologies.

- Source and declaration: [src/jdk.accessibility/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.accessibility/share/classes/module-info.java); Git blob `46baa7d7feefe04304769834899d5ffda1b0bc18`
- Explicit requires: `transitive java.desktop`; `java.base` is implicitly readable
- Exports: `com.sun.java.accessibility.util`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 3 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.attach

Defines the attach API.

- Source and declaration: [src/jdk.attach/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.attach/share/classes/module-info.java); Git blob `0410fd99d03373974cb9f3e84db94b0e21f791e2`
- Explicit requires: `jdk.internal.jvmstat`; `java.base` is implicitly readable
- Exports: `com.sun.tools.attach`; `com.sun.tools.attach.spi`; `sun.tools.attach` (qualified to jdk.jcmd)
- Service consumers: `com.sun.tools.attach.spi.AttachProvider`
- Service implementations: `com.sun.tools.attach.spi.AttachProvider` → sun.tools.attach.AttachProviderImpl
- Package navigation: 7 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.charsets

Provides charsets that are not in java.base (mostly double byte and IBM charsets).

- Source and declaration: [src/jdk.charsets/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.charsets/share/classes/module-info.java); Git blob `95b11dc76aba2b3e1a0ac91b193646c0d3c89403`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `java.nio.charset.spi.CharsetProvider` → sun.nio.cs.ext.ExtendedCharsets
- Package navigation: 1 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.compiler

Defines the implementation of the compiler and its command line equivalent, javac javac tool .

- Source and declaration: [src/jdk.compiler/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.compiler/share/classes/module-info.java); Git blob `79f2b8f270494c8a210bd76a02cbb99bdbac1728`
- Explicit requires: `transitive java.compiler`; `jdk.internal.opt`; `jdk.zipfs`; `java.base` is implicitly readable
- Exports: `com.sun.source.doctree`; `com.sun.source.tree`; `com.sun.source.util`; `com.sun.tools.javac`; `com.sun.tools.doclint` (qualified to jdk.javadoc); `com.sun.tools.javac.api` (qualified to jdk.javadoc, jdk.jshell); `com.sun.tools.javac.resources` (qualified to jdk.jshell); `com.sun.tools.javac.code` (qualified to jdk.javadoc, jdk.jshell); `com.sun.tools.javac.comp` (qualified to jdk.javadoc, jdk.jshell); `com.sun.tools.javac.file` (qualified to jdk.jdeps, jdk.javadoc); `com.sun.tools.javac.jvm` (qualified to jdk.javadoc); `com.sun.tools.javac.main` (qualified to jdk.javadoc, jdk.jshell); `com.sun.tools.javac.model` (qualified to jdk.javadoc); `com.sun.tools.javac.parser` (qualified to jdk.jshell); `com.sun.tools.javac.platform` (qualified to jdk.jdeps, jdk.javadoc); `com.sun.tools.javac.tree` (qualified to jdk.javadoc, jdk.jshell); `com.sun.tools.javac.util` (qualified to jdk.jdeps, jdk.javadoc, jdk.jshell); `jdk.internal.shellsupport.doc` (qualified to jdk.jshell)
- Service consumers: `javax.annotation.processing.Processor`, `com.sun.source.util.Plugin`, `com.sun.tools.doclint.DocLint`, `com.sun.tools.javac.platform.PlatformProvider`
- Service implementations: `java.util.spi.ToolProvider` → com.sun.tools.javac.main.JavacToolProvider; `com.sun.tools.javac.platform.PlatformProvider` → com.sun.tools.javac.platform.JDKPlatformProvider; `javax.tools.JavaCompiler` → com.sun.tools.javac.api.JavacTool; `javax.tools.Tool` → com.sun.tools.javac.api.JavacTool
- Package navigation: 20 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/langtools](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/langtools); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.crypto.cryptoki

Provides the implementation of the SunPKCS11 security provider.

- Source and declaration: [src/jdk.crypto.cryptoki/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.crypto.cryptoki/share/classes/module-info.java); Git blob `3c90b6dfe6c374bdb6674b352c97291388511cb7`
- Explicit requires: `jdk.crypto.ec`; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `java.security.Provider` → sun.security.pkcs11.SunPKCS11
- Package navigation: 2 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.crypto.ec

Provides the implementation of the SunEC security provider.

- Source and declaration: [src/jdk.crypto.ec/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.crypto.ec/share/classes/module-info.java); Git blob `9ba6e6195e3d70a647d3d5915f7c2f482f9ca61c`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `java.security.Provider` → sun.security.ec.SunEC
- Package navigation: 3 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.crypto.mscapi

Provides the implementation of the SunMSCAPI security provider.

- Source and declaration: [src/jdk.crypto.mscapi/windows/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.crypto.mscapi/windows/classes/module-info.java); Git blob `6bbad56ca09e1dd07adbf909f78e01dcc394d3b0`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `java.security.Provider` → sun.security.mscapi.SunMSCAPI
- Package navigation: 1 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.dynalink

Defines the API for dynamic linking of high-level operations on objects.

- Source and declaration: [src/jdk.dynalink/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.dynalink/share/classes/module-info.java); Git blob `3a79c4b8e603442be8eecf49291cf8d41fc90de4`
- Explicit requires: `java.logging`; `java.base` is implicitly readable
- Exports: `jdk.dynalink`; `jdk.dynalink.beans`; `jdk.dynalink.linker`; `jdk.dynalink.linker.support`; `jdk.dynalink.support`
- Service consumers: `jdk.dynalink.linker.GuardingDynamicLinkerExporter`
- Service implementations: none declared
- Package navigation: 6 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.editpad

Provides the implementation of the edit pad service used by jdk.jshell.

- Source and declaration: [src/jdk.editpad/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.editpad/share/classes/module-info.java); Git blob `073443b89482a23231a89a7d861ef9c2c77002ed`
- Explicit requires: `java.desktop`; `jdk.internal.ed`; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `jdk.internal.editor.spi.BuildInEditorProvider` → jdk.editpad.EditPadProvider
- Package navigation: 1 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.hotspot.agent

Defines the implementation of the HotSpot Serviceability Agent.

- Source and declaration: [src/jdk.hotspot.agent/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.hotspot.agent/share/classes/module-info.java); Git blob `8015105d8df8be040ba1802be7f8d23aea3515e7`
- Explicit requires: `java.datatransfer`; `java.desktop`; `java.rmi`; `java.scripting`; `java.base` is implicitly readable
- Exports: `sun.jvm.hotspot.debugger.remote` (qualified to java.rmi)
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 89 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/hotspot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/hotspot); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.httpserver

Defines the JDK-specific HTTP server API, and provides the jwebserver tool for running a minimal HTTP server.

- Source and declaration: [src/jdk.httpserver/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.httpserver/share/classes/module-info.java); Git blob `46dbb6ea64f6843ee3aba111313f283913ce5444`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `com.sun.net.httpserver`; `com.sun.net.httpserver.spi`
- Service consumers: `com.sun.net.httpserver.spi.HttpServerProvider`
- Service implementations: none declared
- Package navigation: 6 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.incubator.vector

Defines an API for expressing computations that can be reliably compiled at runtime into SIMD instructions, such as AVX instructions on x64, and NEON instructions on AArch64.

- Source and declaration: [src/jdk.incubator.vector/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.incubator.vector/share/classes/module-info.java); Git blob `eda1f10b26bf21f47ddb64b65d388bb1d81440ee`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.incubator.vector`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 1 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.ed

Internal editor support for JDK tools.

- Source and declaration: [src/jdk.internal.ed/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.ed/share/classes/module-info.java); Git blob `e1169a50534a3efcb230a530ebf90c5374f4ad20`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.internal.editor.spi` (qualified to jdk.editpad, jdk.jshell); `jdk.internal.editor.external` (qualified to jdk.jshell)
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 2 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.jvmstat

Defines an internal API for monitoring a HotSpot Java Virtual Machine using its shared memory instrumentation buffer.

- Source and declaration: [src/jdk.internal.jvmstat/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.jvmstat/share/classes/module-info.java); Git blob `9e12c98b0168f2b57070877d43657971645eb663`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `sun.jvmstat.monitor` (qualified to jdk.attach, jdk.jcmd, jdk.jconsole, jdk.jstatd); `sun.jvmstat.monitor.event` (qualified to jdk.jcmd, jdk.jstatd); `sun.jvmstat.perfdata.monitor` (qualified to jdk.jstatd)
- Service consumers: `sun.jvmstat.monitor.MonitoredHostService`
- Service implementations: `sun.jvmstat.monitor.MonitoredHostService` → sun.jvmstat.perfdata.monitor.protocol.file.MonitoredHostFileService, sun.jvmstat.perfdata.monitor.protocol.local.MonitoredHostLocalService
- Package navigation: 9 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.le

Internal API for line editing.

- Source and declaration: [src/jdk.internal.le/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.le/share/classes/module-info.java); Git blob `fdc973d36ca23f90fdd9e2e83ee04cd631b84fa1`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.internal.org.jline.keymap` (qualified to jdk.jshell); `jdk.internal.org.jline.reader` (qualified to jdk.jshell); `jdk.internal.org.jline.reader.impl` (qualified to jdk.jshell); `jdk.internal.org.jline.reader.impl.completer` (qualified to jdk.jshell); `jdk.internal.org.jline.reader.impl.history` (qualified to jdk.jshell); `jdk.internal.org.jline.terminal.impl` (qualified to jdk.jshell); `jdk.internal.org.jline.terminal` (qualified to jdk.jshell); `jdk.internal.org.jline.utils` (qualified to jdk.jshell); `jdk.internal.org.jline.terminal.spi` (qualified to jdk.jshell)
- Service consumers: none declared
- Service implementations: `jdk.internal.io.JdkConsoleProvider` → jdk.internal.org.jline.JdkConsoleProviderImpl
- Package navigation: 20 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.opt

Internal option processing API.

- Source and declaration: [src/jdk.internal.opt/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.opt/share/classes/module-info.java); Git blob `67ed1410560dff79bd013be8115fde35c0a80139`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.internal.joptsimple` (qualified to jdk.jlink, jdk.jshell); `jdk.internal.opt` (qualified to jdk.compiler, jdk.jartool, jdk.javadoc, jdk.jlink, jdk.jpackage)
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 4 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.vm.ci

Internal JVM compiler interface and compiler/runtime support, as exposed by the qualified exports in its descriptor.

- Source and declaration: [src/jdk.internal.vm.ci/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.vm.ci/share/classes/module-info.java); Git blob `b4b93f6a747124147bd42d51d914ad88f3c14c7c`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.vm.ci.services` (qualified to jdk.internal.vm.compiler, jdk.internal.vm.compiler.management); `jdk.vm.ci.runtime` (qualified to jdk.internal.vm.compiler, jdk.internal.vm.compiler.management); `jdk.vm.ci.meta` (qualified to jdk.internal.vm.compiler); `jdk.vm.ci.code` (qualified to jdk.internal.vm.compiler); `jdk.vm.ci.hotspot` (qualified to jdk.internal.vm.compiler)
- Service consumers: `jdk.vm.ci.services.JVMCIServiceLocator`, `jdk.vm.ci.hotspot.HotSpotJVMCIBackendFactory`
- Service implementations: `jdk.vm.ci.hotspot.HotSpotJVMCIBackendFactory` → jdk.vm.ci.hotspot.aarch64.AArch64HotSpotJVMCIBackendFactory, jdk.vm.ci.hotspot.amd64.AMD64HotSpotJVMCIBackendFactory, jdk.vm.ci.hotspot.riscv64.RISCV64HotSpotJVMCIBackendFactory
- Package navigation: 14 source directories, 14 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/hotspot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/hotspot); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.vm.compiler.management

Registers JVMCI compiler specific management interfaces for the JVM.

- Source and declaration: [src/jdk.internal.vm.compiler.management/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.vm.compiler.management/share/classes/module-info.java); Git blob `88a5d933fca67c9bdb7ae554be159359f39a36c3`
- Explicit requires: `jdk.internal.vm.ci`; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 0 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/hotspot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/hotspot); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.internal.vm.compiler

JVMCI compiler implementation for the JVM.

- Source and declaration: [src/jdk.internal.vm.compiler/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.internal.vm.compiler/share/classes/module-info.java); Git blob `7284f2f225d3bf0b6659aacae508710d295179d6`
- Explicit requires: `jdk.internal.vm.ci`; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 0 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/hotspot](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/hotspot); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jartool

Defines tools for manipulating Java Archive (JAR) files, including the jar jar tool and jarsigner jarsigner tool tools.

- Source and declaration: [src/jdk.jartool/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jartool/share/classes/module-info.java); Git blob `ce7f1d0ba373d847862afd04cdbd6162ffaa64e4`
- Explicit requires: `jdk.internal.opt`; `java.base` is implicitly readable
- Exports: `jdk.security.jarsigner`
- Service consumers: none declared
- Service implementations: `java.util.spi.ToolProvider` → sun.tools.jar.JarToolProvider
- Package navigation: 3 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.javadoc

Defines the implementation of the tool and its command-line equivalent, javadoc javadoc tool .

- Source and declaration: [src/jdk.javadoc/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.javadoc/share/classes/module-info.java); Git blob `ee84482204a2b04b693a235fca3bf199fb587d99`
- Explicit requires: `java.xml`; `transitive java.compiler`; `transitive jdk.compiler`; `jdk.internal.opt`; `java.base` is implicitly readable
- Exports: `jdk.javadoc.doclet`
- Service consumers: none declared
- Service implementations: `java.util.spi.ToolProvider` → jdk.javadoc.internal.tool.JavadocToolProvider; `javax.tools.DocumentationTool` → jdk.javadoc.internal.api.JavadocTool; `javax.tools.Tool` → jdk.javadoc.internal.api.JavadocTool; `com.sun.tools.doclint.DocLint` → jdk.javadoc.internal.doclint.DocLint
- Package navigation: 14 source directories, 12 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/langtools](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/langtools); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jcmd

Defines tools for diagnostics and troubleshooting a JVM such as the jcmd jcmd tool , jps jps tool , jstat jstat tool tools.

- Source and declaration: [src/jdk.jcmd/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jcmd/share/classes/module-info.java); Git blob `a26565b53967005815d111a37d8813584e5f7e5e`
- Explicit requires: `jdk.attach`; `jdk.internal.jvmstat`; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 8 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jconsole

Defines the JMX graphical tool, jconsole jconsole , for monitoring and managing a running application.

- Source and declaration: [src/jdk.jconsole/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jconsole/share/classes/module-info.java); Git blob `d12d7391c7b9be2aa7112b4d4d7e2279b34ad2c6`
- Explicit requires: `java.management.rmi`; `java.rmi`; `jdk.attach`; `jdk.internal.jvmstat`; `jdk.management`; `jdk.management.agent`; `transitive java.desktop`; `transitive java.management`; `java.base` is implicitly readable
- Exports: `com.sun.tools.jconsole`
- Service consumers: `com.sun.tools.jconsole.JConsolePlugin`
- Service implementations: none declared
- Package navigation: 3 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jdeps

Defines tools for analysing dependencies in Java libraries and programs, including the jdeps jdeps tool , javap javap tool , and jdeprscan jdeprscan tool tools.

- Source and declaration: [src/jdk.jdeps/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jdeps/share/classes/module-info.java); Git blob `a3f47954842ab429e00b48a48034bc288e3c1345`
- Explicit requires: `java.compiler`; `jdk.compiler`; `java.base` is implicitly readable
- Exports: `com.sun.tools.classfile` (qualified to jdk.jlink)
- Service consumers: none declared
- Service implementations: `java.util.spi.ToolProvider` → com.sun.tools.javap.Main.JavapToolProvider, com.sun.tools.jdeps.Main.JDepsToolProvider
- Package navigation: 5 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/langtools](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/langtools); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jdi

Defines the Java Debug Interface.

- Source and declaration: [src/jdk.jdi/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jdi/share/classes/module-info.java); Git blob `3bd2f7fc294b5579373a5c6c1146e8b5df9fb71a`
- Explicit requires: `jdk.attach`; `jdk.jdwp.agent`; `java.base` is implicitly readable
- Exports: `com.sun.jdi`; `com.sun.jdi.connect`; `com.sun.jdi.connect.spi`; `com.sun.jdi.event`; `com.sun.jdi.request`
- Service consumers: `com.sun.jdi.connect.Connector`, `com.sun.jdi.connect.spi.TransportService`
- Service implementations: `com.sun.jdi.connect.Connector` → com.sun.tools.jdi.ProcessAttachingConnector, com.sun.tools.jdi.RawCommandLineLauncher, com.sun.tools.jdi.SocketAttachingConnector, com.sun.tools.jdi.SocketListeningConnector, com.sun.tools.jdi.SunCommandLineLauncher
- Package navigation: 9 source directories, 5 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jdwp.agent

Provides the implementation of the Java Debug Wire Protocol (JDWP) agent.

- Source and declaration: [src/jdk.jdwp.agent/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jdwp.agent/share/classes/module-info.java); Git blob `0d7e888e7e963a9c160c20e29a0fa146f5a09a56`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 0 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jfr

Defines the API for JDK Flight Recorder.

- Source and declaration: [src/jdk.jfr/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jfr/share/classes/module-info.java); Git blob `aab58c319537c91f62298c2db84c1f2949372d2a`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.jfr`; `jdk.jfr.consumer`; `jdk.jfr.internal.management` (qualified to jdk.management.jfr)
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 20 source directories, 3 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jlink

Defines the jlink jlink tool tool for creating run-time images, the jmod jmod tool tool for creating and manipulating JMOD files, and the jimage jimage tool tool for inspecting the JDK implementation-specific container file for classes and resources.

- Source and declaration: [src/jdk.jlink/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jlink/share/classes/module-info.java); Git blob `a7bb8bce18744cada731e40c9ff7c85c6f8a72bb`
- Explicit requires: `jdk.internal.opt`; `jdk.jdeps`; `java.base` is implicitly readable
- Exports: none
- Service consumers: `jdk.tools.jlink.plugin.Plugin`
- Service implementations: `java.util.spi.ToolProvider` → jdk.tools.jmod.Main.JmodToolProvider, jdk.tools.jlink.internal.Main.JlinkToolProvider; `jdk.tools.jlink.plugin.Plugin` → jdk.tools.jlink.internal.plugins.DefaultStripDebugPlugin, jdk.tools.jlink.internal.plugins.StripJavaDebugAttributesPlugin, jdk.tools.jlink.internal.plugins.ExcludePlugin, jdk.tools.jlink.internal.plugins.ExcludeFilesPlugin, jdk.tools.jlink.internal.plugins.ExcludeJmodSectionPlugin, jdk.tools.jlink.internal.plugins.LegalNoticeFilePlugin, jdk.tools.jlink.internal.plugins.SystemModulesPlugin, jdk.tools.jlink.internal.plugins.StripNativeCommandsPlugin, jdk.tools.jlink.internal.plugins.OrderResourcesPlugin, jdk.tools.jlink.internal.plugins.DefaultCompressPlugin, jdk.tools.jlink.internal.plugins.ExcludeVMPlugin, jdk.tools.jlink.internal.plugins.IncludeLocalesPlugin, jdk.tools.jlink.internal.plugins.GenerateJLIClassesPlugin, jdk.tools.jlink.internal.plugins.ReleaseInfoPlugin, jdk.tools.jlink.internal.plugins.AddOptionsPlugin, jdk.tools.jlink.internal.plugins.VendorBugURLPlugin, jdk.tools.jlink.internal.plugins.VendorVMBugURLPlugin, jdk.tools.jlink.internal.plugins.VendorVersionPlugin, jdk.tools.jlink.internal.plugins.CDSPlugin, jdk.tools.jlink.internal.plugins.SaveJlinkArgfilesPlugin
- Package navigation: 7 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jpackage

Defines the Java Packaging tool, jpackage.

- Source and declaration: [src/jdk.jpackage/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jpackage/share/classes/module-info.java); Git blob `ea41abd75d8325451c7603048fdab93b843fed13`
- Explicit requires: `jdk.internal.opt`; `jdk.jlink`; `java.desktop`; `java.base` is implicitly readable
- Exports: none
- Service consumers: `jdk.jpackage.internal.Bundler`, `jdk.jpackage.internal.Bundlers`
- Service implementations: `jdk.jpackage.internal.Bundlers` → jdk.jpackage.internal.BasicBundlers; `java.util.spi.ToolProvider` → jdk.jpackage.internal.JPackageToolProvider
- Package navigation: 7 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jshell

Provides the jshell jshell tool tool for evaluating snippets of Java code, and defines a JDK-specific API for modeling and executing snippets.

- Source and declaration: [src/jdk.jshell/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jshell/share/classes/module-info.java); Git blob `ba3c99b63fa99c49c8d919e42062484a7c1d63cd`
- Explicit requires: `java.logging`; `jdk.compiler`; `jdk.internal.ed`; `jdk.internal.le`; `jdk.internal.opt`; `transitive java.compiler`; `transitive java.prefs`; `transitive jdk.jdi`; `java.base` is implicitly readable
- Exports: `jdk.jshell`; `jdk.jshell.execution`; `jdk.jshell.spi`; `jdk.jshell.tool`
- Service consumers: `jdk.jshell.spi.ExecutionControlProvider`, `jdk.internal.editor.spi.BuildInEditorProvider`
- Service implementations: `javax.tools.Tool` → jdk.internal.jshell.tool.JShellToolProvider; `jdk.jshell.spi.ExecutionControlProvider` → jdk.jshell.execution.JdiExecutionControlProvider, jdk.jshell.execution.LocalExecutionControlProvider, jdk.jshell.execution.FailOverExecutionControlProvider; `jdk.internal.io.JdkConsoleProvider` → jdk.jshell.execution.impl.ConsoleImpl.ConsoleProviderImpl
- Package navigation: 7 source directories, 4 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/langtools](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/langtools); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jsobject

Defines the API for the JavaScript Object.

- Source and declaration: [src/jdk.jsobject/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jsobject/share/classes/module-info.java); Git blob `7903dbb2c2433a70af620279f9685c685bf600d0`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `netscape.javascript`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 1 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.jstatd

Defines the jstatd jstatd tool tool for starting a daemon for the jstat tool to monitor JVM statistics remotely.

- Source and declaration: [src/jdk.jstatd/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.jstatd/share/classes/module-info.java); Git blob `ade59da4248cd1fd54db47bcd18e5d12e14da7bd`
- Explicit requires: `java.rmi`; `jdk.internal.jvmstat`; `java.base` is implicitly readable
- Exports: `sun.jvmstat.monitor.remote` (qualified to java.rmi)
- Service consumers: none declared
- Service implementations: `sun.jvmstat.monitor.MonitoredHostService` → sun.jvmstat.perfdata.monitor.protocol.rmi.MonitoredHostRmiService
- Package navigation: 3 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.localedata

Provides the locale data for locales other than locale.

- Source and declaration: [src/jdk.localedata/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.localedata/share/classes/module-info.java); Git blob `fadb091a92f99f876de43944b2e78f6fcd468313`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `sun.util.locale.provider.LocaleDataMetaInfo` → sun.util.resources.cldr.provider.CLDRLocaleDataMetaInfo, sun.util.resources.provider.NonBaseLocaleDataMetaInfo; `sun.util.resources.LocaleData.CommonResourceBundleProvider` → sun.util.resources.provider.LocaleDataProvider; `sun.util.resources.LocaleData.SupplementaryResourceBundleProvider` → sun.util.resources.provider.SupplementaryLocaleDataProvider
- Package navigation: 3 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.management.agent

Defines the JMX management agent.

- Source and declaration: [src/jdk.management.agent/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.management.agent/share/classes/module-info.java); Git blob `9688e22b9f9740eb4b3df2e38381a03edfc156cb`
- Explicit requires: `java.management`; `java.management.rmi`; `java.base` is implicitly readable
- Exports: `jdk.internal.agent` (qualified to jdk.jconsole)
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 5 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.management.jfr

Defines the Management Interface for JDK Flight Recorder.

- Source and declaration: [src/jdk.management.jfr/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.management.jfr/share/classes/module-info.java); Git blob `341d29d9cfe2f20842f4ef5ac65f25510a3f97e1`
- Explicit requires: `jdk.management`; `transitive java.management`; `transitive jdk.jfr`; `java.base` is implicitly readable
- Exports: `jdk.management.jfr`
- Service consumers: none declared
- Service implementations: `sun.management.spi.PlatformMBeanProvider` → jdk.management.jfr.internal.FlightRecorderMXBeanProvider
- Package navigation: 2 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.management

Defines JDK-specific management interfaces for the JVM.

- Source and declaration: [src/jdk.management/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.management/share/classes/module-info.java); Git blob `092480de9e5797b52f39bbd1368a04c1fa97d5e6`
- Explicit requires: `transitive java.management`; `java.base` is implicitly readable
- Exports: `com.sun.management`
- Service consumers: none declared
- Service implementations: `sun.management.spi.PlatformMBeanProvider` → com.sun.management.internal.PlatformMBeanProviderImpl
- Package navigation: 4 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.naming.dns

Provides the implementation of the DNS Java Naming provider.

- Source and declaration: [src/jdk.naming.dns/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.naming.dns/share/classes/module-info.java); Git blob `5665080e699d565726278412c2cd1d57ed9d377a`
- Explicit requires: `java.naming`; `java.base` is implicitly readable
- Exports: `com.sun.jndi.url.dns` (qualified to java.naming)
- Service consumers: none declared
- Service implementations: `javax.naming.spi.InitialContextFactory` → com.sun.jndi.dns.DnsContextFactory
- Package navigation: 2 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.naming.rmi

Provides the implementation of the RMI Java Naming provider.

- Source and declaration: [src/jdk.naming.rmi/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.naming.rmi/share/classes/module-info.java); Git blob `6afa13aa84dd820bebfdb3da4ce333ca4c021cbe`
- Explicit requires: `java.naming`; `java.rmi`; `java.base` is implicitly readable
- Exports: `com.sun.jndi.url.rmi` (qualified to java.naming); `com.sun.jndi.rmi.registry` (qualified to java.rmi)
- Service consumers: none declared
- Service implementations: `javax.naming.spi.InitialContextFactory` → com.sun.jndi.rmi.registry.RegistryContextFactory
- Package navigation: 2 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.net

Defines the JDK-specific Networking API.

- Source and declaration: [src/jdk.net/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.net/share/classes/module-info.java); Git blob `b141aa7f42d8e69529c35f4866b8b4c87b0bccfa`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.net`; `jdk.nio`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 6 source directories, 2 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.nio.mapmode

Defines JDK-specific file mapping modes.

- Source and declaration: [src/jdk.nio.mapmode/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.nio.mapmode/share/classes/module-info.java); Git blob `26b9c8489da12ab97889b89c8fbfa7bf2b7031cf`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.nio.mapmode`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 1 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.random

Defines implementations of the Interface.

- Source and declaration: [src/jdk.random/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.random/share/classes/module-info.java); Git blob `3d82090f313345e6e59b7fa23689a930591c28ba`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `jdk.random` (qualified to java.base)
- Service consumers: none declared
- Service implementations: `java.util.random.RandomGenerator` → jdk.random.L32X64MixRandom, jdk.random.L64X128MixRandom, jdk.random.L64X128StarStarRandom, jdk.random.L64X256MixRandom, jdk.random.L64X1024MixRandom, jdk.random.L128X128MixRandom, jdk.random.L128X256MixRandom, jdk.random.L128X1024MixRandom, jdk.random.Xoroshiro128PlusPlus, jdk.random.Xoshiro256PlusPlus
- Package navigation: 1 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.sctp

Defines the JDK-specific API for SCTP.

- Source and declaration: [src/jdk.sctp/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.sctp/share/classes/module-info.java); Git blob `f150ed25fc610a70395c0cd5157d6724aebfb8e9`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `com.sun.nio.sctp`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 3 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.security.auth

Provides implementations of the javax.security.auth.* interfaces and various authentication modules.

- Source and declaration: [src/jdk.security.auth/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.security.auth/share/classes/module-info.java); Git blob `50791d55c6b03933f53d58aa7297aeae03ad8b6f`
- Explicit requires: `java.security.jgss`; `transitive java.naming`; `java.base` is implicitly readable
- Exports: `com.sun.security.auth`; `com.sun.security.auth.callback`; `com.sun.security.auth.login`; `com.sun.security.auth.module`
- Service consumers: none declared
- Service implementations: `javax.security.auth.spi.LoginModule` → com.sun.security.auth.module.Krb5LoginModule, com.sun.security.auth.module.UnixLoginModule, com.sun.security.auth.module.JndiLoginModule, com.sun.security.auth.module.KeyStoreLoginModule, com.sun.security.auth.module.LdapLoginModule, com.sun.security.auth.module.NTLoginModule
- Package navigation: 4 source directories, 4 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.security.jgss

Defines JDK extensions to the GSS-API and an implementation of the SASL GSSAPI mechanism.

- Source and declaration: [src/jdk.security.jgss/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.security.jgss/share/classes/module-info.java); Git blob `d8c0473660afe2d50f53d57af985654987b1127e`
- Explicit requires: `java.logging`; `java.security.sasl`; `transitive java.security.jgss`; `java.base` is implicitly readable
- Exports: `com.sun.security.jgss`
- Service consumers: none declared
- Service implementations: `java.security.Provider` → com.sun.security.sasl.gsskerb.JdkSASL
- Package navigation: 2 source directories, 1 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.unsupported.desktop

This module is intended for use only by the javafx.swing module for the purpose of FX/Swing interop.

- Source and declaration: [src/jdk.unsupported.desktop/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.unsupported.desktop/share/classes/module-info.java); Git blob `d307f95a0842bd4ed41b8d6e167bf7891bf819d2`
- Explicit requires: `transitive java.desktop`; `java.base` is implicitly readable
- Exports: `jdk.swing.interop`
- Service consumers: none declared
- Service implementations: `sun.swing.InteropProvider` → jdk.swing.interop.internal.InteropProviderImpl
- Package navigation: 2 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.unsupported

Compatibility access to the unsupported sun.misc, sun.reflect and com.sun.nio.file packages; inspect the exports and opens before use.

- Source and declaration: [src/jdk.unsupported/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.unsupported/share/classes/module-info.java); Git blob `7ec44647dd4afa2af5fd5e66225db32e42bd9bfa`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: `com.sun.nio.file`; `sun.misc`; `sun.reflect`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 3 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.xml.dom

Defines the subset of the W3C Document Object Model (DOM) API that is not part of the Java SE API.

- Source and declaration: [src/jdk.xml.dom/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.xml.dom/share/classes/module-info.java); Git blob `d033c2a76a60d4dde3814f422e13c3bfa9e11138`
- Explicit requires: `transitive java.xml`; `java.base` is implicitly readable
- Exports: `org.w3c.dom.css`; `org.w3c.dom.html`; `org.w3c.dom.stylesheets`; `org.w3c.dom.xpath`
- Service consumers: none declared
- Service implementations: none declared
- Package navigation: 4 source directories, 4 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

### jdk.zipfs

Provides the implementation of the Zip file system provider.

- Source and declaration: [src/jdk.zipfs/share/classes/module-info.java](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/src/jdk.zipfs/share/classes/module-info.java); Git blob `b996006b4fe7b25056147ae67bea2bac7c9b6511`
- Explicit requires: none; `java.base` is implicitly readable
- Exports: none
- Service consumers: none declared
- Service implementations: `java.nio.file.spi.FileSystemProvider` → jdk.nio.zipfs.ZipFileSystemProvider
- Package navigation: 1 source directories, 0 with a tracked package-info.java or package.html; see [package directory inventory](packages/README.md)
- Test starting point: [test/jdk](https://github.com/hsoliwal/M3jdk21/blob/278b221e8365ebeb6095a3a5643b38a0564258b5/test/jdk); this is a harness routing hint, not a mapped or executed package test suite
- Evidence: descriptor observed; implementation/API lifecycle coverage and exact-head tests remain partial or unreviewed

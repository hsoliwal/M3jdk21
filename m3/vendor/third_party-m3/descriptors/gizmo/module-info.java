module gizmo {
    requires org.objectweb.asm.util;

    requires transitive org.jboss.jandex;
    requires transitive org.objectweb.asm;

    exports io.quarkus.gizmo;

}

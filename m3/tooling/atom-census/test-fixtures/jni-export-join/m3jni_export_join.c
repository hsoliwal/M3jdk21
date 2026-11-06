#include <jni.h>
JNIEXPORT jint JNICALL Java_demo_pkg_Outer_count(JNIEnv *env, jclass klass, jintArray values) {
    (void)klass;
    jsize n = (*env)->GetArrayLength(env, values);
    if ((*env)->ExceptionCheck(env)) return 0;
    jint sum=0;
    for (jsize i=0;i<n;i++) {
        jint x=0;
        (*env)->GetIntArrayRegion(env, values, i, 1, &x);
        if ((*env)->ExceptionCheck(env)) return 0;
        sum+=x;
    }
    return sum;
}
JNIEXPORT jint JNICALL Java_demo_pkg_Outer_ping(JNIEnv *env, jclass klass) {
    (void)env; (void)klass;
    return 7;
}
JNIEXPORT jint JNICALL Java_demo_pkg_Outer_00024Inner_do_1Thing(JNIEnv *env, jobject self, jint value) {
    (void)env; (void)self;
    return value + 2;
}
JNIEXPORT jint JNICALL Java_demo_pkg_Outer_overloaded__I(JNIEnv *env, jobject self, jint value) {
    (void)env; (void)self; return value + 2;
}
JNIEXPORT jint JNICALL Java_demo_pkg_Outer_overloaded___3B(JNIEnv *env, jobject self, jbyteArray values) {
    (void)self; return (*env)->GetArrayLength(env, values);
}

/* ML59_BENCH v2: width-1 sparse (.* then literals) and dead (literal miss) throughput, master vs restored advance0 (fake JNIEnv). */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
typedef struct { jsize n; void *data; } FakeArr;
static jsize JNICALL fLen(JNIEnv *e, jarray a) { (void) e; return ((FakeArr *) a)->n; }
static void JNICALL fInt(JNIEnv *e, jintArray a, jsize s, jsize n, jint *b) { (void) e; memcpy(b, (jint *) ((FakeArr *) a)->data + s, (size_t) n * sizeof(jint)); }
static void JNICALL fLong(JNIEnv *e, jlongArray a, jsize s, jsize n, jlong *b) { (void) e; memcpy(b, (jlong *) ((FakeArr *) a)->data + s, (size_t) n * sizeof(jlong)); }
static jboolean JNICALL fExc(JNIEnv *e) { (void) e; return JNI_FALSE; }
typedef jlong (JNICALL *Adv)(JNIEnv *, jclass, jlong, jint, jint, jint, jint, jboolean, jlong, jintArray, jlongArray);
jlong JNICALL adv_master(JNIEnv *, jclass, jlong, jint, jint, jint, jint, jboolean, jlong, jintArray, jlongArray);
jlong JNICALL adv_restored(JNIEnv *, jclass, jlong, jint, jint, jint, jint, jboolean, jlong, jintArray, jlongArray);
static double run(Adv f, JNIEnv *env, unsigned char *t, int len, int count, jint *atoms, jlong *clo, jlong *out) {
    FakeArr A = { count, atoms }, C = { count + 1, clo }; clock_t s = clock(); jlong acc = 0;
    for (int k = 0; k < 4000; k++) acc += f(env, NULL, (jlong) (uintptr_t) t, len, 0, len, 1, JNI_FALSE, clo[0], (jintArray) &A, (jlongArray) &C);
    *out = acc; return (double) (clock() - s) / CLOCKS_PER_SEC;
}
int main(void) {
    struct JNINativeInterface_ fn; JNIEnv env = &fn; static unsigned char t[8192]; jint atoms[32]; jlong clo[33], o1, o2;
    int counts[] = { 3, 6, 12, 24 };
    memset(&fn, 0, sizeof fn); fn.GetArrayLength = fLen; fn.GetIntArrayRegion = fInt; fn.GetLongArrayRegion = fLong; fn.ExceptionCheck = fExc;
    for (int i = 0; i < 8192; i++) t[i] = (unsigned char) "abcab"[i % 5];
    for (int q = 0; q < 4; q++) {
        int count = counts[q];
        for (int i = 0; i < count; i++) atoms[i] = (jint) (q < 2 ? (i == 0 ? '.' | 65536 : 'z') : (i == 0 ? 'q' : 'a'));
        clo[count] = (jlong) (UINT64_C(1) << count);
        for (int i = count - 1; i >= 0; i--) clo[i] = (jlong) ((UINT64_C(1) << i) | ((atoms[i] & 65536) ? (uint64_t) clo[i + 1] : 0));
        double a = run(adv_master, &env, t, 8192, count, atoms, clo, &o1), b = run(adv_restored, &env, t, 8192, count, atoms, clo, &o2);
        printf("count=%d master=%.3fs restored=%.3fs ratio=%.2f same=%d\n", count, a, b, a / (b > 0 ? b : 1e-9), o1 == o2);
    }
    return 0;
}

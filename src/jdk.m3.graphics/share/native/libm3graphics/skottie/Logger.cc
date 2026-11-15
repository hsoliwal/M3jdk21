#include "jdk_m3_graphics_skottie_M3Logger.h"
#include <jni.h>
#include "interop.hh"
#include "Skottie.h"

using namespace skottie;

class SkijaLoggerImpl: public Logger {
public:
    SkijaLoggerImpl() {
    }

    ~SkijaLoggerImpl() {
        JNIScope scope;
        if (scope.env) scope.env->DeleteWeakGlobalRef(fObject);
    }

    void init(JNIEnv* e, jobject o) {
        fEnv = e;
        fObject = fEnv->NewWeakGlobalRef(o);
    }

public:
    void log(Level level, const char message[], const char* json = nullptr) override {
        jobject levelObj;
        switch (level) {
            case Logger::Level::kWarning:
                levelObj = skija::skottie::LogLevel::WARNING;
                break;
            default:
                levelObj = skija::skottie::LogLevel::ERROR;
        }

        fEnv->CallVoidMethod(fObject, skija::skottie::Logger::log, levelObj, javaString(fEnv, message), javaString(fEnv, json));
    }

private:
    JNIEnv* fEnv = nullptr;
    jobject fObject = nullptr;
};

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_skottie_M3Logger__1nMake
  (JNIEnv* env, jclass jclass) {
    SkijaLoggerImpl* instance = new SkijaLoggerImpl();
    return reinterpret_cast<jlong>(instance);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_skottie_M3Logger__1nInit
  (JNIEnv* env, jobject jthis, jlong ptr) {
    SkijaLoggerImpl* instance = reinterpret_cast<SkijaLoggerImpl*>(static_cast<uintptr_t>(ptr));
    instance->init(env, jthis);
}

#include "jdk_m3_graphics_M3DirectContext.h"
#include <iostream>
#include <jni.h>
#include "interop.hh"
#include "include/gpu/ganesh/GrTypes.h"
#include "include/gpu/ganesh/GrDirectContext.h"
#include "include/gpu/ganesh/gl/GrGLDirectContext.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeGL
  (JNIEnv* env, jclass jclass) {
    return reinterpret_cast<jlong>(GrDirectContexts::MakeGL().release());
}

#ifdef SK_METAL
#include "include/gpu/ganesh/mtl/GrMtlBackendContext.h"
#include "include/gpu/ganesh/mtl/GrMtlDirectContext.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeMetal
  (JNIEnv* env, jclass jclass, jlong devicePtr, jlong queuePtr) {
    GrMtlBackendContext backendContext = {};
    GrMTLHandle device = reinterpret_cast<GrMTLHandle>(static_cast<uintptr_t>(devicePtr));
    GrMTLHandle queue = reinterpret_cast<GrMTLHandle>(static_cast<uintptr_t>(queuePtr));
    backendContext.fDevice.retain(device);
    backendContext.fQueue.retain(queue);
    sk_sp<GrDirectContext> instance = GrDirectContexts::MakeMetal(backendContext);
    return reinterpret_cast<jlong>(instance.release());
}
#else
extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeMetal
  (JNIEnv* env, jclass jclass, jlong devicePtr, jlong queuePtr) {
    return skija::unavailableBackend(env, "Metal is unavailable in this jdk.m3.graphics native build");
}
#endif

#ifdef SK_DIRECT3D
#include "include/gpu/ganesh/d3d/GrD3DBackendContext.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeDirect3D
  (JNIEnv* env, jclass jclass, jlong adapterPtr, jlong devicePtr, jlong queuePtr) {
    GrD3DBackendContext backendContext = {};
    IDXGIAdapter1* adapter = reinterpret_cast<IDXGIAdapter1*>(static_cast<uintptr_t>(adapterPtr));
    ID3D12Device* device = reinterpret_cast<ID3D12Device*>(static_cast<uintptr_t>(devicePtr));
    ID3D12CommandQueue* queue = reinterpret_cast<ID3D12CommandQueue*>(static_cast<uintptr_t>(queuePtr));
    backendContext.fAdapter.retain(adapter);
    backendContext.fDevice.retain(device);
    backendContext.fQueue.retain(queue);
    sk_sp<GrDirectContext> instance = GrDirectContext::MakeDirect3D(backendContext);
    return reinterpret_cast<jlong>(instance.release());
}
#else
extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeDirect3D
  (JNIEnv* env, jclass jclass, jlong adapterPtr, jlong devicePtr, jlong queuePtr) {
    return skija::unavailableBackend(env, "Direct3D is unavailable in this jdk.m3.graphics native build");
}
#endif //SK_DIRECT3D

#ifdef SK_VULKAN
#include "include/gpu/ganesh/vk/GrVkDirectContext.h"
#include "include/gpu/ganesh/vk/GrVkTypes.h"
#include "include/gpu/vk/VulkanBackendContext.h"
#include "include/gpu/vk/VulkanExtensions.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeVulkan
  (JNIEnv* env, jclass jclass, jlong instancePtr, jlong physicalDevicePtr, jlong devicePtr, jlong queuePtr, jint graphicsQueueIndex, jlong instanceProcAddr, jlong getProcAddr, jint apiVersion) {
    skgpu::VulkanBackendContext backendContext = {};
    backendContext.fInstance = reinterpret_cast<VkInstance>(static_cast<uintptr_t>(instancePtr));
    backendContext.fPhysicalDevice = reinterpret_cast<VkPhysicalDevice>(static_cast<uintptr_t>(physicalDevicePtr));
    backendContext.fDevice = reinterpret_cast<VkDevice>(static_cast<uintptr_t>(devicePtr));
    backendContext.fQueue = reinterpret_cast<VkQueue>(static_cast<uintptr_t>(queuePtr));
    backendContext.fGraphicsQueueIndex = (uint32_t) graphicsQueueIndex;
    backendContext.fGetProc = [instanceProcAddr, getProcAddr](const char* name, VkInstance instance, VkDevice device) -> PFN_vkVoidFunction {
        if (device != VK_NULL_HANDLE) {
            return reinterpret_cast<PFN_vkGetDeviceProcAddr>(static_cast<uintptr_t>(getProcAddr))(device, name);
        }
        return reinterpret_cast<PFN_vkGetInstanceProcAddr>(static_cast<uintptr_t>(instanceProcAddr))(instance, name);
    };

    skgpu::VulkanExtensions* extensions = new skgpu::VulkanExtensions();
    extensions->init(backendContext.fGetProc, backendContext.fInstance, backendContext.fPhysicalDevice, 0, nullptr, 0, nullptr);
    backendContext.fVkExtensions = extensions;
    backendContext.fMaxAPIVersion = (uint32_t) apiVersion;

    sk_sp<GrDirectContext> instance = GrDirectContexts::MakeVulkan(backendContext);
    return reinterpret_cast<jlong>(instance.release());
}
#else
extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3DirectContext__1nMakeVulkan
  (JNIEnv* env, jclass jclass, jlong instancePtr, jlong physicalDevicePtr, jlong devicePtr,
   jlong queuePtr, jint graphicsQueueIndex, jlong instanceProcAddr, jlong getProcAddr, jint apiVersion) {
    return skija::unavailableBackend(env, "Vulkan is unavailable in this jdk.m3.graphics native build");
}
#endif // SK_VULKAN

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3DirectContext__1nFlush
  (JNIEnv* env, jclass jclass, jlong ptr) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    context->flush(GrFlushInfo());
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3DirectContext__1nSubmit
  (JNIEnv* env, jclass jclass, jlong ptr, jboolean syncCpu) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    return context->submit(syncCpu ? GrSyncCpu::kYes : GrSyncCpu::kNo);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3DirectContext__1nFlushAndSubmit
  (JNIEnv* env, jclass jclass, jlong ptr, jboolean syncCpu) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    context->flushAndSubmit(syncCpu ? GrSyncCpu::kYes : GrSyncCpu::kNo);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3DirectContext__1nFlushSurface
  (JNIEnv* env, jclass jclass, jlong ptr, jlong surfacePtr) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    SkSurface* surface = reinterpret_cast<SkSurface*>(static_cast<uintptr_t>(surfacePtr));
    context->flush(surface);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3DirectContext__1nFlushAndSubmitSurface
  (JNIEnv* env, jclass jclass, jlong ptr, jlong surfacePtr, jboolean syncCpu) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    SkSurface* surface = reinterpret_cast<SkSurface*>(static_cast<uintptr_t>(surfacePtr));
    context->flushAndSubmit(surface, syncCpu ? GrSyncCpu::kYes : GrSyncCpu::kNo);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3DirectContext__1nReset
  (JNIEnv* env, jclass jclass, jlong ptr, jint flags) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    context->resetContext((uint32_t) flags);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3DirectContext__1nAbandon
  (JNIEnv* env, jclass jclass, jlong ptr) {
    GrDirectContext* context = reinterpret_cast<GrDirectContext*>(static_cast<uintptr_t>(ptr));
    context->abandonContext();
}


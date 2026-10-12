/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_cl_abi.h - the OpenCL 1.2 C ABI subset used by the m3vcpu OpenCL backend, self-declared
 * so that no SDK header or import library is needed: OpenCL.dll / libOpenCL.so.1 is loaded at
 * run time and every entry point is resolved by name. Enum values and signatures follow the
 * Khronos OpenCL 1.2 specification (Khronos OpenCL-Headers cl.h, Apache-2.0; values restated,
 * no header text copied). Symbol-table X-macro pattern: IREE cuda_dynamic_symbol_table.h
 * (Apache-2.0 WITH LLVM-exception), pattern only.
 */
#ifndef M3VCPU_CL_ABI_H
#define M3VCPU_CL_ABI_H

#include <stddef.h>
#include <stdint.h>

#if defined(_WIN32)
#define M3CL_CALL __stdcall
#else
#define M3CL_CALL
#endif

typedef int32_t m3cl_int;
typedef uint32_t m3cl_uint;
typedef uint64_t m3cl_ulong;
typedef void *m3cl_platform;
typedef void *m3cl_device;
typedef void *m3cl_context;
typedef void *m3cl_queue;
typedef void *m3cl_mem;
typedef void *m3cl_program;
typedef void *m3cl_kernel;

#define M3CL_SUCCESS 0
#define M3CL_TRUE 1u

#define M3CL_PLATFORM_VERSION 0x0901u
#define M3CL_PLATFORM_NAME 0x0902u

#define M3CL_DEVICE_TYPE_CPU 0x2ull
#define M3CL_DEVICE_TYPE_GPU 0x4ull
#define M3CL_DEVICE_TYPE_ACCELERATOR 0x8ull
#define M3CL_DEVICE_TYPE_ALL 0xFFFFFFFFull

#define M3CL_DEVICE_TYPE 0x1000u
#define M3CL_DEVICE_MAX_COMPUTE_UNITS 0x1002u
#define M3CL_DEVICE_MAX_WORK_GROUP_SIZE 0x1004u
#define M3CL_DEVICE_MAX_MEM_ALLOC_SIZE 0x1010u
#define M3CL_DEVICE_GLOBAL_MEM_SIZE 0x101Fu
#define M3CL_DEVICE_AVAILABLE 0x1027u
#define M3CL_DEVICE_COMPILER_AVAILABLE 0x1028u
#define M3CL_DEVICE_NAME 0x102Bu
#define M3CL_DEVICE_VENDOR 0x102Cu
#define M3CL_DRIVER_VERSION 0x102Du
#define M3CL_DEVICE_DOUBLE_FP_CONFIG 0x1032u
#define M3CL_DEVICE_HOST_UNIFIED_MEMORY 0x1035u
#define M3CL_DEVICE_OPENCL_C_VERSION 0x103Du

#define M3CL_MEM_READ_WRITE 0x1ull
#define M3CL_MEM_WRITE_ONLY 0x2ull
#define M3CL_MEM_READ_ONLY 0x4ull

#define M3CL_PROGRAM_BUILD_LOG 0x1183u
#define M3CL_KERNEL_NUM_ARGS 0x1191u
#define M3CL_KERNEL_WORK_GROUP_SIZE 0x11B0u

/* X(return type, name, parameter list, required) */
#define M3CL_FUNCS(X)                                                                                             \
    X(m3cl_int, clGetPlatformIDs, (m3cl_uint, m3cl_platform *, m3cl_uint *), 1)                                   \
    X(m3cl_int, clGetPlatformInfo, (m3cl_platform, m3cl_uint, size_t, void *, size_t *), 1)                       \
    X(m3cl_int, clGetDeviceIDs, (m3cl_platform, m3cl_ulong, m3cl_uint, m3cl_device *, m3cl_uint *), 1)            \
    X(m3cl_int, clGetDeviceInfo, (m3cl_device, m3cl_uint, size_t, void *, size_t *), 1)                           \
    X(m3cl_context, clCreateContext, (const intptr_t *, m3cl_uint, const m3cl_device *, void *, void *, m3cl_int *), 1) \
    X(m3cl_int, clReleaseContext, (m3cl_context), 1)                                                              \
    X(m3cl_queue, clCreateCommandQueue, (m3cl_context, m3cl_device, m3cl_ulong, m3cl_int *), 1)                   \
    X(m3cl_int, clReleaseCommandQueue, (m3cl_queue), 1)                                                           \
    X(m3cl_mem, clCreateBuffer, (m3cl_context, m3cl_ulong, size_t, void *, m3cl_int *), 1)                        \
    X(m3cl_int, clReleaseMemObject, (m3cl_mem), 1)                                                                \
    X(m3cl_int, clEnqueueWriteBuffer,                                                                             \
      (m3cl_queue, m3cl_mem, m3cl_uint, size_t, size_t, const void *, m3cl_uint, const void *, void *), 1)        \
    X(m3cl_int, clEnqueueReadBuffer,                                                                              \
      (m3cl_queue, m3cl_mem, m3cl_uint, size_t, size_t, void *, m3cl_uint, const void *, void *), 1)              \
    X(m3cl_int, clEnqueueFillBuffer,                                                                              \
      (m3cl_queue, m3cl_mem, const void *, size_t, size_t, size_t, m3cl_uint, const void *, void *), 0)           \
    X(m3cl_program, clCreateProgramWithSource, (m3cl_context, m3cl_uint, const char **, const size_t *, m3cl_int *), 1) \
    X(m3cl_int, clBuildProgram, (m3cl_program, m3cl_uint, const m3cl_device *, const char *, void *, void *), 1)  \
    X(m3cl_int, clGetProgramBuildInfo, (m3cl_program, m3cl_device, m3cl_uint, size_t, void *, size_t *), 1)       \
    X(m3cl_int, clReleaseProgram, (m3cl_program), 1)                                                              \
    X(m3cl_kernel, clCreateKernel, (m3cl_program, const char *, m3cl_int *), 1)                                   \
    X(m3cl_int, clReleaseKernel, (m3cl_kernel), 1)                                                                \
    X(m3cl_int, clGetKernelInfo, (m3cl_kernel, m3cl_uint, size_t, void *, size_t *), 1)                           \
    X(m3cl_int, clGetKernelWorkGroupInfo, (m3cl_kernel, m3cl_device, m3cl_uint, size_t, void *, size_t *), 1)     \
    X(m3cl_int, clSetKernelArg, (m3cl_kernel, m3cl_uint, size_t, const void *), 1)                                \
    X(m3cl_int, clEnqueueNDRangeKernel,                                                                           \
      (m3cl_queue, m3cl_kernel, m3cl_uint, const size_t *, const size_t *, const size_t *, m3cl_uint, const void *, \
       void *), 1)                                                                                                \
    X(m3cl_int, clFinish, (m3cl_queue), 1)

#define M3CL_DECLARE(ret, name, params, required) ret(M3CL_CALL *name) params;
typedef struct m3cl_api {
    M3CL_FUNCS(M3CL_DECLARE)
} m3cl_api;
#undef M3CL_DECLARE

#endif /* M3VCPU_CL_ABI_H */

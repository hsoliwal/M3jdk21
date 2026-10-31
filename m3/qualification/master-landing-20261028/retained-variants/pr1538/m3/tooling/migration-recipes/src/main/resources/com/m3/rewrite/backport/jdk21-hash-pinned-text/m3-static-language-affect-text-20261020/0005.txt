cmake_minimum_required(VERSION 3.16)
project(m3language C)

find_package(JNI REQUIRED)

add_library(m3language SHARED m3_language_jni.c)
target_include_directories(m3language PRIVATE ${JNI_INCLUDE_DIRS})
set_target_properties(m3language PROPERTIES
    C_STANDARD 11
    C_STANDARD_REQUIRED YES
    C_EXTENSIONS NO)

if(MSVC)
    target_compile_options(m3language PRIVATE /W4)
else()
    target_compile_options(m3language PRIVATE -Wall -Wextra -Werror)
endif()

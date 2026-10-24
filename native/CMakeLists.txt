cmake_minimum_required(VERSION 4.2)
project(doomjni C)

include(CheckCCompilerFlag)

option(DOOM_FCOMMON "" OFF)
set(DOOM_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../DOOM/linuxdoom-1.10" CACHE PATH "")
set(JNI_HOME "$ENV{JAVA_HOME}" CACHE PATH "")

file(GLOB DOOM_SOURCES "${DOOM_SRC_DIR}/*.c")

set_source_files_properties("${DOOM_SRC_DIR}/i_main.c" PROPERTIES COMPILE_DEFINITIONS "main=doom_unused_main")

add_library(doomjni SHARED ${DOOM_SOURCES} src/doom_jni.c)

target_include_directories(doomjni PRIVATE "${DOOM_SRC_DIR}" "${CMAKE_CURRENT_SOURCE_DIR}/include")

if(NOT ANDROID)
    if(NOT EXISTS "${JNI_HOME}/include/jni.h")
        message(FATAL_ERROR "jni.h not found, set JAVA_HOME or -DJNI_HOME")
    endif()
    if(WIN32)
        set(JNI_MD win32)
    elseif(APPLE)
        set(JNI_MD darwin)
    else()
        set(JNI_MD linux)
    endif()
    target_include_directories(doomjni PRIVATE "${JNI_HOME}/include" "${CMAKE_CURRENT_SOURCE_DIR}/include/jni_md/${JNI_MD}")
endif()

target_compile_definitions(doomjni PRIVATE NORMALUNIX LINUX)

set_target_properties(doomjni PROPERTIES
    C_STANDARD 99
    C_EXTENSIONS ON
    POSITION_INDEPENDENT_CODE ON
    C_VISIBILITY_PRESET hidden
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/out"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/out"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/out-implib")

if(WIN32)
    set_target_properties(doomjni PROPERTIES PREFIX "")
    target_link_options(doomjni PRIVATE -static-libgcc)
    target_link_libraries(doomjni PRIVATE ws2_32)
endif()

if(ANDROID)
    target_link_options(doomjni PRIVATE -Wl,-z,max-page-size=16384)
endif()

if(DOOM_FCOMMON)
    target_compile_options(doomjni PRIVATE -fcommon)
endif()

target_compile_options(doomjni PRIVATE -Wall -Wno-unused -fno-strict-aliasing)



foreach(flag
        -Wno-error=implicit-function-declaration
        -Wno-error=implicit-int
        -Wno-error=int-conversion
        -Wno-error=incompatible-pointer-types
        -Wno-error=return-type)
    string(MAKE_C_IDENTIFIER "HAS${flag}" var)
    check_c_compiler_flag("${flag}" ${var})
    if(${var})
        target_compile_options(doomjni PRIVATE ${flag})
    endif()
endforeach()

if(UNIX)
    target_link_libraries(doomjni PRIVATE m)
endif()

target_link_options(doomjni PRIVATE "$<$<AND:$<CONFIG:Release>,$<NOT:$<PLATFORM_ID:Darwin>>>:-s>")

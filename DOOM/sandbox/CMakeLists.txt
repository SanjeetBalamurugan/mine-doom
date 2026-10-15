cmake_minimum_required(VERSION 4.2)

set(SANDBOX_SOURCES
    main.c
    stb_image_write.h
    stb_image_write.c
)

add_executable(doom_sandbox ${SANDBOX_SOURCES})

target_include_directories(doom_sandbox PRIVATE
    ${CMAKE_SOURCE_DIR}/DOOM/linuxdoom-1.10
)

target_link_libraries(doom_sandbox
    doomheadless
    m
)

set_target_properties(doom_sandbox PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)

option(DOOM_DEBUG "Enable debug output" OFF)
if(DOOM_DEBUG)
    target_compile_definitions(doom_sandbox PRIVATE DOOM_DEBUG)
endif()
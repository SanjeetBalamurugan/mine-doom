cmake_minimum_required(VERSION 4.2)
project(DoomHeadless C)

option(NORMAL_BUILD "Build normal executable" OFF)

set(CMAKE_C_STANDARD 99)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NORMAL_BUILD)
    if(MSVC)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /DNORMALUNIX /DLINUX /W3")
    else()
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g -Wall -DNORMALUNIX -DLINUX")
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused-but-set-variable")
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-pointer-to-int-cast")
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused-const-variable")
    endif()
else()
    if(MSVC)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /DNORMALUNIX /DLINUX /DHEADLESS /W3")
    else()
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g -Wall -DNORMALUNIX -DLINUX -DHEADLESS")
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused-but-set-variable")
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-pointer-to-int-cast")
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-unused-const-variable")
    endif()
endif()

file(GLOB SOURCES "*.c")
list(APPEND SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/doom_lib.c")

if(NORMAL_BUILD)
    add_executable(doom_exec ${SOURCES})
    set_target_properties(doom_exec PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
    )
    if(NOT MSVC)
        find_package(X11 REQUIRED)
        target_include_directories(doom_exec PRIVATE ${X11_INCLUDE_DIR})
        target_link_libraries(doom_exec m ${X11_LIBRARIES})
    endif()
    install(TARGETS doom_exec RUNTIME DESTINATION bin)
else()
    add_library(doomheadless STATIC ${SOURCES})
    set_target_properties(doomheadless PROPERTIES
        ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
        LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib
    )
    if(NOT MSVC)
        target_link_libraries(doomheadless m)
    endif()
    install(TARGETS doomheadless ARCHIVE DESTINATION lib LIBRARY DESTINATION lib)
endif()

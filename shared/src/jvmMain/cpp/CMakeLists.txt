cmake_minimum_required(VERSION 3.22.1)
project("fmsynthesizer_desktop")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(JNI REQUIRED)

set(JUCE_DIR vcpkg/packages/juce_x64-windows/share/juce)

find_package(JUCE CONFIG REQUIRED)

add_library(${CMAKE_PROJECT_NAME} SHARED
        fmsynthesizer.cpp
        NativeSynthesizer.cpp
)

if (MSVC)
    target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE /Zc:__cplusplus)
endif()

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${JNI_INCLUDE_DIRS}
)

target_link_libraries(${CMAKE_PROJECT_NAME} PRIVATE
        ${JNI_LIBRARIES}
        juce::juce_audio_utils
        juce::juce_audio_devices
        juce::juce_audio_basics
        juce::juce_core
        juce::juce_events
)

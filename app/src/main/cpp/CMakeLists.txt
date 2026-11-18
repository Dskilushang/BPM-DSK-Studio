cmake_minimum_required(VERSION 3.22.1)
project("bpmengine")

find_package(oboe REQUIRED CONFIG)

add_library(bpmengine SHARED native-lib.cpp)

target_link_libraries(
    bpmengine
    log
    oboe::oboe
)

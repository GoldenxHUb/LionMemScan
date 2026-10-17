cmake_minimum_required(VERSION 3.18.1)
project("memscan")

# ---- KittyMemoryEx source ----
set(KITTY_DIR ${CMAKE_SOURCE_DIR}/KittyMemoryEx)

# ดึง source ของ KittyMemoryEx ทั้งหมด (ทั้ง .cpp และ subfolder)
file(GLOB_RECURSE KITTY_SRC
    ${KITTY_DIR}/KittyMemoryEx/*.cpp
)

# ---- Native ของเรา ----
add_library(memscan SHARED
    MemScan.cpp
    ${KITTY_SRC}
)

# ---- Include paths ----
target_include_directories(memscan PRIVATE
    ${CMAKE_SOURCE_DIR}
    ${KITTY_DIR}
    ${KITTY_DIR}/KittyMemoryEx
    ${KITTY_DIR}/KittyMemoryEx/KittyMemory
    ${KITTY_DIR}/KittyMemoryEx/KittyUtils
)

# ---- Link libraries ----
find_library(log-lib log)
find_library(android-lib android)
target_link_libraries(memscan
    ${log-lib}
    ${android-lib}
    dl
    log
)

# ---- Compile flags ----
set_target_properties(memscan PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)
target_compile_options(memscan PRIVATE -O2 -fvisibility=hidden -Wall)

cmake_minimum_required(VERSION 3.22.1)
project("oblivion_secure" CXX C)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Aggressive symbol stripping & visibility flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -O3")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden -fdata-sections -ffunction-sections -O3")

# Optional OLLVM / O-MVLL flags if supported by toolchain compiler
if(OLLVM_ENABLED)
    add_compile_options(-mllvm -fla -mllvm -sub -mllvm -bcf)
endif()

# Locate Rust static library header
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../oblivion-crypto/include)

# Include native sources
add_library(oblivion_secure SHARED
    src/main/cpp/rasp_daemon.cpp
    src/main/cpp/jni_bridge.cpp
)

# Search for prebuilt liboblivion_crypto.a from Rust Cargo output
find_library(CRYPTO_LIB oblivion_crypto
    PATHS
    ${CMAKE_CURRENT_SOURCE_DIR}/../oblivion-crypto/target/${ANDROID_ABI}/release
    ${CMAKE_CURRENT_SOURCE_DIR}/../oblivion-crypto/target/release
)

if(CRYPTO_LIB)
    add_definitions(-DHAS_OBLIVION_CRYPTO=1)
    target_link_libraries(oblivion_secure ${CRYPTO_LIB} log)
else()
    target_link_libraries(oblivion_secure log)
endif()

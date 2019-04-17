# SPDX-License-Identifier: Apache-2.0
#
# Copyright © 2019 WireGuard LLC. All Rights Reserved.

cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

# Work around https://github.com/android-ndk/ndk/issues/602
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -fuse-ld=gold")

# Dummy lib to get libwg-go compiled as dependency.
# IMPORTED exec or lib doesn't work for some reason
add_library(dummy SHARED dummy/dummy.c)
target_compile_options(dummy PUBLIC -O3 -std=gnu11 -Wall -pedantic -Wno-missing-field-initializers -Wno-unused-function)

add_custom_target(libwg-go.so WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/libwg-go" COMMENT "Building wireguard-go" VERBATIM COMMAND make
    ANDROID_ARCH_NAME=${ANDROID_ARCH_NAME}
    ANDROID_C_COMPILER=${ANDROID_C_COMPILER}
    ANDROID_TOOLCHAIN_ROOT=${ANDROID_TOOLCHAIN_ROOT}
    ANDROID_LLVM_TRIPLE=${ANDROID_LLVM_TRIPLE}
    ANDROID_SYSROOT=${ANDROID_SYSROOT}
    ANDROID_PACKAGE_NAME=${ANDROID_PACKAGE_NAME}
    CFLAGS=${CMAKE_C_FLAGS}\ -Wno-unused-command-line-argument
    LDFLAGS=${CMAKE_SHARED_LINKER_FLAGS}\ -fuse-ld=gold
    DESTDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
    BUILDDIR=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/../generated-src
)

# cmake hack
add_dependencies(dummy libwg-go.so)
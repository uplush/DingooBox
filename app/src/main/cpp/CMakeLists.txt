cmake_minimum_required(VERSION 3.22.1)
project(dingoo_jni LANGUAGES CXX)

add_library(dingoo_core SHARED IMPORTED)
set_target_properties(dingoo_core PROPERTIES
    IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libdingooemu.so"
    IMPORTED_NO_SONAME TRUE
)

# Keep the historical prebuilt libdingoo_jni.so in jniLibs for extraction-over-
# existing-project compatibility.  The application deliberately loads this
# source-built frontend under a new name so Gradle never has two producers for
# the same packaged path.
add_library(dingoo_jni_live SHARED libretro_frontend.cpp)

find_library(log_library log)
find_library(jnigraphics_library jnigraphics)

target_compile_features(dingoo_jni_live PRIVATE cxx_std_20)
target_compile_options(dingoo_jni_live PRIVATE -Wall -Wextra -Wpedantic)
target_link_options(dingoo_jni_live PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries(dingoo_jni_live PRIVATE dingoo_core ${log_library} ${jnigraphics_library})

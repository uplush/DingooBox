#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <linux/memfd.h>
#include <mutex>
#include <string>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <vector>

namespace {

using environment_callback = bool (*)(unsigned, void *);
using video_callback = void (*)(const void *, unsigned, unsigned, size_t);
using audio_callback = void (*)(int16_t, int16_t);
using audio_batch_callback = size_t (*)(const int16_t *, size_t);
using input_poll_callback = void (*)();
using input_state_callback = int16_t (*)(unsigned, unsigned, unsigned, unsigned);

struct retro_game_info {
    const char *path;
    const void *data;
    size_t size;
    const char *meta;
};

struct retro_variable {
    const char *key;
    const char *value;
};

using log_callback = void (*)(unsigned, const char *);

struct retro_log_callback {
    log_callback log;
};

extern "C" {
void retro_set_environment(environment_callback);
void retro_set_video_refresh(video_callback);
void retro_set_audio_sample(audio_callback);
void retro_set_audio_sample_batch(audio_batch_callback);
void retro_set_input_poll(input_poll_callback);
void retro_set_input_state(input_state_callback);
void retro_init();
void retro_deinit();
bool retro_load_game(const retro_game_info *);
void retro_unload_game();
void retro_run();
void retro_reset();
size_t retro_serialize_size();
bool retro_serialize(void *, size_t);
bool retro_unserialize(const void *, size_t);
}

constexpr unsigned ENV_SET_PERFORMANCE_LEVEL = 8;
constexpr unsigned ENV_SET_PIXEL_FORMAT = 10;
constexpr unsigned ENV_SET_INPUT_DESCRIPTORS = 11;
constexpr unsigned ENV_GET_VARIABLE = 15;
constexpr unsigned ENV_SET_VARIABLES = 16;
constexpr unsigned ENV_GET_VARIABLE_UPDATE = 17;
constexpr unsigned ENV_GET_LOG_INTERFACE = 27;
constexpr unsigned ENV_GET_SAVE_DIRECTORY = 31;
constexpr unsigned ENV_SET_MEMORY_MAPS = 36 | 0x10000;
constexpr unsigned PIXEL_FORMAT_RGB565 = 2;
constexpr unsigned DEVICE_JOYPAD = 1;

constexpr const char *CORE_LOG_TAG = "DingooCore";
constexpr const char *STATE_LOG_TAG = "DingooState";

std::mutex core_mutex;
std::atomic<uint64_t> input_mask{0};
std::vector<uint16_t> frame(320 * 240, 0);
std::vector<int16_t> audio;
std::string save_directory;
int rom_file_descriptor = -1;
std::string rom_alias_path;
std::string rom_alias_directory;
bool core_initialized = false;
bool game_loaded = false;

void core_log(unsigned level, const char *message) {
    // The core emits very large JIT IR dumps at info level. The Android
    // frontend only needs warnings and errors; DingooState supplies concise
    // operation-level info for state diagnostics.
    if (level < 2) return;
    const int priority = [&] {
        switch (level) {
            case 0: return ANDROID_LOG_DEBUG;
            case 1: return ANDROID_LOG_INFO;
            case 2: return ANDROID_LOG_WARN;
            case 3: return ANDROID_LOG_ERROR;
            default: return ANDROID_LOG_INFO;
        }
    }();
    __android_log_write(priority, CORE_LOG_TAG, message != nullptr ? message : "");
}

bool environment(unsigned command, void *data) {
    switch (command) {
        case ENV_SET_PIXEL_FORMAT:
            return data != nullptr && *static_cast<unsigned *>(data) == PIXEL_FORMAT_RGB565;
        case ENV_GET_SAVE_DIRECTORY:
            if (data == nullptr) return false;
            *static_cast<const char **>(data) = save_directory.c_str();
            return true;
        case ENV_GET_VARIABLE_UPDATE:
            if (data != nullptr) *static_cast<bool *>(data) = false;
            return true;
        case ENV_GET_VARIABLE:
            return false;
        case ENV_GET_LOG_INTERFACE:
            if (data == nullptr) return false;
            static_cast<retro_log_callback *>(data)->log = core_log;
            return true;
        case ENV_SET_PERFORMANCE_LEVEL:
        case ENV_SET_INPUT_DESCRIPTORS:
        case ENV_SET_VARIABLES:
        case ENV_SET_MEMORY_MAPS:
            return true;
        default:
            return false;
    }
}

void video_refresh(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (data == nullptr || width == 0 || height == 0) return;
    const auto copy_width = std::min(width, 320u);
    const auto copy_height = std::min(height, 240u);
    const auto *source = static_cast<const uint8_t *>(data);
    for (unsigned y = 0; y < copy_height; ++y) {
        std::memcpy(
            frame.data() + y * 320,
            source + static_cast<size_t>(y) * pitch,
            static_cast<size_t>(copy_width) * sizeof(uint16_t)
        );
    }
}

void audio_sample(int16_t left, int16_t right) {
    audio.push_back(left);
    audio.push_back(right);
}

size_t audio_sample_batch(const int16_t *data, size_t frames) {
    if (data != nullptr && frames > 0) {
        audio.insert(audio.end(), data, data + frames * 2);
    }
    return frames;
}

void input_poll() {}

int16_t input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port != 0 || device != DEVICE_JOYPAD || index != 0 || id >= 64) return 0;
    return (input_mask.load(std::memory_order_relaxed) & (uint64_t{1} << id)) != 0 ? 1 : 0;
}

std::string from_jstring(JNIEnv *env, jstring value) {
    if (value == nullptr) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

int create_rom_memory_file(const std::string &name) {
    return static_cast<int>(
        syscall(
            __NR_memfd_create,
            name.c_str(),
            MFD_CLOEXEC
        )
    );
}

bool write_rom_memory_file(
    int file_descriptor,
    const jbyte *data,
    size_t size
) {
    size_t written = 0;
    while (written < size) {
        const ssize_t result = write(
            file_descriptor,
            data + written,
            size - written
        );
        if (result < 0 && errno == EINTR) continue;
        if (result <= 0) return false;
        written += static_cast<size_t>(result);
    }
    return lseek(file_descriptor, 0, SEEK_SET) == 0;
}

std::string safe_rom_name(std::string name) {
    if (name.empty()) return "game.app";
    std::replace_if(
        name.begin(),
        name.end(),
        [](char value) {
            return value == '/' || value == '\\';
        },
        '_'
    );
    return name;
}

std::string create_rom_path_alias(
    int file_descriptor,
    const std::string &rom_name,
    std::string *alias_path,
    std::string *alias_directory
) {
    const std::string descriptor_path =
        "/proc/self/fd/" + std::to_string(file_descriptor);
    const size_t separator = save_directory.find_last_of('/');
    if (separator == std::string::npos) return descriptor_path;

    *alias_directory = save_directory.substr(0, separator) + "/.rom-runtime";
    if (
        mkdir(alias_directory->c_str(), 0700) != 0 &&
        errno != EEXIST
    ) {
        alias_directory->clear();
        return descriptor_path;
    }

    *alias_path = *alias_directory + "/" + safe_rom_name(rom_name);
    unlink(alias_path->c_str());
    if (symlink(descriptor_path.c_str(), alias_path->c_str()) != 0) {
        alias_path->clear();
        rmdir(alias_directory->c_str());
        alias_directory->clear();
        return descriptor_path;
    }
    return *alias_path;
}

void release_rom_memory_file() {
    if (!rom_alias_path.empty()) {
        unlink(rom_alias_path.c_str());
        rom_alias_path.clear();
    }
    if (rom_file_descriptor >= 0) {
        close(rom_file_descriptor);
        rom_file_descriptor = -1;
    }
    if (!rom_alias_directory.empty()) {
        rmdir(rom_alias_directory.c_str());
        rom_alias_directory.clear();
    }
}

void shutdown_core() {
    if (game_loaded) {
        retro_unload_game();
        game_loaded = false;
    }
    if (core_initialized) {
        retro_deinit();
        core_initialized = false;
    }
    // DingooEmu retains the content path as app_path and can resolve files
    // relative to it after retro_load_game() has returned. Keep the anonymous
    // ROM descriptor and its filename alias alive for the complete session,
    // then release both only after the core has unloaded.
    release_rom_memory_file();
    input_mask.store(0, std::memory_order_relaxed);
}

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeInitialize(
    JNIEnv *env,
    jobject,
    jbyteArray rom_data_value,
    jstring rom_name_value,
    jstring save_directory_value
) {
    std::lock_guard lock(core_mutex);
    shutdown_core();
    if (rom_data_value == nullptr) return JNI_FALSE;

    const std::string rom_name = from_jstring(env, rom_name_value);
    save_directory = from_jstring(env, save_directory_value);
    const jsize rom_size = env->GetArrayLength(rom_data_value);
    if (rom_name.empty() || save_directory.empty() || rom_size <= 0) return JNI_FALSE;

    jbyte *rom_bytes = env->GetByteArrayElements(rom_data_value, nullptr);
    if (rom_bytes == nullptr) return JNI_FALSE;

    rom_file_descriptor = create_rom_memory_file(rom_name);
    if (
        rom_file_descriptor < 0 ||
        !write_rom_memory_file(
            rom_file_descriptor,
            rom_bytes,
            static_cast<size_t>(rom_size)
        )
    ) {
        const int saved_errno = errno;
        release_rom_memory_file();
        env->ReleaseByteArrayElements(
            rom_data_value,
            rom_bytes,
            JNI_ABORT
        );
        __android_log_print(
            ANDROID_LOG_ERROR,
            "DingooJNI",
            "Unable to create anonymous ROM file for %s: errno=%d",
            rom_name.c_str(),
            saved_errno
        );
        return JNI_FALSE;
    }
    env->ReleaseByteArrayElements(
        rom_data_value,
        rom_bytes,
        JNI_ABORT
    );

    const std::string game_path = create_rom_path_alias(
        rom_file_descriptor,
        rom_name,
        &rom_alias_path,
        &rom_alias_directory
    );

    retro_set_environment(environment);
    retro_set_video_refresh(video_refresh);
    retro_set_audio_sample(audio_sample);
    retro_set_audio_sample_batch(audio_sample_batch);
    retro_set_input_poll(input_poll);
    retro_set_input_state(input_state);
    retro_init();
    core_initialized = true;

    const retro_game_info info{game_path.c_str(), nullptr, 0, nullptr};
    game_loaded = retro_load_game(&info);
    if (!game_loaded) {
        shutdown_core();
        __android_log_print(
            ANDROID_LOG_ERROR,
            "DingooJNI",
            "Failed to load %s from anonymous memory",
            rom_name.c_str()
        );
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeRunFrame(
    JNIEnv *env, jobject, jobject bitmap, jshortArray audio_buffer
) {
    std::lock_guard lock(core_mutex);
    if (!game_loaded || bitmap == nullptr || audio_buffer == nullptr) return 0;

    audio.clear();
    retro_run();

    AndroidBitmapInfo bitmap_info{};
    if (AndroidBitmap_getInfo(env, bitmap, &bitmap_info) == ANDROID_BITMAP_RESULT_SUCCESS &&
        bitmap_info.format == ANDROID_BITMAP_FORMAT_RGB_565 &&
        bitmap_info.width >= 320 && bitmap_info.height >= 240) {
        void *pixels = nullptr;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
            auto *destination = static_cast<uint8_t *>(pixels);
            for (unsigned y = 0; y < 240; ++y) {
                std::memcpy(destination + static_cast<size_t>(y) * bitmap_info.stride,
                            frame.data() + y * 320,
                            320 * sizeof(uint16_t));
            }
            AndroidBitmap_unlockPixels(env, bitmap);
        }
    }

    const jsize capacity = env->GetArrayLength(audio_buffer);
    const jsize count = std::min<jsize>(capacity, static_cast<jsize>(audio.size()));
    if (count > 0) env->SetShortArrayRegion(audio_buffer, 0, count, audio.data());
    return count;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeSetButton(
    JNIEnv *, jobject, jint button_id, jboolean pressed
) {
    if (button_id < 0 || button_id >= 64) return;
    const uint64_t bit = uint64_t{1} << static_cast<unsigned>(button_id);
    if (pressed == JNI_TRUE) input_mask.fetch_or(bit, std::memory_order_relaxed);
    else input_mask.fetch_and(~bit, std::memory_order_relaxed);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeSaveState(
    JNIEnv *env, jobject, jstring path_value
) {
    std::lock_guard lock(core_mutex);
    if (!game_loaded) {
        __android_log_print(ANDROID_LOG_ERROR, STATE_LOG_TAG, "Save failed: no game is loaded");
        return JNI_FALSE;
    }
    const std::string path = from_jstring(env, path_value);
    const size_t size = retro_serialize_size();
    if (path.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, STATE_LOG_TAG, "Save failed: destination path is empty");
        return JNI_FALSE;
    }
    if (size == 0) {
        __android_log_print(ANDROID_LOG_ERROR, STATE_LOG_TAG,
                            "Save failed: core returned a zero serialization size; path=%s",
                            path.c_str());
        return JNI_FALSE;
    }
    __android_log_print(ANDROID_LOG_INFO, STATE_LOG_TAG,
                        "Saving state: size=%zu path=%s", size, path.c_str());
    std::vector<uint8_t> state(size);
    if (!retro_serialize(state.data(), state.size())) {
        __android_log_print(ANDROID_LOG_ERROR, STATE_LOG_TAG,
                            "Save failed: retro_serialize returned false; size=%zu path=%s",
                            size, path.c_str());
        return JNI_FALSE;
    }
    std::ofstream output(path, std::ios::binary | std::ios::trunc);
    if (!output.is_open()) {
        __android_log_print(ANDROID_LOG_ERROR, STATE_LOG_TAG,
                            "Save failed: could not open destination; path=%s", path.c_str());
        return JNI_FALSE;
    }
    output.write(reinterpret_cast<const char *>(state.data()), static_cast<std::streamsize>(state.size()));
    output.flush();
    if (!output.good()) {
        __android_log_print(ANDROID_LOG_ERROR, STATE_LOG_TAG,
                            "Save failed: write or flush error; size=%zu path=%s",
                            size, path.c_str());
        return JNI_FALSE;
    }
    __android_log_print(ANDROID_LOG_INFO, STATE_LOG_TAG,
                        "Save completed: size=%zu path=%s", size, path.c_str());
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeLoadState(
    JNIEnv *env, jobject, jstring path_value
) {
    std::lock_guard lock(core_mutex);
    if (!game_loaded) return JNI_FALSE;
    const std::string path = from_jstring(env, path_value);
    std::ifstream input(path, std::ios::binary | std::ios::ate);
    if (!input.good()) return JNI_FALSE;
    const auto end = input.tellg();
    if (end <= 0) return JNI_FALSE;
    std::vector<uint8_t> state(static_cast<size_t>(end));
    input.seekg(0);
    input.read(reinterpret_cast<char *>(state.data()), static_cast<std::streamsize>(state.size()));
    return input.good() && retro_unserialize(state.data(), state.size()) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeReset(JNIEnv *, jobject) {
    std::lock_guard lock(core_mutex);
    if (game_loaded) retro_reset();
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_uplush_dingoobox_NativeBridge_nativeDeinitialize(JNIEnv *, jobject) {
    std::lock_guard lock(core_mutex);
    shutdown_core();
}

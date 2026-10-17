#include <jni.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <sstream>
#include <android/log.h>

// KittyMemoryEx
#include "KittyMemoryEx/KittyMemoryEx.hpp"
#include "KittyMemoryEx/KittyMemoryMgr.hpp"
#include "KittyMemoryEx/KittyScanner.hpp"

#define LOG_TAG "LionMemScan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_lion_memscan_MainActivity_scanMemory(
    JNIEnv* env, jobject /*this*/,
    jstring packageName, jstring pattern) {

    const char* pkg = env->GetStringUTFChars(packageName, nullptr);
    const char* pat = env->GetStringUTFChars(pattern, nullptr);
    if (!pkg || !pat) {
        return env->NewStringUTF("❌ null input");
    }

    std::string pkgStr = pkg;
    std::string patStr = pat;
    env->ReleaseStringUTFChars(packageName, pkg);
    env->ReleaseStringUTFChars(pattern, pat);

    std::ostringstream oss;
    oss << "Package: " << pkgStr << "\n";
    oss << "Pattern: \"" << patStr << "\"\n";
    oss << "────────────────────\n";

    // 1. หา PID
    pid_t pid = KittyMemoryEx::getProcessID(pkgStr);
    if (pid <= 0) {
        oss << "❌ ไม่พบ process\n";
        return env->NewStringUTF(oss.str().c_str());
    }
    oss << "✅ PID: " << pid << "\n\n";

    // 2. สร้าง Manager
    KittyMemoryMgr mem;
    if (!mem.initialize(pid, EK_MEM_OP_SYSCALL, false)) {
        oss << "❌ init KittyMemoryMgr failed\n";
        return env->NewStringUTF(oss.str().c_str());
    }
    oss << "✅ Manager initialized\n\n";

    // 3. Maps
    auto maps = KittyMemoryEx::getAllMaps(pid);
    oss << "📋 Total maps: " << maps.size() << "\n\n";

    // 4. Scan
    int found_count = 0;
    int scanned = 0;
    std::string mask(patStr.length(), 'x');

    for (auto& map : maps) {
        if (!map.isValid() || !map.readable) continue;
        if (map.length > 100 * 1024 * 1024) continue;

        scanned++;

        uintptr_t addr = mem.memScanner.findBytesFirst(
            map.start, map.end, patStr.c_str(), mask.c_str());

        if (addr != 0) {
            char buf[256];
            std::string path = map.pathname.empty() ? "?" : map.pathname;
            snprintf(buf, sizeof(buf), "  [%d] 0x%lx\n       %s\n",
                     ++found_count, (unsigned long)addr, path.c_str());
            oss << buf;

            if (found_count >= 30) {
                oss << "  ... (จำกัด 30 ตำแหน่ง)\n";
                break;
            }
        }
    }

    oss << "\n════════════════════\n";
    oss << "Scanned: " << scanned << " regions\n";
    oss << "Found:   " << found_count << " matches\n";

    return env->NewStringUTF(oss.str().c_str());
}

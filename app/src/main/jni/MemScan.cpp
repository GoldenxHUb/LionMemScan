#include <jni.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <sstream>
#include <android/log.h>

// KittyMemoryEx
#include "KittyMemoryEx.hpp"
#include "KittyMemoryMgr.hpp"
#include "KittyScanner.hpp"

#define LOG_TAG "LionMemScan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// หา PID ด้วย popen("su -c pidof ...")
static pid_t find_pid_by_name(const char* pkg) {
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "su -c pidof %s", pkg);
    LOGI("Running: %s", cmd);

    FILE* fp = popen(cmd, "r");
    if (!fp) {
        LOGE("popen failed");
        return -1;
    }
    char buf[64] = {0};
    if (!fgets(buf, sizeof(buf), fp)) {
        LOGE("pidof empty");
        pclose(fp);
        return -1;
    }
    pclose(fp);
    LOGI("pidof output: %s", buf);
    return atoi(buf);
}

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

    LOGI("=== Scan request ===");
    LOGI("Package: %s", pkgStr.c_str());
    LOGI("Pattern: %s", patStr.c_str());

    std::ostringstream oss;
    oss << "Package: " << pkgStr << "\n";
    oss << "Pattern: \"" << patStr << "\"\n";
    oss << "────────────────────\n";

    // 1. หา PID ด้วย su -c pidof
    pid_t pid = find_pid_by_name(pkgStr.c_str());
    LOGI("Resolved PID: %d", pid);

    if (pid <= 0) {
        oss << "❌ ไม่พบ process\n\n";
        oss << "ตรวจสอบ:\n";
        oss << "- เกมเปิดอยู่หรือไม่?\n";
        oss << "- KernelSU grant root ให้แอปหรือยัง?\n";
        oss << "- su ทำงานใน Termux ไหม?\n";
        return env->NewStringUTF(oss.str().c_str());
    }
    oss << "✅ PID: " << pid << "\n\n";

    // 2. สร้าง Manager
    KittyMemoryMgr mem;
    if (!mem.initialize(pid, EK_MEM_OP_SYSCALL, false)) {
        LOGE("initialize failed");
        oss << "❌ KittyMemoryMgr init failed\n";
        oss << "(SELinux? setenforce 0)\n";
        return env->NewStringUTF(oss.str().c_str());
    }
    oss << "✅ Manager initialized\n\n";

    // 3. ดึง Maps
    auto maps = KittyMemoryEx::getAllMaps(pid);
    oss << "📋 Memory maps: " << maps.size() << "\n\n";

    // 4. สแกน
    int found_count = 0;
    int scanned = 0;
    std::string mask(patStr.length(), 'x');

    for (auto& map : maps) {
        if (!map.readable) continue;
        if (map.length > 100 * 1024 * 1024) continue;

        scanned++;

        uintptr_t addr = mem.memScanner.findBytesFirst(
            map.startAddress, map.endAddress,
            patStr.c_str(), mask.c_str());

        if (addr != 0) {
            char buf[256];
            std::string path = map.pathname.empty() ? "?" : map.pathname;
            snprintf(buf, sizeof(buf), "  [%d] 0x%lx\n       %s\n",
                     ++found_count, (unsigned long)addr, path.c_str());
            oss << buf;
            LOGI("Found at 0x%lx in %s", (unsigned long)addr, path.c_str());

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

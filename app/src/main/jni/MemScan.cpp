#include <jni.h>
#include <unistd.h>
#include <dirent.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <string>
#include <sstream>
#include <vector>
#include <android/log.h>

#include "KittyMemoryEx.hpp"
#include "KittyMemoryMgr.hpp"

#define LOG_TAG "LionMemScan"

namespace OFF {
    constexpr uintptr_t InitBase = 0xA342EFC;
    constexpr uintptr_t CurrentMatch = 0x34;
    constexpr uintptr_t MatchStatus = 0xC;
    constexpr uintptr_t LocalPlayer = 0x230;
    constexpr uintptr_t Camera = 0x18;
    constexpr uintptr_t Player_Data = 0x48;
    constexpr uintptr_t Player_Name = 0xA4;
    constexpr uintptr_t Player_IsDead = 0x54;
    constexpr uintptr_t IsFiring = 0x58C;
    constexpr uintptr_t UnlimitedAmmo = 0x114;
    constexpr uintptr_t MainCameraTransform = 0x28C;
}

static uintptr_t rdPtr(KittyMemoryMgr& mem, pid_t, uintptr_t addr) {
    uintptr_t v = 0;
    if (!mem.readMem(addr, &v, sizeof(v))) return 0;
    return v;
}

static int rdInt(KittyMemoryMgr& mem, pid_t, uintptr_t addr) {
    int v = 0;
    mem.readMem(addr, &v, sizeof(v));
    return v;
}

static float rdFloat(KittyMemoryMgr& mem, pid_t, uintptr_t addr) {
    float v = 0;
    mem.readMem(addr, &v, sizeof(v));
    return v;
}

static bool rdBool(KittyMemoryMgr& mem, pid_t, uintptr_t addr) {
    uint8_t v = 0;
    mem.readMem(addr, &v, sizeof(v));
    return v != 0;
}

static std::string rdUString(KittyMemoryMgr& mem, pid_t pid, uintptr_t addr) {
    if (addr == 0) return "";
    int32_t length = rdInt(mem, pid, addr + 0x10);
    if (length <= 0 || length > 64) return "";
    std::vector<uint16_t> buf(length);
    if (!mem.readMem(addr + 0x14, buf.data(), length * 2)) return "";
    std::string out;
    for (int i = 0; i < length; i++) out.push_back((char)(buf[i] & 0xFF));
    return out;
}

static pid_t find_pid_by_pkg(const std::string& pkg) {
    DIR* dir = opendir("/proc");
    if (dir) {
        struct dirent* e;
        while ((e = readdir(dir))) {
            if (e->d_type != DT_DIR) continue;
            pid_t p = atoi(e->d_name);
            if (p <= 0) continue;
            char path[64];
            snprintf(path, sizeof(path), "/proc/%d/cmdline", p);
            FILE* f = fopen(path, "r");
            if (!f) continue;
            char buf[512] = {0};
            size_t n = fread(buf, 1, sizeof(buf) - 1, f);
            fclose(f);
            if (n > 0 && pkg == buf) {
                closedir(dir);
                return p;
            }
        }
        closedir(dir);
    }

    char cmd[256];
    snprintf(cmd, sizeof(cmd), "su -c pidof %s 2>/dev/null", pkg.c_str());
    FILE* fp = popen(cmd, "r");
    if (fp) {
        char buf[32] = {0};
        if (fgets(buf, sizeof(buf), fp)) {
            pclose(fp);
            pid_t p = atoi(buf);
            if (p > 0) return p;
        } else {
            pclose(fp);
        }
    }

    return -1;
}

static uintptr_t find_il2cpp_base(pid_t pid, std::ostringstream& dbg) {
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "su -c cat /proc/%d/maps 2>/dev/null", pid);
    FILE* fp = popen(cmd, "r");
    if (!fp) {
        dbg << "Cannot open maps via su\n";
        return 0;
    }

    char line[1024];
    uintptr_t base = 0;
    uintptr_t lowest_start = (uintptr_t)-1;
    int found_count = 0;

    dbg << "=== il2cpp segments ===\n";

    while (fgets(line, sizeof(line), fp)) {
        if (!strstr(line, "libil2cpp.so")) continue;

        uintptr_t start = 0, end = 0, offset = 0;
        char perms[8] = {0};
        if (sscanf(line, "%lx-%lx %7s %lx", &start, &end, perms, &offset) != 4) continue;

        if (found_count < 5) {
            dbg << "  " << std::hex << start << "-" << end
                << " " << perms << " off=0x" << offset << std::dec << "\n";
            found_count++;
        }

        if (offset == 0 && strchr(perms, 'r')) {
            if (start < lowest_start) {
                lowest_start = start;
                base = start;
            }
        }
    }
    pclose(fp);

    return base;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lion_memscan_MainActivity_scanMemory(
    JNIEnv* env, jobject, jstring packageName, jstring /*pattern*/) {

    const char* pkg = env->GetStringUTFChars(packageName, nullptr);
    std::string pkgStr = pkg;
    env->ReleaseStringUTFChars(packageName, pkg);

    std::ostringstream oss;

    pid_t pid = find_pid_by_pkg(pkgStr);
    if (pid <= 0) {
        std::string msg = "Cannot find process: " + pkgStr;
        return env->NewStringUTF(msg.c_str());
    }

    oss << "Package: " << pkgStr << "\n";
    oss << "PID: " << pid << "\n\n";

    KittyMemoryMgr mem;
    if (!mem.initialize(pid, EK_MEM_OP_SYSCALL, false)) {
        return env->NewStringUTF("KittyMemoryMgr init failed (SELinux?)");
    }

    uintptr_t il2cpp = find_il2cpp_base(pid, oss);
    if (il2cpp == 0) {
        std::string err = oss.str() + "\nlibil2cpp.so not found";
        return env->NewStringUTF(err.c_str());
    }
    oss << "\nil2cpp base: 0x" << std::hex << il2cpp << std::dec << "\n\n";

    uintptr_t gameFacade = rdPtr(mem, pid, il2cpp + OFF::InitBase);
    oss << "GameFacade: 0x" << std::hex << gameFacade << std::dec << "\n";
    if (gameFacade == 0) {
        oss << "(GameFacade null - not in lobby?)";
        return env->NewStringUTF(oss.str().c_str());
    }

    uintptr_t currentMatch = rdPtr(mem, pid, gameFacade + OFF::CurrentMatch);
    oss << "CurrentMatch: 0x" << std::hex << currentMatch << std::dec << "\n";

    uintptr_t localPlayer = 0;
    if (currentMatch != 0) {
        localPlayer = rdPtr(mem, pid, currentMatch + OFF::LocalPlayer);
    }
    oss << "LocalPlayer: 0x" << std::hex << localPlayer << std::dec << "\n";

    if (localPlayer != 0) {
        uintptr_t namePtr = rdPtr(mem, pid, localPlayer + OFF::Player_Name);
        std::string name = rdUString(mem, pid, namePtr);
        oss << "  Name: " << name << "\n";

        bool dead = rdBool(mem, pid, localPlayer + OFF::Player_IsDead);
        oss << "  Dead: " << (dead ? "YES" : "NO") << "\n";
    }

    oss << "\nDONE\n";
    return env->NewStringUTF(oss.str().c_str());
}

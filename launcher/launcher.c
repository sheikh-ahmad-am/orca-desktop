/*
 * Orca desktop launcher — a tiny native Windows stub.
 *
 * Double-clicking Orca.exe runs the bundled Java runtime (jre\bin\javaw.exe
 * next to the exe) against orca-desktop.jar sitting beside it. If the
 * bundled runtime is missing it falls back to a system javaw (PATH, then
 * JAVA_HOME). No console window (GUI subsystem), no network, no install.
 */
#define UNICODE
#define _UNICODE
#include <windows.h>

static void showError(const wchar_t *msg) {
    MessageBoxW(NULL, msg, L"Orca", MB_OK | MB_ICONERROR);
}

static BOOL fileExists(const wchar_t *path) {
    DWORD a = GetFileAttributesW(path);
    return a != INVALID_FILE_ATTRIBUTES && !(a & FILE_ATTRIBUTE_DIRECTORY);
}

int WINAPI WinMain(HINSTANCE hInst, HINSTANCE hPrev, LPSTR cmdLine, int nShow) {
    (void)hInst; (void)hPrev; (void)cmdLine; (void)nShow;

    wchar_t dir[MAX_PATH];
    DWORD n = GetModuleFileNameW(NULL, dir, MAX_PATH);
    if (n == 0 || n >= MAX_PATH) {
        showError(L"Orca could not find its own location.");
        return 1;
    }
    wchar_t *slash = wcsrchr(dir, L'\\');
    if (slash) *slash = L'\0';

    wchar_t jar[MAX_PATH];
    wsprintfW(jar, L"%s\\orca-desktop.jar", dir);
    if (!fileExists(jar)) {
        showError(L"orca-desktop.jar is missing next to Orca.exe.\n"
                  L"Please extract the whole Orca-Desktop.zip first, keeping "
                  L"Orca.exe, orca-desktop.jar and the jre folder together.");
        return 1;
    }

    wchar_t javaw[MAX_PATH];
    wsprintfW(javaw, L"%s\\jre\\bin\\javaw.exe", dir);
    if (!fileExists(javaw)) {
        /* Fall back to a system Java runtime. */
        wchar_t found[MAX_PATH];
        if (SearchPathW(NULL, L"javaw.exe", NULL, MAX_PATH, found, NULL) > 0) {
            lstrcpynW(javaw, found, MAX_PATH);
        } else {
            wchar_t home[MAX_PATH];
            DWORD h = GetEnvironmentVariableW(L"JAVA_HOME", home, MAX_PATH);
            if (h > 0 && h < MAX_PATH) {
                wsprintfW(javaw, L"%s\\bin\\javaw.exe", home);
            }
            if (!fileExists(javaw)) {
                showError(L"Orca's bundled Java runtime (the jre folder) is missing.\n"
                          L"Please extract the whole Orca-Desktop.zip first, or install Java 17+.");
                return 1;
            }
        }
    }

    wchar_t cmd[2 * MAX_PATH + 64];
    wsprintfW(cmd, L"\"%s\" -jar \"%s\"", javaw, jar);

    STARTUPINFOW si;
    PROCESS_INFORMATION pi;
    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));

    if (!CreateProcessW(NULL, cmd, NULL, NULL, FALSE, 0, NULL, dir, &si, &pi)) {
        showError(L"Orca could not start its runtime. "
                  L"If this keeps happening, re-extract Orca-Desktop.zip to a fresh folder.");
        return 1;
    }
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return 0;
}

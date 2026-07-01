package org.example;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinNT;

interface Kernel32 extends Library {
    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    WinNT.HANDLE OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);

    boolean TerminateProcess(WinNT.HANDLE hProcess, int uExitCode);

    boolean CloseHandle(WinNT.HANDLE hObject);
}

package org.example;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;

interface User32 extends Library {
    User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

    WinDef.HWND GetForegroundWindow();

    int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference lpdwProcessId);
}

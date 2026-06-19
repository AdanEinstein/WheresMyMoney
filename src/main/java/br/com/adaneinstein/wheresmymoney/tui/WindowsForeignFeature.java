package br.com.adaneinstein.wheresmymoney.tui;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

import java.lang.foreign.FunctionDescriptor;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Registra em build-time os downcalls FFM da kernel32 usados por
 * {@link WindowsNativeTerminal}. Sem isso o native image lança
 * MissingForeignRegistrationError no &lt;clinit&gt; daquele terminal.
 */
public class WindowsForeignFeature implements Feature {
    @Override
    public void duringSetup(DuringSetupAccess access) {
        // GetStdHandle(int) -> HANDLE
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(ADDRESS, JAVA_INT));
        // GetConsoleMode(HANDLE, LPDWORD) -> BOOL ; idem GetConsoleScreenBufferInfo(HANDLE, ptr)
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        // SetConsoleMode(HANDLE, DWORD) -> BOOL
        RuntimeForeignAccess.registerForDowncall(FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    }
}

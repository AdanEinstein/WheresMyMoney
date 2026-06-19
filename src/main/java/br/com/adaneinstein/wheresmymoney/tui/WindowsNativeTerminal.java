package br.com.adaneinstein.wheresmymoney.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.terminal.ansi.CygwinTerminal;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * Terminal Lanterna para binário nativo no Windows. Substitui stty por
 * chamadas diretas à kernel32.dll via Java 25 FFM API.
 *
 * Habilita ANSI/VT100 no stdout e configura raw mode no stdin via
 * Windows Console API, sem dependência de Cygwin ou processo Java.
 */
public class WindowsNativeTerminal extends CygwinTerminal {

    private static final int STD_INPUT_HANDLE  = -10;
    private static final int STD_OUTPUT_HANDLE = -11;

    // ENABLE_PROCESSED_OUTPUT | ENABLE_WRAP_AT_EOL_OUTPUT | ENABLE_VIRTUAL_TERMINAL_PROCESSING
    private static final int OUT_VT_FLAGS = 0x0001 | 0x0002 | 0x0004;

    private static final int ENABLE_ECHO_INPUT = 0x0004;
    private static final int ENABLE_LINE_INPUT  = 0x0002;

    // ponytail: static init garante handles FFM prontos antes de super() chamar acquire()
    private static final MethodHandle GET_STD_HANDLE;
    private static final MethodHandle GET_CONSOLE_MODE;
    private static final MethodHandle SET_CONSOLE_MODE;
    private static final MethodHandle GET_CONSOLE_SCREEN_BUFFER_INFO;

    static {
        Linker linker = Linker.nativeLinker();
        SymbolLookup k32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());
        GET_STD_HANDLE = linker.downcallHandle(
                k32.find("GetStdHandle").orElseThrow(),
                FunctionDescriptor.of(ADDRESS, JAVA_INT));
        GET_CONSOLE_MODE = linker.downcallHandle(
                k32.find("GetConsoleMode").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        SET_CONSOLE_MODE = linker.downcallHandle(
                k32.find("SetConsoleMode").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        GET_CONSOLE_SCREEN_BUFFER_INFO = linker.downcallHandle(
                k32.find("GetConsoleScreenBufferInfo").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    }

    private int savedInputMode;
    private int savedOutputMode;

    public WindowsNativeTerminal() throws IOException {
        super(System.in, System.out, StandardCharsets.UTF_8);
    }

    // Bloqueia todas as chamadas stty — ponto central de falha no Windows
    @Override
    protected String runSTTYCommand(String... commands) throws IOException {
        return "";
    }

    @Override
    protected void saveTerminalSettings() throws IOException {
        savedInputMode  = readConsoleMode(STD_INPUT_HANDLE);
        savedOutputMode = readConsoleMode(STD_OUTPUT_HANDLE);
        // Habilita VT100 no output imediatamente
        writeConsoleMode(STD_OUTPUT_HANDLE, savedOutputMode | OUT_VT_FLAGS);
    }

    @Override
    protected void restoreTerminalSettings() throws IOException {
        writeConsoleMode(STD_INPUT_HANDLE,  savedInputMode);
        writeConsoleMode(STD_OUTPUT_HANDLE, savedOutputMode);
    }

    @Override
    protected void keyEchoEnabled(boolean enabled) throws IOException {
        int mode = readConsoleMode(STD_INPUT_HANDLE);
        writeConsoleMode(STD_INPUT_HANDLE, enabled ? (mode | ENABLE_ECHO_INPUT) : (mode & ~ENABLE_ECHO_INPUT));
    }

    @Override
    protected void canonicalMode(boolean enabled) throws IOException {
        int mode = readConsoleMode(STD_INPUT_HANDLE);
        writeConsoleMode(STD_INPUT_HANDLE, enabled ? (mode | ENABLE_LINE_INPUT) : (mode & ~ENABLE_LINE_INPUT));
    }

    @Override
    protected void keyStrokeSignalsEnabled(boolean enabled) throws IOException {
        // Windows trata Ctrl+C diferentemente; no-op aqui
    }

    @Override
    protected void registerTerminalResizeListener(Runnable runnable) throws IOException {
        // SIGWINCH não existe no Windows; no-op
    }

    @Override
    protected TerminalSize findTerminalSize() {
        // 22 bytes: CONSOLE_SCREEN_BUFFER_INFO
        // Offsets: srWindow.Left=10, Top=12, Right=14, Bottom=16 (shorts)
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(22);
            MemorySegment hOut = stdHandle(STD_OUTPUT_HANDLE);
            int ok = (int) GET_CONSOLE_SCREEN_BUFFER_INFO.invoke(hOut, buf);
            if (ok != 0) {
                int cols = (buf.get(JAVA_SHORT, 14) & 0xFFFF) - (buf.get(JAVA_SHORT, 10) & 0xFFFF) + 1;
                int rows = (buf.get(JAVA_SHORT, 16) & 0xFFFF) - (buf.get(JAVA_SHORT, 12) & 0xFFFF) + 1;
                return new TerminalSize(cols, rows);
            }
        } catch (Throwable t) {
            // fallthrough to default
        }
        return new TerminalSize(80, 24);
    }

    private MemorySegment stdHandle(int id) {
        try {
            return (MemorySegment) GET_STD_HANDLE.invoke(id);
        } catch (Throwable t) {
            throw new RuntimeException("GetStdHandle(" + id + ") failed", t);
        }
    }

    private int readConsoleMode(int handleId) throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment modePtr = arena.allocate(JAVA_INT);
            GET_CONSOLE_MODE.invoke(stdHandle(handleId), modePtr);
            return modePtr.get(JAVA_INT, 0);
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException("GetConsoleMode failed", t);
        }
    }

    private void writeConsoleMode(int handleId, int mode) throws IOException {
        try {
            SET_CONSOLE_MODE.invoke(stdHandle(handleId), mode);
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException("SetConsoleMode failed", t);
        }
    }
}

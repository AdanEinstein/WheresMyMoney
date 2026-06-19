package br.com.adaneinstein.wheresmymoney.tui;

import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;

public class TerminalFactory extends DefaultTerminalFactory {

    @Override
    public Terminal createHeadlessTerminal() throws IOException {
        if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            return new WindowsNativeTerminal();
        }
        return super.createHeadlessTerminal();
    }
}

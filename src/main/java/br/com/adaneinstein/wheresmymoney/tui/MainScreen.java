package br.com.adaneinstein.wheresmymoney.tui;

import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MainScreen {

    public void start() throws IOException {
        var terminal = new DefaultTerminalFactory().createTerminal();
        var screen = new TerminalScreen(terminal);

    }
}

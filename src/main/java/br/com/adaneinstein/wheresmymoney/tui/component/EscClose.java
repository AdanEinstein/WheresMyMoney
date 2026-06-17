package br.com.adaneinstein.wheresmymoney.tui.component;

import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.concurrent.atomic.AtomicBoolean;

/** WindowListener que fecha a janela ao pressionar Esc. */
public final class EscClose extends WindowListenerAdapter {

    private final Window window;

    private EscClose(Window window) {
        this.window = window;
    }

    public static EscClose of(Window window) {
        return new EscClose(window);
    }

    @Override
    public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
        if (keyStroke.getKeyType() == KeyType.Escape) {
            deliverEvent.set(false);
            window.close();
        }
    }
}

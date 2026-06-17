package br.com.adaneinstein.wheresmymoney.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;

/** Tema da TUI: fundo escuro, destaque ciano, seleção verde. */
public final class AppTheme {

    private AppTheme() {
    }

    public static SimpleTheme build() {
        return SimpleTheme.makeTheme(
                true,                          // negrito quando ativo
                TextColor.ANSI.WHITE_BRIGHT,   // texto base
                TextColor.ANSI.BLACK,          // fundo base
                TextColor.ANSI.BLACK,          // texto editável
                TextColor.ANSI.CYAN,           // fundo editável
                TextColor.ANSI.BLACK,          // texto selecionado
                TextColor.ANSI.GREEN,          // fundo selecionado
                TextColor.ANSI.BLACK           // fundo do GUI
        );
    }
}

package br.com.adaneinstein.wheresmymoney.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;

/**
 * Tema e paleta da TUI (dark coeso, estilo "Tokyo Night").
 *
 * <p>As telas devem referenciar as cores semânticas daqui ({@link #INCOME},
 * {@link #EXPENSE}, {@link #MUTED}, …) em vez de {@code TextColor.ANSI.*} cru,
 * para manter o visual consistente.
 */
public final class AppTheme {

    // ── Base ──────────────────────────────────────────────────────────────
    public static final TextColor BG     = new TextColor.RGB(0x1a, 0x1b, 0x26); // fundo
    public static final TextColor FG     = new TextColor.RGB(0xc0, 0xca, 0xf5); // texto
    public static final TextColor FIELD  = new TextColor.RGB(0x41, 0x48, 0x68); // fundo de campo editável
    public static final TextColor MUTED  = new TextColor.RGB(0x6c, 0x76, 0x9a); // dicas, metadados
    public static final TextColor ACCENT = new TextColor.RGB(0x7a, 0xa2, 0xf7); // foco, títulos, seleção

    // ── Semânticas ────────────────────────────────────────────────────────
    public static final TextColor INCOME  = new TextColor.RGB(0x9e, 0xce, 0x6a); // receitas / ok
    public static final TextColor EXPENSE = new TextColor.RGB(0xf7, 0x76, 0x8e); // despesas / erro
    public static final TextColor WARN    = new TextColor.RGB(0xe0, 0xaf, 0x68); // atenção / pendente
    public static final TextColor INFO    = new TextColor.RGB(0x7d, 0xcf, 0xff); // informação

    private AppTheme() {
    }

    public static SimpleTheme build() {
        return SimpleTheme.makeTheme(
                true,        // negrito quando ativo
                FG,          // texto base
                BG,          // fundo base
                FG,          // texto editável
                FIELD,       // fundo editável
                BG,          // texto selecionado
                ACCENT,      // fundo selecionado
                BG           // fundo do GUI
        );
    }
}

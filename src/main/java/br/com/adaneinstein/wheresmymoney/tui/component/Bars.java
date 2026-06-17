package br.com.adaneinstein.wheresmymoney.tui.component;

import java.math.BigDecimal;

/** Gera barras ASCII para os relatórios. */
public final class Bars {

    private Bars() {
    }

    public static String bar(BigDecimal value, BigDecimal max, int width) {
        if (max == null || max.signum() <= 0 || value == null || value.signum() <= 0) {
            return "";
        }
        double ratio = value.doubleValue() / max.doubleValue();
        int filled = (int) Math.round(ratio * width);
        filled = Math.max(0, Math.min(width, filled));
        return "█".repeat(filled) + "░".repeat(width - filled);
    }
}

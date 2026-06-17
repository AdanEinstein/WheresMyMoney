package br.com.adaneinstein.wheresmymoney.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatação monetária em Real (pt-BR) para a TUI. */
public final class CurrencyUtil {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private CurrencyUtil() {
    }

    public static String format(BigDecimal value) {
        if (value == null) {
            value = BigDecimal.ZERO;
        }
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    /** Aceita "1234.56" ou "1.234,56" / "1234,56"; lança IllegalArgumentException se inválido. */
    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Valor vazio");
        }
        String normalized = raw.trim().replace("R$", "").trim();
        if (normalized.contains(",")) {
            // formato pt-BR: ponto é milhar, vírgula é decimal
            normalized = normalized.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido: " + raw);
        }
    }
}

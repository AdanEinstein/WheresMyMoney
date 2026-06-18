package br.com.adaneinstein.wheresmymoney.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatação monetária em Real (pt-BR) para a TUI. */
public final class CurrencyUtil {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Máscara sem símbolo: agrupa milhar com ponto e usa vírgula decimal (ex: 9.999,99). */
    private static final DecimalFormat MASK =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));

    private CurrencyUtil() {
    }

    /**
     * Máscara de digitação monetária: trata os dígitos como centavos e formata
     * como 9.999,99. Ex: "9" → "0,09", "999999" → "9.999,99". Sem dígitos → "".
     */
    public static String maskMoney(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        BigDecimal value = new BigDecimal(digits).movePointLeft(2);
        return MASK.format(value);
    }

    /** Aplica a máscara a um valor já existente (ex: ao editar). */
    public static String maskMoney(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return MASK.format(value.setScale(2, RoundingMode.HALF_UP));
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

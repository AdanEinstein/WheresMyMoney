package br.com.adaneinstein.wheresmymoney.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyUtilTest {

    @Test
    void masksDigitsAsCents() {
        assertThat(CurrencyUtil.maskMoney("197895")).isEqualTo("1.978,95");
        assertThat(CurrencyUtil.maskMoney("9")).isEqualTo("0,09");
    }

    @Test
    void parsesBrazilianFormat() {
        assertThat(CurrencyUtil.parse("1.978,95")).isEqualByComparingTo(new BigDecimal("1978.95"));
    }

    @Test
    void formatsWithThousandSeparator() {
        assertThat(CurrencyUtil.format(new BigDecimal("1978.95"))).isEqualTo("1.978,95");
    }

    /** Regressão do bug 197895 → 1.98: máscara/parse precisam ser consistentes mesmo sem locale pt-BR. */
    @Test
    void roundTripFromKeystrokesPreservesValue() {
        assertThat(CurrencyUtil.parse(CurrencyUtil.maskMoney("197895")))
                .isEqualByComparingTo(new BigDecimal("1978.95"));
    }
}

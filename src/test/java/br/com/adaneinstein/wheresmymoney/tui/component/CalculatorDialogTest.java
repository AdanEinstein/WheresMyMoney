package br.com.adaneinstein.wheresmymoney.tui.component;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculatorDialogTest {

    @Test
    void evaluatesExpressionWithPrecedence() {
        assertThat(CalculatorDialog.evaluateExpression("10 + 2 * 3"))
                .isEqualByComparingTo(new BigDecimal("16"));
    }

    @Test
    void evaluatesParenthesesAndUnarySigns() {
        assertThat(CalculatorDialog.evaluateExpression("-(5 + 3) * 2"))
                .isEqualByComparingTo(new BigDecimal("-16"));
    }

    @Test
    void acceptsBrazilianAndDotDecimalNumbers() {
        assertThat(CalculatorDialog.evaluateExpression("1.000,50 + 2,50"))
                .isEqualByComparingTo(new BigDecimal("1003.00"));
        assertThat(CalculatorDialog.evaluateExpression("1.5 + 2"))
                .isEqualByComparingTo(new BigDecimal("3.5"));
    }

    @Test
    void rejectsInvalidExpressions() {
        assertThatThrownBy(() -> CalculatorDialog.evaluateExpression("2 + * 3"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalculatorDialog.evaluateExpression("10 / 0"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package br.com.adaneinstein.wheresmymoney.tui.component;

import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.gui2.TextBox;

/**
 * Aplica máscara monetária pt-BR (9.999,99) a um {@link TextBox} enquanto o
 * usuário digita. Os dígitos são tratados como centavos e reformatados a cada
 * alteração; o cursor é mantido no fim do texto.
 */
public final class MoneyMask {

    private MoneyMask() {
    }

    /** Conecta a máscara ao TextBox e reformata o conteúdo inicial. */
    public static void apply(TextBox box) {
        box.setTextChangeListener((newText, changedByUser) -> {
            String masked = CurrencyUtil.maskMoney(newText);
            if (!masked.equals(newText)) {
                box.setText(masked); // máscara idempotente: não recursiona
                box.setCaretPosition(0, masked.length());
            }
        });
        // reformata valor já presente (ex: edição)
        box.setText(CurrencyUtil.maskMoney(box.getText()));
    }
}

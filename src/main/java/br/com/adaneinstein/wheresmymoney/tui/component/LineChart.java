package br.com.adaneinstein.wheresmymoney.tui.component;

import br.com.adaneinstein.wheresmymoney.tui.AppTheme;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractComponent;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;

/**
 * Gráfico de linhas com duas séries sobrepostas (ex.: despesas × receitas) no
 * mesmo plano cartesiano. Eixo X = meses (colunas), eixo Y = valor.
 *
 * <p>Componente desenhado célula a célula (precisa de cor por célula, o que um
 * {@link com.googlecode.lanterna.gui2.Label} monocromático não permite).
 * Expõe {@link #monthAtColumn(int)} para que a tela traduza a posição do mouse
 * em índice de mês e abra o popover correspondente.
 */
public class LineChart extends AbstractComponent<LineChart> {

    private static final char POINT = '●'; // ●

    private final double[] seriesA;
    private final double[] seriesB;
    private final TextColor colorA;
    private final TextColor colorB;
    private final String[] labels;

    /** Mês destacado pela navegação por teclado (-1 = nenhum). */
    private int selected = -1;

    public LineChart(double[] seriesA, double[] seriesB, String[] labels,
                     TextColor colorA, TextColor colorB) {
        this.seriesA = seriesA;
        this.seriesB = seriesB;
        this.labels = labels;
        this.colorA = colorA;
        this.colorB = colorB;
    }

    public void setSelected(int month) {
        this.selected = month;
        invalidate();
    }

    @Override
    protected ComponentRenderer<LineChart> createDefaultRenderer() {
        return new Renderer();
    }

    private final class Renderer implements ComponentRenderer<LineChart> {

        @Override
        public TerminalSize getPreferredSize(LineChart c) {
            return new TerminalSize(Math.max(24, labels.length * 4), 14);
        }

        @Override
        public void drawComponent(TextGUIGraphics g, LineChart c) {
            TerminalSize size = g.getSize();
            int cols = size.getColumns();
            int rows = size.getRows();
            g.setForegroundColor(AppTheme.FG);
            g.fill(' ');
            if (cols < 4 || rows < 3 || labels.length == 0) {
                return;
            }

            int n = labels.length;
            int colWidth = Math.max(1, cols / n);

            double max = 0;
            for (int i = 0; i < n; i++) {
                if (i < seriesA.length) max = Math.max(max, seriesA[i]);
                if (i < seriesB.length) max = Math.max(max, seriesB[i]);
            }
            if (max <= 0) {
                g.setForegroundColor(AppTheme.MUTED);
                g.putString(0, 0, "Sem dados no período.");
                return;
            }

            int plotRows = rows - 1; // última linha = rótulos dos meses
            int[] xs = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = i * colWidth + colWidth / 2;
            }

            // Guia vertical do mês selecionado (atrás das séries).
            if (selected >= 0 && selected < n) {
                g.setForegroundColor(AppTheme.MUTED);
                for (int row = 0; row < plotRows; row++) {
                    g.setCharacter(xs[selected], row, '┊'); // ┊
                }
            }

            drawSeries(g, seriesA, xs, plotRows, max, colorA);
            drawSeries(g, seriesB, xs, plotRows, max, colorB);

            // Eixo X: rótulos centralizados; o selecionado em destaque.
            for (int i = 0; i < n; i++) {
                String s = labels[i];
                int start = xs[i] - s.length() / 2;
                if (start < 0) start = 0;
                if (start + s.length() > cols) start = Math.max(0, cols - s.length());
                g.setForegroundColor(i == selected ? AppTheme.ACCENT : AppTheme.MUTED);
                g.putString(start, rows - 1, s);
            }
        }

        private void drawSeries(TextGUIGraphics g, double[] values, int[] xs,
                                int plotRows, double max, TextColor color) {
            int n = xs.length;
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                double v = i < values.length ? values[i] : 0;
                int level = (int) Math.round(v / max * (plotRows - 1));
                ys[i] = (plotRows - 1) - level; // valor maior = mais alto (row menor)
            }
            g.setForegroundColor(color);
            // Segmentos ligando pontos consecutivos.
            for (int i = 0; i < n - 1; i++) {
                line(g, xs[i], ys[i], xs[i + 1], ys[i + 1]);
            }
            // Marcadores por cima dos segmentos.
            for (int i = 0; i < n; i++) {
                g.setCharacter(xs[i], ys[i], POINT);
            }
        }

        /** Bresenham: desenha reta entre dois pontos com '·'. */
        private void line(TextGUIGraphics g, int x0, int y0, int x1, int y1) {
            int dx = Math.abs(x1 - x0);
            int dy = -Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx + dy;
            while (true) {
                g.setCharacter(x0, y0, '·'); // ·
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        }
    }
}

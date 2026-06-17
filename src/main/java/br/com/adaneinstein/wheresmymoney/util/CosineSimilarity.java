package br.com.adaneinstein.wheresmymoney.util;

/** Similaridade de cosseno entre dois vetores. */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    /**
     * Retorna o cosseno entre {@code a} e {@code b} no intervalo [-1, 1].
     * Retorna 0 se algum vetor for nulo, de tamanhos diferentes ou de norma zero.
     */
    public static double between(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

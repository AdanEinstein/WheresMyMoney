package br.com.adaneinstein.wheresmymoney.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CosineSimilarityTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        float[] v = {1f, 2f, 3f};
        assertThat(CosineSimilarity.between(v, v)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        assertThat(CosineSimilarity.between(new float[]{1f, 0f}, new float[]{0f, 1f})).isZero();
    }

    @Test
    void oppositeVectorsHaveSimilarityMinusOne() {
        assertThat(CosineSimilarity.between(new float[]{1f, 1f}, new float[]{-1f, -1f}))
                .isCloseTo(-1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void nullOrMismatchedReturnsZero() {
        assertThat(CosineSimilarity.between(null, new float[]{1f})).isZero();
        assertThat(CosineSimilarity.between(new float[]{1f, 2f}, new float[]{1f})).isZero();
        assertThat(CosineSimilarity.between(new float[]{0f, 0f}, new float[]{1f, 1f})).isZero();
    }
}

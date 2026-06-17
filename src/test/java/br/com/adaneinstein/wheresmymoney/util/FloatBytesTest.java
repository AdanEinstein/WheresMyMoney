package br.com.adaneinstein.wheresmymoney.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FloatBytesTest {

    @Test
    void roundTripPreservesValues() {
        float[] original = {0.1f, -2.5f, 3.14159f, 0f, 1234.5f};
        float[] restored = FloatBytes.toFloats(FloatBytes.toBytes(original));
        assertThat(restored).containsExactly(original);
    }

    @Test
    void byteLengthMatchesVectorSize() {
        assertThat(FloatBytes.toBytes(new float[]{1f, 2f, 3f})).hasSize(3 * Float.BYTES);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(FloatBytes.toBytes(null)).isNull();
        assertThat(FloatBytes.toFloats(null)).isNull();
    }
}

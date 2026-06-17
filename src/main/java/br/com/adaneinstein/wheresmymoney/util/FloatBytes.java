package br.com.adaneinstein.wheresmymoney.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Serialização compacta de float[] para byte[] (little-endian) e volta. */
public final class FloatBytes {

    private FloatBytes() {
    }

    public static byte[] toBytes(float[] vector) {
        if (vector == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    public static float[] toFloats(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("byte[] não múltiplo de " + Float.BYTES);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] out = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < out.length; i++) {
            out[i] = buffer.getFloat();
        }
        return out;
    }
}

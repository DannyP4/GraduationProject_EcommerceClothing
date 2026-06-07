package com.uniform.store.service.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

final class Vectors {

    private Vectors() {
    }

    static float[] normalize(float[] v) {
        double sum = 0.0;
        for (float x : v) sum += (double) x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0.0) return v;
        float inv = (float) (1.0 / norm);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] * inv;
        return out;
    }

    static double dot(float[] a, float[] b) {
        double s = 0.0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) s += (double) a[i] * b[i];
        return s;
    }

    static byte[] toBytes(float[] v) {
        ByteBuffer bb = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float x : v) bb.putFloat(x);
        return bb.array();
    }

    static float[] toFloats(byte[] bytes) {
        FloatBuffer fb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] v = new float[fb.remaining()];
        fb.get(v);
        return v;
    }
}

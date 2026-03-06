package com.muyi.common.util.codec;

/**
 * RC4 单方向状态机
 *
 * @author muyi
 */
class Rc4State {
    
    private final int[] s = new int[256];
    private int i;
    private int j;
    
    Rc4State(byte[] key, int dropBytes) {
        // KSA (Key-Scheduling Algorithm)
        for (int k = 0; k < 256; k++) {
            s[k] = k;
        }
        int jj = 0;
        for (int k = 0; k < 256; k++) {
            jj = (jj + s[k] + (key[k % key.length] & 0xFF)) & 0xFF;
            swap(k, jj);
        }
        i = 0;
        j = 0;
        
        // Drop-N: discard initial keystream to mitigate known biases
        for (int d = 0; d < dropBytes; d++) {
            nextByte();
        }
    }
    
    void xor(byte[] data) {
        for (int k = 0; k < data.length; k++) {
            data[k] ^= (byte) nextByte();
        }
    }
    
    private int nextByte() {
        i = (i + 1) & 0xFF;
        j = (j + s[i]) & 0xFF;
        swap(i, j);
        return s[(s[i] + s[j]) & 0xFF];
    }
    
    private void swap(int a, int b) {
        int tmp = s[a];
        s[a] = s[b];
        s[b] = tmp;
    }
}

package com.muyi.rpc.compress;

/**
 * 无压缩（透传）
 *
 * @author muyi
 */
public class NoneCompressor implements Compressor {
    
    @Override
    public byte[] compress(byte[] bytes) {
        return bytes;
    }
    
    @Override
    public byte[] decompress(byte[] bytes) {
        return bytes;
    }
    
    @Override
    public byte getType() {
        return NONE;
    }
}

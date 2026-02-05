package com.muyi.rpc.compress;

/**
 * 压缩器接口
 *
 * @author muyi
 */
public interface Compressor {
    
    /**
     * 无压缩
     */
    byte NONE = 0;
    
    /**
     * 压缩
     *
     * @param bytes 原始字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] bytes);
    
    /**
     * 解压
     *
     * @param bytes 压缩后的字节数组
     * @return 原始字节数组
     */
    byte[] decompress(byte[] bytes);
    
    /**
     * 获取压缩器类型
     */
    byte getType();
}

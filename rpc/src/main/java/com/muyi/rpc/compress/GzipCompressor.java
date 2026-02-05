package com.muyi.rpc.compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip 压缩器
 * 压缩率高，CPU 开销中等，适合大数据量传输
 *
 * @author muyi
 */
public class GzipCompressor implements Compressor {
    
    public static final byte TYPE = 1;
    
    private static final int BUFFER_SIZE = 4096;
    
    /** 解压缩最大大小限制（防止压缩炸弹，默认 10MB） */
    private static final int MAX_DECOMPRESS_SIZE = 10 * 1024 * 1024;
    
    @Override
    public byte[] compress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
            gzip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Gzip compress error", e);
        }
    }
    
    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             GZIPInputStream gzip = new GZIPInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            int totalSize = 0;
            while ((n = gzip.read(buffer)) >= 0) {
                totalSize += n;
                // 解压过程中检查大小，防止压缩炸弹在解压完成前就 OOM
                if (totalSize > MAX_DECOMPRESS_SIZE) {
                    throw new RuntimeException("Decompressed data exceeds max size limit: " + MAX_DECOMPRESS_SIZE);
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Gzip decompress error", e);
        }
    }
    
    @Override
    public byte getType() {
        return TYPE;
    }
}

package com.muyi.config.converter;

import com.muyi.config.IFieldConverter;

/**
 * 整数数组转换�?
 * �?"1,2,3" 转换�?int[]
 *
 * @author muyi
 */
public class IntArrayConverter implements IFieldConverter<int[]> {
    
    private static final String SEPARATOR = ",";
    private static final int[] EMPTY = new int[0];
    
    @Override
    public int[] convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue();
        }
        
        String[] parts = value.split(SEPARATOR);
        int[] result = new int[parts.length];
        int count = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result[count++] = Integer.parseInt(trimmed);
            }
        }
        
        if (count < parts.length) {
            int[] trimmed = new int[count];
            System.arraycopy(result, 0, trimmed, 0, count);
            return trimmed;
        }
        return result;
    }
    
    @Override
    public int[] defaultValue() {
        return EMPTY;
    }
}

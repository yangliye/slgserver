package com.muyi.config.converter;

import com.muyi.config.IFieldConverter;

/**
 * 二维整数数组转换�?
 * �?"1,2,3;4,5,6;7,8,9" 转换�?int[][]
 * 
 * 格式：行内用逗号分隔，行间用分号分隔
 *
 * @author muyi
 */
public class Int2DArrayConverter implements IFieldConverter<int[][]> {
    
    private static final String ROW_SEPARATOR = ";";
    private static final String COL_SEPARATOR = ",";
    private static final int[][] EMPTY = new int[0][];
    
    @Override
    public int[][] convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue();
        }
        
        String[] rows = value.split(ROW_SEPARATOR);
        int[][] result = new int[rows.length][];
        
        for (int i = 0; i < rows.length; i++) {
            String row = rows[i].trim();
            if (row.isEmpty()) {
                result[i] = new int[0];
                continue;
            }
            
            String[] cols = row.split(COL_SEPARATOR);
            result[i] = new int[cols.length];
            for (int j = 0; j < cols.length; j++) {
                result[i][j] = Integer.parseInt(cols[j].trim());
            }
        }
        
        return result;
    }
    
    @Override
    public int[][] defaultValue() {
        return EMPTY;
    }
}

package com.muyi.gameconfig.converter;

import com.muyi.gameconfig.IFieldConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 整数列表转换器
 * 将 "1,2,3" 转换为 List<Integer>
 *
 * @author muyi
 */
public class IntListConverter implements IFieldConverter<List<Integer>> {
    
    /** 分隔符 */
    private static final String SEPARATOR = ",";
    
    @Override
    public List<Integer> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue();
        }
        
        String[] parts = value.split(SEPARATOR);
        List<Integer> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(Integer.parseInt(trimmed));
            }
        }
        // 返回不可变列表，防止意外修改配置数据
        return Collections.unmodifiableList(result);
    }
    
    @Override
    public List<Integer> defaultValue() {
        return Collections.emptyList();
    }
}

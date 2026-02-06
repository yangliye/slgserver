package com.muyi.gameconfig.converter;

import com.muyi.gameconfig.IFieldConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 字符串列表转换器
 * 将 "a,b,c" 转换为 List<String>
 *
 * @author muyi
 */
public class StringListConverter implements IFieldConverter<List<String>> {
    
    private static final String SEPARATOR = ",";
    
    @Override
    public List<String> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue();
        }
        
        String[] parts = value.split(SEPARATOR);
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        // 返回不可变列表，防止意外修改配置数据
        return Collections.unmodifiableList(result);
    }
    
    @Override
    public List<String> defaultValue() {
        return Collections.emptyList();
    }
}

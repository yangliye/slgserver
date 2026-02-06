package com.muyi.gameconfig.converter;

import com.muyi.gameconfig.IFieldConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 整数-整数映射转换器
 * 将 "1:100,2:200,3:300" 转换为 Map<Integer, Integer>
 * 
 * 格式：key:value,key:value,...
 *
 * @author muyi
 */
public class IntIntMapConverter implements IFieldConverter<Map<Integer, Integer>> {
    
    private static final String ENTRY_SEPARATOR = ",";
    private static final String KV_SEPARATOR = ":";
    
    @Override
    public Map<Integer, Integer> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue();
        }
        
        String[] entries = value.split(ENTRY_SEPARATOR);
        Map<Integer, Integer> result = new HashMap<>(entries.length);
        
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            
            String[] kv = trimmed.split(KV_SEPARATOR);
            if (kv.length >= 2) {
                int key = Integer.parseInt(kv[0].trim());
                int val = Integer.parseInt(kv[1].trim());
                result.put(key, val);
            }
        }
        
        // 返回不可变 Map，防止意外修改配置数据
        return Collections.unmodifiableMap(result);
    }
    
    @Override
    public Map<Integer, Integer> defaultValue() {
        return Collections.emptyMap();
    }
}

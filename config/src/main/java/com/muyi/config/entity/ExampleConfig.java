package com.muyi.config.entity;

import com.muyi.config.ConfigConverter;
import com.muyi.config.ConfigFile;
import com.muyi.config.IConfig;
import com.muyi.config.converter.IntListConverter;
import com.muyi.config.converter.IntIntMapConverter;

import java.util.List;
import java.util.Map;

/**
 * 示例配置
 * 
 * 演示如何定义一个配置类，包含：
 * - 基础类型字段自动映射
 * - @ConfigConverter 自定义类型转�?
 * - afterLoad() 生命周期方法
 * - validate() 数据校验
 * 
 * 对应 XML 示例�?
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config>
 *     <item id="1" name="示例1" type="1" value="100" tags="1,2,3" rewards="1001:10,1002:20" />
 *     <item id="2" name="示例2" type="2" value="200" tags="4,5" rewards="1003:30" />
 * </config>
 * }</pre>
 *
 * @author muyi
 */
@ConfigFile("example.xml")
public class ExampleConfig implements IConfig {
    
    /** 配置ID */
    private int id;
    
    /** 名称 */
    private String name;
    
    /** 类型 */
    private int type;
    
    /** 数�?*/
    private int value;
    
    /** 标签列表：XML 中为 "1,2,3" */
    @ConfigConverter(IntListConverter.class)
    private List<Integer> tags;
    
    /** 奖励映射：XML 中为 "1001:10,1002:20" */
    @ConfigConverter(IntIntMapConverter.class)
    private Map<Integer, Integer> rewards;
    
    // ========== 自定义组装的字段 ==========
    
    /** 计算字段（在 afterLoad 中赋值） */
    private transient int computed;
    
    @Override
    public void afterLoad(Map<String, String> rawAttributes) {
        // 自定义计�?
        this.computed = value * type;
        
        // 可从 rawAttributes 获取未映射的属�?
    }
    
    @Override
    public void validate() {
        if (id <= 0) {
            throw new IllegalStateException("ExampleConfig id must > 0, got: " + id);
        }
    }
    
    @Override
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public List<Integer> getTags() {
        return tags;
    }
    
    public Map<Integer, Integer> getRewards() {
        return rewards;
    }
    
    public int getComputed() {
        return computed;
    }
    
    @Override
    public String toString() {
        return "ExampleConfig{id=" + id + ", name='" + name + "', computed=" + computed + "}";
    }
}

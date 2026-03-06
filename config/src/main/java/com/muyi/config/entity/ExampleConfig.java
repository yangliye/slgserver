package com.muyi.config.entity;

import com.muyi.config.annotation.ConfigConverter;
import com.muyi.config.annotation.ConfigFile;
import com.muyi.config.IConfig;
import com.muyi.config.converter.IntListConverter;
import com.muyi.config.converter.IntIntMapConverter;

import java.util.List;
import java.util.Map;

/**
 * 示例配置
 *
 * @author muyi
 */
@ConfigFile("example.xml")
public class ExampleConfig implements IConfig {

    private int id;
    private String name;
    private int type;
    private int value;

    /** 标签列表 */
    @ConfigConverter(IntListConverter.class)
    private List<Integer> tags;

    /** 奖励映射 */
    @ConfigConverter(IntIntMapConverter.class)
    private Map<Integer, Integer> rewards;

    /** 计算字段，在 afterLoad 中赋值 */
    private transient int computed;

    @Override
    public void afterLoad(Map<String, String> rawAttributes) {
        this.computed = value * type;
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
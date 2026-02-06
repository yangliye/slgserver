package com.muyi.gameconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XML 配置加载器
 * 
 * XML 格式示例：
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config>
 *     <item id="1" name="小兵" attack="10" defense="5" />
 *     <item id="2" name="骑兵" attack="20" defense="10" />
 * </config>
 * }</pre>
 *
 * @author muyi
 */
public class XmlConfigLoader implements ConfigLoader {
    
    private static final Logger log = LoggerFactory.getLogger(XmlConfigLoader.class);
    
    /** 支持的扩展名（避免每次调用创建新数组） */
    private static final String[] SUPPORTED_EXTENSIONS = {".xml"};
    
    /** StAX 工厂（线程安全） */
    private final XMLInputFactory xmlFactory;
    
    /** 转换器缓存 Class -> Instance */
    private final Map<Class<? extends IFieldConverter<?>>, IFieldConverter<?>> converterCache = new ConcurrentHashMap<>();
    
    /** 字段元数据缓存 ConfigClass -> FieldMeta[] （避免重复反射） */
    private final Map<Class<?>, FieldMeta[]> fieldMetaCache = new ConcurrentHashMap<>();
    
    public XmlConfigLoader() {
        this.xmlFactory = XMLInputFactory.newInstance();
        // 安全配置：禁用外部实体，防止 XXE 攻击
        xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }
    
    /**
     * 字段元数据（缓存反射信息，避免每次解析都反射）
     */
    private static class FieldMeta {
        final Field field;
        final String name;
        final Class<?> type;
        final Class<? extends IFieldConverter<?>> converterClass;
        
        FieldMeta(Field field) {
            field.setAccessible(true);  // 一次性设置
            this.field = field;
            this.name = field.getName();
            this.type = field.getType();
            
            ConfigConverter annotation = field.getAnnotation(ConfigConverter.class);
            this.converterClass = annotation != null ? annotation.value() : null;
        }
    }
    
    @Override
    public <T extends IConfig> List<T> load(String path, Class<T> configClass) throws Exception {
        // 尝试从文件系统加载
        File file = new File(path);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                return parseStream(input, configClass);
            }
        }
        
        // 尝试从类路径加载
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input != null) {
                return parseStream(input, configClass);
            }
        }
        
        throw new IllegalArgumentException("Config file not found: " + path);
    }
    
    /**
     * 从输入流加载
     */
    public <T extends IConfig> List<T> load(InputStream input, Class<T> configClass) throws Exception {
        return parseStream(input, configClass);
    }
    
    /**
     * 使用 StAX 流式解析（低内存占用）
     */
    private <T extends IConfig> List<T> parseStream(InputStream input, Class<T> configClass) throws Exception {
        List<T> result = new ArrayList<>();
        FieldMeta[] fieldMetas = getFieldMetas(configClass);
        
        XMLStreamReader reader = xmlFactory.createXMLStreamReader(input);
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                
                if (event == XMLStreamConstants.START_ELEMENT && "item".equals(reader.getLocalName())) {
                    // 解析单个 item 元素
                    T config = parseItem(reader, configClass, fieldMetas);
                    result.add(config);
                }
            }
        } finally {
            reader.close();
        }
        
        log.debug("Loaded {} items of {}", result.size(), configClass.getSimpleName());
        return result;
    }
    
    /**
     * 解析单个 item 元素（StAX 流式）
     */
    private <T extends IConfig> T parseItem(XMLStreamReader reader, Class<T> configClass, FieldMeta[] fieldMetas) throws Exception {
        T config;
        try {
            config = configClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Config class must have a no-arg constructor: " + configClass.getName(), e);
        }
        
        // 提取属性
        int attrCount = reader.getAttributeCount();
        Map<String, String> rawAttributes = new HashMap<>((int) (attrCount / 0.75) + 1);
        
        for (int i = 0; i < attrCount; i++) {
            rawAttributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }
        
        // 字段赋值
        for (FieldMeta meta : fieldMetas) {
            String attrValue = rawAttributes.get(meta.name);
            
            if (meta.converterClass != null) {
                Object value = convertWithConverter(attrValue, meta.converterClass);
                meta.field.set(config, value);
            } else if (attrValue != null && !attrValue.isEmpty()) {
                Object value = convertValue(attrValue, meta.type);
                meta.field.set(config, value);
            }
        }
        
        // 生命周期回调
        config.afterLoad(rawAttributes);
        config.validate();
        
        return config;
    }
    
    /**
     * 获取字段元数据（带缓存）
     */
    private FieldMeta[] getFieldMetas(Class<?> configClass) {
        return fieldMetaCache.computeIfAbsent(configClass, clazz -> {
            List<Field> fields = getAllFields(clazz);
            FieldMeta[] metas = new FieldMeta[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                metas[i] = new FieldMeta(fields.get(i));
            }
            return metas;
        });
    }
    
    /**
     * 获取所有实例字段（包括父类，排除 static、transient 和 synthetic）
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                // 跳过 static、transient 和 synthetic 字段
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
    
    /**
     * 使用自定义转换器转换值
     */
    @SuppressWarnings("unchecked")
    private Object convertWithConverter(String value, Class<? extends IFieldConverter<?>> converterClass) throws Exception {
        IFieldConverter<?> converter = converterCache.computeIfAbsent(converterClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create converter: " + clazz.getName(), e);
            }
        });
        
        if (value == null || value.isEmpty()) {
            return converter.defaultValue();
        }
        
        return converter.convert(value);
    }
    
    /**
     * 类型转换
     */
    private Object convertValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, value);
        }
        
        // 默认返回字符串
        return value;
    }
    
    @Override
    public String[] supportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}

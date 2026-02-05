package com.muyi.common.util.codec;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XML 工具类
 * 基于 Dom4j 实现，用于游戏配置文件读取
 */
public final class XmlUtils {
    
    private static final Logger log = LoggerFactory.getLogger(XmlUtils.class);
    
    private XmlUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== 读取 XML ====================
    
    /**
     * 从文件路径读取 XML
     */
    public static Document readFromFile(String filePath) {
        return readFromFile(Path.of(filePath));
    }
    
    /**
     * 从 Path 读取 XML
     */
    public static Document readFromFile(Path path) {
        try {
            SAXReader reader = new SAXReader();
            return reader.read(path.toFile());
        } catch (DocumentException e) {
            log.error("Failed to read XML file: {}", path, e);
            return null;
        }
    }
    
    /**
     * 从 InputStream 读取 XML
     */
    public static Document readFromStream(InputStream inputStream) {
        try {
            SAXReader reader = new SAXReader();
            return reader.read(inputStream);
        } catch (DocumentException e) {
            log.error("Failed to read XML from stream", e);
            return null;
        }
    }
    
    /**
     * 从类路径资源读取 XML
     */
    public static Document readFromResource(String resourcePath) {
        try (InputStream is = XmlUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("Resource not found: {}", resourcePath);
                return null;
            }
            return readFromStream(is);
        } catch (IOException e) {
            log.error("Failed to read XML resource: {}", resourcePath, e);
            return null;
        }
    }
    
    /**
     * 从字符串解析 XML
     */
    public static Document readFromString(String xml) {
        try {
            return DocumentHelper.parseText(xml);
        } catch (DocumentException e) {
            log.error("Failed to parse XML string", e);
            return null;
        }
    }
    
    // ==================== 写入 XML ====================
    
    /**
     * 将 Document 写入文件
     */
    public static boolean writeToFile(Document document, String filePath) {
        return writeToFile(document, Path.of(filePath));
    }
    
    /**
     * 将 Document 写入 Path
     */
    public static boolean writeToFile(Document document, Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");
            try (OutputStream os = Files.newOutputStream(path)) {
                XMLWriter writer = new XMLWriter(os, format);
                writer.write(document);
                writer.close();
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to write XML file: {}", path, e);
            return false;
        }
    }
    
    /**
     * Document 转字符串
     */
    public static String toString(Document document) {
        return toString(document, true);
    }
    
    /**
     * Document 转字符串
     * @param pretty 是否格式化输出
     */
    public static String toString(Document document, boolean pretty) {
        if (document == null) {
            return null;
        }
        try {
            OutputFormat format = pretty ? OutputFormat.createPrettyPrint() : OutputFormat.createCompactFormat();
            format.setEncoding("UTF-8");
            StringWriter sw = new StringWriter();
            XMLWriter writer = new XMLWriter(sw, format);
            writer.write(document);
            return sw.toString();
        } catch (IOException e) {
            log.error("Failed to convert document to string", e);
            return null;
        }
    }
    
    // ==================== 创建 XML ====================
    
    /**
     * 创建空 Document
     */
    public static Document createDocument() {
        return DocumentHelper.createDocument();
    }
    
    /**
     * 创建带根元素的 Document
     */
    public static Document createDocument(String rootName) {
        Document doc = DocumentHelper.createDocument();
        doc.addElement(rootName);
        return doc;
    }
    
    /**
     * 创建元素
     */
    public static Element createElement(String name) {
        return DocumentHelper.createElement(name);
    }
    
    // ==================== 元素操作 ====================
    
    /**
     * 获取根元素
     */
    public static Element getRoot(Document document) {
        return document != null ? document.getRootElement() : null;
    }
    
    /**
     * 获取子元素
     */
    public static Element getChild(Element parent, String name) {
        return parent != null ? parent.element(name) : null;
    }
    
    /**
     * 获取所有子元素
     */
    @SuppressWarnings("unchecked")
    public static List<Element> getChildren(Element parent) {
        return parent != null ? parent.elements() : new ArrayList<>();
    }
    
    /**
     * 获取指定名称的所有子元素
     */
    @SuppressWarnings("unchecked")
    public static List<Element> getChildren(Element parent, String name) {
        return parent != null ? parent.elements(name) : new ArrayList<>();
    }
    
    /**
     * 通过 XPath 查找单个元素
     */
    public static Element selectSingleNode(Document document, String xpath) {
        Node node = document.selectSingleNode(xpath);
        return node instanceof Element ? (Element) node : null;
    }
    
    /**
     * 通过 XPath 查找多个元素
     */
    @SuppressWarnings("unchecked")
    public static List<Element> selectNodes(Document document, String xpath) {
        List<Node> nodes = document.selectNodes(xpath);
        List<Element> elements = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof Element) {
                elements.add((Element) node);
            }
        }
        return elements;
    }
    
    // ==================== 属性与文本 ====================
    
    /**
     * 获取属性值
     */
    public static String getAttr(Element element, String name) {
        return getAttr(element, name, null);
    }
    
    /**
     * 获取属性值（带默认值）
     */
    public static String getAttr(Element element, String name, String defaultValue) {
        if (element == null) {
            return defaultValue;
        }
        String value = element.attributeValue(name);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取整数属性
     */
    public static int getAttrInt(Element element, String name) {
        return getAttrInt(element, name, 0);
    }
    
    /**
     * 获取整数属性（带默认值）
     */
    public static int getAttrInt(Element element, String name, int defaultValue) {
        String value = getAttr(element, name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取长整数属性
     */
    public static long getAttrLong(Element element, String name) {
        return getAttrLong(element, name, 0L);
    }
    
    /**
     * 获取长整数属性（带默认值）
     */
    public static long getAttrLong(Element element, String name, long defaultValue) {
        String value = getAttr(element, name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取浮点数属性
     */
    public static double getAttrDouble(Element element, String name) {
        return getAttrDouble(element, name, 0.0);
    }
    
    /**
     * 获取浮点数属性（带默认值）
     */
    public static double getAttrDouble(Element element, String name, double defaultValue) {
        String value = getAttr(element, name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔属性
     */
    public static boolean getAttrBoolean(Element element, String name) {
        return getAttrBoolean(element, name, false);
    }
    
    /**
     * 获取布尔属性（带默认值）
     */
    public static boolean getAttrBoolean(Element element, String name, boolean defaultValue) {
        String value = getAttr(element, name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }
    
    /**
     * 获取元素文本内容
     */
    public static String getText(Element element) {
        return getText(element, null);
    }
    
    /**
     * 获取元素文本内容（带默认值）
     */
    public static String getText(Element element, String defaultValue) {
        if (element == null) {
            return defaultValue;
        }
        String text = element.getTextTrim();
        return text != null && !text.isEmpty() ? text : defaultValue;
    }
    
    /**
     * 获取子元素文本
     */
    public static String getChildText(Element parent, String childName) {
        return getChildText(parent, childName, null);
    }
    
    /**
     * 获取子元素文本（带默认值）
     */
    public static String getChildText(Element parent, String childName, String defaultValue) {
        Element child = getChild(parent, childName);
        return getText(child, defaultValue);
    }
    
    // ==================== 游戏配置常用 ====================
    
    /**
     * 将元素的所有属性转为 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> attributesToMap(Element element) {
        Map<String, String> map = new LinkedHashMap<>();
        if (element != null) {
            for (Attribute attr : (List<Attribute>) element.attributes()) {
                map.put(attr.getName(), attr.getValue());
            }
        }
        return map;
    }
    
    /**
     * 解析整数数组属性（如 "1,2,3,4"）
     */
    public static int[] parseIntArray(Element element, String name) {
        return parseIntArray(element, name, ",");
    }
    
    /**
     * 解析整数数组属性
     */
    public static int[] parseIntArray(Element element, String name, String separator) {
        String value = getAttr(element, name);
        if (value == null || value.isEmpty()) {
            return new int[0];
        }
        String[] parts = value.split(separator);
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse int: '{}' in attribute '{}'", parts[i], name);
                result[i] = 0;
            }
        }
        return result;
    }
    
    /**
     * 解析字符串数组属性
     */
    public static String[] parseStringArray(Element element, String name) {
        return parseStringArray(element, name, ",");
    }
    
    /**
     * 解析字符串数组属性
     */
    public static String[] parseStringArray(Element element, String name, String separator) {
        String value = getAttr(element, name);
        if (value == null || value.isEmpty()) {
            return new String[0];
        }
        String[] parts = value.split(separator);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
    
    // ==================== List<Map> 批量读取 ====================
    
    /**
     * 读取 XML 文件并将所有子元素转为 List<Map>
     * 
     * 示例 XML:
     * <items>
     *   <item id="1" name="剑" price="100"/>
     *   <item id="2" name="盾" price="200"/>
     * </items>
     * 
     * 返回: [{id=1, name=剑, price=100}, {id=2, name=盾, price=200}]
     */
    public static List<Map<String, String>> readToList(String filePath) {
        Document doc = readFromFile(filePath);
        if (doc == null) {
            return new ArrayList<>();
        }
        return elementsToList(getChildren(getRoot(doc)));
    }
    
    /**
     * 读取 XML 文件并将指定路径的子元素转为 List<Map>
     * @param filePath 文件路径
     * @param childName 子元素名称
     */
    public static List<Map<String, String>> readToList(String filePath, String childName) {
        Document doc = readFromFile(filePath);
        if (doc == null) {
            return new ArrayList<>();
        }
        return elementsToList(getChildren(getRoot(doc), childName));
    }
    
    /**
     * 从类路径资源读取并转为 List<Map>
     */
    public static List<Map<String, String>> readResourceToList(String resourcePath) {
        Document doc = readFromResource(resourcePath);
        if (doc == null) {
            return new ArrayList<>();
        }
        return elementsToList(getChildren(getRoot(doc)));
    }
    
    /**
     * 从类路径资源读取并转为 List<Map>
     * @param resourcePath 资源路径
     * @param childName 子元素名称
     */
    public static List<Map<String, String>> readResourceToList(String resourcePath, String childName) {
        Document doc = readFromResource(resourcePath);
        if (doc == null) {
            return new ArrayList<>();
        }
        return elementsToList(getChildren(getRoot(doc), childName));
    }
    
    /**
     * 将元素列表转为 List<Map>
     */
    public static List<Map<String, String>> elementsToList(List<Element> elements) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Element element : elements) {
            result.add(attributesToMap(element));
        }
        return result;
    }
    
    /**
     * 读取 XML 并转为 Map（以指定属性为 key）
     * 
     * 示例 XML:
     * <items>
     *   <item id="1" name="剑" price="100"/>
     *   <item id="2" name="盾" price="200"/>
     * </items>
     * 
     * readToMap(filePath, "id") 返回:
     * {1={id=1, name=剑, price=100}, 2={id=2, name=盾, price=200}}
     */
    public static Map<String, Map<String, String>> readToMap(String filePath, String keyAttr) {
        Document doc = readFromFile(filePath);
        if (doc == null) {
            return new LinkedHashMap<>();
        }
        return elementsToMap(getChildren(getRoot(doc)), keyAttr);
    }
    
    /**
     * 读取 XML 并转为 Map（以指定属性为 key）
     * @param filePath 文件路径
     * @param childName 子元素名称
     * @param keyAttr 作为 key 的属性名
     */
    public static Map<String, Map<String, String>> readToMap(String filePath, String childName, String keyAttr) {
        Document doc = readFromFile(filePath);
        if (doc == null) {
            return new LinkedHashMap<>();
        }
        return elementsToMap(getChildren(getRoot(doc), childName), keyAttr);
    }
    
    /**
     * 从类路径资源读取并转为 Map
     */
    public static Map<String, Map<String, String>> readResourceToMap(String resourcePath, String keyAttr) {
        Document doc = readFromResource(resourcePath);
        if (doc == null) {
            return new LinkedHashMap<>();
        }
        return elementsToMap(getChildren(getRoot(doc)), keyAttr);
    }
    
    /**
     * 将元素列表转为 Map（以指定属性为 key）
     */
    public static Map<String, Map<String, String>> elementsToMap(List<Element> elements, String keyAttr) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Element element : elements) {
            String key = getAttr(element, keyAttr);
            if (key != null) {
                result.put(key, attributesToMap(element));
            }
        }
        return result;
    }
    
    /**
     * 读取 XML 并转为 Map（以指定属性为 int key）
     */
    public static Map<Integer, Map<String, String>> readToIntKeyMap(String filePath, String keyAttr) {
        Document doc = readFromFile(filePath);
        if (doc == null) {
            return new LinkedHashMap<>();
        }
        return elementsToIntKeyMap(getChildren(getRoot(doc)), keyAttr);
    }
    
    /**
     * 读取 XML 并转为 Map（以指定属性为 int key）
     */
    public static Map<Integer, Map<String, String>> readToIntKeyMap(String filePath, String childName, String keyAttr) {
        Document doc = readFromFile(filePath);
        if (doc == null) {
            return new LinkedHashMap<>();
        }
        return elementsToIntKeyMap(getChildren(getRoot(doc), childName), keyAttr);
    }
    
    /**
     * 将元素列表转为 Map（以指定属性为 int key）
     */
    public static Map<Integer, Map<String, String>> elementsToIntKeyMap(List<Element> elements, String keyAttr) {
        Map<Integer, Map<String, String>> result = new LinkedHashMap<>();
        for (Element element : elements) {
            int key = getAttrInt(element, keyAttr, -1);
            if (key >= 0) {
                result.put(key, attributesToMap(element));
            }
        }
        return result;
    }
}

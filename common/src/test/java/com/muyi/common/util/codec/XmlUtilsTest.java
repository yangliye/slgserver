package com.muyi.common.util.codec;

import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XmlUtils 测试类
 */
class XmlUtilsTest {

    @TempDir
    Path tempDir;
    
    private File testXmlFile;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试 XML 文件
        testXmlFile = tempDir.resolve("test.xml").toFile();
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <config>
                <server id="1" name="Server1" port="8080"/>
                <server id="2" name="Server2" port="8081"/>
                <items>
                    <item id="100" type="weapon" damage="10,20,30"/>
                    <item id="101" type="armor" defense="5"/>
                </items>
                <settings>
                    <debug>true</debug>
                    <maxPlayers>1000</maxPlayers>
                </settings>
            </config>
            """;
        java.nio.file.Files.writeString(testXmlFile.toPath(), xml);
    }

    @Test
    void testReadFromFile() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        assertNotNull(doc);
        assertNotNull(doc.getRootElement());
        assertEquals("config", doc.getRootElement().getName());
    }

    @Test
    void testReadFromFileNonExistent() {
        Document doc = XmlUtils.readFromFile("nonexistent.xml");
        assertNull(doc);
    }

    @Test
    void testGetRoot() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        assertNotNull(root);
        assertEquals("config", root.getName());
    }

    @Test
    void testGetChildren() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        List<Element> servers = XmlUtils.getChildren(root, "server");
        
        assertEquals(2, servers.size());
        assertEquals("1", servers.get(0).attributeValue("id"));
        assertEquals("2", servers.get(1).attributeValue("id"));
    }

    @Test
    void testGetChild() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element settings = XmlUtils.getChild(root, "settings");
        
        assertNotNull(settings);
        assertEquals("settings", settings.getName());
    }

    @Test
    void testGetAttr() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element server = XmlUtils.getChild(root, "server");
        
        assertEquals("Server1", XmlUtils.getAttr(server, "name"));
        assertNull(XmlUtils.getAttr(server, "nonexistent"));
        assertEquals("default", XmlUtils.getAttr(server, "nonexistent", "default"));
    }

    @Test
    void testGetAttrInt() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element server = XmlUtils.getChild(root, "server");
        
        assertEquals(1, XmlUtils.getAttrInt(server, "id"));
        assertEquals(8080, XmlUtils.getAttrInt(server, "port"));
        assertEquals(0, XmlUtils.getAttrInt(server, "nonexistent"));
        assertEquals(-1, XmlUtils.getAttrInt(server, "nonexistent", -1));
    }

    @Test
    void testGetAttrLong() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element server = XmlUtils.getChild(root, "server");
        
        assertEquals(8080L, XmlUtils.getAttrLong(server, "port"));
        assertEquals(-1L, XmlUtils.getAttrLong(server, "nonexistent", -1L));
    }

    @Test
    void testGetAttrDouble() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element server = XmlUtils.getChild(root, "server");
        
        assertEquals(8080.0, XmlUtils.getAttrDouble(server, "port"));
        assertEquals(-1.0, XmlUtils.getAttrDouble(server, "nonexistent", -1.0));
    }

    @Test
    void testGetAttrBoolean() {
        // 测试布尔属性
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        
        assertFalse(XmlUtils.getAttrBoolean(root, "nonexistent"));
        assertTrue(XmlUtils.getAttrBoolean(root, "nonexistent", true));
    }

    @Test
    void testGetText() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element settings = XmlUtils.getChild(root, "settings");
        Element debug = XmlUtils.getChild(settings, "debug");
        
        assertEquals("true", XmlUtils.getText(debug));
    }

    @Test
    void testGetChildText() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element settings = XmlUtils.getChild(root, "settings");
        
        assertEquals("true", XmlUtils.getChildText(settings, "debug"));
        assertEquals("1000", XmlUtils.getChildText(settings, "maxPlayers"));
        assertNull(XmlUtils.getChildText(settings, "nonexistent"));
    }

    @Test
    void testParseIntArray() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element items = XmlUtils.getChild(root, "items");
        Element weapon = XmlUtils.getChildren(items, "item").get(0);
        
        int[] damage = XmlUtils.parseIntArray(weapon, "damage", ",");
        assertArrayEquals(new int[]{10, 20, 30}, damage);
        
        // 不存在的属性返回空数组
        int[] empty = XmlUtils.parseIntArray(weapon, "nonexistent", ",");
        assertEquals(0, empty.length);
    }

    @Test
    void testParseStringArray() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element items = XmlUtils.getChild(root, "items");
        Element weapon = XmlUtils.getChildren(items, "item").get(0);
        
        String[] damage = XmlUtils.parseStringArray(weapon, "damage", ",");
        assertArrayEquals(new String[]{"10", "20", "30"}, damage);
    }

    @Test
    void testSelectSingleNode() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element weapon = XmlUtils.selectSingleNode(doc, "//item[@type='weapon']");
        
        assertNotNull(weapon);
        assertEquals("100", weapon.attributeValue("id"));
    }

    @Test
    void testSelectNodes() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        List<Element> items = XmlUtils.selectNodes(doc, "//item");
        
        assertEquals(2, items.size());
    }

    @Test
    void testAttributesToMap() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        Element server = XmlUtils.getChild(root, "server");
        
        Map<String, String> map = XmlUtils.attributesToMap(server);
        
        assertEquals("1", map.get("id"));
        assertEquals("Server1", map.get("name"));
        assertEquals("8080", map.get("port"));
    }

    @Test
    void testElementsToList() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        List<Element> servers = XmlUtils.getChildren(root, "server");
        
        List<Map<String, String>> mapList = XmlUtils.elementsToList(servers);
        
        assertEquals(2, mapList.size());
        assertEquals("1", mapList.get(0).get("id"));
        assertEquals("Server1", mapList.get(0).get("name"));
        assertEquals("2", mapList.get(1).get("id"));
    }

    @Test
    void testElementsToMap() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        List<Element> servers = XmlUtils.getChildren(root, "server");
        
        Map<String, Map<String, String>> map = XmlUtils.elementsToMap(servers, "id");
        
        assertEquals(2, map.size());
        assertTrue(map.containsKey("1"));
        assertTrue(map.containsKey("2"));
        assertEquals("Server1", map.get("1").get("name"));
        assertEquals("8081", map.get("2").get("port"));
    }

    @Test
    void testElementsToIntKeyMap() {
        Document doc = XmlUtils.readFromFile(testXmlFile.getAbsolutePath());
        Element root = XmlUtils.getRoot(doc);
        List<Element> servers = XmlUtils.getChildren(root, "server");
        
        Map<Integer, Map<String, String>> map = XmlUtils.elementsToIntKeyMap(servers, "id");
        
        assertEquals(2, map.size());
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        assertEquals("Server1", map.get(1).get("name"));
    }

    @Test
    void testReadToList() {
        List<Map<String, String>> list = XmlUtils.readToList(testXmlFile.getAbsolutePath(), "server");
        
        assertEquals(2, list.size());
        assertEquals("1", list.get(0).get("id"));
    }

    @Test
    void testReadToMap() {
        Map<String, Map<String, String>> map = XmlUtils.readToMap(testXmlFile.getAbsolutePath(), "server", "id");
        
        assertEquals(2, map.size());
        assertEquals("Server1", map.get("1").get("name"));
    }

    @Test
    void testReadToIntKeyMap() {
        Map<Integer, Map<String, String>> map = XmlUtils.readToIntKeyMap(testXmlFile.getAbsolutePath(), "server", "id");
        
        assertEquals(2, map.size());
        assertEquals("Server1", map.get(1).get("name"));
    }

    @Test
    void testWriteDocument() throws Exception {
        // 创建新文档
        Document doc = XmlUtils.createDocument("root");
        Element root = doc.getRootElement();
        Element child = root.addElement("child");
        child.addAttribute("id", "1");
        child.setText("value");
        
        File outputFile = tempDir.resolve("output.xml").toFile();
        boolean success = XmlUtils.writeToFile(doc, outputFile.getAbsolutePath());
        
        assertTrue(success);
        assertTrue(outputFile.exists());
        
        // 验证写入内容
        Document readBack = XmlUtils.readFromFile(outputFile.getAbsolutePath());
        assertNotNull(readBack);
        assertEquals("root", readBack.getRootElement().getName());
    }

    @Test
    void testCreateDocument() {
        Document doc = XmlUtils.createDocument("root");
        assertNotNull(doc);
        assertEquals("root", doc.getRootElement().getName());
    }

    @Test
    void testCreateElement() {
        Element element = XmlUtils.createElement("test");
        assertNotNull(element);
        assertEquals("test", element.getName());
    }

    @Test
    void testReadFromString() {
        String xml = "<root><child id=\"1\">value</child></root>";
        Document doc = XmlUtils.readFromString(xml);
        
        assertNotNull(doc);
        assertEquals("root", doc.getRootElement().getName());
        Element child = XmlUtils.getChild(doc.getRootElement(), "child");
        assertEquals("1", XmlUtils.getAttr(child, "id"));
        assertEquals("value", XmlUtils.getText(child));
    }

    @Test
    void testToString() {
        Document doc = XmlUtils.createDocument("root");
        doc.getRootElement().addElement("child").setText("value");
        
        String xml = XmlUtils.toString(doc);
        assertNotNull(xml);
        assertTrue(xml.contains("root"));
        assertTrue(xml.contains("child"));
        assertTrue(xml.contains("value"));
    }

    @Test
    void testNullElementHandling() {
        assertNull(XmlUtils.getChild(null, "test"));
        assertTrue(XmlUtils.getChildren(null, "test").isEmpty());
        assertNull(XmlUtils.getAttr(null, "test"));
        assertEquals(0, XmlUtils.getAttrInt(null, "test"));
        assertNull(XmlUtils.getText(null));
    }
}

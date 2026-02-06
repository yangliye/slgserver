package com.muyi.shared.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端参数
 * 
 * 用于登录/注册等接口，封装客户端传递的所有参数
 * 可扩展，各项目可通过 extra 字段传递自定义数据
 *
 * @author muyi
 */
public class ClientParams implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ==================== 基础字段 ====================
    
    /** 账号 */
    private String account;
    
    /** 密码（建议传输前加密） */
    private String password;
    
    // ==================== 设备信息 ====================
    
    /** 设备ID（唯一标识设备） */
    private String deviceId;
    
    /** 设备型号（如 iPhone 15, Pixel 8） */
    private String deviceModel;
    
    /** 操作系统版本（如 iOS 17.0, Android 14） */
    private String osVersion;
    
    // ==================== 客户端信息 ====================
    
    /** 平台类型：ios, android, pc, web */
    private String platform;
    
    /** 客户端版本号（如 1.0.0） */
    private String version;
    
    /** 渠道标识（如 appstore, googleplay, taptap） */
    private String channel;
    
    /** 语言（如 zh_CN, en_US） */
    private String language;
    
    // ==================== 网络信息 ====================
    
    /** 客户端IP（可由服务端填充） */
    private String clientIp;
    
    // ==================== 扩展字段 ====================
    
    /** 扩展参数（用于传递自定义数据） */
    private Map<String, Object> extra;
    
    public ClientParams() {
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 获取扩展参数
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key) {
        if (extra == null) {
            return null;
        }
        return (T) extra.get(key);
    }
    
    /**
     * 获取扩展参数（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key, T defaultValue) {
        if (extra == null) {
            return defaultValue;
        }
        Object value = extra.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 设置扩展参数
     */
    public ClientParams putExtra(String key, Object value) {
        if (extra == null) {
            extra = new HashMap<>();
        }
        extra.put(key, value);
        return this;
    }
    
    // ==================== Getter/Setter ====================
    
    public String getAccount() {
        return account;
    }
    
    public void setAccount(String account) {
        this.account = account;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceModel() {
        return deviceModel;
    }
    
    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public Map<String, Object> getExtra() {
        return extra;
    }
    
    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
    
    @Override
    public String toString() {
        return "ClientParams{" +
                "account='" + account + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", platform='" + platform + '\'' +
                ", version='" + version + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }
}

package com.hocc.fun.store;

// AppItem.java
public class AppItem {
    private final String iconUrl;
    private final String appName;
    private final String version;
    private final String provider;
    private final String buttonText;

    public AppItem(String iconUrl, String appName, String version, String provider, String buttonText) {
        this.iconUrl = iconUrl;
        this.appName = appName;
        this.version = version;
        this.provider = provider;
        this.buttonText = buttonText;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getAppName() {
        return appName;
    }

    public String getVersion() {
        return version;
    }

    public String getProvider() {
        return provider;
    }

    public String getButtonText() {
        return buttonText;
    }
}
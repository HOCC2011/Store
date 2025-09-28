package com.hocc.fun.store;

// AppItem.java
public class AppItem {
    private final String iconUrl;
    private final String appName;
    private final String version;
    private final String provider;
    private final String buttonText;
    private final String DownloadUrl;

    public AppItem(String iconUrl, String appName, String version, String provider, String buttonText, String DownloadUrl) {
        this.iconUrl = iconUrl;
        this.appName = appName;
        this.version = version;
        this.provider = provider;
        this.buttonText = buttonText;
        this.DownloadUrl = DownloadUrl;
    }

    public String getDownloadUrl() {
        return DownloadUrl;
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
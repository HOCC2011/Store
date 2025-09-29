package com.hocc.fun.store;

public class RepoItem {
    private final String iconUrl;
    private final String repoName;
    private final String repoUrl;

    public RepoItem(String iconUrl, String repoName, String repoUrl) {
        this.iconUrl = iconUrl;
        this.repoName = repoName;
        this.repoUrl = repoUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

}
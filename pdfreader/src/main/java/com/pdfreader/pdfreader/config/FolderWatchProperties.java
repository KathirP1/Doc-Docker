package com.pdfreader.pdfreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "pdfreader.watch")
public class FolderWatchProperties {

    private boolean enabled;
    private List<String> tempFolders;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTempFolders() {
        return tempFolders;
    }

    public void setTempFolders(List<String> tempFolders) {
        this.tempFolders = tempFolders;
    }
}

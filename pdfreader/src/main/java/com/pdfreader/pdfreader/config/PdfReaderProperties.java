package com.pdfreader.pdfreader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "pdfreader")
public class PdfReaderProperties {

    private DocumentProperties documents;
    private WatchProperties watch;
    private IndexProperties index;

    // getters and setters
    public DocumentProperties getDocuments() {
        return documents;
    }

    public void setDocuments(DocumentProperties documents) {
        this.documents = documents;
    }

    public WatchProperties getWatch() {
        return watch;
    }

    public void setWatch(WatchProperties watch) {
        this.watch = watch;
    }

    public IndexProperties getIndex() {
        return index;
    }

    public void setIndex(IndexProperties index) {
        this.index = index;
    }

    public static class DocumentProperties {
        private List<String> baseFolders; // <--- change from String to List<String>

        public List<String> getBaseFolders() {
            return baseFolders;
        }

        public void setBaseFolders(List<String> baseFolders) {
            this.baseFolders = baseFolders;
        }
    }

    public static class WatchProperties {
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

    public static class IndexProperties {
        private List<String> tempDirs;
        private List<String> permDirs;

        public List<String> getTempDirs() {
            return tempDirs;
        }

        public void setTempDirs(List<String> tempDirs) {
            this.tempDirs = tempDirs;
        }

        public List<String> getPermDirs() {
            return permDirs;
        }

        public void setPermDirs(List<String> permDirs) {
            this.permDirs = permDirs;
        }
    }
}

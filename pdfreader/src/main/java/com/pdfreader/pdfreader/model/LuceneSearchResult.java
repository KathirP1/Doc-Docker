package com.pdfreader.pdfreader.model;

import java.util.List;

public class LuceneSearchResult {
    private String branch;
    private List<FileResult> files;

    public LuceneSearchResult(String branch, List<FileResult> files) {
        this.branch = branch;
        this.files = files;
    }

    public String getBranch() {
        return branch;
    }

    public List<FileResult> getFiles() {
        return files;
    }

    public static class FileResult {
        private String title;
        private String lastModified;
        private long sizeKB;
        private String fileType;

        public FileResult(String title, String lastModified, long sizeKB, String fileType) {
            this.title = title;
            this.lastModified = lastModified;
            this.sizeKB = sizeKB;
            this.fileType = fileType;
        }

        public String getTitle() {
            return title;
        }

        public String getLastModified() {
            return lastModified;
        }

        public long getSizeKB() {
            return sizeKB;
        }

        public String getFileType() {
            return fileType;
        }
    }
}

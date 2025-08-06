package com.pdfreader.pdfreader.helper;

import com.pdfreader.pdfreader.config.PdfReaderProperties;
import com.pdfreader.pdfreader.service.LuceneSearchServiceImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

@Component
public class TempFolderWatcher {

    @Autowired
    private PdfReaderProperties props;

    @Autowired
    private LuceneSearchServiceImpl luceneSearchService;

    @PostConstruct
    public void startWatching() {
        if (!props.getWatch().isEnabled()) {
            System.out.println("📁 Folder watching is disabled via config.");
            return;
        }

        var tempFolders = props.getWatch().getTempFolders();
        var tempDirs = props.getIndex().getTempDirs();

        if (tempFolders.size() != tempDirs.size()) {
            throw new IllegalStateException("❌ Mismatch between number of tempFolders and tempDirs in configuration.");
        }

        for (int i = 0; i < tempFolders.size(); i++) {
            String folderPath = tempFolders.get(i);
            String indexDir = tempDirs.get(i);

            Thread thread = new Thread(() -> watchFolder(folderPath, indexDir));
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void watchFolder(String folderPath, String indexDir) {
        try {
            Path path = Paths.get(folderPath);
            if (!Files.exists(path)) {
                System.err.println("⚠️ Watch path does not exist: " + folderPath);
                return;
            }

            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);

            System.out.println("📂 Watching folder: " + folderPath + " → Indexing to: " + indexDir);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                        System.out.println("📄 Change in " + folderPath + ": " + event.context());

                        // Index to the correct index directory
                        luceneSearchService.indexFiles(folderPath, indexDir);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    System.err.println("❌ Watch key no longer valid for: " + folderPath);
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

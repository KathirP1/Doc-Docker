package com.pdfreader.pdfreader.controller;

import com.pdfreader.pdfreader.model.LuceneSearchResult;
import com.pdfreader.pdfreader.service.LuceneSearchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lucene")
public class LuceneSearchController {

    @Autowired
    private LuceneSearchServiceImpl luceneService;

    // ✅ Index files into temporary or permanent index
    @PostMapping("/index")
    public String indexFiles(@RequestParam String path, @RequestParam String indexPath) {
        try {
            luceneService.indexFiles(path, indexPath);
            return "Indexing complete for path: " + path + " → indexPath: " + indexPath;
        } catch (Exception e) {
            return "Indexing failed: " + e.getMessage();
        }
    }


    // ✅ Merge temporary index into permanent index
    @PostMapping("/merge")
    public String mergeTempIntoPermanent() {
        try {
            luceneService.mergeTemporaryToPermanent();
            return "Temporary index merged into permanent index successfully.";
        } catch (Exception e) {
            return "Merge failed: " + e.getMessage();
        }
    }

    // ✅ Search both indexes
    @GetMapping("/search")
    public List<LuceneSearchResult> search(@RequestParam String query) {
        try {
            return luceneService.searchBothIndexes(query);
        } catch (Exception e) {
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }
}

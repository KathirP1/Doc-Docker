package com.pdfreader.pdfreader.service;

import com.pdfreader.pdfreader.model.LuceneSearchResult;
import java.util.List;

public interface LuceneSearchService {
    List<LuceneSearchResult> searchBothIndexes(String queryStr) throws Exception;
//    void indexFiles(String folderPath, boolean isTemporary) throws Exception;
//    void mergeTemporaryToPermanent() throws Exception;
}

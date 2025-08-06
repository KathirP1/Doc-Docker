package com.pdfreader.pdfreader.helper;

import com.pdfreader.pdfreader.service.LuceneSearchServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MergeScheduler {

    private final LuceneSearchServiceImpl luceneSearchService;

    public MergeScheduler(LuceneSearchServiceImpl luceneSearchService) {
        this.luceneSearchService = luceneSearchService;
    }

    /**
     * Merge temp index to permanent at 10:00 PM every day
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void runMergeTask() {
        try {
            System.out.println("Running scheduled merge at 10 PM...");
            luceneSearchService.mergeTemporaryToPermanent();
        } catch (Exception e) {
            System.err.println("Scheduled merge failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

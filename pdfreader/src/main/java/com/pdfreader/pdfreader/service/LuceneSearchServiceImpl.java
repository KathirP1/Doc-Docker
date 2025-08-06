package com.pdfreader.pdfreader.service;

import com.pdfreader.pdfreader.config.PdfReaderProperties;
import com.pdfreader.pdfreader.model.LuceneSearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

@Service
public class LuceneSearchServiceImpl implements LuceneSearchService {

    private final PdfReaderProperties props;

    public LuceneSearchServiceImpl(PdfReaderProperties props) {
        this.props = props;
    }

    private static final String TEMP_INDEX_DIR = "E:/PDF READER Search (concurrency)/PDF READER Search/PDF READER/PKNS_index_temp";
    private static final String PERM_INDEX_DIR = "E:/PDF READER Search (concurrency)/PDF READER Search/PDF READER/ANOTHER_index_temp";

    @Override
    public List<LuceneSearchResult> searchBothIndexes(String queryStr) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<InternalResult>> futures = new ArrayList<>();

        List<String> indexNames = new ArrayList<>();
        indexNames.addAll(props.getIndex().getTempDirs());
        indexNames.addAll(props.getIndex().getPermDirs());

        for (String indexName : indexNames) {
            Directory dir = FSDirectory.open(Paths.get(indexName));
            if (DirectoryReader.indexExists(dir)) {
                futures.add(executor.submit(() -> {
                    IndexSearcher[] ref = new IndexSearcher[1];
                    TopDocs td = searchSingleIndex(queryStr, indexName, ref);
                    return new InternalResult(indexName, td, ref[0]);
                }));
            } else {
                System.out.println("Index not found for " + indexName + ". Skipping.");
            }
        }

        List<LuceneSearchResult> finalResults = new ArrayList<>();
        for (Future<InternalResult> future : futures) {
            InternalResult result = future.get();
            if (isValidResult(result)) {
                finalResults.add(convert(result));
            }
            closeSearcher(result);
        }

        executor.shutdown();
        return finalResults;
    }

    private boolean isValidResult(InternalResult result) {
        return result != null && result.topDocs != null && result.topDocs.scoreDocs.length > 0 && result.searcher != null;
    }

    private LuceneSearchResult convert(InternalResult result) throws Exception {
        List<LuceneSearchResult.FileResult> files = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (ScoreDoc sd : result.topDocs.scoreDocs) {
            Document d = result.searcher.storedFields().document(sd.doc);
            String filename = d.get("filename");
            long lastModified = Long.parseLong(d.get("lastModified"));
            long size = Long.parseLong(d.get("size"));
            String fileType = filename != null && filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                    : "unknown";

            files.add(new LuceneSearchResult.FileResult(
                    filename, sdf.format(new Date(lastModified)), size / 1024, fileType
            ));
        }

        return new LuceneSearchResult(result.name, files);
    }

    private void closeSearcher(InternalResult result) throws Exception {
        if (result != null && result.searcher != null) {
            result.searcher.getIndexReader().close();
        }
    }

    private TopDocs searchSingleIndex(String query, String indexName, IndexSearcher[] ref) throws Exception {
        Directory directory = FSDirectory.open(Paths.get(indexName));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("content", analyzer);
        Query luceneQuery = parser.parse(query);

        TopDocs topDocs = searcher.search(luceneQuery, 50);
        ref[0] = searcher;
        return topDocs;
    }

    private static class InternalResult {
        String name;
        TopDocs topDocs;
        IndexSearcher searcher;

        InternalResult(String n, TopDocs t, IndexSearcher s) {
            this.name = n;
            this.topDocs = t;
            this.searcher = s;
        }
    }

    private int pdfCount, docCount, docxCount;

    public void indexFiles(String folderPath, String indexPath) throws Exception {
        pdfCount = docCount = docxCount = 0;

        ensureDirectory(indexPath);
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());

        try (IndexWriter writer = new IndexWriter(dir, config)) {
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                throw new IllegalArgumentException("Invalid folder path: " + folderPath);
            }

            indexDirectory(writer, folder);
            writer.commit();
        }

        System.out.printf("✅ Indexing complete: PDF=%d, DOC=%d, DOCX=%d → %s%n",
                pdfCount, docCount, docxCount, indexPath);
    }

    private void indexDirectory(IndexWriter writer, File folder) throws Exception {
        File[] files = folder.listFiles();
        if (files == null) return;

        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            for (File file : files) {
                if (file.isDirectory()) {
                    indexDirectory(writer, file);
                    continue;
                }

                String name = file.getName().toLowerCase();
                String content = null;

                try {
                    long fileLastModified = file.lastModified();

                    Query query = new TermQuery(new Term("filename", file.getAbsolutePath()));
                    TopDocs topDocs = searcher.search(query, 1);

                    if (topDocs.scoreDocs.length > 0) {
                        int docId = topDocs.scoreDocs[0].doc;
                        Document indexedDoc = searcher.storedFields().document(docId);
                        long indexedLastModified = Long.parseLong(indexedDoc.get("lastModified"));

                        if (fileLastModified == indexedLastModified) {
                            continue;
                        }
                    }

                    if (name.endsWith(".pdf")) {
                        content = extractTextFromPdf(file);
                        pdfCount++;
                    } else if (name.endsWith(".doc")) {
                        content = extractTextFromDoc(file);
                        docCount++;
                    } else if (name.endsWith(".docx")) {
                        content = extractTextFromDocx(file);
                        docxCount++;
                    } else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
                        content = extractTextFromExcel(file);
                    } else if (name.endsWith(".ppt") || name.endsWith(".pptx")) {
                        content = extractTextFromPowerPoint(file);
                    } else if (name.endsWith(".csv") || name.endsWith(".txt")) {
                        content = extractTextFromTextFile(file);
                    }

                    if (content != null && !content.isBlank()) {
                        addDoc(writer, file, content);
                    }

                } catch (Exception e) {
                    System.err.println("Failed to index " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }

    private void addDoc(IndexWriter writer, File file, String content) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("filename", file.getAbsolutePath(), Field.Store.YES));
        doc.add(new TextField("content", content, Field.Store.NO));
        doc.add(new LongPoint("lastModified", file.lastModified()));
        doc.add(new StoredField("lastModified", file.lastModified()));
        doc.add(new LongPoint("size", file.length()));
        doc.add(new StoredField("size", file.length()));

        writer.updateDocument(new Term("filename", file.getAbsolutePath()), doc);
    }

    private void ensureDirectory(String dir) throws Exception {
        if (!Files.exists(Paths.get(dir))) {
            Files.createDirectories(Paths.get(dir));
        }
    }

    public void mergeTemporaryToPermanent() throws Exception {
        List<String> tempDirs = props.getIndex().getTempDirs();
        List<String> permDirs = props.getIndex().getPermDirs();

        if (tempDirs.size() != permDirs.size()) {
            throw new IllegalStateException("Mismatch: tempDirs and permDirs must be of same size.");
        }

        for (int i = 0; i < tempDirs.size(); i++) {
            String tempDirPath = tempDirs.get(i);
            String permDirPath = permDirs.get(i);

            ensureDirectory(tempDirPath);
            ensureDirectory(permDirPath);

            Path tempPath = Paths.get(tempDirPath);
            Path permPath = Paths.get(permDirPath);

            try (
                    FSDirectory tempDir = FSDirectory.open(tempPath);
                    FSDirectory permDir = FSDirectory.open(permPath);
                    IndexWriter permWriter = new IndexWriter(permDir, new IndexWriterConfig(new StandardAnalyzer()))
            ) {
                System.out.printf("Merging temp index %d into perm index...%n", i);
                permWriter.addIndexes(tempDir);
                permWriter.commit();
            }

            try (
                    FSDirectory tempDir = FSDirectory.open(tempPath);
                    IndexWriter tempWriter = new IndexWriter(tempDir, new IndexWriterConfig(new StandardAnalyzer()))
            ) {
                tempWriter.deleteAll();
                tempWriter.commit();
                System.out.printf("Merge %d complete. Temp index cleared.%n", i);
            }
        }
    }

    private String extractTextFromPdf(File file) throws Exception {
        try (PDDocument document = PDDocument.load(file)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractTextFromDoc(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis)) {
            return new WordExtractor(doc).getText();
        }
    }

    private String extractTextFromDocx(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            return new XWPFWordExtractor(doc).getText();
        }
    }

    private String extractTextFromExcel(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = file.getName().endsWith(".xls") ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        switch (cell.getCellType()) {
                            case STRING -> sb.append(cell.getStringCellValue()).append(" ");
                            case NUMERIC -> sb.append(cell.getNumericCellValue()).append(" ");
                            case BOOLEAN -> sb.append(cell.getBooleanCellValue()).append(" ");
                            default -> {}
                        }
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String extractTextFromPowerPoint(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            if (file.getName().endsWith(".ppt")) {
                HSLFSlideShow ppt = new HSLFSlideShow(fis);
                for (HSLFSlide slide : ppt.getSlides()) {
                    for (HSLFShape shape : slide.getShapes()) {
                        if (shape instanceof HSLFTextShape textShape) {
                            sb.append(textShape.getText()).append(" ");
                        }
                    }
                }
            } else {
                XMLSlideShow pptx = new XMLSlideShow(fis);
                for (XSLFSlide slide : pptx.getSlides()) {
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape textShape) {
                            sb.append(textShape.getText()).append(" ");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    private String extractTextFromTextFile(File file) throws Exception {
        return Files.readString(file.toPath());
    }
}

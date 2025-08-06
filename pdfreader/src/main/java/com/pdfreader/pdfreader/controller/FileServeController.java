package com.pdfreader.pdfreader.controller;

import com.pdfreader.pdfreader.config.PdfReaderProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileServeController {

    private final List<String> baseFolders;

    public FileServeController(PdfReaderProperties props) {
        this.baseFolders = props.getDocuments().getBaseFolders();
    }

    @GetMapping
    public ResponseEntity<Resource> serveFile(@RequestParam String path) throws IOException {
        for (String baseFolder : baseFolders) {
            Path basePath = Paths.get(baseFolder);
            Path resolvedPath = basePath.resolve(path).normalize();

            // Prevent path traversal attacks
            if (!resolvedPath.startsWith(basePath)) {
                continue;
            }

            Resource fileResource = new UrlResource(resolvedPath.toUri());
            if (fileResource.exists() && fileResource.isReadable()) {
                String contentType = Files.probeContentType(resolvedPath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + fileResource.getFilename() + "\"")
                        .body(fileResource);
            }
        }

        // If not found in any folder
        return ResponseEntity.notFound().build();
    }
}

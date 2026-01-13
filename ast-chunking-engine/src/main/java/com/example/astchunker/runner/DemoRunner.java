
package com.example.astchunker.runner;

import com.example.astchunker.service.AstChunkingService;
import com.example.astchunker.model.CodeChunk;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.*;

@Component
public class DemoRunner implements CommandLineRunner {

    private final AstChunkingService service;

    public DemoRunner(AstChunkingService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        Path sample = Paths.get("Sample.java");
        if (!Files.exists(sample)) return;

        for (CodeChunk c : service.chunk(sample)) {
            System.out.println(c);
        }
    }
}

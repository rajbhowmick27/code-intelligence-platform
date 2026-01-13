
package com.example.rebuild.runner;

import com.example.rebuild.service.RebuildOrchestrator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class RebuildRunner implements CommandLineRunner {

    private final RebuildOrchestrator orchestrator;

    public RebuildRunner(RebuildOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) return;
        orchestrator.rebuild(Path.of(args[0]));
    }
}

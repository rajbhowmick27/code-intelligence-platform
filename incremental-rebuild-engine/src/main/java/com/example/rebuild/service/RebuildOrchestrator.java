
package com.example.rebuild.service;

import com.example.rebuild.git.GitDiffService;
import com.example.rebuild.swap.AliasManager;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class RebuildOrchestrator {

    private final GitDiffService diffService;
    private final AliasManager aliasManager;

    public RebuildOrchestrator(GitDiffService diffService, AliasManager aliasManager) {
        this.diffService = diffService;
        this.aliasManager = aliasManager;
    }

    public void rebuild(Path repo) throws Exception {
        java.util.List<String> changed = diffService.changedJavaFiles(repo, "develop");

        System.out.println("Changed files: " + changed);

        // Here we would:
        // 1. Re-chunk changed files
        // 2. Re-embed changed chunks
        // 3. Delete stale vectors
        // 4. Rebuild service graph

        aliasManager.swapVectorAlias("repo-v1", "repo-v2");
        aliasManager.swapGraphAlias("graph-v1", "graph-v2");
    }
}

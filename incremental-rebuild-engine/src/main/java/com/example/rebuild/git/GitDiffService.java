
package com.example.rebuild.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GitDiffService {

    public List<String> changedJavaFiles(Path repo, String baseBranch) throws Exception {
        Git git = Git.open(repo.toFile());

        ObjectId oldTree = git.getRepository().resolve(baseBranch);
        ObjectId newTree = git.getRepository().resolve("HEAD");

        DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(git.getRepository());

        return df.scan(oldTree, newTree).stream()
                .map(DiffEntry::getNewPath)
                .filter(p -> p.endsWith(".java"))
                .collect(Collectors.toList());
    }
}

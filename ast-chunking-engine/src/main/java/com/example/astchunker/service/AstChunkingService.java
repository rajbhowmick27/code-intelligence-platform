
package com.example.astchunker.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.example.astchunker.model.CodeChunk;
import com.example.astchunker.detector.LayerDetector;
import com.example.astchunker.extractor.SqlExtractor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;

@Service
public class AstChunkingService {

    public List<CodeChunk> chunk(Path javaFile) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        List<CodeChunk> chunks = new ArrayList<>();

        for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String layer = LayerDetector.detect(c);

            chunks.add(CodeChunk.builder()
                .file(javaFile.toString())
                .className(c.getNameAsString())
                .chunkType(layer + "_CLASS")
                .content(c.toString())
                .hash(DigestUtils.sha256Hex(c.toString()))
                .build());

            for (MethodDeclaration m : c.getMethods()) {
                chunks.add(CodeChunk.builder()
                    .file(javaFile.toString())
                    .className(c.getNameAsString())
                    .methodName(m.getNameAsString())
                    .chunkType(layer + "_METHOD")
                    .content(m.toString())
                    .hash(DigestUtils.sha256Hex(m.toString()))
                    .build());

                for (String sql : SqlExtractor.extract(m)) {
                    chunks.add(CodeChunk.builder()
                        .file(javaFile.toString())
                        .className(c.getNameAsString())
                        .methodName(m.getNameAsString())
                        .chunkType("SQL")
                        .content(sql)
                        .hash(DigestUtils.sha256Hex(sql))
                        .build());
                }
            }
        }
        return chunks;
    }
}


package com.example.astchunker.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChunk {
    private String file;
    private String className;
    private String methodName;
    private String chunkType;
    private String content;
    private String hash;
}

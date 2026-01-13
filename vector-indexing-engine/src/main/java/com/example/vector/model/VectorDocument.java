
package com.example.vector.model;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument {
    private String id;
    private String content;
    private Map<String, Object> metadata;
}

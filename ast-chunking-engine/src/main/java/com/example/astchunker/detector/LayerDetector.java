
package com.example.astchunker.detector;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class LayerDetector {
    public static String detect(ClassOrInterfaceDeclaration c) {
        if (c.isAnnotationPresent("RestController")) return "CONTROLLER";
        if (c.isAnnotationPresent("Service")) return "SERVICE";
        if (c.isAnnotationPresent("Repository")) return "REPOSITORY";
        if (c.isAnnotationPresent("Configuration")) return "CONFIG";
        if (c.isAnnotationPresent("Entity")) return "ENTITY";
        return "OTHER";
    }
}

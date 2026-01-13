
package com.example.astchunker.extractor;

import java.util.*;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;

public class SqlExtractor {
    public static List<String> extract(MethodDeclaration method) {
        List<String> sql = new ArrayList<>();

        method.findAll(StringLiteralExpr.class).forEach(lit -> {
            String v = lit.getValue().toLowerCase();
            if (v.contains("select ") || v.contains("insert ")
                || v.contains("update ") || v.contains("delete ")) {
                sql.add(lit.getValue());
            }
        });

        method.getAnnotations().forEach(a -> {
            if (a.getNameAsString().equals("Query")) {
                sql.add(a.toString());
            }
        });

        return sql;
    }
}

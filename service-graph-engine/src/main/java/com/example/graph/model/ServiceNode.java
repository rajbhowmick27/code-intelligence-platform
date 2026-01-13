
package com.example.graph.model;

import org.springframework.data.neo4j.core.schema.*;

@Node("Service")
public class ServiceNode {

    @Id
    private String name;

    public ServiceNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}


package com.example.graph.model;

import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class ServiceCall {

    @Id @GeneratedValue
    private Long id;

    @Property
    private String type;

    @TargetNode
    private ServiceNode target;

    public ServiceCall(ServiceNode target, String type) {
        this.target = target;
        this.type = type;
    }
}

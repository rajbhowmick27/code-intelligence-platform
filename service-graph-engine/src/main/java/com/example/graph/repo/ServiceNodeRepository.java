
package com.example.graph.repo;

import com.example.graph.model.ServiceNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ServiceNodeRepository extends Neo4jRepository<ServiceNode, String> {}

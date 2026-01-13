
package com.example.graph.service;

import com.example.graph.model.ServiceNode;
import com.example.graph.repo.ServiceNodeRepository;
import org.springframework.stereotype.Service;

@Service
public class ServiceGraphBuilder {

    private final ServiceNodeRepository repo;

    public ServiceGraphBuilder(ServiceNodeRepository repo) {
        this.repo = repo;
    }

    public void registerService(String name) {
        repo.save(new ServiceNode(name));
    }

    public void link(String from, String to, String type) {
        ServiceNode src = repo.findById(from).orElse(new ServiceNode(from));
        ServiceNode tgt = repo.findById(to).orElse(new ServiceNode(to));
        repo.save(src);
        repo.save(tgt);
        // Neo4j relationship handled via Cypher in prod
    }
}

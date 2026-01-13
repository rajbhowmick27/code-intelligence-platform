
package com.example.graph.service;

import com.example.graph.model.ServiceNode;
import com.example.graph.repo.ServiceNodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceGraphQueryService {

    private final ServiceNodeRepository repo;

    public ServiceGraphQueryService(ServiceNodeRepository repo) {
        this.repo = repo;
    }

    public List<ServiceNode> allServices() {
        return repo.findAll();
    }
}

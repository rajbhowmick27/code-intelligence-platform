
package com.example.graph.controller;

import com.example.graph.model.ServiceNode;
import com.example.graph.service.ServiceGraphQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/graph")
public class GraphController {

    private final ServiceGraphQueryService query;

    public GraphController(ServiceGraphQueryService query) {
        this.query = query;
    }

    @GetMapping("/services")
    public List<ServiceNode> services() {
        return query.allServices();
    }
}

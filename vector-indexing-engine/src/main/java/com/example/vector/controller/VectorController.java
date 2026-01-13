
package com.example.vector.controller;

import com.example.vector.model.VectorDocument;
import com.example.vector.service.VectorIndexService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vector")
public class VectorController {

    private final VectorIndexService service;

    public VectorController(VectorIndexService service) {
        this.service = service;
    }

    @PostMapping("/upsert")
    public void upsert(@RequestBody VectorDocument doc) {
        service.upsert(doc);
    }

    @GetMapping("/search")
    public List<?> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int k,
            @RequestParam Map<String,Object> filter) {
        return service.search(q, k, filter);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.deleteById(id);
    }
}


package com.example.mcp.controller;

import com.example.mcp.dto.SearchRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mcp")
public class McpController {

    @PostMapping("/search_chunks")
    public List<String> search(@RequestBody SearchRequest req) {
        return List.of("stub-search-result");
    }

    @GetMapping("/get_chunk_by_symbol/{symbol}")
    public String bySymbol(@PathVariable String symbol) {
        return "stub-chunk-for-" + symbol;
    }

    @GetMapping("/get_service_dependencies/{service}")
    public List<String> deps(@PathVariable String service) {
        return List.of("payment-service","inventory-service");
    }

    @GetMapping("/get_upstream_callers/{service}")
    public List<String> upstream(@PathVariable String service) {
        return List.of("api-gateway");
    }
}


package com.example.mcp.dto;

import java.util.Map;

public record SearchRequest(String query, Map<String,Object> filters) {}

package com._ach.backend.search.controller;

import com._ach.backend.search.service.IndexSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search/sync")
@RequiredArgsConstructor
@Tag(name = "Index Sync", description = "Operations to sync data between PostgreSQL and Elasticsearch")
public class IndexSyncController {

    private final IndexSyncService indexSyncService;

    @PostMapping("/all")
    @Operation(
            summary = "Sync all items",
            description = "Synchronizes all items from PostgreSQL to Elasticsearch index"
    )
    public ResponseEntity<Map<String, Object>> syncAll() {
        long count = indexSyncService.syncAllItems();
        return ResponseEntity.ok(Map.of(
                "message", "Sync completed successfully",
                "itemsSynced", count
        ));
    }

    @PostMapping("/item/{id}")
    @Operation(
            summary = "Sync single item",
            description = "Synchronizes a single item from PostgreSQL to Elasticsearch"
    )
    public ResponseEntity<Map<String, String>> syncItem(@PathVariable Long id) {
        indexSyncService.syncItem(id);
        return ResponseEntity.ok(Map.of(
                "message", "Item synced successfully",
                "itemId", String.valueOf(id)
        ));
    }

    @DeleteMapping("/item/{id}")
    @Operation(
            summary = "Remove item from index",
            description = "Removes an item from the Elasticsearch index"
    )
    public ResponseEntity<Map<String, String>> removeItem(@PathVariable Long id) {
        indexSyncService.removeItem(id);
        return ResponseEntity.ok(Map.of(
                "message", "Item removed from index successfully",
                "itemId", String.valueOf(id)
        ));
    }
}

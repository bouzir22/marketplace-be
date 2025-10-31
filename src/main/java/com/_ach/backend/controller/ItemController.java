package com._ach.backend.controller;

import com._ach.backend.entity.Item;
import com._ach.backend.service.ItemService;
import com.querydsl.core.types.Predicate;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    // CREATE - Create a new item
    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        Item createdItem = itemService.createItem(item);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    // READ - Get all items with optional filtering
    @GetMapping
    public ResponseEntity<List<Item>> filterItems(
            @QuerydslPredicate(root = Item.class) Predicate predicate) {
        List<Item> items = itemService.filterItems(predicate);
        return ResponseEntity.ok(items);
    }

    // READ - Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        Item item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    // UPDATE - Update an existing item
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(
            @PathVariable Long id,
             @RequestBody Item item) {
        Item updatedItem = itemService.updateItem(id, item);
        return ResponseEntity.ok(updatedItem);
    }

    // UPDATE - Partial update (PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<Item> partialUpdateItem(
            @PathVariable Long id,
            @RequestBody Item item) {
        Item updatedItem = itemService.partialUpdateItem(id, item);
        return ResponseEntity.ok(updatedItem);
    }

    // DELETE - Delete an item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE - Delete multiple items
    @DeleteMapping
    public ResponseEntity<Void> deleteItems(@RequestBody List<Long> ids) {
        itemService.deleteItems(ids);
        return ResponseEntity.noContent().build();
    }
}

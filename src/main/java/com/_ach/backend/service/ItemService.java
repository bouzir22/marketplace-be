package com._ach.backend.service;

import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.exception.ResourceNotFoundException;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // CREATE - Create a new item
    public Item createItem(Item item) {
        return itemRepository.save(item);
    }

    // READ - Filter items with Querydsl predicate
    public List<Item> filterItems(Predicate predicate) {
        return (List<Item>) itemRepository.findAll(predicate);
    }

    // READ - Get item by ID
    public Item getItemById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    // UPDATE - Full update of an item
    public Item updateItem(Long id, Item itemDetails) {
        Item item = getItemById(id);

        // Update all fields
        item.setName(itemDetails.getName());
        item.setDescription(itemDetails.getDescription());
        item.setPrice(itemDetails.getPrice());
        // Add other fields as needed

        return itemRepository.save(item);
    }

    // UPDATE - Partial update of an item
    public Item partialUpdateItem(Long id, Item itemDetails) {
        Item item = getItemById(id);

        // Update only non-null fields
        if (itemDetails.getName() != null) {
            item.setName(itemDetails.getName());
        }
        if (itemDetails.getDescription() != null) {
            item.setDescription(itemDetails.getDescription());
        }
        if (itemDetails.getPrice() != 0) {
            item.setPrice(itemDetails.getPrice());
      }

        return itemRepository.save(item);
    }

    // DELETE - Delete a single item
    public void deleteItem(Long id) {
        Item item = getItemById(id);
        itemRepository.delete(item);
    }

    // DELETE - Delete multiple items
    public void deleteItems(List<Long> ids) {
        itemRepository.deleteAllById(ids);
    }
}
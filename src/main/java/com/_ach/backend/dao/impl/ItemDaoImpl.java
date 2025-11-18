package com._ach.backend.dao.impl;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.dao.ItemDao;
import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.service.ItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ItemDaoImpl implements ItemDao {

    private final ItemService itemService;
    private final ItemRepository itemRepository;

    public ItemDaoImpl(ItemService itemService, ItemRepository itemRepository) {
        this.itemService = itemService;
        this.itemRepository = itemRepository;
    }

    @Override
    public ItemRepresentation create(ItemRepresentation item) {
        return itemService.createItem(item);
    }

    @Override
    public Optional<ItemRepresentation> findById(Long id) {
        try {
            return Optional.of(itemService.getItemById(id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ItemRepresentation> findAll(Pageable pageable, Map<String, String> filter) {
        // Get paginated items from repository
        Page<Item> itemsPage = itemRepository.findAll(pageable);

        // Convert to ItemRepresentation
        return itemsPage.getContent().stream()
                .map(this::convertToRepresentation)
                .toList();
    }

    @Override
    public ItemRepresentation update(ItemRepresentation item) {
        if (!itemRepository.existsById(item.getId())) {
            throw new IllegalArgumentException("Item with id " + item.getId() + " does not exist");
        }
        return itemService.updateItem(item.getId(), item);
    }

    @Override
    public void delete(Long id) {
        itemService.deleteItem(id);
    }

    @Override
    public boolean exists(Long id) {
        return itemRepository.existsById(id);
    }

    @Override
    public long count() {
        return itemRepository.count();
    }

    private ItemRepresentation convertToRepresentation(Item item) {
        return itemService.getItemById(item.getId());
    }
}

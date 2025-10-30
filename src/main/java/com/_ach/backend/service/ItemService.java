package com._ach.backend.service;

import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {
private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Item> filterItems(Predicate predicate) {
     return (List<Item>) itemRepository.findAll(predicate);


    }

}

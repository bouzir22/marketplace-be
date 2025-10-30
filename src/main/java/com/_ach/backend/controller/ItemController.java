package com._ach.backend.controller;

import com._ach.backend.entity.Item;
import com._ach.backend.service.ItemService;
import com.querydsl.core.types.Predicate;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping()
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }


    @GetMapping("items")
    public List<Item> filterItems(@QuerydslPredicate(root = Item.class) Predicate predicate) {
        return itemService.filterItems(predicate);

    }


}

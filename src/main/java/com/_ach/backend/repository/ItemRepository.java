package com._ach.backend.repository;

import com._ach.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface ItemRepository extends JpaRepository<Item, Long> , QuerydslPredicateExecutor<Item> {
}
package com._ach.backend.repository;

import com._ach.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> , QuerydslPredicateExecutor<Item> {

    @Query("SELECT DISTINCT i FROM Item i LEFT JOIN FETCH i.images WHERE i.attributesMapId IN :attributesMapIds")
    List<Item> findByAttributesMapIdIn(@Param("attributesMapIds") List<String> attributesMapIds);
}
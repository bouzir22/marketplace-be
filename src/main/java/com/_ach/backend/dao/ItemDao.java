package com._ach.backend.dao;

import com._ach.backend.Model.ItemRepresentation;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ItemDao {
    ItemRepresentation create(ItemRepresentation item);

    Optional<ItemRepresentation> findById(Long id);

    List<ItemRepresentation> findAll(Pageable pageable, Map<String, String> filter);

    ItemRepresentation update(ItemRepresentation item);

    void delete(Long id);

    boolean exists(Long id);

    long count();
}

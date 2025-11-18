package com._ach.backend.repository;

import com._ach.backend.document.ItemAttributesDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemAttributesRepository extends ElasticsearchRepository<ItemAttributesDocument, String> {

    Optional<ItemAttributesDocument> findByItemId(Long itemId);

    void deleteByItemId(Long itemId);
}

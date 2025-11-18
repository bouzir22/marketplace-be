package com._ach.backend.search.repository;

import com._ach.backend.search.document.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemSearchRepository extends ElasticsearchRepository<ItemDocument, String> {
}

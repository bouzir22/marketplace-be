package com._ach.backend.repository;

import com._ach.backend.document.AttributesMap;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributesMapRepository extends ElasticsearchRepository<AttributesMap, String> {
}

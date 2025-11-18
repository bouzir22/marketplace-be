package com._ach.backend.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@Document(indexName = "attributes_maps")
@Getter
@Setter
public class AttributesMap {
    @Id
    private String id;

    @Field(type = FieldType.Object)
    private Map<String, Object> attributes;
}

package com._ach.backend.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "item_attributes")
public class ItemAttributesDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long itemId;

    @Field(type = FieldType.Object)
    private Map<String, Object> attributes;
}

package com._ach.backend.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Map;

@Getter
@Setter
public class ItemDetails {
    @Id
    private String id;

    private Map<String, Object> attributes;
}

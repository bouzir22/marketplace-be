package com._ach.backend.Model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ItemRepresentation {
    private Long id;
    private List<ItemImageDTO> images = new ArrayList<>();
    private Map<String, Object> attributes;
}

package com._ach.backend.Model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
@Getter
@Setter
public class ItemRepresentation   {
    private Long id;
    private String image; // Main image URL
    private List<String> images; // Additional images
    private Map<String,Object> attributes;

}

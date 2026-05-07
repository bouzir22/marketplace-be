package com._ach.backend.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemImageDTO {
    private Long id;
    private String url;
    private boolean isMain;
    private Integer displayOrder;
    private String altText;
}

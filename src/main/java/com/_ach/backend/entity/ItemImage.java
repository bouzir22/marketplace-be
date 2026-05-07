package com._ach.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "item_images")
public class ItemImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
    
    @Column(nullable = false)
    private String url;
    
    @Column(nullable = false)
    private boolean isMain = false;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    private String altText;
}

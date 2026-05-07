package com._ach.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String attributesMapId;
    
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ItemImage> images = new ArrayList<>();
    
    public void addImage(ItemImage image) {
        images.add(image);
        image.setItem(this);
    }
    
    public void removeImage(ItemImage image) {
        images.remove(image);
        image.setItem(null);
    }
    
    public Optional<ItemImage> getMainImage() {
        return images.stream()
                .filter(ItemImage::isMain)
                .findFirst();
    }
    
    public void setMainImage(ItemImage newMainImage) {
        images.forEach(img -> img.setMain(false));
        newMainImage.setMain(true);
    }
}
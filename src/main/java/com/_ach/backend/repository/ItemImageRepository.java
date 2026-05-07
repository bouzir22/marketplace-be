package com._ach.backend.repository;

import com._ach.backend.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {
    
    List<ItemImage> findByItemIdOrderByDisplayOrderAsc(Long itemId);
    
    Optional<ItemImage> findByItemIdAndIsMainTrue(Long itemId);
    
    @Query("SELECT img FROM ItemImage img WHERE img.item.id = :itemId AND img.id = :imageId")
    Optional<ItemImage> findByItemIdAndImageId(Long itemId, Long imageId);
    
    void deleteByItemId(Long itemId);
    
    long countByItemId(Long itemId);
}

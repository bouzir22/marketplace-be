package com._ach.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String brand;
    private String description;
    private double price;
    private double originalPrice;
    private String size;
    private String condition; // e.g. "Like New", "Excellent", "Good"
    private String availability; // "store" or "merchant"
    private String location; // Optional

    private String image; // Main image URL

    @ElementCollection
    private List<String> images; // Additional images

}
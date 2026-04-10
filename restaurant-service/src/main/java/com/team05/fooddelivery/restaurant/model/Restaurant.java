package com.team05.fooddelivery.restaurant.model;

import com.team05.fooddelivery.restaurant.enums.CuisineTypeEnum;
import com.team05.fooddelivery.restaurant.enums.RestaurantStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CuisineTypeEnum cuisineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatusEnum status = RestaurantStatusEnum.OPEN;

    // [S2-F7] Running average rating; recalculated each time a delivered order is rated.
    @Column(nullable = false)
    private Double rating = 0.0;

    // [S2-F7] Tracks the number of ratings submitted; used together with rating to compute the new average.
    @Column(nullable = false)
    private Integer totalRatings = 0;

    // [S2-F2, S2-F5] JSONB column storing flexible restaurant attributes (e.g. address, deliveryRadius).
    // S2-F2 merges new fields into this map; S2-F5 filters restaurants by a key-value pair within it.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MenuItem> menuItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.totalRatings = 0 ;
        this.rating = 0.0 ;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public CuisineTypeEnum getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(CuisineTypeEnum cuisineType) {
        this.cuisineType = cuisineType;
    }

    public RestaurantStatusEnum getStatus() {
        return status;
    }

    public void setStatus(RestaurantStatusEnum status) {
        this.status = status;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }
}
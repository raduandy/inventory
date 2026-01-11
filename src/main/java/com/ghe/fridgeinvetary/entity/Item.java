package com.ghe.fridgeinvetary.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Quantity is required")
    @PositiveOrZero(message = "Quantity cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @NotBlank(message = "Unit is required")
    @Column(nullable = false)
    private String unit;

    @NotBlank(message = "Location is required")
    @Column(nullable = false)
    private String location;

    private String category;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    private LocalDate expirationDate;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean finished = false;

    @PrePersist
    protected void onCreate() {
        if (purchaseDate == null) {
            purchaseDate = LocalDateTime.now();
        }
    }

    // Getters and Setters
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    // Helper methods for expiry status
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int warningDays) {
        if (expirationDate == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(warningDays);
        return !isExpired() && !expirationDate.isAfter(warningDate);
    }

    // Helper method for low quantity status
    public boolean isLowQuantity(BigDecimal threshold) {
        return quantity != null && quantity.compareTo(threshold) <= 0;
    }
}

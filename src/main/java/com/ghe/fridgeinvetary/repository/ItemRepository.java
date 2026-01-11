package com.ghe.fridgeinvetary.repository;

import com.ghe.fridgeinvetary.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // Find all active (not finished) items
    List<Item> findByFinishedFalseOrderByExpirationDateAsc();

    // Find active items by location
    List<Item> findByFinishedFalseAndLocationOrderByExpirationDateAsc(String location);

    // Find all finished items (history)
    List<Item> findByFinishedTrueOrderByPurchaseDateDesc();

    // Find items expiring on or before a date (for scheduled jobs)
    @Query("SELECT i FROM Item i WHERE i.finished = false AND i.expirationDate IS NOT NULL AND i.expirationDate <= :date ORDER BY i.expirationDate ASC")
    List<Item> findItemsExpiringByDate(@Param("date") LocalDate date);

    // Find expired items
    @Query("SELECT i FROM Item i WHERE i.finished = false AND i.expirationDate IS NOT NULL AND i.expirationDate < :today ORDER BY i.expirationDate ASC")
    List<Item> findExpiredItems(@Param("today") LocalDate today);

    // Find items expiring soon (within N days from today, but not yet expired)
    @Query("SELECT i FROM Item i WHERE i.finished = false AND i.expirationDate IS NOT NULL AND i.expirationDate >= :today AND i.expirationDate <= :warningDate ORDER BY i.expirationDate ASC")
    List<Item> findItemsExpiringSoon(@Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);

    // Get distinct locations for filter dropdown
    @Query("SELECT DISTINCT i.location FROM Item i WHERE i.finished = false ORDER BY i.location")
    List<String> findDistinctLocations();

    // Get distinct categories for filter dropdown
    @Query("SELECT DISTINCT i.category FROM Item i WHERE i.category IS NOT NULL AND i.category != '' ORDER BY i.category")
    List<String> findDistinctCategories();

    // Find category by item name (for auto-matching) - returns most recent
    @Query(value = "SELECT category FROM items WHERE LOWER(name) = LOWER(:name) AND category IS NOT NULL AND category != '' ORDER BY purchase_date DESC LIMIT 1", nativeQuery = true)
    String findCategoryByItemName(@Param("name") String name);
}

package com.ghe.fridgeinvetary.service;

import com.ghe.fridgeinvetary.entity.Item;
import com.ghe.fridgeinvetary.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final int warningDays;
    private final BigDecimal lowQuantityThreshold;

    public ItemService(ItemRepository itemRepository,
                       @Value("${app.expiry.warning-days:3}") int warningDays,
                       @Value("${app.quantity.low-threshold:2}") BigDecimal lowQuantityThreshold) {
        this.itemRepository = itemRepository;
        this.warningDays = warningDays;
        this.lowQuantityThreshold = lowQuantityThreshold;
    }

    public int getWarningDays() {
        return warningDays;
    }

    public BigDecimal getLowQuantityThreshold() {
        return lowQuantityThreshold;
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public List<Item> saveAll(List<Item> items) {
        return itemRepository.saveAll(items);
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public List<Item> findAllActive() {
        return itemRepository.findByFinishedFalseOrderByExpirationDateAsc();
    }

    public List<Item> findByLocation(String location) {
        if (location == null || location.isBlank() || "all".equalsIgnoreCase(location)) {
            return findAllActive();
        }
        return itemRepository.findByFinishedFalseAndLocationOrderByExpirationDateAsc(location);
    }

    public List<Item> findFinishedItems() {
        return itemRepository.findByFinishedTrueOrderByPurchaseDateDesc();
    }

    public List<String> getDistinctLocations() {
        return itemRepository.findDistinctLocations();
    }

    /**
     * Decrease quantity by specified amount.
     * If quantity reaches 0 or below, mark as finished.
     */
    public Item consumeItem(Long id, BigDecimal amount) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));

        BigDecimal newQuantity = item.getQuantity().subtract(amount);

        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            item.setQuantity(BigDecimal.ZERO);
            item.setFinished(true);
        } else {
            item.setQuantity(newQuantity);
        }

        return itemRepository.save(item);
    }

    /**
     * Mark an item as finished manually.
     */
    public Item markAsFinished(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
        item.setFinished(true);
        return itemRepository.save(item);
    }

    /**
     * Restore a finished item back to active.
     */
    public Item restoreItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
        item.setFinished(false);
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    // ============ Methods for scheduled jobs / expiry checks ============

    /**
     * Get all expired items (expiration date before today).
     * Designed for use by a scheduled job.
     */
    public List<Item> getExpiredItems() {
        return itemRepository.findExpiredItems(LocalDate.now());
    }

    /**
     * Get items expiring within the configured warning period.
     * Designed for use by a scheduled job.
     */
    public List<Item> getItemsExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(warningDays);
        return itemRepository.findItemsExpiringSoon(today, warningDate);
    }

    /**
     * Get all items expiring by a specific date.
     * Designed for use by a scheduled job.
     */
    public List<Item> getItemsExpiringByDate(LocalDate date) {
        return itemRepository.findItemsExpiringByDate(date);
    }

    /**
     * Check expiring items - can be called by a scheduled job.
     * Returns a summary of expired and expiring soon items.
     */
    public ExpiryCheckResult checkExpiringItems() {
        List<Item> expired = getExpiredItems();
        List<Item> expiringSoon = getItemsExpiringSoon();
        return new ExpiryCheckResult(expired, expiringSoon);
    }

    /**
     * Result of expiry check, for use by scheduled jobs or notifications.
     */
    public record ExpiryCheckResult(List<Item> expiredItems, List<Item> expiringSoonItems) {
        public boolean hasWarnings() {
            return !expiredItems.isEmpty() || !expiringSoonItems.isEmpty();
        }

        public int totalWarnings() {
            return expiredItems.size() + expiringSoonItems.size();
        }
    }
}

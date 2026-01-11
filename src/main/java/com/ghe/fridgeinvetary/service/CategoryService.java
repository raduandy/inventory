package com.ghe.fridgeinvetary.service;

import com.ghe.fridgeinvetary.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing item categories.
 * Categories are learned from user input - when you add an item with a category,
 * that category becomes available for future items.
 * When adding an item with the same name, it auto-suggests the previous category.
 */
@Service
public class CategoryService {

    private final ItemRepository itemRepository;

    public CategoryService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Get all categories that have been used (from existing items).
     */
    public List<String> getAllCategories() {
        return itemRepository.findDistinctCategories();
    }

    /**
     * Suggest a category based on the item name.
     * Looks up if this item name was used before and returns its category.
     * Returns null if no match found (user can create a new category).
     */
    public String suggestCategory(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }
        return itemRepository.findCategoryByItemName(itemName.trim());
    }
}

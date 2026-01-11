package com.ghe.fridgeinvetary.controller;

import com.ghe.fridgeinvetary.entity.Item;
import com.ghe.fridgeinvetary.service.CategoryService;
import com.ghe.fridgeinvetary.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    private final ItemService itemService;
    private final CategoryService categoryService;

    public ItemController(ItemService itemService, CategoryService categoryService) {
        this.itemService = itemService;
        this.categoryService = categoryService;
    }

    // ============ Dashboard ============

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String location,
                            @RequestParam(required = false) String category,
                            Model model) {
        List<Item> items = itemService.findByLocation(location);
        
        // Filter by category if specified
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            items = items.stream()
                    .filter(i -> category.equals(i.getCategory()))
                    .toList();
        }
        
        List<String> locations = itemService.getDistinctLocations();
        List<String> categories = categoryService.getAllCategories();
        int warningDays = itemService.getWarningDays();
        BigDecimal lowQuantityThreshold = itemService.getLowQuantityThreshold();

        // Count warnings for the header
        long expiredCount = items.stream().filter(Item::isExpired).count();
        long expiringSoonCount = items.stream().filter(i -> i.isExpiringSoon(warningDays)).count();
        long lowQuantityCount = items.stream().filter(i -> i.isLowQuantity(lowQuantityThreshold)).count();

        model.addAttribute("items", items);
        model.addAttribute("locations", locations);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("warningDays", warningDays);
        model.addAttribute("lowQuantityThreshold", lowQuantityThreshold);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("expiringSoonCount", expiringSoonCount);
        model.addAttribute("lowQuantityCount", lowQuantityCount);

        return "dashboard";
    }

    // ============ Add Single Item ============

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("item", new Item());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "add-item";
    }

    @PostMapping("/add")
    public String addItem(@Valid @ModelAttribute("item") Item item,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "add-item";
        }

        // Auto-suggest category if not set (from previous items with same name)
        if (item.getCategory() == null || item.getCategory().isBlank()) {
            String suggestedCategory = categoryService.suggestCategory(item.getName());
            if (suggestedCategory != null && !suggestedCategory.isBlank()) {
                item.setCategory(suggestedCategory);
            }
        }

        itemService.save(item);
        redirectAttributes.addFlashAttribute("success", item.getName() + " added successfully!");

        return "redirect:/";
    }

    // ============ Category Suggestion API ============

    @GetMapping("/api/suggest-category")
    @ResponseBody
    public ResponseEntity<Map<String, String>> suggestCategory(@RequestParam String name) {
        String category = categoryService.suggestCategory(name);
        if (category == null || category.isBlank()) {
            return ResponseEntity.ok(Map.of("category", ""));
        }
        return ResponseEntity.ok(Map.of("category", category));
    }

    @GetMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // ============ Edit Item ============

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return itemService.findById(id)
                .map(item -> {
                    model.addAttribute("item", item);
                    model.addAttribute("categories", categoryService.getAllCategories());
                    return "edit-item";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Item not found");
                    return "redirect:/";
                });
    }

    @PostMapping("/edit/{id}")
    public String updateItem(@PathVariable Long id,
                             @Valid @ModelAttribute("item") Item item,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllCategories());
            return "edit-item";
        }

        return itemService.findById(id)
                .map(existingItem -> {
                    existingItem.setName(item.getName());
                    existingItem.setQuantity(item.getQuantity());
                    existingItem.setUnit(item.getUnit());
                    existingItem.setLocation(item.getLocation());
                    existingItem.setCategory(item.getCategory());
                    existingItem.setExpirationDate(item.getExpirationDate());
                    existingItem.setNotes(item.getNotes());
                    existingItem.setFinished(item.isFinished());

                    itemService.save(existingItem);
                    redirectAttributes.addFlashAttribute("success", "Item updated successfully!");
                    return "redirect:/";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Item not found");
                    return "redirect:/";
                });
    }

    // ============ Consume Item ============

    @PostMapping("/consume/{id}")
    public String consumeItem(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              RedirectAttributes redirectAttributes) {
        try {
            Item item = itemService.consumeItem(id, amount);
            if (item.isFinished()) {
                redirectAttributes.addFlashAttribute("success", item.getName() + " is now finished!");
            } else {
                redirectAttributes.addFlashAttribute("success", "Removed " + amount + " " + item.getUnit() + " of " + item.getName());
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    // ============ Mark as Finished ============

    @PostMapping("/finish/{id}")
    public String finishItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Item item = itemService.markAsFinished(id);
            redirectAttributes.addFlashAttribute("success", item.getName() + " marked as finished");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    // ============ History ============

    @GetMapping("/history")
    public String showHistory(Model model) {
        List<Item> finishedItems = itemService.findFinishedItems();
        model.addAttribute("items", finishedItems);
        return "history";
    }

    @PostMapping("/restore/{id}")
    public String restoreItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Item item = itemService.restoreItem(id);
            redirectAttributes.addFlashAttribute("success", item.getName() + " restored to active items");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/history";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        itemService.deleteItem(id);
        redirectAttributes.addFlashAttribute("success", "Item deleted permanently");
        return "redirect:/history";
    }

    // ============ Login ============

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

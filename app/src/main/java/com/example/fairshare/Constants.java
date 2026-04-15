package com.example.fairshare;

import java.util.Arrays;
import java.util.List;

/**
 * Global constants for the FairShare app
 */
public class Constants {
    
    // Expense categories
    public static final String CATEGORY_FOOD = "Food";
    public static final String CATEGORY_TRANSPORT = "Transport";
    public static final String CATEGORY_SHOPPING = "Shopping";
    public static final String CATEGORY_BILLS = "Bills";
    public static final String CATEGORY_OTHER = "Other";
    
    // Standardized category list (without Salary)
    public static final List<String> EXPENSE_CATEGORIES = Arrays.asList(
        CATEGORY_FOOD,
        CATEGORY_TRANSPORT,
        CATEGORY_SHOPPING,
        CATEGORY_BILLS,
        CATEGORY_OTHER
    );
    
    // Default category
    public static final String DEFAULT_CATEGORY = CATEGORY_OTHER;
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}

package com.example.loginsignup

data class Expense(
    val name: String,
    val description: String,
    val time: String,
    val totalAmount: String,
    val owedAmount: String,
    val isSettled: Boolean = false,
    val hasImage: Boolean = false
)

object ExpenseRepository {
    val expenses = mutableListOf(
        Expense("Matt", "Bachelor Party", "Time: 1 hour ago", "₱1,800", "you owe: ₱450", isSettled = false),
        Expense("Sarah", "Dinner", "Time: 2 hours ago", "₱1,200", "you owe: ₱300", isSettled = true),
        Expense("John", "Movies", "Time: 3 hours ago", "₱800", "you owe: ₱200", isSettled = false, hasImage = true),
        Expense("Matt", "Bachelor Party", "Time: 5 hours ago", "₱1,800", "you owe: ₱450", isSettled = true)
    )

    fun addExpense(expense: Expense) {
        expenses.add(0, expense) // Add to the top
    }
}
package com.example.loginsignup

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    
    private lateinit var adapter: ExpenseAdapter
    private val displayList = mutableListOf<Expense>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnScanReceipt = findViewById<MaterialButton>(R.id.btnScanReceipt)
        val btnAddExpense = findViewById<MaterialButton>(R.id.btnAddExpense)
        val btnQcr = findViewById<ImageButton>(R.id.btnQcr)
        val ivMore = findViewById<ImageView>(R.id.ivMore)
        val rvExpenses = findViewById<RecyclerView>(R.id.rvExpenses)
        
        val tabAll = findViewById<TextView>(R.id.tabAll)
        val tabSettled = findViewById<TextView>(R.id.tabSettled)
        val tabImages = findViewById<TextView>(R.id.tabImages)

        // Setup RecyclerView with the display list
        adapter = ExpenseAdapter(displayList)
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = adapter

        // Initial load
        applyFilter("All")

        // Tab click listeners
        tabAll.setOnClickListener { applyFilter("All") }
        tabSettled.setOnClickListener { applyFilter("Settled") }
        tabImages.setOnClickListener { applyFilter("Images") }

        // Navigation
        btnScanReceipt.setOnClickListener {
            startActivity(Intent(this, OcrScannerActivity::class.java))
        }

        btnAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        btnQcr.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        ivMore.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun applyFilter(filter: String) {
        currentFilter = filter
        displayList.clear()
        
        when (filter) {
            "All" -> displayList.addAll(ExpenseRepository.expenses)
            "Settled" -> displayList.addAll(ExpenseRepository.expenses.filter { it.isSettled })
            "Images" -> displayList.addAll(ExpenseRepository.expenses.filter { it.hasImage })
        }
        
        updateTabUI()
        adapter.notifyDataSetChanged()
    }

    private fun updateTabUI() {
        val tabAll = findViewById<TextView>(R.id.tabAll)
        val tabSettled = findViewById<TextView>(R.id.tabSettled)
        val tabImages = findViewById<TextView>(R.id.tabImages)
        
        val tabs = listOf(tabAll to "All", tabSettled to "Settled", tabImages to "Images")
        
        tabs.forEach { (view, name) ->
            if (name == currentFilter) {
                view.setTypeface(null, Typeface.BOLD)
                view.alpha = 1.0f
            } else {
                view.setTypeface(null, Typeface.NORMAL)
                view.alpha = 0.5f
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply current filter to include any new expenses
        applyFilter(currentFilter)
    }
}
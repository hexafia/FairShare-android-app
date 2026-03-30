package com.example.loginsignup

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class AddExpenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        val btnBack = findViewById<ImageView>(R.id.btnBackAddExpense)
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirm)
        
        val tilName = findViewById<TextInputLayout>(R.id.tilPersonName)
        val tilAmount = findViewById<TextInputLayout>(R.id.tilAmount)
        val actvGroup = findViewById<AutoCompleteTextView>(R.id.actvGroup)
        val actvSplittingMethod = findViewById<AutoCompleteTextView>(R.id.actvSplittingMethod)
        val etDate = findViewById<TextInputEditText>(R.id.etDate)
        val tilNote = findViewById<TextInputLayout>(R.id.tilNote)

        // Setup Group Dropdown
        val groups = arrayOf("Barkada", "Family", "Work", "Travel")
        val groupAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, groups)
        actvGroup.setAdapter(groupAdapter)

        // Setup Splitting Method Dropdown
        val methods = arrayOf("Split Evenly", "Split by Portion spend")
        val methodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, methods)
        actvSplittingMethod.setAdapter(methodAdapter)

        // Set Default Date to Today
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val initialDateStr = String.format("%02d/%02d/%02d", month + 1, day, year % 100)
        etDate.setText(initialDateStr)

        // Setup Date Picker
        etDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val dateStr = String.format("%02d/%02d/%02d", selectedMonth + 1, selectedDay, selectedYear % 100)
                etDate.setText(dateStr)
            }, year, month, day)
            datePickerDialog.show()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnConfirm.setOnClickListener {
            val name = tilName.editText?.text?.toString() ?: ""
            val totalStr = tilAmount.editText?.text?.toString() ?: ""
            val group = actvGroup.text.toString()
            val note = tilNote.editText?.text?.toString() ?: ""
            
            if (name.isNotEmpty() && totalStr.isNotEmpty()) {
                val total = totalStr.toDoubleOrNull() ?: 0.0
                val owed = total / 4 
                
                val newExpense = Expense(
                    name = name,
                    description = "$group - $note",
                    time = "Just now",
                    totalAmount = "₱${String.format("%,.0f", total)}", 
                    owedAmount = "you owe: ₱${String.format("%,.0f", owed)}"
                )
                
                ExpenseRepository.addExpense(newExpense)
                finish()
            }
        }
    }
}
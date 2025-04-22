package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class AddCardDialog : DialogFragment() {

    var onCardAdded: ((Card) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_add_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardTypeSpinner: Spinner = view.findViewById(R.id.card_type_spinner)
        val cardNumberInput: EditText = view.findViewById(R.id.card_number_input)
        val cardHolderNameInput: EditText = view.findViewById(R.id.card_holder_name_input)
        val cardCvvInput: EditText = view.findViewById(R.id.card_cvv_input)
        val cardBalanceInput: EditText = view.findViewById(R.id.card_balance_input)
        val addButton: Button = view.findViewById(R.id.add_button)
        val cancelButton: Button = view.findViewById(R.id.cancel_button)

        // Set up card type spinner
        val cardTypes = listOf("Credit Card", "Debit Card")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cardTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cardTypeSpinner.adapter = adapter

        addButton.setOnClickListener {
            val type = cardTypeSpinner.selectedItem.toString()
            val number = cardNumberInput.text.toString().trim()
            val holderName = cardHolderNameInput.text.toString().trim()
            val cvv = cardCvvInput.text.toString().trim()
            val balance = cardBalanceInput.text.toString().trim().toDoubleOrNull() ?: 0.00

            // Validation
            when {
                number.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter card number", Toast.LENGTH_SHORT).show()
                    cardNumberInput.requestFocus()
                }
                number.length != 16 -> {
                    Toast.makeText(requireContext(), "Card number must be 16 digits", Toast.LENGTH_SHORT).show()
                    cardNumberInput.requestFocus()
                }
                holderName.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter cardholder name", Toast.LENGTH_SHORT).show()
                    cardHolderNameInput.requestFocus()
                }
                cvv.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter CVV", Toast.LENGTH_SHORT).show()
                    cardCvvInput.requestFocus()
                }
                cvv.length != 3 -> {
                    Toast.makeText(requireContext(), "CVV must be 3 digits", Toast.LENGTH_SHORT).show()
                    cardCvvInput.requestFocus()
                }
                cardBalanceInput.text.toString().trim().isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter balance", Toast.LENGTH_SHORT).show()
                    cardBalanceInput.requestFocus()
                }
                else -> {
                    val card = Card(type, number, holderName, cvv, balance)
                    onCardAdded?.invoke(card)
                    dismiss()
                }
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
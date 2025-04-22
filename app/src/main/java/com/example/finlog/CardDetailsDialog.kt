package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.finlog.databinding.DialogCardDetailsBinding

class CardDetailsDialog : DialogFragment() {

    private var _binding: DialogCardDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_CARD = "card"

        fun newInstance(card: Card): CardDetailsDialog {
            val fragment = CardDetailsDialog()
            val args = Bundle()
            args.putSerializable(ARG_CARD, card)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCardDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val card = arguments?.getSerializable(ARG_CARD) as Card

        binding.cardType.text = card.type.uppercase()
        binding.cardNumber.text = card.number
        binding.cardHolderName.text = card.holderName
        binding.cardCvv.text = card.cvv
        binding.cardBalance.text = "$${String.format("%.2f", card.balance)}"
        binding.root.backgroundTintList = when (card.type) {
            "Credit Card" -> binding.root.context.getColorStateList(R.color.colorError)
            "Debit Card" -> binding.root.context.getColorStateList(R.color.colorAccent)
            else -> binding.root.context.getColorStateList(R.color.colorPrimary)
        }

        binding.closeButton.setOnClickListener {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
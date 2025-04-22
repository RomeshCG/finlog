package com.example.finlog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finlog.databinding.ItemCardBinding

class CardAdapter(
    private val cards: List<Card>,
    private val onCardClick: (Card) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int = cards.size

    inner class CardViewHolder(private val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: Card) {
            binding.cardType.text = card.type.uppercase()
            // Mask the card number (show only last 4 digits)
            val maskedNumber = "**** **** **** ${card.number.takeLast(4)}"
            binding.cardNumber.text = maskedNumber
            binding.cardHolderName.text = card.holderName
            binding.cardCvv.text = "***" // Mask CVV
            // Set background color based on card type
            binding.root.backgroundTintList = when (card.type) {
                "Credit Card" -> binding.root.context.getColorStateList(R.color.colorError)
                "Debit Card" -> binding.root.context.getColorStateList(R.color.colorAccent)
                else -> binding.root.context.getColorStateList(R.color.colorPrimary)
            }
            binding.root.setOnClickListener { onCardClick(card) }
        }
    }
}
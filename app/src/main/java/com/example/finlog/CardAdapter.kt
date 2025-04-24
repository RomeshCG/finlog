package com.example.finlog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private val cards: MutableList<Card>,
    private val onCardClick: (Card) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardType: TextView = itemView.findViewById(R.id.card_type)
        val cardNumber: TextView = itemView.findViewById(R.id.card_number)
        val cardHolderName: TextView = itemView.findViewById(R.id.card_holder_name)
        val cardCvv: TextView = itemView.findViewById(R.id.card_cvv)
        val deleteIcon: ImageView = itemView.findViewById(R.id.delete_card_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        holder.cardType.text = card.type
        holder.cardNumber.text = "**** **** **** ${card.number.takeLast(4)}" // Mask the card number
        holder.cardHolderName.text = card.holderName
        holder.cardCvv.text = "CVV: ${card.cvv}"

        // Handle card click to show details
        holder.itemView.setOnClickListener {
            onCardClick(card)
        }

        // Handle delete icon click
        holder.deleteIcon.setOnClickListener {
            onDeleteClick(card.number)
        }
    }

    override fun getItemCount(): Int = cards.size

    fun updateCards(newCards: List<Card>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged()
    }
}
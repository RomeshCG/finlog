package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView // Added import
import com.example.finlog.databinding.FragmentCardsBinding

class CardsFragment : Fragment() {

    private var _binding: FragmentCardsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataManager: DataManager
    private lateinit var cardAdapter: CardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataManager = DataManager(requireContext())

        setupRecyclerView()
        setupAddCardButton()
    }

    private fun setupRecyclerView() {
        cardAdapter = CardAdapter(mutableListOf(), { card ->
            // Show card details dialog
            val dialog = CardDetailsDialog.newInstance(card)
            dialog.show(parentFragmentManager, "CardDetailsDialog")
        }, { cardNumber ->
            // Delete card
            dataManager.deleteCard(cardNumber)
            updateCardList()
            Toast.makeText(requireContext(), "Card deleted", Toast.LENGTH_SHORT).show()
        })
        binding.cardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cardsRecyclerView.adapter = cardAdapter as RecyclerView.Adapter<*>
        updateCardList()
    }

    private fun setupAddCardButton() {
        binding.addCardIcon.setOnClickListener {
            val dialog = AddCardDialog()
            dialog.onCardAdded = { newCard ->
                dataManager.addCard(newCard)
                updateCardList()
            }
            dialog.show(parentFragmentManager, "AddCardDialog")
        }
    }

    private fun updateCardList() {
        val cards = dataManager.getCards()
        cardAdapter.updateCards(cards)
    }

    override fun onResume() {
        super.onResume()
        updateCardList() // Refresh the list when returning to the fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.finlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        cardAdapter = CardAdapter(dataManager.getCards()) { card ->
            // Show card details dialog
            val dialog = CardDetailsDialog.newInstance(card)
            dialog.show(parentFragmentManager, "CardDetailsDialog")
        }
        binding.cardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cardsRecyclerView.adapter = cardAdapter as RecyclerView.Adapter<*>
    }

    private fun setupAddCardButton() {
        binding.addCardIcon.setOnClickListener {
            val dialog = AddCardDialog()
            dialog.onCardAdded = { newCard ->
                dataManager.addCard(newCard)
                cardAdapter = CardAdapter(dataManager.getCards()) { card ->
                    val detailDialog = CardDetailsDialog.newInstance(card)
                    detailDialog.show(parentFragmentManager, "CardDetailsDialog")
                }
                binding.cardsRecyclerView.adapter = cardAdapter as RecyclerView.Adapter<*>
            }
            dialog.show(parentFragmentManager, "AddCardDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
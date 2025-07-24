package com.zadudoder.spmhelpermobile;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardViewHolder> {
    private List<Card> cards;
    private final SharedPreferences sharedPref;

    public CardAdapter(List<Card> cards, SharedPreferences sharedPref) {
        this.cards = cards;
        this.sharedPref = sharedPref;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view, cards, sharedPref, this);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        holder.bind(cards.get(position));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public List<Card> getCards() {
        return cards;
    }

    public void updateCards(List<Card> newCards) {
        this.cards = newCards;
        notifyDataSetChanged();
    }
}
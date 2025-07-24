package com.zadudoder.spmhelpermobile;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class CardViewHolder extends RecyclerView.ViewHolder {
    private final TextView cardName;
    private final TextView cardBalance;
    private final List<Card> cards;
    private final SharedPreferences sharedPref;
    private final CardAdapter adapter;

    public CardViewHolder(@NonNull View itemView, List<Card> cards, SharedPreferences sharedPref, CardAdapter adapter) {
        super(itemView);
        this.cardName = itemView.findViewById(R.id.card_name);
        this.cardBalance = itemView.findViewById(R.id.card_balance);
        this.cards = cards;
        this.sharedPref = sharedPref;
        this.adapter = adapter;
    }

    public void bind(Card card) {
        cardName.setText(card.getName());
        cardBalance.setText("Баланс: " + card.getBalance() + " АР");

        itemView.findViewById(R.id.delete_button).setOnClickListener(v -> {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Удаление карты")
                    .setMessage("Вы уверены, что хотите удалить карту " + card.getName() + "?")
                    .setPositiveButton("Удалить", (dialog, which) -> removeCard(card))
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    private void removeCard(Card card) {
        int position = getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            cards.remove(position);
            adapter.notifyItemRemoved(position);
            saveCardsToPref();
        }
    }

    private void saveCardsToPref() {
        JSONArray jsonArray = new JSONArray();
        for (Card card : cards) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", card.getId());
                jsonObject.put("token", card.getToken());
                jsonObject.put("name", card.getName());
                jsonObject.put("number", card.getNumber());
                jsonObject.put("balance", card.getBalance());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sharedPref.edit()
                .putString("saved_cards", jsonArray.toString())
                .apply();
    }
}
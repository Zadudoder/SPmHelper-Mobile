package com.zadudoder.spmhelpermobile;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class CardViewHolder extends RecyclerView.ViewHolder {
    private final TextView cardName;
    private final TextView cardBalance;
    private final TextView selectedLabel;
    private final List<Card> cards;
    private final SharedPreferences sharedPref;
    private final CardAdapter adapter;
    private Card selectedCard;

    public CardViewHolder(@NonNull View itemView, List<Card> cards, SharedPreferences sharedPref, CardAdapter adapter) {
        super(itemView);
        this.cardName = itemView.findViewById(R.id.card_name);
        this.cardBalance = itemView.findViewById(R.id.card_balance);
        this.selectedLabel = itemView.findViewById(R.id.selected_label);
        this.cards = cards;
        this.sharedPref = sharedPref;
        this.adapter = adapter;

        String selectedCardId = sharedPref.getString("selected_card_id", null);
        if (selectedCardId != null) {
            for (Card card : cards) {
                if (card.getId().equals(selectedCardId)) {
                    selectedCard = card;
                    break;
                }
            }
        }
    }

    public void bind(Card card) {
        cardName.setText(card.getName());
        cardBalance.setText("Баланс: " + card.getBalance() + " АР");

        String selectedCardId = sharedPref.getString("selected_card_id", "");
        boolean isSelected = card.getId().equals(selectedCardId);

        selectedLabel.setText("Для оплаты");
        selectedLabel.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        itemView.setOnClickListener(v -> {
            sharedPref.edit()
                    .putString("selected_card_id", card.getId())
                    .apply();

            adapter.notifyDataSetChanged();

            ((FragmentActivity)itemView.getContext())
                    .getSupportFragmentManager()
                    .popBackStack();
        });

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
            String selectedCardId = sharedPref.getString("selected_card_id", null);
            if (card.getId().equals(selectedCardId)) {
                sharedPref.edit().remove("selected_card_id").apply();
            }

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
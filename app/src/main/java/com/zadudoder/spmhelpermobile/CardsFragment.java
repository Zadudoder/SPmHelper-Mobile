package com.zadudoder.spmhelpermobile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CardsFragment extends Fragment {

    private RecyclerView cardsRecyclerView;
    private MaterialButton addCardButton;
    private CardAdapter cardAdapter;
    private SharedPreferences sharedPref;
    private ProgressBar progressBar;
    private OkHttpClient httpClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpClient = new OkHttpClient();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cards, container, false);

        cardsRecyclerView = view.findViewById(R.id.cards_recyclerView);
        addCardButton = view.findViewById(R.id.add_card_button);
        progressBar = view.findViewById(R.id.progressBar);

        sharedPref = requireActivity().getSharedPreferences("SPWorldsPref", Context.MODE_PRIVATE);

        return view;
    }

    public void updateSelectedCard() {
        if (cardAdapter != null) {
            cardAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cardAdapter = new CardAdapter(new ArrayList<>(), sharedPref);
        cardsRecyclerView.setAdapter(cardAdapter);


        loadSavedCards();

        addCardButton.setOnClickListener(v -> showAddCardDialog());
    }

    private void loadSavedCards() {
        String cardsJson = sharedPref.getString("saved_cards", "[]");
        List<Card> cards = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(cardsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                cards.add(new Card(
                        jsonObject.getString("id"),
                        jsonObject.getString("token"),
                        jsonObject.getString("name"),
                        jsonObject.getString("number"),
                        jsonObject.getInt("balance")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        cardAdapter.updateCards(cards);
    }

    private void showAddCardDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_card, null);
        EditText cardIdEditText = dialogView.findViewById(R.id.card_id_edit_text);
        EditText cardTokenEditText = dialogView.findViewById(R.id.card_token_edit_text);

        new AlertDialog.Builder(getContext())
                .setTitle("Добавить карту")
                .setView(dialogView)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String cardId = cardIdEditText.getText().toString().trim();
                    String cardToken = cardTokenEditText.getText().toString().trim();

                    if (cardId.isEmpty() || cardToken.isEmpty()) {
                        Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                    } else {
                        verifyAndAddCard(cardId, cardToken);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void setCards(List<Card> cards) {
        cardAdapter.updateCards(cards);
    }

    private void verifyAndAddCard(String cardId, String cardToken) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String authString = cardId + ":" + cardToken;
                String encodedAuth = android.util.Base64.encodeToString(
                        authString.getBytes(),
                        android.util.Base64.NO_WRAP
                );
                String authHeader = "Bearer " + encodedAuth;

                // 1. Получаем информацию о карте
                Request cardRequest = new Request.Builder()
                        .url("https://spworlds.ru/api/public/card")
                        .addHeader("Authorization", authHeader)
                        .build();

                Response cardResponse = httpClient.newCall(cardRequest).execute();

                if (!cardResponse.isSuccessful()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Неверные данные карты", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 2. Получаем информацию об аккаунте (для названия карты и номера)
                Request accountRequest = new Request.Builder()
                        .url("https://spworlds.ru/api/public/accounts/me")
                        .addHeader("Authorization", authHeader)
                        .build();

                Response accountResponse = httpClient.newCall(accountRequest).execute();

                String cardName = "Карта";
                String cardNumber = "неизвестен";
                int balance = 0;

                if (accountResponse.isSuccessful()) {
                    String responseBody = accountResponse.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray cardsArray = jsonResponse.getJSONArray("cards");

                    // Ищем текущую карту в списке
                    for (int i = 0; i < cardsArray.length(); i++) {
                        JSONObject card = cardsArray.getJSONObject(i);
                        if (card.getString("id").equals(cardId)) {
                            cardName = card.getString("name"); // Получаем настоящее название
                            cardNumber = card.getString("number");
                            break;
                        }
                    }

                    // Получаем баланс
                    JSONObject cardJson = new JSONObject(cardResponse.body().string());
                    balance = cardJson.getInt("balance");
                }

                // 3. Формируем полное название для отображения
                String displayName = cardName + " (" + cardNumber + ")";
                Card newCard = new Card(cardId, cardToken, displayName, cardNumber, balance);

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    saveCard(newCard);
                    Toast.makeText(getContext(), "Карта успешно добавлена", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveCard(Card newCard) {
        List<Card> currentCards = cardAdapter.getCards();
        currentCards.add(newCard);

        JSONArray jsonArray = new JSONArray();
        for (Card card : currentCards) {
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

        cardAdapter.updateCards(currentCards);
    }
}
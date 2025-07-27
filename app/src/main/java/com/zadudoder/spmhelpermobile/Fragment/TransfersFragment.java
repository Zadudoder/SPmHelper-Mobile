package com.zadudoder.spmhelpermobile.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zadudoder.spmhelpermobile.Card;
import com.zadudoder.spmhelpermobile.R;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TransfersFragment extends Fragment {

    private TextInputEditText receiverEditText;
    private TextInputEditText amountEditText;
    private TextInputEditText commentEditText;
    private TextInputEditText selectedCardEditText;
    private TextInputLayout receiverInputLayout;
    private TextInputLayout amountInputLayout;
    private TextInputLayout commentInputLayout;
    private TextInputLayout cardSelectionLayout;
    private Button transferButton;
    private ProgressBar progressBar;
    private TextView selectedCardInfo;
    private RadioGroup transferTypeGroup;
    private RadioButton transferByCard;
    private RadioButton transferByNickname;
    private OkHttpClient httpClient;
    private SharedPreferences sharedPref;
    private String selectedReceiverCardNumber;
    private List<Card> receiverCards = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        sharedPref = requireActivity().getSharedPreferences("SPWorldsPref", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        transferTypeGroup = view.findViewById(R.id.transferTypeGroup);
        transferByCard = view.findViewById(R.id.transferByCard);
        transferByNickname = view.findViewById(R.id.transferByNickname);
        receiverEditText = view.findViewById(R.id.receiver_edit_text);
        amountEditText = view.findViewById(R.id.amount_edit_text);
        commentEditText = view.findViewById(R.id.comment_edit_text);
        selectedCardEditText = view.findViewById(R.id.selected_card_edit_text);
        receiverInputLayout = view.findViewById(R.id.receiver_input_layout);
        cardSelectionLayout = view.findViewById(R.id.card_selection_layout);
        amountInputLayout = view.findViewById(R.id.amount_input_layout);
        commentInputLayout = view.findViewById(R.id.comment_input_layout);
        transferButton = view.findViewById(R.id.transfer_button);
        progressBar = view.findViewById(R.id.progressBar);
        selectedCardInfo = view.findViewById(R.id.selected_card_info);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSelectedCardInfo();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateSelectedCardInfo();

        if (transferByCard.isChecked()) {
            receiverInputLayout.setHint("Номер карты получателя");
            cardSelectionLayout.setVisibility(View.GONE);
        } else {
            receiverInputLayout.setHint("Никнейм игрока");
            cardSelectionLayout.setVisibility(View.VISIBLE);
        }

        transferTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.transferByCard) {
                receiverInputLayout.setHint("Номер карты получателя");
                cardSelectionLayout.setVisibility(View.GONE);
                selectedCardEditText.setText("");
                selectedReceiverCardNumber = null;
            } else {
                receiverInputLayout.setHint("Никнейм игрока");
                cardSelectionLayout.setVisibility(View.VISIBLE);
            }
        });

        selectedCardEditText.setOnClickListener(v -> {
            String username = receiverEditText.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(getContext(), "Введите никнейм игрока", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchUserCards(username);
        });

        transferButton.setOnClickListener(v -> {
            String receiver = receiverEditText.getText().toString().trim();
            String amountStr = amountEditText.getText().toString().trim();
            String comment = commentEditText.getText().toString().trim();

            if (comment.isEmpty()) {
                comment = " ";
            }

            if (comment.length() > 32) {
                Toast.makeText(getContext(), "Комментарий не больше 32 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            if (receiver.isEmpty()) {
                receiverInputLayout.setError("Заполните поле");
                return;
            } else {
                receiverInputLayout.setError(null);
            }

            if (amountStr.isEmpty()) {
                amountInputLayout.setError("Введите сумму");
                return;
            } else {
                amountInputLayout.setError(null);
            }

            try {
                int amount = Integer.parseInt(amountStr);
                if (amount <= 0) {
                    amountInputLayout.setError("Сумма должна быть больше 0");
                    return;
                }

                if (transferByCard.isChecked()) {
                    makeTransfer(receiver, amount, comment);
                } else {
                    if (selectedReceiverCardNumber == null || selectedReceiverCardNumber.isEmpty()) {
                        Toast.makeText(getContext(), "Выберите карту получателя", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    makeTransfer(selectedReceiverCardNumber, amount, comment);
                }
            } catch (NumberFormatException e) {
                amountInputLayout.setError("Введите корректную сумму");
            }
        });
    }

    private void fetchUserCards(String username) {
        progressBar.setVisibility(View.VISIBLE);
        transferButton.setEnabled(false);

        new Thread(() -> {
            try {
                String cardsJson = sharedPref.getString("saved_cards", "[]");
                List<Card> cards = Card.fromJsonArray(cardsJson);

                if (cards.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(), "Нет доступных карт для авторизации", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Card senderCard = cards.get(0);
                String authHeader = getAuthHeader(senderCard);

                Request request = new Request.Builder()
                        .url("https://spworlds.ru/api/public/accounts/" + username + "/cards")
                        .addHeader("Authorization", authHeader)
                        .build();

                Response response = httpClient.newCall(request).execute();

                if (!response.isSuccessful()) {
                    final String errorMessage;
                    if (response.code() == 404) {
                        errorMessage = "Игрок не найден";
                    } else if (response.code() == 401) {
                        errorMessage = "Ошибка авторизации";
                    } else {
                        errorMessage = "Ошибка: " + response.code();
                    }

                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String responseBody = response.body().string();
                JSONArray cardsArray = new JSONArray(responseBody);

                if (cardsArray.length() == 0) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(), "У игрока нет доступных карт", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                receiverCards.clear();
                for (int i = 0; i < cardsArray.length(); i++) {
                    JSONObject cardJson = cardsArray.getJSONObject(i);
                    receiverCards.add(new Card(
                            "",
                            "",
                            cardJson.getString("name"),
                            cardJson.getString("number"),
                            0
                    ));
                }

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    transferButton.setEnabled(true);
                    showCardSelectionDialog();
                });

            } catch (JSONException e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    transferButton.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка обработки данных", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    transferButton.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showCardSelectionDialog() {
        if (receiverCards.isEmpty()) {
            Toast.makeText(getContext(), "У игрока нет доступных карт", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] cardNames = new String[receiverCards.size()];
        for (int i = 0; i < receiverCards.size(); i++) {
            cardNames[i] = receiverCards.get(i).getName() + " (" + receiverCards.get(i).getNumber() + ")";
        }

        AlertDialog SelectCard = new AlertDialog.Builder(requireContext())
                .setTitle("Выберите карту")
                .setItems(cardNames, (dialog, which) -> {
                    selectedReceiverCardNumber = receiverCards.get(which).getNumber();
                    selectedCardEditText.setText(cardNames[which]);
                })
                .setNegativeButton("Отмена", null)
                .create();
        if (SelectCard.getWindow() != null) {
            SelectCard.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_bg);
        }
        SelectCard.show();
    }

    private void makeTransfer(String receiver, int amount, String comment) {
        progressBar.setVisibility(View.VISIBLE);
        transferButton.setEnabled(false);

        new Thread(() -> {
            try {
                if (amount <= 0 || amount > 10000) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        amountInputLayout.setError(amount <= 0 ?
                                "Сумма должна быть больше 0" :
                                "Максимальная сумма - 10000 АР");
                    });
                    return;
                }

                String cardsJson = sharedPref.getString("saved_cards", "[]");
                List<Card> cards = Card.fromJsonArray(cardsJson);

                if (cards.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(), "Нет доступных карт", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Card senderCard = getSelectedCard(cards);
                if (senderCard == null) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(), "Не выбрана карта для перевода", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String authString = senderCard.getId() + ":" + senderCard.getToken();
                String encodedAuth = android.util.Base64.encodeToString(
                        authString.getBytes(),
                        android.util.Base64.NO_WRAP
                );
                String authHeader = "Bearer " + encodedAuth;

                String targetReceiver = receiver;
                if (!receiver.matches("\\d+")) {
                    targetReceiver = getCardNumberByUsername(receiver, authHeader);
                    if (targetReceiver == null) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            transferButton.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Не удалось найти карту игрока",
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }

                if (senderCard.getNumber().equals(targetReceiver)) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Нельзя перевести на ту же карту",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                JSONObject transferJson = new JSONObject();
                transferJson.put("receiver", targetReceiver);
                transferJson.put("amount", amount);
                transferJson.put("comment", comment);

                RequestBody body = RequestBody.create(
                        transferJson.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://spworlds.ru/api/public/transactions")
                        .addHeader("Authorization", authHeader)
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();

                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e("API_ERROR", "Code: " + response.code() + ", Body: " + errorBody);

                    requireActivity().runOnUiThread(() -> {
                        String errorMsg = "Ошибка " + response.code();
                        try {
                            JSONObject errorJson = new JSONObject(errorBody);
                            if (errorJson.has("error")) {
                                errorMsg += ": " + errorJson.getString("error");
                            }
                        } catch (JSONException ignored) {}

                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    });
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                int newBalance = jsonResponse.getInt("balance");
                updateCardBalance(senderCard.getId(), newBalance);

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    transferButton.setEnabled(true);
                    updateSelectedCardInfo();
                    Toast.makeText(getContext(),
                            "Перевод успешен! Новый баланс: " + newBalance + " АР",
                            Toast.LENGTH_LONG).show();

                    receiverEditText.setText("");
                    amountEditText.setText("");
                    commentEditText.setText("");
                    selectedCardEditText.setText("");
                    selectedReceiverCardNumber = null;
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    transferButton.setEnabled(true);
                    Toast.makeText(getContext(),
                            "Ошибка: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String getErrorMessage(int code) {
        switch (code) {
            case 400: return "Неверные параметры запроса";
            case 401: return "Ошибка авторизации - неверный ID или токен карты";
            case 402: return "Недостаточно средств";
            case 404: return "Карта получателя не найдена";
            case 442: return "Ошибка аутентификации. Проверьте ID и токен карты";
            default: return "Ошибка перевода: " + code;
        }
    }

    public void updateSelectedCardInfo() {
        try {
            String cardsJson = sharedPref.getString("saved_cards", "[]");
            List<Card> cards = Card.fromJsonArray(cardsJson);

            if (cards.isEmpty()) {
                selectedCardInfo.setText("Нет доступных карт");
                return;
            }

            String selectedCardId = sharedPref.getString("selected_card_id", null);
            Card selectedCard = null;

            if (selectedCardId != null) {
                for (Card card : cards) {
                    if (card.getId().equals(selectedCardId)) {
                        selectedCard = card;
                        break;
                    }
                }
            }

            if (selectedCard == null) {
                selectedCard = cards.get(0);
                sharedPref.edit()
                        .putString("selected_card_id", selectedCard.getId())
                        .apply();
            }

            selectedCardInfo.setText(
                    "Перевод с: " + selectedCard.getName() + "\n" +
                            "Баланс: " + selectedCard.getBalance() + " АР"
            );

        } catch (JSONException e) {
            selectedCardInfo.setText("Ошибка загрузки карт");
        }
    }

    private void updateCardBalance(String cardId, int newBalance) throws JSONException {
        String cardsJson = sharedPref.getString("saved_cards", "[]");
        JSONArray cardsArray = new JSONArray(cardsJson);

        for (int i = 0; i < cardsArray.length(); i++) {
            JSONObject card = cardsArray.getJSONObject(i);
            if (card.getString("id").equals(cardId)) {
                card.put("balance", newBalance);
                break;
            }
        }

        sharedPref.edit()
                .putString("saved_cards", cardsArray.toString())
                .apply();
    }

    private String getAuthHeader(Card card) {
        String cleanToken = card.getToken().replaceAll("\\s+", "");
        String authString = card.getId() + ":" + cleanToken;

        String encodedAuth = android.util.Base64.encodeToString(
                authString.getBytes(),
                android.util.Base64.NO_WRAP
        );
        return "Bearer " + encodedAuth.trim();
    }


    private Card getSelectedCard(List<Card> cards) {
        String selectedCardId = sharedPref.getString("selected_card_id", null);
        if (selectedCardId != null) {
            for (Card card : cards) {
                if (card.getId().equals(selectedCardId)) {
                    return card;
                }
            }
        }
        return null;
    }

    private int getCardBalance(Card card, String authHeader) throws Exception {
        Request request = new Request.Builder()
                .url("https://spworlds.ru/api/public/card")
                .addHeader("Authorization", authHeader)
                .build();

        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            return -1;
        }

        String responseBody = response.body().string();
        JSONObject jsonResponse = new JSONObject(responseBody);
        return jsonResponse.getInt("balance");
    }

    private String getCardNumberByUsername(String username, String authHeader) throws Exception {
        Request userRequest = new Request.Builder()
                .url("https://spworlds.ru/api/public/users/" + username)
                .addHeader("Authorization", authHeader)
                .build();

        Response userResponse = httpClient.newCall(userRequest).execute();
        if (!userResponse.isSuccessful()) {
            return null;
        }

        Request cardsRequest = new Request.Builder()
                .url("https://spworlds.ru/api/public/accounts/" + username + "/cards")
                .addHeader("Authorization", authHeader)
                .build();

        Response cardsResponse = httpClient.newCall(cardsRequest).execute();
        if (!cardsResponse.isSuccessful()) {
            return null;
        }

        String responseBody = cardsResponse.body().string();
        JSONArray cardsArray = new JSONArray(responseBody);
        if (cardsArray.length() == 0) {
            return null;
        }

        return cardsArray.getJSONObject(0).getString("number");
    }
}
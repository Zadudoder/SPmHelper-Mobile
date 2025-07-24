package com.zadudoder.spmhelpermobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

public class TransfersFragment extends Fragment {

    private TextInputEditText receiverEditText;
    private TextInputEditText amountEditText;
    private TextInputEditText commentEditText;
    private TextInputLayout receiverInputLayout;
    private TextInputLayout amountInputLayout;
    private TextInputLayout commentInputLayout;
    private Button transferButton;
    private ProgressBar progressBar;
    private TextView selectedCardInfo;
    private OkHttpClient httpClient;
    private SharedPreferences sharedPref;

    private TextInputEditText selectedCardEditText;
    private TextInputLayout cardSelectionLayout;
    private RadioGroup transferTypeGroup;
    private RadioButton transferByCard;
    private RadioButton transferByNickname;

    private String selectedReceiverCardNumber;
    private List<Card> receiverCards = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        httpClient = new OkHttpClient();
        sharedPref = requireActivity().getSharedPreferences("SPWorldsPref", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        // Инициализация всех view элементов
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
        transferButton = view.findViewById(R.id.transfer_button);
        progressBar = view.findViewById(R.id.progressBar);
        selectedCardInfo = view.findViewById(R.id.selected_card_info);

        return view;
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

        // Обработчик изменения типа перевода
        transferTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.transferByCard) {
                receiverInputLayout.setHint("Номер карты получателя");
                cardSelectionLayout.setVisibility(View.GONE);
            } else {
                receiverInputLayout.setHint("Никнейм игрока");
                cardSelectionLayout.setVisibility(View.VISIBLE);
            }
        });

        // Обработчик клика по полю выбора карты
        selectedCardEditText.setOnClickListener(v -> {
            String username = receiverEditText.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(getContext(), "Введите никнейм игрока", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchUserCards(username);
        });

        // Обработчик кнопки перевода
        transferButton.setOnClickListener(v -> {
            String receiver = receiverEditText.getText().toString().trim();
            String amountStr = amountEditText.getText().toString().trim();
            String comment = commentEditText.getText().toString().trim();

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

                // Определяем способ перевода
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
                        Toast.makeText(getContext(), "Нет доступных карт для перевода", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Card senderCard = cards.get(0); // Берем первую карту для авторизации
                String authString = senderCard.getId() + ":" + senderCard.getToken();
                String encodedAuth = android.util.Base64.encodeToString(
                        authString.getBytes(),
                        android.util.Base64.NO_WRAP
                );
                String authHeader = "Bearer " + encodedAuth;

                // Получаем карты пользователя
                Request request = new Request.Builder()
                        .url("https://spworlds.ru/api/public/accounts/" + username + "/cards")
                        .addHeader("Authorization", authHeader)
                        .build();

                Response response = httpClient.newCall(request).execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONArray cardsArray = new JSONArray(responseBody);

                    receiverCards.clear();
                    for (int i = 0; i < cardsArray.length(); i++) {
                        JSONObject cardJson = cardsArray.getJSONObject(i);
                        receiverCards.add(new Card(
                                "", // id не нужен
                                "", // token не нужен
                                cardJson.getString("name"),
                                cardJson.getString("number"),
                                0 // баланс не нужен
                        ));
                    }

                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        showCardSelectionDialog();
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Не удалось получить карты игрока",
                                Toast.LENGTH_SHORT).show();
                    });
                }
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

    private void showCardSelectionDialog() {
        if (receiverCards.isEmpty()) {
            Toast.makeText(getContext(), "У игрока нет доступных карт", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] cardNames = new String[receiverCards.size()];
        for (int i = 0; i < receiverCards.size(); i++) {
            cardNames[i] = receiverCards.get(i).getName() + " (" + receiverCards.get(i).getNumber() + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите карту")
                .setItems(cardNames, (dialog, which) -> {
                    selectedReceiverCardNumber = receiverCards.get(which).getNumber();
                    selectedCardEditText.setText(cardNames[which]);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void updateSelectedCardInfo() {
        try {
            String cardsJson = sharedPref.getString("saved_cards", "[]");
            List<Card> cards = Card.fromJsonArray(cardsJson);

            if (cards.isEmpty()) {
                selectedCardInfo.setText("Нет доступных карт");
                return;
            }

            // Всегда выбираем первую карту, если не выбрана другая
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
            e.printStackTrace();
        }
    }

    private void makeTransfer(String receiver, int amount, String comment) {
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
                        Toast.makeText(getContext(), "Нет доступных карт", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Получаем выбранную карту или первую из списка
                Card senderCard = null;
                String selectedCardId = sharedPref.getString("selected_card_id", null);

                if (selectedCardId != null) {
                    for (Card card : cards) {
                        if (card.getId().equals(selectedCardId)) {
                            senderCard = card;
                            break;
                        }
                    }
                }

                if (senderCard == null) {
                    senderCard = cards.get(0);
                }

                // Проверка суммы
                if (amount == 0) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        amountInputLayout.setError("Сумма не может быть равна 0");
                    });
                    return;
                }

                if (amount > 10000) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        amountInputLayout.setError("Максимальная сумма перевода - 10000 АР");
                    });
                    return;
                }

                String authString = senderCard.getId() + ":" + senderCard.getToken();
                String encodedAuth = android.util.Base64.encodeToString(
                        authString.getBytes(),
                        android.util.Base64.NO_WRAP
                );
                String authHeader = "Bearer " + encodedAuth;

                // Если получатель - никнейм, получаем номер его карты
                String targetReceiver = receiver;
                if (!receiver.matches("\\d+")) {
                    targetReceiver = getCardNumberByUsername(receiver, authHeader);
                    if (targetReceiver == null) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            transferButton.setEnabled(true);
                            Toast.makeText(getContext(),
                                    "Не удалось найти карту пользователя " + receiver,
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }

                // Проверка на совпадение карт
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

                // Формируем запрос на перевод
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

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    int newBalance = jsonResponse.getInt("balance");

                    // Обновляем баланс карты
                    updateCardBalance(senderCard.getId(), newBalance);

                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        updateSelectedCardInfo();
                        Toast.makeText(getContext(),
                                "Перевод успешен!\nНовый баланс: " + newBalance + " АР",
                                Toast.LENGTH_LONG).show();

                        // Очищаем поля
                        receiverEditText.setText("");
                        amountEditText.setText("");
                        commentEditText.setText("");
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        transferButton.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Ошибка перевода: " + response.message(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
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

    private String getCardNumberByUsername(String username, String authHeader) throws Exception {
        // Проверяем существование пользователя
        Request userRequest = new Request.Builder()
                .url("https://spworlds.ru/api/public/users/" + username)
                .addHeader("Authorization", authHeader)
                .build();

        Response userResponse = httpClient.newCall(userRequest).execute();

        if (!userResponse.isSuccessful()) {
            return null;
        }

        // Получаем карты пользователя
        Request cardsRequest = new Request.Builder()
                .url("https://spworlds.ru/api/public/accounts/" + username + "/cards")
                .addHeader("Authorization", authHeader)
                .build();

        Response cardsResponse = httpClient.newCall(cardsRequest).execute();

        if (cardsResponse.isSuccessful()) {
            String responseBody = cardsResponse.body().string();
            JSONArray cardsArray = new JSONArray(responseBody);

            if (cardsArray.length() > 0) {
                // Для простоты берем первую карту
                return cardsArray.getJSONObject(0).getString("number");
            }
        }

        return null;
    }
}
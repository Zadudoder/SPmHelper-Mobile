package com.zadudoder.spmhelpermobile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TransfersFragment extends Fragment {

    private EditText amountEditText;
    private EditText nicknameEditText;
    private EditText commentEditText;
    private Button transferButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        amountEditText = view.findViewById(R.id.editTextText3);
        nicknameEditText = view.findViewById(R.id.editTextText);
        commentEditText = view.findViewById(R.id.editTextText2);
        transferButton = view.findViewById(R.id.button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        transferButton.setOnClickListener(v -> {
            String amount = amountEditText.getText().toString();
            String nickname = nicknameEditText.getText().toString();
            String comment = commentEditText.getText().toString();

            if (!amount.isEmpty() && !nickname.isEmpty()) {
                try {
                    makeTransfer(Integer.parseInt(amount), nickname, comment);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Введите корректную сумму", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Заполните сумму и никнейм", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeTransfer(int amount, String nickname, String comment) {
        // Здесь будет реализация перевода
        Toast.makeText(getContext(), "Перевод " + amount + " АР игроку " + nickname, Toast.LENGTH_SHORT).show();
    }
}
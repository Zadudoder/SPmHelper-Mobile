package com.zadudoder.spmhelpermobile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Card {
    private String id;
    private String token;
    private String name;
    private String number;
    private int balance;

    public Card(String id, String token, String name, String number, int balance) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.number = number;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getToken() { return token; }
    public String getName() { return name; }
    public String getNumber() { return number; }
    public int getBalance() { return balance; }

    public static List<Card> fromJsonArray(String json) throws JSONException {
        List<Card> cards = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(json);

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
        return cards;
    }

    public String getShortNumber() {
        return number;
    }

    public String getDisplayName() {
        return name + " (" + getShortNumber() + ")";
    }
}
package com.zadudoder.spmhelpermobile;

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

    // Геттеры
    public String getId() { return id; }
    public String getToken() { return token; }
    public String getName() { return name; }
    public String getNumber() { return number; }
    public int getBalance() { return balance; }
}
package com.example.asystent_ekologiczny;

public class Product {
    private long id;
    private String name;
    private double price;
    private String expirationDate; // yyyy-MM-dd
    private String category;
    private String description;
    private String store;
    private String purchaseDate; // yyyy-MM-dd

    public Product(long id, String name, double price, String expirationDate, String category, String description, String store, String purchaseDate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.expirationDate = expirationDate;
        this.category = category;
        this.description = description;
        this.store = store;
        this.purchaseDate = purchaseDate;
    }

    public Product(String name, double price, String expirationDate, String category, String description, String store, String purchaseDate) {
        this(-1, name, price, expirationDate, category, description, store, purchaseDate);
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getExpirationDate() { return expirationDate; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getStore() { return store; }
    public String getPurchaseDate() { return purchaseDate; }
}


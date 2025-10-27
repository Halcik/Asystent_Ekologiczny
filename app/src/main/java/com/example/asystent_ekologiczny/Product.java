package com.example.asystent_ekologiczny;

/**
 * Model prostego produktu przechowywanego w lokalnej bazie SQLite.
 * id == -1 oznacza jeszcze nie zapisany rekord.
 */
public class Product {
    /** Klucz główny (autoincrement w DB). */
    private final long id;
    /** Nazwa produktu. */
    private final String name;
    /** Cena w zł */
    private final double price;
    /** Data ważności . Format yyyy-MM-dd. */
    private final String expirationDate;
    /** Kategoria tekstowa (użytkownik wpisuje lub wybiera z listy). */
    private final String category;
    /** Opis produktu. */
    private final String description;
    /** Sklep (źródło zakupu). */
    private final String store;
    /** Data zakupu w formacie yyyy-MM-dd. */
    private final String purchaseDate;

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

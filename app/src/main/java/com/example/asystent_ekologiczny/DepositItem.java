package com.example.asystent_ekologiczny;

/**
 * Model pojedynczego opakowania kaucyjnego.
 * id == -1 oznacza rekord jeszcze nie zapisany.
 */
public class DepositItem {
    private final long id;
    private final String category; // kategoria opakowania (np. Butelka, Puszka, Inne) wybierana z listy
    private final String name; // nazwa opakowania (np. "Butelka szklana 0,5L")
    private final double value; // wartość kaucji w zł
    private final String barcode; // kod kreskowy (opcjonalnie)
    private final boolean returned; // czy opakowanie zostało zwrócone

    public DepositItem(long id, String category, String name, double value, String barcode, boolean returned) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.value = value;
        this.barcode = barcode;
        this.returned = returned;
    }
    public DepositItem(String category, String name, double value, String barcode) { this(-1, category, name, value, barcode, false); }

    public long getId() { return id; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public double getValue() { return value; }
    public String getBarcode() { return barcode; }
    public boolean isReturned() { return returned; }
}

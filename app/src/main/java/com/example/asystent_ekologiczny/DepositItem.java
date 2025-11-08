package com.example.asystent_ekologiczny;

/**
 * Model pojedynczego opakowania kaucyjnego.
 * id == -1 oznacza rekord jeszcze nie zapisany.
 */
public class DepositItem {
    private final long id;
    private final String type; // typ opakowania (np. Butelka szklana 0,5L)
    private final double value; // wartość kaucji w zł
    private final String barcode; // kod kreskowy (opcjonalnie)

    public DepositItem(long id, String type, double value, String barcode) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.barcode = barcode;
    }
    public DepositItem(String type, double value, String barcode) { this(-1, type, value, barcode); }

    public long getId() { return id; }
    public String getType() { return type; }
    public double getValue() { return value; }
    public String getBarcode() { return barcode; }
}


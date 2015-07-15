package com.rsb.bubblepop;

public interface PurchaseHelper {
    void BeginPurchase(String itemName, Double price);
    void CompletePurchase(int quantity);
    void CancelPurchase();
}

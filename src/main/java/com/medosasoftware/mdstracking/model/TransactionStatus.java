package com.medosasoftware.mdstracking.model;

public enum TransactionStatus {
    PENDING,      // Başlangıç durumu
    IN_PROGRESS,  // İşlem devam ediyor
    COMPLETED,    // Tamamlandı
    CANCELLED     // İptal edildi
}
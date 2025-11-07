package com.gcodes.aacctracker.model;

public enum ResetRequestStatus {
    PENDING,    // Bekliyor
    APPROVED,   // Onaylandı (token oluşturuldu)
    REJECTED,   // Reddedildi
    COMPLETED   // Kullanıcı şifresini değiştirdi
}
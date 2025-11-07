package com.gcodes.aacctracker.model;

public enum GlobalRole {
    SUPER_ADMIN,    // Sistem sahibi (sen)
    BROKER_ADMIN,   // Gümrük firma yöneticisi - tam yetki
    BROKER_USER,    // Gümrük firma çalışanı - işlem yönetimi
    CLIENT_USER     // Müşteri firma kullanıcısı - sadece okuma (READ-ONLY)
}
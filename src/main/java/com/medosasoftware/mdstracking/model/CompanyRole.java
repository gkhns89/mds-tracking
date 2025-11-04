package com.medosasoftware.mdstracking.model;

public enum CompanyRole {
    COMPANY_ADMIN,   // Firma yöneticisi - firma içi tam yetki
    COMPANY_MANAGER, // Ara yetkili - kullanıcı yönetimi yapabilir
    COMPANY_USER     // Normal kullanıcı - sadece okuma
}
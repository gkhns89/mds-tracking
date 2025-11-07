# AACC Tracker

Gümrük acenteleri ve müşteri firmaları arasındaki iş süreçlerini ve yapılan anlaşmaları takip etmeye yönelik bir yönetim
sistemi.

## İçerik

- Projenin amacı ve genel yapısı
- Hızlı kurulum ve çalıştırma
- Kullanılan teknolojiler
- API yapısı – Tüm ana endpointler ve açıklamaları
- Roller ve yetkilendirme
- Ortak hatalar & çözümleri
- Nasıl katkı sağlanır?
- İletişim

---

## Projenin Amacı

AACC Tracker; gümrük işlemlerinin, acente anlaşmalarının, kullanıcı ve şirket yönetiminin merkezi şekilde takibini,
raporlanmasını ve denetlenmesini sağlar.

---

## Hızlı Kurulum

1. **Java 17+ gerekmektedir.**
2. **Veritabanı:** MySQL tercih edilmiştir, `application.local.properties` dosyasından bağlantıları yapılandırınız.
3. Projeyi klonlayın:
   ```
   git clone https://github.com/gkhns89/aacc-tracker.git
   ```
4. Gerekli dosyaları yükleyin:
   ```
   mvn clean install
   ```
5. Ortam değişkenlerini tanımlayın:
    - `DB_URL`, `DB_USER`, `DB_PASS`
    - (Varsa) SMTP ayarları, admin email, ihtiyaca göre diğer değişkenler.
6. Uygulamayı başlatın:
   ```
   mvn spring-boot:run
   ```
7. İlk kurulumda süper admin oluşturmak için:
   ```
   POST /api/setup/create-super-admin
   {
     "email": "admin@admin.com",
     "username": "admin",
     "password": "sifre"
   }
   ```

---

## Kullanılan Teknolojiler

- **Backend Framework:** Spring Boot
- **Dil:** Java
- **ORM:** Hibernate/JPA
- **Veri Tabanı:** MySQL
- **Authentication:** JWT + Rol tabanlı yetkilendirme (SUPER_ADMIN, BROKER, CLIENT vs.)

---

## API Endpoint Yapısı

Ana endpointler aşağıda özetlenmiştir. Detaylar için [Postman koleksiyonu](#) dosyasına bakınız.

### Genel Test ve Sağlık

| Metod | Endpoint         | Açıklama            |
|-------|------------------|---------------------|
| GET   | `/api/health`    | Servis çalışıyor mu |
| GET   | `/api/cors-test` | CORS kontrolü       |

### Setup (İlk kurulum/süper admin)

| Metod | Endpoint                        | Açıklama                              |
|-------|---------------------------------|---------------------------------------|
| POST  | `/api/setup/create-super-admin` | İlk süper admin kullanıcıyı oluşturur |
| GET   | `/api/setup/status`             | Setup durumunu döndürür               |

### Kullanıcı İşlemleri

| Metod  | Endpoint                                              | Açıklama                             |
|--------|-------------------------------------------------------|--------------------------------------|
| POST   | `/api/users/{userId}/assign-role`                     | Kullanıcıya rol atama                |
| DELETE | `/api/users/{userId}/remove-from-company/{companyId}` | Şirketten kullanıcı çıkarma          |
| GET    | `/api/users/by-email/{email}`                         | Email ile kullanıcı sorgula          |
| GET    | `/api/users/{id}`                                     | ID ile kullanıcı getirme             |
| GET    | `/api/users/my-companies`                             | Kullanıcının erişebileceği şirketler |
| GET    | `/api/users/manageable-companies`                     | Yönetebileceği şirketler             |
| GET    | `/api/users/company/{companyId}`                      | Şirkete bağlı kullanıcılar           |

### Anlaşma İşlemleri

| Metod | Endpoint                                      | Açıklama                          |
|-------|-----------------------------------------------|-----------------------------------|
| POST  | `/api/agreements`                             | Yeni anlaşma oluştur              |
| PUT   | `/api/agreements/{id}`                        | Anlaşma güncelle                  |
| POST  | `/api/agreements/{id}/suspend`                | Anlaşma Askıya Al (Suspend)       |
| POST  | `/api/agreements/{id}/terminate`              | Anlaşma Sonlandır                 |
| POST  | `/api/agreements/{id}/reactivate`             | Anlaşmayı yeniden aktifleştir     |
| GET   | `/api/agreements/{id}`                        | Tek anlaşmayı getir               |
| GET   | `/api/agreements/by-number/{agreementNumber}` | Anlaşma numarası ile getir        |
| GET   | `/api/agreements`                             | SUPER_ADMIN tüm anlaşmaları çeker |

### Gümrük İşlemleri

| Metod | Endpoint                                    | Açıklama                        |
|-------|---------------------------------------------|---------------------------------|
| POST  | `/api/transactions/{id}/complete`           | İşlemi tamamla                  |
| POST  | `/api/transactions/{id}/cancel`             | İşlemi iptal et                 |
| GET   | `/api/transactions/{id}`                    | Tek işlem bilgisi çek           |
| GET   | `/api/transactions/by-file-no/{fileNo}`     | Dosya numarasına göre işlem çek |
| GET   | `/api/transactions/broker/{brokerId}`       | Belirli brokerage işlemleri     |
| GET   | `/api/transactions/recent`                  | Son işlemleri getir             |
| GET   | `/api/transactions/stats/broker/{brokerId}` | Broker’a ait istatistikler      |

### Dashboard

| Metod | Endpoint                           | Açıklama                  |
|-------|------------------------------------|---------------------------|
| GET   | `/api/dashboard/stats`             | Dashboard istatistikleri  |
| GET   | `/api/dashboard/recent-activities` | Son aktiviteler           |
| GET   | `/api/dashboard/menu-items`        | Kullanıcıya uygun menüler |

---

## Roller ve Yetkilendirme (Security)

- **SUPER_ADMIN**: Şirket oluşturma, kullanıcı yönetimi, tüm anlaşmalara erişim vb.
- **BROKER/CLIENT**: Kendi işlemlerini ve anlaşmalarını görebilir.
- **JWT Authentication**: Her API çağrısında HTTP `Authorization: Bearer <JWT TOKEN>` zorunlu.

Public endpoints: `/api/auth/**`, `/api/setup/**`, `/api/health`, `/api/cors-test`
Diğer işlemler için authentication gerekmekte.

---

## Hatalar ve Genel Çözümler

- `401 Unauthorized`: Token eksik/hatalı, giriş yapmanız gerek.
- `403 Forbidden`: Yetkiniz yok, rolünüzü kontrol edin.
- `400 Bad Request`: Eksik/yanlış veri.
- Sunucu hatalarında sistem loglarından ve response mesajından bilgi alabilirsiniz.

---

## Katkı Sağlama

Pull request ile katkı için önce bir issue açın, kodunuz ve testler ile branch üzerinden ilerleyin.

---

## İletişim

Sorular için [github issues](https://github.com/gkhns89/aacc-tracker/issues) kullanabilir veya proje sahibi ile
iletişime geçebilirsiniz.

---

# Postman Koleksiyon Şablonu

Projenin ana endpointleri ve örnek istekler için aşağıdaki JSON veya Markdown şablonunu Postman’a import edebilirsiniz.

```
{
  "info": {
    "name": "AACC Tracker API",
    "_postman_id": "aacc-tracker-collection",
    "description": "AACC Tracker için API örnekleri ve şablonları.",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/health",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "health"]
        }
      }
    },
    {
      "name": "Create Super Admin",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"admin@domain.com\",\n  \"username\": \"admin\",\n  \"password\": \"sifre\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/setup/create-super-admin",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "setup", "create-super-admin"]
        }
      }
    },
    {
      "name": "Get Agreements (SUPER_ADMIN)",
      "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer <JWT_TOKEN>"}],
        "url": {
          "raw": "http://localhost:8080/api/agreements",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "agreements"]
        }
      }
    },
    {
      "name": "Create Agreement",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"},
          {"key": "Authorization", "value": "Bearer <JWT_TOKEN>"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"brokerId\": 1,\n  \"clientId\": 2,\n  \"agreementDetails\": \"Detaylar...\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/agreements",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "agreements"]
        }
      }
    },
    {
      "name": "Get Recent Transactions",
      "request": {
        "method": "GET",
        "header": [{"key": "Authorization", "value": "Bearer <JWT_TOKEN>"}],
        "url": {
          "raw": "http://localhost:8080/api/transactions/recent",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "transactions", "recent"]
        }
      }
    }
    // Diğer endpoint örnekleri benzer şekilde eklenebilir.
  ]
}
```

Koleksiyonu `.json` olarak kaydedip Postman'da import edebilirsin.

---

Daha gelişmiş örnekler, field açıklamaları ve hata senaryoları için projenin controller/service dosyalarına
bakabilirsiniz. API endpointlerini özelleştirmen ve koleksiyona eklemen mümkündür.

---

Herhangi özel bir alan, endpoint veya örnek isteği istiyorsan belirtmen yeterli, detay ekleyebilirim!
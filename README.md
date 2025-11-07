# AACC Tracker

**Gümrük Acenteleri ve Müşteri Firmaları Arasındaki İş Süreçlerini Takip Sistemi**

Gümrük işlemlerinin, acente anlaşmalarının, kullanıcı ve şirket yönetiminin merkezi şekilde takibini, raporlanmasını ve
denetlenmesini sağlayan profesyonel bir yönetim sistemi.

---

## 📋 İçindekiler

- [Özellikler](#-özellikler)
- [Teknolojiler](#-teknolojiler)
- [Hızlı Kurulum](#-hızlı-kurulum)
- [Yapılandırma](#-yapılandırma)
- [API Dokümantasyonu](#-api-dokümantasyonu)
- [Roller ve Yetkilendirme](#-roller-ve-yetkilendirme)
- [Veritabanı Yapısı](#-veritabanı-yapısı)
- [Postman Koleksiyonu](#-postman-koleksiyonu)
- [Güvenlik](#-güvenlik)
- [Hata Ayıklama](#-hata-ayıklama)
- [Katkıda Bulunma](#-katkıda-bulunma)
- [Lisans](#-lisans)

---

## ✨ Özellikler

### 🔐 Güvenlik ve Yetkilendirme

- JWT tabanlı authentication
- Rol bazlı erişim kontrolü (RBAC)
- Şirket bazlı yetkilendirme sistemi
- Şifre sıfırlama özelliği
- Audit log takibi

### 👥 Kullanıcı Yönetimi

- Kullanıcı oluşturma, güncelleme, silme (soft delete)
- Şirket bazlı rol atama (ADMIN, MANAGER, USER)
- Çoklu şirket desteği
- Kullanıcı profil yönetimi

### 🏢 Şirket Yönetimi

- Gümrük acentesi (CUSTOMS_BROKER) ve müşteri firma (CLIENT) ayrımı
- Şirket oluşturma, güncelleme, silme
- Aktif/pasif durum yönetimi
- Hiyerarşik şirket yapısı

### 📜 Anlaşma Yönetimi

- Broker-Client anlaşma oluşturma
- Anlaşma durumu takibi (ACTIVE, SUSPENDED, TERMINATED)
- Otomatik anlaşma numarası oluşturma
- Anlaşma istatistikleri ve raporlama

### 📦 Gümrük İşlemleri

- Detaylı işlem kaydı (16+ alan)
- Dosya numarası ile takip
- İşlem durumu yönetimi
- Gecikme takibi ve raporlama
- Tarih bazlı sorgulama

### 📊 Dashboard ve Raporlama

- Rol bazlı dashboard
- İstatistikler ve grafikler
- Son aktiviteler
- Performans metrikleri

---

## 🛠 Teknolojiler

### Backend

- **Framework:** Spring Boot 3.4.3
- **Language:** Java 21
- **Security:** Spring Security + JWT
- **ORM:** Hibernate/JPA
- **Database:** MySQL 8.x
- **Build Tool:** Maven

### Kütüphaneler

- **Lombok** - Boilerplate kod azaltma
- **JJWT** - JWT token işlemleri
- **Dotenv** - Environment variable yönetimi
- **Validation** - Input validasyon

---

## 🚀 Hızlı Kurulum

### Gereksinimler

- Java 17 veya üzeri
- MySQL 8.0 veya üzeri
- Maven 3.6 veya üzeri

### Adım 1: Projeyi İndirin

```bash
git clone https://github.com/gkhns89/aacc-tracker.git
cd aacc-tracker
```

### Adım 2: Veritabanını Hazırlayın

```sql
CREATE
DATABASE aacc_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Adım 3: Environment Değişkenlerini Ayarlayın

Proje kök dizininde `.env` dosyası oluşturun:

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=aacc_tracker
DB_USER=root
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-very-long-secret-key-at-least-256-bits
JWT_EXPIRATION=3600000

# Application Configuration
APP_PORT=8080

# Default Super Admin (İlk kurulum için)
APP_EMAIL=admin@admin.com
APP_USERNAME=admin
APP_PASSWORD=Admin123!
```

### Adım 4: Bağımlılıkları Yükleyin

```bash
mvn clean install
```

### Adım 5: Uygulamayı Başlatın

```bash
mvn spring-boot:run
```

veya

```bash
./mvnw spring-boot:run
```

### Adım 6: İlk Giriş

Uygulama otomatik olarak süper admin kullanıcısı oluşturacaktır:

- **Email:** `.env` dosyasındaki `APP_EMAIL`
- **Şifre:** `.env` dosyasındaki `APP_PASSWORD`

**⚠️ ÖNEMLİ:** İlk giriş sonrası varsayılan şifreyi mutlaka değiştirin!

---

## ⚙️ Yapılandırma

### Profiller

Uygulama iki farklı profille çalışabilir:

#### Local Development (Varsayılan)

```properties
spring.profiles.active=local
```

- Veritabanı auto-create
- SQL logları aktif
- CORS gevşek

#### Production

```properties
spring.profiles.active=prod
```

- Veritabanı validate
- SQL logları kapalı
- CORS sıkı

### CORS Ayarları

`application-local.properties` veya `application-prod.properties` dosyasında:

```properties
cors.allowed-origins=http://localhost:3000,http://localhost:4200
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
cors.allowed-headers=*
cors.allow-credentials=true
```

---

## 📚 API Dokümantasyonu

### Base URL

```
http://localhost:8080/api
```

### Authentication

Çoğu endpoint JWT token gerektirir:

```
Authorization: Bearer <your-jwt-token>
```

### Ana Endpoint Kategorileri

#### 🔧 Health & Setup

- `GET /health` - Servis durumu
- `GET /cors-test` - CORS kontrolü
- `GET /setup/status` - Setup durumu
- `POST /setup/create-super-admin` - İlk süper admin

#### 🔐 Authentication

- `POST /auth/register` - Kullanıcı kaydı
- `POST /auth/login` - Giriş (token alır)
- `POST /auth/forgot-password` - Şifre sıfırlama talebi
- `POST /auth/reset-password` - Şifre sıfırlama

#### 👥 User Management

- `POST /users/create` - Kullanıcı oluştur (SUPER_ADMIN)
- `GET /users/all` - Tüm kullanıcılar
- `GET /users/:id` - Kullanıcı detayı
- `PUT /users/:id` - Kullanıcı güncelle
- `DELETE /users/:id` - Kullanıcı sil
- `POST /users/:userId/assign-role` - Rol ata
- `DELETE /users/:userId/remove-from-company/:companyId` - Şirketten çıkar
- `GET /users/my-companies` - Erişilebilir şirketler
- `GET /users/manageable-companies` - Yönetilebilir şirketler
- `GET /users/company/:companyId` - Şirket kullanıcıları
- `GET /users/profile` - Mevcut kullanıcı profili

#### 🏢 Company Management

- `POST /companies/create` - Şirket oluştur (SUPER_ADMIN)
- `GET /companies` - Tüm şirketler
- `GET /companies/:id` - Şirket detayı
- `PUT /companies/:id` - Şirket güncelle
- `DELETE /companies/:id` - Şirket sil
- `PATCH /companies/:id/status` - Durum değiştir
- `GET /companies/my-companies` - Erişilebilir şirketler
- `GET /companies/manageable` - Yönetilebilir şirketler

#### 📜 Agreement Management

- `POST /agreements` - Anlaşma oluştur
- `GET /agreements` - Tüm anlaşmalar (SUPER_ADMIN)
- `GET /agreements/:id` - Anlaşma detayı
- `GET /agreements/by-number/:agreementNumber` - Numaraya göre
- `PUT /agreements/:id` - Anlaşma güncelle
- `POST /agreements/:id/suspend` - Anlaşmayı askıya al
- `POST /agreements/:id/terminate` - Anlaşmayı sonlandır
- `POST /agreements/:id/reactivate` - Anlaşmayı aktifleştir
- `GET /agreements/broker/:brokerId` - Broker anlaşmaları
- `GET /agreements/client/:clientId` - Client anlaşmaları
- `GET /agreements/check` - Aktif anlaşma kontrolü
- `GET /agreements/recent` - Son anlaşmalar
- `GET /agreements/stats/broker/:brokerId` - Broker istatistikleri
- `GET /agreements/stats/client/:clientId` - Client istatistikleri

#### 📦 Customs Transactions

- `POST /transactions` - İşlem oluştur
- `GET /transactions/:id` - İşlem detayı
- `GET /transactions/by-file-no/:fileNo` - Dosya numarasına göre
- `PUT /transactions/:id` - İşlem güncelle
- `PATCH /transactions/:id/status` - Durum güncelle
- `POST /transactions/:id/complete` - İşlemi tamamla
- `POST /transactions/:id/cancel` - İşlemi iptal et
- `GET /transactions/broker/:brokerId` - Broker işlemleri
- `GET /transactions/client/:clientId` - Client işlemleri
- `GET /transactions/delayed` - Gecikmeli işlemler
- `GET /transactions/date-range` - Tarih aralığı
- `GET /transactions/recent` - Son işlemler
- `GET /transactions/stats/broker/:brokerId` - Broker istatistikleri

#### 📊 Dashboard

- `GET /dashboard/stats` - Dashboard istatistikleri
- `GET /dashboard/recent-activities` - Son aktiviteler
- `GET /dashboard/menu-items` - Menü öğeleri

### Örnek İstekler

#### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@admin.com",
    "password": "Admin123!"
  }'
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### Şirket Oluşturma

```bash
curl -X POST http://localhost:8080/api/companies/create \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ABC Gümrük Müşavirliği",
    "description": "Profesyonel gümrük hizmetleri",
    "companyType": "CUSTOMS_BROKER",
    "isActive": true
  }'
```

#### İşlem Oluşturma

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "brokerCompanyId": 1,
    "clientCompanyId": 2,
    "fileNo": "FILE-2024-001",
    "recipientName": "ABC İthalat A.Ş.",
    "weight": 1500.50,
    "tax": 25000.00
  }'
```

---

## 🔒 Roller ve Yetkilendirme

### Global Roller

#### SUPER_ADMIN

- **Yetkileri:**
    - Tüm şirketleri görüntüleme ve yönetme
    - Kullanıcı oluşturma, silme
    - Sistem ayarlarını değiştirme
    - Tüm anlaşmaları ve işlemleri görüntüleme
    - Şirket durumlarını değiştirme

#### USER

- **Yetkileri:**
    - Atandığı şirketleri görüntüleme
    - Şirket rolüne göre işlem yapma

### Şirket Rolleri

#### COMPANY_ADMIN

- **Yetkileri:**
    - Şirket bilgilerini güncelleme
    - Kullanıcı ekleme/çıkarma
    - Rol atama (tüm roller)
    - Anlaşma yönetimi
    - İşlem oluşturma ve güncelleme

#### COMPANY_MANAGER

- **Yetkileri:**
    - Kullanıcı ekleme/çıkarma
    - Rol atama (sadece COMPANY_USER)
    - İşlem oluşturma ve güncelleme
    - Şirket bilgilerini görüntüleme

#### COMPANY_USER

- **Yetkileri:**
    - Şirket bilgilerini görüntüleme
    - İşlemleri görüntüleme (sadece okuma)
    - Kendi profilini güncelleme

### Özel Kurallar

#### Broker (CUSTOMS_BROKER) Şirketi

- ✅ Client şirketler ile anlaşma yapabilir
- ✅ İşlem oluşturabilir ve yönetebilir
- ✅ Müşteri istatistiklerini görebilir
- ❌ Başka broker'ın işlemlerini göremez

#### Client Şirketi

- ✅ Kendi işlemlerini görüntüleyebilir (READ ONLY)
- ✅ İstatistikleri görebilir
- ❌ İşlem oluşturamaz veya güncelleyemez
- ❌ Başka client'in işlemlerini göremez

---

## 🗄 Veritabanı Yapısı

### Ana Tablolar

#### users

```sql
- id (PK)
- email (UNIQUE)
- username (UNIQUE)
- password (hashed)
- global_role (SUPER_ADMIN, USER)
- is_active
- created_at
```

#### companies

```sql
- id (PK)
- name (UNIQUE)
- description
- company_type (CUSTOMS_BROKER, CLIENT)
- parent_broker_id (FK -> companies)
- is_active
- created_at
```

#### company_user_roles

```sql
- id (PK)
- user_id (FK -> users)
- company_id (FK -> companies)
- role (COMPANY_ADMIN, COMPANY_MANAGER, COMPANY_USER)
- assigned_by (FK -> users)
- assigned_at
```

#### agency_agreements

```sql
- id (PK)
- broker_company_id (FK -> companies)
- client_company_id (FK -> companies)
- created_by (FK -> users)
- status (ACTIVE, SUSPENDED, TERMINATED)
- agreement_number (UNIQUE)
- start_date
- end_date
- notes
- created_at
- updated_at
```

#### customs_transactions

```sql
- id (PK)
- broker_company_id (FK -> companies)
- client_company_id (FK -> companies)
- created_by_user_id (FK -> users)
- file_no (UNIQUE)
- recipient_name
- customs_warehouse
- gate
- weight
- tax
- sender_name
- warehouse_arrival_date
- registration_date
- declaration_number
- line_closure_date
- import_processing_time
- withdrawal_date
- description
- total_processing_time
- delay_reason
- status (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
- created_at
- updated_at
- last_modified_by
```

#### audit_logs

```sql
- id (PK)
- user_id (FK -> users)
- action
- entity_type
- entity_id
- timestamp
- change_details (JSON)
- ip_address
- result (SUCCESS, FAILURE)
- error_message
```

---

## 📮 Postman Koleksiyonu

Projenin tam Postman koleksiyonunu indirip kullanabilirsiniz.

### İçerik

- ✅ Tüm endpoint'ler
- ✅ Örnek request body'ler
- ✅ Otomatik token yönetimi
- ✅ Environment değişkenleri
- ✅ Test script'leri

### Kullanım

1. Postman'ı açın
2. `Import` butonuna tıklayın
3. Yukarıdaki JSON'u yapıştırın veya dosya olarak import edin
4. Collection Variables'da `base_url`'i ayarlayın
5. `Login` isteğini yapın (token otomatik kaydedilir)
6. Diğer endpointleri test edin

### Environment Variables

```json
{
  "base_url": "http://localhost:8080/api",
  "token": ""
  // Login sonrası otomatik dolar
}
```

---

## 🔐 Güvenlik

### Best Practices

1. **JWT Secret:** Üretimde mutlaka güçlü ve uzun bir secret kullanın (minimum 256 bit)
2. **Şifre Politikası:** Minimum 8 karakter, büyük/küçük harf, sayı ve özel karakter
3. **HTTPS:** Üretimde mutlaka HTTPS kullanın
4. **Rate Limiting:** API rate limiting ekleyin
5. **Input Validation:** Tüm inputları validate edin
6. **SQL Injection:** JPA kullandığı için otomatik korumalı
7. **XSS:** Spring Security otomatik korumalı

### Güvenlik Özellikleri

- ✅ JWT token authentication
- ✅ Password encryption (BCrypt)
- ✅ CORS protection
- ✅ CSRF protection (API için devre dışı)
- ✅ SQL injection protection (JPA)
- ✅ Role-based access control
- ✅ Audit logging
- ✅ Soft delete (veri kaybı önleme)

---

## 🐛 Hata Ayıklama

### Yaygın Hatalar ve Çözümler

#### 1. Veritabanı Bağlantı Hatası

```
Error: Could not connect to database
```

**Çözüm:**

- MySQL servisinin çalıştığını kontrol edin
- `.env` dosyasındaki veritabanı bilgilerini kontrol edin
- Veritabanının oluşturulduğunu kontrol edin

#### 2. JWT Token Hatası

```
401 Unauthorized
```

**Çözüm:**

- Token'ın geçerli olduğunu kontrol edin
- Token'ın Authorization header'ında olduğunu kontrol edin
- Format: `Bearer <token>`

#### 3. CORS Hatası

```
Access to XMLHttpRequest blocked by CORS policy
```

**Çözüm:**

- `application.properties` dosyasında CORS ayarlarını kontrol edin
- Frontend URL'sinin allowed origins listesinde olduğunu kontrol edin

#### 4. Port Zaten Kullanılıyor

```
Port 8080 is already in use
```

**Çözüm:**

- `.env` dosyasında `APP_PORT` değiştirebilirsiniz
- Veya çalışan servisi durdurun

### Log Seviyeleri

Development için:

```properties
logging.level.com.gcodes.aacctracker=DEBUG
logging.level.org.springframework.security=DEBUG
```

Production için:

```properties
logging.level.root=INFO
logging.level.com.gcodes.aacctracker=INFO
```

---

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen şu adımları takip edin:

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/AmazingFeature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add some AmazingFeature'`)
4. Branch'inizi push edin (`git push origin feature/AmazingFeature`)
5. Pull Request oluşturun

### Geliştirme Kuralları

- Clean Code prensiplerini takip edin
- Javadoc yorumları ekleyin
- Unit testler yazın
- Commit mesajlarını anlamlı tutun
- Branch isimlendirmede convention kullanın

---

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için `LICENSE` dosyasına bakın.

---

## 📞 İletişim

**Proje Sahibi:** Gökhan

- GitHub: [@gkhns89](https://github.com/gkhns89)
- Issues: [GitHub Issues](https://github.com/gkhns89/aacc-tracker/issues)

---

## 🎯 Roadmap

### v1.0 (Mevcut)

- ✅ Kullanıcı yönetimi
- ✅ Şirket yönetimi
- ✅ Anlaşma yönetimi
- ✅ Gümrük işlemleri
- ✅ Dashboard

### v1.1 (Planlanan)

- 📧 Email bildirim sistemi
- 📱 SMS bildirim
- 📄 PDF rapor oluşturma
- 📊 Gelişmiş analitik
- 🔍 Gelişmiş arama ve filtreleme

### v2.0 (Gelecek)

- 📱 Mobile uygulama
- 🤖 Otomatik bildirimler
- 📈 Tahminleme ve AI
- 🌍 Çoklu dil desteği
- ☁️ Cloud deployment

---

## 🙏 Teşekkürler

Bu projeyi geliştirirken kullanılan açık kaynak teknolojilere ve topluluğa teşekkürler:

- Spring Boot Team
- Hibernate Team
- JWT Community
- MySQL Team
- Maven Community

---

**Not:** Bu proje aktif olarak geliştirilmektedir. Önerileriniz ve geri bildirimleriniz için GitHub Issues
kullanabilirsiniz.

---

**Son Güncelleme:** Kasım 2025
**Versiyon:** 1.0.0-SNAPSHOT
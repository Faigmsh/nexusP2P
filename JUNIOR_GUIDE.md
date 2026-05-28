# NexusP2P — Junior Developer Öyrənmə Bələdçisi

Bu sənəd layihəni öyrənmək istəyən gənc developerlər üçün yazılıb.  
Layihə real bir **P2P pul köçürmə sistemi**nin əsaslarını əhatə edir.

---

## Layihənin Arxitekturası

```
src/
├── config/
│   └── DBConnection.java         → Verilənlər bazasına bağlantı idarəsi
├── model/
│   └── Account.java              → DB cədvəlinin Java obyekti (Entity/POJO)
├── repository/
│   ├── AccountRepository.java    → accounts cədvəli üzərindəki DB əməliyyatları
│   └── TransactionRepository.java→ transactions cədvəlinə tarixçə yazma
├── service/
│   └── TransferService.java      → Biznes məntiqi (transfer qaydaları)
├── exception/
│   └── InsufficientBalanceException.java → Xüsusi xəta sinifi
└── test/
    └── ConcurencyTest.java       → Paralel yük testi
```

Bu **Layered (Qatlı) Arxitektura** adlanır. Hər qat yalnız öz işini görür.

---

## Burada Öyrənə Biləcəyin Mövzular

### 1. JDBC (Java Database Connectivity)
Hər hansı framework olmadan Java ilə MySQL-ə necə birbaşa danışılır.
- `Connection`, `PreparedStatement`, `ResultSet` siniflərinin işi
- `try-with-resources` — resursu avtomatik bağlamaq
- `executeQuery()` vs `executeUpdate()` fərqi

**Bax:** `AccountRepository.java`, `TransactionRepository.java`

---

### 2. PreparedStatement — SQL Injection Qorunması
```java
// YANLIŞ (SQL Injection riski var!):
String sql = "SELECT * FROM accounts WHERE account_number = '" + number + "'";

// DOĞRU (Parametrik, təhlükəsiz):
String sql = "SELECT * FROM accounts WHERE account_number = ?";
preparedStatement.setString(1, number);
```
`?` yer tutucu (placeholder) işarəsidir. MySQL dəyəri kod kimi deyil, data kimi qəbul edir.

---

### 3. Database Transaction — ACID
Bir bank transferi iki addımdan ibarətdir: göndərəndən çıx + alıcıya əlavə et.
Əgər birinci addım uğurlu, ikincisi uğursuz olarsa, pul yox olur. Transaction bunu önləyir.

```java
connection.setAutoCommit(false); // Tranzaksiya başla
// ... bütün DB əməliyyatları ...
connection.commit();             // Hər şey OK → dəyişiklikləri yaz
connection.rollback();           // Xəta var → hamısını geri al
```

**ACID nədir?**
- **A**tomicity — ya hamısı, ya heç biri
- **C**onsistency — verilənlər həmişə düzgün vəziyyətdə olur
- **I**solation — paralel tranzaksiyalar bir-birinə toxunmur
- **D**urability — commit sonrası data itirilmir

**Bax:** `TransferService.java`

---

### 4. Row-Level Locking — Race Condition Önləmə
Eyni hesaba 2 transfer eyni anda gəlsə nə baş verər? Balans mənfiyə düşə bilər.

```sql
SELECT * FROM accounts WHERE account_number = ? FOR UPDATE
```

`FOR UPDATE` həmin sətiri tranzaksiya bitənə qədər **kilidləyir**.  
Digər thread-lər bu sətiri oxumaq üçün **növbədə gözləyir**.

**Bu olmadan:** 2 thread balansı eyni anda 100 AZN oxuyar, ikisi də 90 AZN yazar → 20 AZN çıxmalıydı, yalnız 10 AZN çıxdı (Race Condition).  
**Bununla:** İkinci thread birinci bitiənə qədər gözləyir.

---

### 5. ThreadLocal — Thread-Specific Dəyişən
```java
private static final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();
```

`ThreadLocal` hər thread-ə **özünəməxsus** dəyişən verir. Thread-A-nın Connection-ı Thread-B-ninkindən tamamilə ayrıdır.

**Niyə lazımdır?** Connection thread-safe deyil. Eyni Connection-ı 2 thread paylaşsa, tranzaksiya qarışar.

---

### 6. Custom Exception — Xüsusi Xəta Sinifi
```java
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
```

`RuntimeException`-dan miras alan siniflər **unchecked** xətadır — metodun imzasında `throws` yazmaq məcburi deyil.  
Bu, biznes xətalarını (`IllegalArgumentException` kimi ümumi xətalardan) ayırd etməyə imkan verir.

---

### 7. Multi-Threading — ExecutorService & CountDownLatch
```java
ExecutorService pool = Executors.newFixedThreadPool(10); // 10 paralel thread
CountDownLatch startSignal = new CountDownLatch(1);      // Start tapançası

for (int i = 0; i < 10; i++) {
    pool.submit(() -> {
        startSignal.await(); // Hamı start xəttindəki gözlə
        transferService.makeTransfer(...);
    });
}
startSignal.countDown(); // BAŞLA — hamısı eyni anda qaçır
```

`CountDownLatch` bütün thread-lərin eyni anda başlamasını təmin edir ki, **real paralel yük** simulyasiyası olsun.

---

## Tapılan Səhvlər (Sən Düzəlt!)

Aşağıdakılar sənin üçün praktika tapşırıqlarıdır.

### Səhv 1 — main() metod imzası yanlışdır
`Main.java` və `ConcurencyTest.java` fayllarında:
```java
// YANLIŞ — bu metod heç vaxt çalışmayacaq:
static void main() { ... }

// DOĞRU — JVM yalnız bu imzanı tanıyır:
public static void main(String[] args) { ... }
```

### Səhv 2 — DB Credentials hardcoded-dır
`DBConnection.java`-da parol birbaşa kodda yazılıb:
```java
private static final String PASSWORD = "12345"; // ← GÜVƏNLİK RİSKİ!
```
**Düzəliş:** `.env` faylı və ya `System.getenv("DB_PASSWORD")` istifadə et.

### Səhv 3 — SQL formatlaması pis
```java
// YANLIŞ:
String sql = "SELECT*FROM accounts WHERE ...";

// DOĞRU (oxunabilirlik üçün boşluq):
String sql = "SELECT * FROM accounts WHERE ...";
```

### Səhv 4 — Thread pool ölçüsü ilə test sayı uyuşmur
`ConcurencyTest.java`-da:
```java
int totalThreads = 10;                        // 10 thread test edilir
ExecutorService pool = Executors.newFixedThreadPool(20); // Amma pool 20-dir?
```
Bunları bir-birinə uyğunlaşdır.

### Səhv 5 — Şərhdə yanlış say
```java
// "15 thread eyni anda transferə başladı..." ← lakin totalThreads = 10
```

### Səhv 6 — Neqativ məbləğ yoxlanılmır
`makeTransfer()` metodu neqativ məbləğ göndərilsə nə edir?
```java
// Bu validasiya əskikdir:
if (amount.compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalArgumentException("Transfer mebleghi sifirdan boyuk olmalidir!");
}
```

### Səhv 7 — Null məbləğ ehtimalı yoxlanılmır
`amount` parametri null gələ bilər → `NullPointerException`.

---

## Növbəti Addımlar — Nə Əlavə Edə Bilərsən?

1. **HikariCP Connection Pool** — manual `ThreadLocal` yerinə sənaye standartı pool
2. **Unit Test** — JUnit 5 + Mockito ilə `TransferService`-i test et
3. **Commission hesablaması** — `BigDecimal.ZERO` yerinə real faiz
4. **Currency conversion** — fərqli valyutalar arasında köçürmə
5. **`transactions` cədvəlini oxuma** — Account statement (hesab çıxarışı) feature-i
6. **Logging frameworku** — `java.util.logging` yerinə SLF4J + Logback

---

## Suallar

Bu layihəni başa düşmək üçün aşağıdakı suallara cavab tapa bilməlisən:

1. Niyə `connection.setAutoCommit(false)` çağırılır?
2. `FOR UPDATE` olmadan nə baş verərdi? Konkret ssenarini izah et.
3. `ThreadLocal.remove()` çağırılmasa nə olar?
4. `PreparedStatement` niyə `Statement`-dən daha təhlükəsizdir?
5. `CountDownLatch(1)` yerinə `CountDownLatch(0)` yaza bilərdikmi?
6. `RuntimeException` ilə `Exception`-ın fərqi nədir?

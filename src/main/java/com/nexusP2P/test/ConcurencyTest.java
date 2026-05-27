package com.nexusP2P.test;
import com.nexusP2P.service.TransferService; // Test edəcəyimiz transfer xidməti.
import java.math.BigDecimal; // Dəqiq maliyyə məbləğləri üçün.
import java.util.concurrent.CountDownLatch; // Thread-ləri eyni saniyədə başlatmaq üçün sinif.
import java.util.concurrent.ExecutorService; // Thread-ləri idarə edən hovuz (Thread Pool).
import java.util.concurrent.Executors; // ExecutorService obyektləri yaratmaq üçün köməkçi sinif.
public class ConcurencyTest {
    static void main() throws InterruptedException {
// Test edəcəyimiz TransferService obyektini yaradırıq.
        TransferService transferService=new TransferService();
        // Test parametrləri:
        String sender = "ACC-12345-SENDER";    // Təsəvvür edək ki, bazada bu adda hesab var və balansı 100 AZN-dir.
        String receiver = "ACC-54321-RECEIVER"; // Təsəvvür edək ki, bu hesabın balansı isə 0 AZN-dir.
        BigDecimal amountPerTransfer = new BigDecimal("10.00"); // Hər thread-in köçürəcəyi məbləğ.
        int totalThreads=10;// Eyni anda (parallel) sistemə hücum edəcək thread sayı.
        // DİQQƏT: Əgər balans 100 AZN-dirsə və 15 thread-in hər biri 10 AZN köçürməyə çalışırsa,
        // cəmi 10 transfer uğurlu olmalı, yerdə qalan 5-i isə "Məbləğ yetərli deyil" xətası almalıdır!
        // Balans əsla mənfiyə düşməməlidir!

        // 1. THREAD HOVUZUNUN YARADILMASI:
        // Executors.newFixedThreadPool(10) metodu RAM-da eyni anda parallel işləyə bilən 10 ədəd aktiv işçi (Thread) yaradır.
        ExecutorService executorService=Executors.newFixedThreadPool(20);

        // 2. COUNTDOWNLATCH (Kritik Sinif):
        // Bu sinif thread-lərin hamısını eyni anda yarışa başlatmaq üçün bir start tapançası rolunu oynayır.
        // Sayğacı 1 olaraq təyin edirik.
        CountDownLatch startSignal=new CountDownLatch(1);

        //Thread-larin hazirlanmasi
        for(int i=0;i<totalThreads;i++){
            final int threadNumber=i+1; //her thread-e unnikal nomre veririk Log ucun

        // executorService.submit() metodu: Hovuzdakı bir thread-ə yerinə yetirməsi üçün tapşırıq verir.
        // () -> { ... } yazılışı bir Lambda funksiyasıdır (Runnable tapşırıq)
        executorService.submit(()->{
            try {
                // startSignal.await() metodu: Bütün thread-ləri burada dondurur və gözlədir.
                // Nə vaxt ki, əsas thread start signalını verəcək, hamısı eyni saniyədə aşağıdakı koda hücum edəcək.
                startSignal.await();
                System.out.println("[Thread-" + threadNumber + "] Transfer başladılır...");
                // Bizim yazdığımız transfer xidmətini çağırırıq.
                transferService.makeTransfer(sender, receiver, amountPerTransfer);
                System.out.println("[Thread-" + threadNumber + "] SUCCESS: Transfer tamamlandı.");
            }catch (Exception ex){
                // Əgər balans bitibsə və ya SQL-də kilid gözləyərkən problem olubsa, xətanı buraya yazdırırıq.
                System.err.println("[Thread-" + threadNumber + "] FAILED: Xəta: " + ex.getMessage());
            }
        });

    }
        // Qısa bir fasilə veririk ki, bütün thread-lər yaransın və start xəttində (await() metodunda) düzülsünlər.
        Thread.sleep(1000);
        System.out.println("🔥 START TAPANÇASI ATILDI! 15 Thread eyni anda transferə başladı...");
        // 4. START SİQNALINI VERMƏK:
        // startSignal.countDown() metodu sayğacı 1-dən 0-a düşürür.
        // Bu an await() metodunda donub qalan 15 thread-in HAMSı eyni anda sərbəst buraxılır və parallel olaraq verilənlər bazasına qaçır!
        startSignal.countDown();
        // Hovuzu bağlayırıq (Yeni tapşırıq qəbul etmir, mövcudları bitirir)
        executorService.shutdown();

    }
}

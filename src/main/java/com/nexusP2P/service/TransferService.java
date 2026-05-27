package com.nexusP2P.service;

import com.nexusP2P.config.DBConnection;
import com.nexusP2P.exception.InsufficientBalanceException;
import com.nexusP2P.model.Account;
import com.nexusP2P.repository.AccountRepository;
import com.nexusP2P.repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransferService {
    private static final Logger logger = Logger.getLogger(TransferService.class.getName());

    private final AccountRepository accountRepository = new AccountRepository();
    private final TransactionRepository transactionRepository = new TransactionRepository();

    /**
     * İki hesab arasında pul köçürülməsini icra edən əsas biznes metodu.
     * Vahid tranzaksiya, Row-level locking və Multi-thread safety təmin olunub.
     */
    public void makeTransfer(String senderAccountNumber, String receiverAccountNumber, BigDecimal amount) {
        Connection connection = null;
        Account senderAccount = null;
        Account receiverAccount = null;
        try {
            connection = DBConnection.getConnection();

            // 1. TRANZAKSİYANI BAŞLATMAQ:
            // Avtomatik commit-i bağlayırıq ki, vahid tranzaksiya (Atomicity) təmin olunsun.
            connection.setAutoCommit(false);
            logger.log(Level.INFO, "Tranzaksiya basladildi: " + senderAccountNumber + " -> " + receiverAccountNumber + " amount  " + amount);

            // 2. HESABLARI BAZADAN KİLİDLƏYƏRƏK SEÇMƏK (Row-level Locking):
            // 'FOR UPDATE' sayəsində thread-lər bir-birini gözləyir (Race condition qarşısı alınır).
             senderAccount = accountRepository.findByAccountNumberWithLock(connection, senderAccountNumber);
             receiverAccount = accountRepository.findByAccountNumberWithLock(connection, receiverAccountNumber);

            // 3. VALİDASİYA VƏ BİZNES YOXLAMALARI
            if (senderAccount == null) {
                throw new IllegalArgumentException("Gonderen hesab tapilmadi: " + senderAccountNumber);
            }
            if (receiverAccount == null) {
                throw new IllegalArgumentException("Kocurme edilecek hesab tapilmadi: " + receiverAccountNumber);
            }
            if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
                throw new IllegalArgumentException("Ferqli valyutalar arasinda birbasa kocurme mumkun deyil!");
            }

            // 4. BALANSIN YOXLANILMASI:
            if (senderAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException("Hesabda kifayet qeder vesait yoxdur! Cari balans: " + senderAccount.getBalance());
            }

            // 5. BALANSLARIN YENİLƏNMƏSİ (Hesablama):
            BigDecimal newSenderBalance = senderAccount.getBalance().subtract(amount);
            BigDecimal newReceiverBalance = receiverAccount.getBalance().add(amount);

            // 6. BAZADA UPDATE ƏMƏLİYYATLARI:
            accountRepository.updateBalance(connection, senderAccount.getId(), newSenderBalance);
            accountRepository.updateBalance(connection, receiverAccount.getId(), newReceiverBalance);

            // 7. TRANSFER TARİXÇƏSİNİN YAZILMASI:
            transactionRepository.saveTransaction(
                    connection,
                    senderAccount.getId(),
                    receiverAccount.getId(),
                    amount,
                    BigDecimal.ZERO, // Hələlik komissiya 0.0
                    "SUCCESS_TRANSFER"
            );

            // 8. TRANZAKSİYANIN SONLANDIRILMASI (COMMIT):
            // Hər şey uğurludursa, dəyişiklikləri eyni anda MySQL-ə həkk edirik.
            connection.commit();
            logger.log(Level.INFO, "Transfer ugurla tamamlandi: " + amount + " " + senderAccount.getCurrency());

        } catch (InsufficientBalanceException e) {
            // 9. XƏTA BAŞ VERƏNDƏ GERİ ALMAQ (ROLLBACK):
            if (connection != null&&senderAccount!=null&&receiverAccount!=null) {
                try {
//                    logger.log(Level.WARNING, "Tranzaksiyada xeta bas verdi, deyisiklikler geri alinir. Səbəb: " + e.getMessage());
                    connection.rollback();
                    // Yeni təmiz bir tarixçə yazısı açırıq (Eyni tranzaksiya daxilində)
                    transactionRepository.saveTransaction(
                            connection,
                            senderAccount.getId(),
                            receiverAccount.getId(),
                            amount,
                            BigDecimal.ZERO,
                            "FAILED_INSUFFICIENT_BALANCE"
                    );
                    connection.commit();
                } catch (SQLException sqlException) {
                    logger.log(Level.SEVERE, "Uğursuz transfer loqu yazılarkən SQL xətası! !", sqlException);
                }
            }
            // Multi-threading testinin çökməməsi, loqları rahat oxuya bilməyimiz üçün xətanı konsola sadə şəkildə çıxarırıq
            System.out.println("[" + Thread.currentThread().getName() + "] FAILED: Xəta: " + e.getMessage());

        }catch (Exception exception){
            if (connection != null) {
                try {
                    logger.log(Level.WARNING, "Tranzaksiyada xeta bas verdi, deyisiklikler geri alinir. Səbəb: " + exception.getMessage());
                    connection.rollback();
                } catch (SQLException sqlException) {
                    logger.log(Level.SEVERE, "Rollback edilerken kritik sql xetasi !", sqlException);
                }
            }
            System.out.println("[" + Thread.currentThread().getName() + "] FAILED: Xəta: " + exception.getMessage());
        }
        finally {
            // 10. BAĞLANTI AYARLARINI BƏRPA ETMƏK VƏ BAĞLANTINI QAPATMAQ:
            if (connection != null) {
                try {
                    // Digər keş bağlantıların işini pozmamaq üçün statusu sıfırlayırıq
                    connection.setAutoCommit(true);
                    // CRITICAL: Bağlantı mütləq bağlanmalıdır (və ya pool-a qaytarılmalıdır)

                } catch (SQLException sqlException) {
                    logger.log(Level.WARNING, "Baglanti qapadilarken xeta yarandi!");
                }finally {
                    // CRITICAL: Birbaşa connection.close() yox, DBConnection-ın metodunu çağırırıq!
                    DBConnection.closeConnection();
                }
            }
        }
    }
}
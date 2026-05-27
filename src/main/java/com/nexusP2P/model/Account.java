package com.nexusP2P.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
//Bu sinif verilənlər bazasındakı accounts cədvəlindən oxuduğumuz sətirləri Java daxilində bir obyekt kimi istifadə etmək üçün nəzərdə tutulmuş Entity (POJO) sinfidir.
//Entity / POJO — Plain Old Java Object
//Sadə Java obyektidir. Heç bir xüsusi framework və ya interface-dən asılı deyil. Yalnız:
//
//field-lər (dəyişənlər)
//constructor
//getter/setter-lər
public class Account {
    private int id; // Hesabin unikal ID nömrəsi (Primary Key)
    private int userId; // Bu hesabın hansı istifadəçiyə aid olduğunu bildirən ID (Foreign Key)
    private String accountNumber; // 16 rəqəmli hesab nömrəsi (Məs: 4169...)
    private String currency; // Valyuta növü (AZN, USDT, USD)
    private BigDecimal balance; // Hesabın balansı (BigDecimal növündə)
    private String status; // Hesabın vəziyyəti (ACTIVE, FROZEN)
    private Timestamp updatedAt; // Son yenilənmə vaxtı

    /**
     * Boş Constructor (Default Constructor)
     * Heç bir parametr qəbul etmir və içi boşdur.
     * Niyə lazımdır? JDBC ilə verilənlər bazasından məlumatları çəkəndə, Java ilk öncə bu boş obyekti yaradır,
     * sonra isə SQL-dən gələn dataları setX() metodları ilə bura doldurur.
     */
    public Account() {
    }

    public Account(int id, int userId, String accountNumber, String currency, BigDecimal balance, String status) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Account{" +
                ", accountNumber='" + accountNumber + '\'' +
                ", currency='" + currency + '\'' +
                ", balance=" + balance +
                ", status='" + status + '\'' +
                '}';
    }
}

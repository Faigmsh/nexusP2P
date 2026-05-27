package com.nexusP2P.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

//Bu sinif edilən hər bir pul köçürməsinin tarixçəsini transactions cədvəlinə əlavə edir.
// Bu, maliyyə sistemlərində audit (yoxlama) və şəffaflıq üçün məcburidir.
public class TransactionRepository {
    //Hər bir uğurlu transfer əməliyyatını verilənlər bazasına qeyd edir.
    public void saveTransaction(Connection connection, int senderAccountId,
                                int receiverId, BigDecimal amount, BigDecimal fee, String type)throws SQLException {
        // INSERT INTO sorğumuz: transactions cədvəlinə yeni sətir əlavə edir.
        String sql = "INSERT INTO transactions (sender_account_id, receiver_account_id, amount, " +
                "commission_fee, transaction_type) VALUES (?, ?, ?, ?, ?)";
        try(PreparedStatement preparedStatement=connection.prepareStatement(sql)) {
            // Sual işarələrinin yerinə metod parametrindən gələn dəyərləri ardıcıllıqla düzürük:
            preparedStatement.setInt(1,senderAccountId);
            preparedStatement.setInt(2,receiverId);
            preparedStatement.setBigDecimal(3,amount);
            preparedStatement.setBigDecimal(4,fee);
            preparedStatement.setString(5,type); // transfer novu

            // preparedStatement.executeUpdate() metodu: Yeni sətiri bazaya yazır.// or P2P
            preparedStatement.executeUpdate();

        }
    }


}

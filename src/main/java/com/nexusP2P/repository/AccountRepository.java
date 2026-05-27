package com.nexusP2P.repository;
//Bu sinif verilənlər bazasındakı accounts cədvəli üzərində axtarış və yeniləmə əməliyyatlarını yerinə yetirir.
import com.nexusP2P.model.Account; // SQL-dən gələn datanı dolduracağımız Account modelini daxil edir.
import java.math.BigDecimal; // Balans yeniləmələri üçün yüksək dəqiqlikli riyazi sinif.
import java.sql.Connection; // SQL sorğusunun hansı bağlantı üzərindən gedəcəyini müəyyən edən interfeys.
import java.sql.PreparedStatement; // SQL Injection təhlükəsizliyi təmin edən və sorğuları idarə edən sinif.
import java.sql.ResultSet; // SQL-dən gələn cavab cədvəlini (məlumatları) tutan sinif.
import java.sql.SQLException; // SQL xətalarını idarə etmək üçün sinif.
import java.util.logging.Level; // Log dərəcələrini təyin etmək üçün.
import java.util.logging.Logger; // Konsola peşəkar loqlar yazmaq üçün.


public class AccountRepository {
    private static final Logger logger = Logger.getLogger(AccountRepository.class.getName());
    /**
     * Hesab nömrəsinə görə hesabı bazadan tapır.
     * Connection parametrini kənardan (Service qatından) alırıq. Çünki tranzaksiyanı idarə edərkən
     * bütün əməliyyatlar eyni connection üzərindən icra olunmalıdır.
     */
    public Account findByAccountNumber(Connection connection,String accountNumber){
        // SQL sorğumuz. Sual işarəsi (?) xaricdən dinamik dəyər gələcəyini bildirir (Placeholder).
        String sql = "SELECT*FROM accounts WHERE account_number=?";
        // PreparedStatement yaratmaq: connection.prepareStatement(sql) metodu sorğunu MySQL-ə göndərir
        // və MySQL bu sorğunun strukturunu əvvəlcədən hazırlayır (compile edir). Bu, SQL Injection-ın qarşısını alır.
        // try-with-resources (mötərizə daxilində yazılış) istifadə edirik ki, iş bitəndə 'ps' avtomatik qapansın.
        try(PreparedStatement preparedStatement=connection.prepareStatement(sql)){
            // ps.setString(1, ...) metodu: Birinci sual işarəsinin (?) yerinə 'accountNumber' dəyişənini təhlükəsiz şəkildə yerləşdirir.
            preparedStatement.setString(1,accountNumber);
            // ps.executeQuery() metodu: SELECT sorğusunu işə salır və gələn dataları ResultSet obyektinə doldurur.
            try(ResultSet resultSet=preparedStatement.executeQuery()){
                // rs.next() metodu: Gələn cavab cədvəlində növbəti sətirin olub-olmadığını yoxlayır.
                // Əgər sətir varsa, o sətrin üzərinə keçir və true qaytarır.
                if(resultSet.next()){
                    return mapResultSetToAccount(resultSet);
                }
            }
        }catch (SQLException e) {
            // Hər hansı SQL xətası olarsa loq qeyd edirik.
            logger.log(Level.SEVERE, "Hesab tapılarkən SQL xətası: " + accountNumber, e);
        }
        // Hesab tapılmazsa geriyə boş (null) dəyər qaytarır.
        return null;
    }
   /** Row-level Locking (Səviyyəli Səkil kilidləmə).
            * Sorğunun sonundakı 'FOR UPDATE' əmri, bu sətir üzərində əməliyyat aparan tranzaksiya bitənə qədər
     * həmin hesabı MySQL səviyyəsində KİLİDLƏYİR. Digər thread-lər bu hesabı oxumaq üçün növbədə gözləyirlər.
     * Bu, eyni saniyədə iki transfer gəldikdə balansın mənfiyə düşməsinin (Race Condition) qarşısını alır.
  */
   public Account findByAccountNumberWithLock(Connection connection,String accountNumber)throws SQLException{
       String sql = "SELECT * FROM accounts WHERE account_number = ? FOR UPDATE";
       try(PreparedStatement preparedStatement=connection.prepareStatement(sql)){
           preparedStatement.setString(1,accountNumber);
           try(ResultSet resultSet=preparedStatement.executeQuery()){
               if(resultSet.next()){
                   return mapResultSetToAccount(resultSet);
               }
           }
       }
       return  null;
   }
    /**
     * Hesabın balansını yeniləyən metod.
     * xətanı burada tutmuruq (throws SQLException), Service qatına fırladırıq ki, orada Rollback edə bilək.
     */
    public void updateBalance(Connection connection,int accountId,BigDecimal newBalance)throws SQLException{
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try(PreparedStatement preparedStatement=connection.prepareStatement(sql)){
            // 1-ci sual işarəsinə yeni balansı (BigDecimal) mənimsədir
            preparedStatement.setBigDecimal(1,newBalance);
            // 2-ci sual işarəsinə hesabın ID nömrəsini (int) mənimsədir
            preparedStatement.setInt(2,accountId);
            // ps.executeUpdate() metodu: UPDATE, INSERT və ya DELETE sorğularını işə salır.
            // Bizə bu sorğudan təsirlənən sətirlərin sayını (int) qaytarır
            int affectedRows=preparedStatement.executeUpdate();
            // Əgər təsirlənən sətir sayı 0-dırsa, deməli belə bir ID-li hesab bazada yoxdur. Xəta fırladırıq.
            if(affectedRows==0){
                throw  new SQLException("Balans yenilenmedi, hesab tapilmadi. Id: " +accountId);
            }
        }


    }


    /**
     * Data Mapping (Məlumat Dönüşdürülməsi) köməkçi metodu.
     * ResultSet-dəki sütunları tək-tək oxuyub yeni bir Account obyekti yaradır.
     */
    private Account mapResultSetToAccount(ResultSet resultSet)throws SQLException {
        // rs.getInt("sütun_adı") və rs.getBigDecimal("sütun_adı") metodları
        // verilənlər bazasındakı həmin sütunun dəyərini müvafiq Java tipində oxuyur.
    return new Account(
            resultSet.getInt("id"),
            resultSet.getInt("user_id"),
            resultSet.getString("account_number"),
            resultSet.getString("currency"),
            resultSet.getBigDecimal("balance"),
            resultSet.getString("status"));
    }

}

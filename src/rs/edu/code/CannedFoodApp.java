package rs.edu.code;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;

public class CannedFoodApp {

    // pravimo konstante koje ce nam sluziti za konekciju na bazu
    private final String dbURL = "jdbc:mysql://localhost:3306/canned_food_jdbc";
    private final String user = "root";
    private final String password = "root";

    public static void main(String[] args) {
         CannedFoodApp cannedFoodApp = new CannedFoodApp();
         cannedFoodApp.initDataBase();
      // cannedFoodApp.listBin(1);
         cannedFoodApp.listAllBins();
      // cannedFoodApp.addCan("cola", LocalDate.parse("2022-08-31"));
       //  cannedFoodApp.addCan("sprite", LocalDate.parse("2022-09-30"));
         System.out.println("Kraj programa.");

    }

    private void listAllBins() {
        try (Connection connection = DriverManager.getConnection(dbURL, user, password);
             Statement statement = connection.createStatement();) {

            ResultSet result = statement.executeQuery("SELECT * FROM inventory;");
            while (result.next()) {
                System.out.println(result.getInt(1) + "\t" + result.getString(2) + "\t" + result.getInt(3) + "\n");
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addCan(String type, LocalDate expiryDate) {
        String sql1 = "UPDATE bins SET type = ? WHERE id = ?;";

        String sql2 = "INSERT INTO cans (type, expiry_date, bin_id) VALUES (?, ?, ?);";

        try (Connection connection = DriverManager.getConnection(dbURL, user, password);
             Statement statement = connection.createStatement();
             PreparedStatement preparedStatement1 = connection.prepareStatement(sql1);
             PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);) {

            // pronadji odgovarajuci bin
            String sql = "SELECT id AS bin_id, stanje " +
                    "FROM inventory " +
                    "WHERE (type = '" + type + "' or stanje = 0) AND stanje < 10 " +
                    "ORDER BY stanje DESC;";

            ResultSet result = statement.executeQuery(sql);

            // ne postoji odgovarajuci bin
            if (!result.next()) {
                System.out.println("Nije moguce dodati konzervu u bin");
                return;
            }

            // zanima nas samo prvi sledeci red, zato ne koristimo while petlju
            result.next();
            int binId = result.getInt(1);
            int stanje = result.getInt(2);

            if (stanje == 0) {
                preparedStatement1.setString(1, type);
                preparedStatement1.setInt(2, binId);
                preparedStatement1.executeUpdate();
            }

            preparedStatement2.setString(1, type);
            preparedStatement2.setDate(2, Date.valueOf(expiryDate));
            preparedStatement2.setInt(3, binId);
            preparedStatement2.executeUpdate();

        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void listBin(int binId) {
        String sql = "SELECT id, type, expiry_date FROM cans WHERE bin_id = ?;";
        try (Connection connection = DriverManager.getConnection(dbURL, user, password);
             PreparedStatement statement = connection.prepareStatement(sql);) {

            statement.setInt(1, binId);

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                System.out.println(result.getInt(1) + "\t" + result.getString(2) + "\t" + result.getDate(3) + "\n");
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initDataBase() {
        try (Connection connection = DriverManager.getConnection(dbURL, user, password);
             Statement statement = connection.createStatement();) {

            String ddl = "CREATE TABLE IF NOT EXISTS bins (" +
                    "id INT AUTO_INCREMENT, " +
                    "type VARCHAR(45), " +
                    "PRIMARY KEY (id) " +
                    ");";
            statement.execute(ddl);

            ddl = "CREATE TABLE IF NOT EXISTS cans (" +
                    "id INT AUTO_INCREMENT, " +
                    "type VARCHAR(45) NOT NULL, " +
                    "expiry_date DATE NOT NULL, " +
                    "bin_id INT NOT NULL, " +
                    "PRIMARY KEY (id), " +
                    "CONSTRAINT fk_bin_id FOREIGN KEY (bin_id) REFERENCES bins(id)" +
                    ");";
            statement.execute(ddl);

            ddl = "CREATE OR REPLACE View inventory AS " +
                    "SELECT bins.id, bins.type, count(cans.id) AS stanje, min(cans.expiry_date) AS min_exp_date " +
                    "FROM cans RIGHT JOIN bins ON cans.bin_id = bins.id " +
                    "GROUP BY bins.id;";
            statement.executeUpdate(ddl);

            // inicijalizacija podataka

            String sql = "SELECT count(*) AS stanje " +
                    "FROM bins;";
            ResultSet result = statement.executeQuery(sql);
            result.next();
            int stanje = result.getInt("stanje");

            if (stanje == 0) {
                sql = "INSERT INTO bins (type) " +
                        "VALUES " +
                        "('cola'), " +
                        "('fanta'), " +
                        "(null), " +
                        "(null), " +
                        "(null), " +
                        "(null), " +
                        "(null), " +
                        "(null), " +
                        "(null), " +
                        "(null);";
                statement.executeUpdate(sql);
            }

            sql = "INSERT INTO cans (type, expiry_date, bin_id) " +
                    "VALUES " +
                    "('cola', '2022-10-01', 1), " +
                    "('cola', '2022-07-01', 1), " +
                    "('fanta', '2022-09-01', 2); ";
            statement.executeUpdate(sql);
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
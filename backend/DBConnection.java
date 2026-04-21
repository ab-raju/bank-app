import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL  = System.getenv("DB_URL") != null
        ? System.getenv("DB_URL")
        : "jdbc:mysql://localhost:3306/bankdb?useSSL=false&serverTimezone=UTC";

    private static final String USER = System.getenv("DB_USER") != null
        ? System.getenv("DB_USER") : "root";

    private static final String PASS = System.getenv("DB_PASS") != null
        ? System.getenv("DB_PASS") : "your_local_password";

    public static Connection get() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            throw new RuntimeException("DB Connection Failed: " + e.getMessage());
        }
    }
}
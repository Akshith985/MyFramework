
import com.simpleweb.MiniWeb;
import com.simpleweb.Database;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TestApp {

    public static void main(String[] args) {
        // DEBUG: Check if index.html is in the build folder
        System.out.println("Checking for file...");
        var url = TestApp.class.getResource("/web/index.html");
        if (url == null) {
            System.out.println("❌ CRITICAL ERROR: Java cannot find '/web/index.html' in the classpath. Please Rebuild Project.");
        } else {
            System.out.println("✅ FOUND: " + url);
        }

        // 1. DATABASE CONFIGURATION
        // 1. DATABASE CONFIGURATION
        // Cloud: Read from Environment Variables (Render)
        // Replace the placeholder with your REAL Neon URL
        String dbUrl = "jdbc:postgresql://ep-steep-leaf-a4xzf6wj-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require";

// 2. The Username (Keep it clean)
        String dbUser = "neondb_owner";

// 3. The Password (The one starting with npg_)
        String dbPass = "npg_UsW0hA9ETjbl";

// 4. Initialize your Database class
        Database db = new Database(dbUrl, dbUser, dbPass);
        MiniWeb app = new MiniWeb(8080);

        // --- MIDDLEWARE: CORS (The "Bulletproof" Version) ---
        // This is the ONLY place CORS is handled.
        app.use((ctx, next) -> {
            // 1. Clear existing headers to prevent duplicates
            ctx.exchange().getResponseHeaders().remove("Access-Control-Allow-Origin");
            ctx.exchange().getResponseHeaders().remove("Access-Control-Allow-Methods");
            ctx.exchange().getResponseHeaders().remove("Access-Control-Allow-Headers");

            // 2. Set headers fresh
            ctx.exchange().getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            ctx.exchange().getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.exchange().getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            // 3. Handle Preflight (OPTIONS)
            if (ctx.exchange().getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                ctx.status(204).send("");
                return;
            }
            next.run();
        });

        // --- MIDDLEWARE: Logger ---
        app.use((ctx, next) -> {
            System.out.println("🔔 " + ctx.exchange().getRequestMethod() + " " + ctx.exchange().getRequestURI());
            next.run();
        });

        // --- ROUTE 1: SERVE STATIC FILES (HTML/JS) ---
        // This was missing in your snippet! It is required to see the webpage.
        app.get("/static/*path", ctx -> {
            String staticFile = ctx.path("path");

            // Security check to prevent reading system files
            if (staticFile == null || staticFile.contains("..")) {
                ctx.status(403).send("Access Denied");
                return;
            }

            InputStream is = TestApp.class.getResourceAsStream("/web/" + staticFile);
            if (is == null) {
                ctx.status(404).send("File Not Found: " + staticFile);
            } else {
                byte[] data = is.readAllBytes();
                ctx.sendBytes(data);
            }
        });

        // --- ROUTE 2: GET ALL USERS (READ) ---
        // --- ROUTE 2: GET USERS (With Search!) ---
        app.get("/users", ctx -> {
            List<User> userList = new ArrayList<>();

            // 1. Check if the user sent a search query
            String searchTerm = ctx.query("search");

            try (Connection conn = db.connect()) {
                String sql;
                PreparedStatement stmt;

                // 2. Dynamic SQL: If searching, filter results. If not, get all.
                if (searchTerm != null && !searchTerm.isEmpty()) {
                    // ILIKE is Postgres specific for "Case-Insensitive Search"
                    sql = "SELECT * FROM users WHERE name ILIKE ?";
                    stmt = conn.prepareStatement(sql);
                    stmt.setString(1, "%" + searchTerm + "%"); // % matches anything around the word
                } else {
                    sql = "SELECT * FROM users";
                    stmt = conn.prepareStatement(sql);
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    userList.add(new User(
                            rs.getString("name"),
                            rs.getString("role"),
                            rs.getInt("id")
                    ));
                }
                ctx.json(userList);

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).send("DB Error: " + e.getMessage());
            }
        });

        // --- ROUTE 3: CREATE USER (JSON POST) ---
        // --- ROUTE 3: CREATE USER (JSON POST) ---
        app.post("/users/create", ctx -> {
            // REMOVED the debug print that was stealing the data!

            // Now this is the FIRST and ONLY time we read the stream
            User u = ctx.bodyAs(User.class);

            if (u == null) {
                // If this happens, check your IntelliJ console for the red error log!
                ctx.status(400).send("Invalid JSON");
                return;
            }

            try (Connection conn = db.connect()) {
                String sql = "INSERT INTO users (name, role) VALUES (?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, u.name());
                stmt.setString(2, u.role());

                stmt.executeUpdate();
                System.out.println("✅ User inserted: " + u.name());
                ctx.json(u);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).send("DB Error: " + e.getMessage());
            }
        });

        app.start();
    }
}

// Record Definition
record User(String name, String role, int id) {}
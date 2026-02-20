package motion.soft.XmlLoader.App;

import io.github.cdimascio.dotenv.Dotenv;
import motion.soft.XmlLoader.Models.TableSchema;
import motion.soft.XmlLoader.Services.DbService;
import motion.soft.XmlLoader.Services.XmlParserService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Dotenv dotenv;

    static {
        Dotenv tempDotenv = null;
        try {
            tempDotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Exception e) {
            System.out.println("Dotenv file not found, relying on system environment variables.");
        }
        dotenv = tempDotenv;
    }

    public static String getEnv(String key) {
        String val = System.getenv(key);
        if (val == null && dotenv != null) {
            val = dotenv.get(key);
        }
        if (val == null) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return val;
    }

    private static final String XML_URL = getEnv("XML_URL");
    private static final String DB_URL = getEnv("DB_URL");
    private static final String DB_USER = getEnv("DB_USER");
    private static final String DB_PASS = getEnv("DB_PASS");
    private static final String DB_DRIVER = getEnv("DB_DRIVER");

    public static void main(String[] args) {
        System.out.println("=== XML to PostgreSQL Loader ===");
        System.out.println("Loading config from .env...");

        boolean interactive = args.length == 0 || "--interactive".equals(args[0]);
        String targetTable = (args.length > 1) ? args[1] : null;

        try {
            Class.forName(DB_DRIVER);

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                System.out.println("Connected to database successfully.");

                XmlParserService parser = new XmlParserService(XML_URL);
                DbService dbService = new DbService(conn);

                if (interactive) {
                    runInteractive(parser, dbService);
                } else {
                    if (targetTable != null) {
                        processTable(parser, dbService, targetTable);
                    } else {
                        List<String> tables = parser.getTableNames();
                        System.out.println("Processing all tables: " + tables);
                        for (String t : tables) {
                            processTable(parser, dbService, t);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Critical Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void runInteractive(XmlParserService parser, DbService dbService) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nCommands: [list] [schema <table>] [update <table>] [update_all] [exit]");
            System.out.print("> ");
            String cmd = scanner.nextLine().trim();
            String[] parts = cmd.split("\\s+");

            try {
                switch (parts[0]) {
                    case "list":
                        System.out.println("Tables in XML: " + parser.getTableNames());
                        break;
                    case "schema":
                        if (parts.length < 2) throw new IllegalArgumentException("Specify table name");
                        System.out.println("DDL:\n" + parser.getTableDDL(parts[1]));
                        break;
                    case "update":
                        if (parts.length < 2) throw new IllegalArgumentException("Specify table name");
                        TableSchema schema = parser.parseTable(parts[1]);
                        dbService.update(schema);
                        System.out.println("Updated successfully.");
                        break;
                    case "update_all":
                        for (String t : parser.getTableNames()) {
                            dbService.update(parser.parseTable(t));
                        }
                        System.out.println("All tables updated.");
                        break;
                    case "exit":
                        return;
                    default:
                        System.out.println("Unknown command.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void processTable(XmlParserService parser, DbService dbService, String tableName) throws SQLException {
        System.out.println("Processing table: " + tableName);
        TableSchema schema = parser.parseTable(tableName);
        dbService.update(schema);
    }
}
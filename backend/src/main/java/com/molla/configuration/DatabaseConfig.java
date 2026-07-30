package com.molla.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${MYSQLDATABASE_URL:}")
    private String mysqlDatabaseUrl;

    @Value("${MYSQLHOST:}")
    private String mysqlHost;

    @Value("${MYSQLPORT:3306}")
    private String mysqlPort;

    @Value("${MYSQLDATABASE:}")
    private String mysqlDatabase;

    @Value("${MYSQLUSER:}")
    private String mysqlUser;

    @Value("${MYSQLPASSWORD:}")
    private String mysqlPassword;

    @Value("${spring.datasource.url:}")
    private String appDataSourceUrl;

    @Value("${spring.datasource.username:}")
    private String appDataSourceUsername;

    @Value("${spring.datasource.password:}")
    private String appDataSourcePassword;

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        // Create fresh properties object - don't bind from application.properties
        // to avoid picking up the bad mysql.railway.internal URL
        DataSourceProperties properties = new DataSourceProperties();

        logger.info("=== Database Configuration Debug ===");
        logger.info("MYSQLDATABASE_URL: {}",
                mysqlDatabaseUrl != null && !mysqlDatabaseUrl.isEmpty() ? "SET" : "NOT SET");
        logger.info("MYSQLHOST: {}", mysqlHost != null && !mysqlHost.isEmpty() ? mysqlHost : "NOT SET");
        logger.info("MYSQLDATABASE: {}", mysqlDatabase != null && !mysqlDatabase.isEmpty() ? mysqlDatabase : "NOT SET");
        logger.info("MYSQLUSER: {}", mysqlUser != null && !mysqlUser.isEmpty() ? "SET" : "NOT SET");

        // Priority 1: If Railway's MYSQLDATABASE_URL is provided, parse it
        if (mysqlDatabaseUrl != null && !mysqlDatabaseUrl.isEmpty()) {
            try {
                logger.info("Parsing MYSQLDATABASE_URL...");
                // Railway format: mysql://user:password@host:port/database
                URI dbUri = new URI(mysqlDatabaseUrl.replace("mysql://", "http://"));
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 3306 : dbUri.getPort();
                String database = dbUri.getPath().replaceFirst("/", "");

                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
                properties.setUrl(jdbcUrl);
                properties.setUsername(username);
                properties.setPassword(password);
                logger.info("Using MYSQLDATABASE_URL - Host: {}, Database: {}", host, database);
                return properties;
            } catch (Exception e) {
                logger.warn("Failed to parse MYSQLDATABASE_URL: {}", e.getMessage());
                // If parsing fails, continue to check individual variables
            }
        }

        // Priority 2: Use Railway's individual MySQL variables
        if (mysqlHost != null && !mysqlHost.isEmpty() &&
                mysqlDatabase != null && !mysqlDatabase.isEmpty()) {
            String jdbcUrl = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase;
            properties.setUrl(jdbcUrl);
            if (mysqlUser != null && !mysqlUser.isEmpty()) {
                properties.setUsername(mysqlUser);
            }
            if (mysqlPassword != null && !mysqlPassword.isEmpty()) {
                properties.setPassword(mysqlPassword);
            }
            logger.info("Using Railway MySQL variables - Host: {}, Database: {}", mysqlHost, mysqlDatabase);
            return properties;
        }

        // Priority 3: Check for SPRING_DATASOURCE_URL from environment
        // BUT: Skip if it contains mysql.railway.internal (internal hostname that
        // doesn't work locally)
        String springUrl = System.getenv("SPRING_DATASOURCE_URL");
        String springUser = System.getenv("SPRING_DATASOURCE_USERNAME");
        String springPass = System.getenv("SPRING_DATASOURCE_PASSWORD");

        if (springUrl != null && !springUrl.isEmpty() && !springUrl.contains("mysql.railway.internal")) {
            try {
                String tempUrl = springUrl;
                if (springUrl.startsWith("jdbc:")) {
                    tempUrl = springUrl.substring(5);
                }

                URI dbUri = new URI(tempUrl);
                if (dbUri.getUserInfo() != null) {
                    logger.info("Parsing database URL with credentials...");
                    String[] userInfo = dbUri.getUserInfo().split(":");
                    String username = userInfo[0];
                    String password = userInfo.length > 1 ? userInfo[1] : "";
                    String host = dbUri.getHost();
                    int port = dbUri.getPort();
                    String path = dbUri.getPath();
                    String scheme = dbUri.getScheme();

                    if ("postgres".equals(scheme)) {
                        scheme = "postgresql";
                    }

                    String cleanJdbcUrl = "jdbc:" + scheme + "://" + host + (port != -1 ? ":" + port : "") + path;

                    properties.setUrl(cleanJdbcUrl);
                    properties.setUsername(username);
                    properties.setPassword(password);

                    if ("postgresql".equals(scheme)) {
                        properties.setDriverClassName("org.postgresql.Driver");
                    } else if ("mysql".equals(scheme)) {
                        properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    }

                    logger.info("Parsed Database URL. Scheme: {}, Host: {}, Port: {}, Path: {}", scheme, host, port,
                            path);
                    return properties;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse URL from environment: {}", e.getMessage());
            }

            // Fallback for standard URLs or if parsing failed
            if (!springUrl.startsWith("jdbc:")) {
                springUrl = "jdbc:" + springUrl;
            }
            properties.setUrl(springUrl);
            if (springUser != null && !springUser.isEmpty()) {
                properties.setUsername(springUser);
            }
            if (springPass != null && !springPass.isEmpty()) {
                properties.setPassword(springPass);
            }
            logger.info("Using SPRING_DATASOURCE_URL from environment");
            return properties;
        } else if (springUrl != null && springUrl.contains("mysql.railway.internal")) {
            logger.warn("SPRING_DATASOURCE_URL contains mysql.railway.internal (internal hostname). " +
                    "Ignoring and falling back to application.properties.");
        }

        // Priority 4: Fall back to application.properties values
        if (appDataSourceUrl != null && !appDataSourceUrl.isEmpty()) {
            String sanitizedUrl = appDataSourceUrl.trim();
            if (!sanitizedUrl.startsWith("jdbc:")) {
                sanitizedUrl = "jdbc:" + sanitizedUrl;
            }
            properties.setUrl(sanitizedUrl);
            if (appDataSourceUsername != null && !appDataSourceUsername.isEmpty()) {
                properties.setUsername(appDataSourceUsername.trim());
            }
            if (appDataSourcePassword != null && !appDataSourcePassword.isEmpty()) {
                properties.setPassword(appDataSourcePassword.trim());
            }
            logger.info("Using database configuration from application.properties");
            logger.info("URL: {}", sanitizedUrl.replaceAll(":[^:@]+@", ":****@"));
            return properties;
        }

        // If we get here, no database configuration was found
        logger.error("================================================");
        logger.error("DATABASE CONFIGURATION ERROR:");
        logger.error("No database connection details found!");
        logger.error("Please ensure one of the following is set:");
        logger.error("1. Railway MySQL service is linked to your app service");
        logger.error("2. Or set SPRING_DATASOURCE_URL environment variable (NOT mysql.railway.internal)");
        logger.error("3. Or set MYSQLHOST, MYSQLDATABASE, MYSQLUSER, MYSQLPASSWORD");
        logger.error("4. Or configure spring.datasource.* in application.properties");
        logger.error("================================================");

        throw new IllegalStateException(
                "Database configuration not found. Please link Railway MySQL service or set valid database connection variables. "
                        +
                        "If SPRING_DATASOURCE_URL contains 'mysql.railway.internal', please unset it to use application.properties.");
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String url = properties.getUrl();
        if (url != null) {
            try {
                String tempUrl = url;
                if (url.startsWith("jdbc:")) {
                    tempUrl = url.substring(5);
                }

                URI dbUri = new URI(tempUrl);
                if (dbUri.getUserInfo() != null) {
                    logger.info(
                            "DataSource URL contains credentials. Parsing and stripping them for driver compatibility...");
                    String[] userInfo = dbUri.getUserInfo().split(":");
                    String username = userInfo[0];
                    String password = userInfo.length > 1 ? userInfo[1] : "";
                    String host = dbUri.getHost();
                    int port = dbUri.getPort();
                    String path = dbUri.getPath();
                    String scheme = dbUri.getScheme();

                    if ("postgres".equals(scheme)) {
                        scheme = "postgresql";
                    }

                    String cleanJdbcUrl = "jdbc:" + scheme + "://" + host + (port != -1 ? ":" + port : "") + path;

                    properties.setUrl(cleanJdbcUrl);
                    properties.setUsername(username);
                    properties.setPassword(password);

                    if ("postgresql".equals(scheme)) {
                        properties.setDriverClassName("org.postgresql.Driver");
                    } else if ("mysql".equals(scheme)) {
                        properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    }

                    logger.info("Reconstructed Clean JDBC URL: {}", cleanJdbcUrl);
                } else {
                    // Even if no userInfo, ensure jdbc: prefix is there
                    if (!url.startsWith("jdbc:")) {
                        properties.setUrl("jdbc:" + url);
                    }
                    // Explicitly set driver based on URL if not set above
                    if (properties.getUrl().contains("postgresql")) {
                        properties.setDriverClassName("org.postgresql.Driver");
                    } else if (properties.getUrl().contains("mysql")) {
                        properties.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to parse/clean URL in dataSource method: {}", e.getMessage());
                // Fallback: Ensure jdbc prefix at minimum
                if (!url.startsWith("jdbc:")) {
                    properties.setUrl("jdbc:" + url);
                }
            }
        }

        logger.info("Creating DataSource with URL: {}",
                properties.getUrl() != null ? properties.getUrl().replaceAll(":[^:@]+@", ":****@") : "null");

        return properties.initializeDataSourceBuilder().build();
    }
}

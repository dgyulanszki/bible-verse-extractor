package hu.szegedibibliaszol.app.service;

import java.nio.file.Files;
import java.nio.file.Path;

final class SqliteDatabaseSupport {

    SqliteDatabaseSupport() {
    }

    static boolean databaseFileExists(Path databasePath) {
        return databasePath != null && Files.isRegularFile(databasePath);
    }

    static boolean hasMissingTable(Throwable throwable, String tableName) {
        Throwable currentThrowable = throwable;
        while (currentThrowable != null) {
            String message = currentThrowable.getMessage();
            if (message != null && message.contains("no such table: " + tableName)) {
                return true;
            }
            currentThrowable = currentThrowable.getCause();
        }
        return false;
    }
}


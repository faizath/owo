package com.owotest.support;

import com.owo.utils.DBHelper;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gives a test its own empty database.
 *
 * <p>Tests must never touch {@code src/app/owo.db}, which holds development data.
 * Use with {@code @BeforeEach} / {@code @AfterEach} so each test starts from a clean schema.
 */
public final class TempDatabase implements AutoCloseable {
    private final Path file;

    public TempDatabase() throws Exception {
        file = Files.createTempFile("owo-test-", ".db");
        // createTempFile leaves an empty file behind; SQLite is happy to adopt it.
        DBHelper.setDatabasePath(file);
        DBHelper.initializeDatabase();
    }

    public Path path() {
        return file;
    }

    @Override
    public void close() throws Exception {
        DBHelper.setDatabasePath(null);
        Files.deleteIfExists(file);
    }
}

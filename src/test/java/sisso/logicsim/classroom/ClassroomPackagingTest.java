package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.Properties;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;

/** Runs on Windows/Linux CI using only the actual plugin JAR and platform Java classes. */
class ClassroomPackagingTest {
    @TempDir Path directory;
    @Test void productionJarContainsAndLoadsSqliteWithItsNativeResources() throws Exception {
        var path = Path.of(System.getProperty("logicgates.productionJar"));
        try (var jar = new JarFile(path.toFile())) {
            for (String resource : new String[]{"org/sqlite/JDBC.class", "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll",
                    "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so", "META-INF/services/java.sql.Driver"}) {
                assertNotNull(jar.getEntry(resource), resource);
            }
            assertNull(jar.getEntry("com/hypixel/hytale/server/core/HytaleServer.class"), "Never bundle the server dependency");
        }
        try (var loader = new URLClassLoader(new java.net.URL[]{path.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var driver = (Driver) Class.forName("org.sqlite.JDBC", true, loader).getConstructor().newInstance();
            try (var connection = driver.connect("jdbc:sqlite:" + directory.resolve("packaged.db"), new Properties());
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE smoke(value TEXT)");
                statement.execute("INSERT INTO smoke VALUES('00123')");
                try (var rows = statement.executeQuery("SELECT value FROM smoke")) {
                    assertTrue(rows.next());
                    assertEquals("00123", rows.getString(1));
                }
            }
        }
    }
}

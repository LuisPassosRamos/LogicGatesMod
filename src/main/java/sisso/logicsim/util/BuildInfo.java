package sisso.logicsim.util;

import java.io.IOException;
import java.util.Properties;

/** Identity of the compiled sources, independent of the JAR filename or installed checkout. */
public final class BuildInfo {
    private static final Properties VALUES = load();
    public static final String VERSION = VALUES.getProperty("version", "unknown");
    public static final String BUILD_ID = VALUES.getProperty("buildId", "unknown");
    public static final String HYTALE_VERSION = VALUES.getProperty("hytaleVersion", "unknown");

    private BuildInfo() { }

    private static Properties load() {
        var values = new Properties();
        try (var input = BuildInfo.class.getResourceAsStream("/logicgates-build.properties")) {
            if (input != null) values.load(input);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read LogicGates build identity", error);
        }
        return values;
    }
}

package sisso.logicsim;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import sisso.logicsim.util.BuildInfo;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class BuildInfoTest {
    @Test void packagedIdentityMatchesManifestAndContainsAStableSourceFingerprint() throws Exception {
        var values = new Properties();
        try (var input = getClass().getResourceAsStream("/logicgates-build.properties")) {
            assertNotNull(input);
            values.load(input);
        }
        try (var input = getClass().getResourceAsStream("/manifest.json")) {
            assertNotNull(input);
            var manifest = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(manifest.get("Version").getAsString(), BuildInfo.VERSION);
            assertEquals(manifest.get("ServerVersion").getAsString(), "=" + BuildInfo.HYTALE_VERSION);
        }
        String fingerprint = values.getProperty("sourceFingerprint");
        assertTrue(fingerprint.matches("[a-f0-9]{64}"));
        assertEquals(BuildInfo.VERSION + "+" + fingerprint.substring(0, 12), BuildInfo.BUILD_ID);
    }
}

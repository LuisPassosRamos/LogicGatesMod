package sisso.logicsim.classroom.learning;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import sisso.logicsim.circuit.CircuitNode.Kind;

import java.util.*;
import java.util.regex.Pattern;

import static sisso.logicsim.classroom.learning.LearningException.Code.INVALID_RULES;

/** Publication-owned teaching criteria. Missing quantities are unrestricted; zero forbids a kind. */
public record VerificationRules(Map<Kind, Integer> quantities, String table) {
    public static final int MAX_INPUTS = 6;
    public static final int MAX_OUTPUTS = 4;
    public static final int MAX_COMPONENTS = 2048;
    public static final int MAX_QUANTITY = 16384;
    private static final int MAX_TABLE_LENGTH = 4096;
    private static final int MAX_ENCODED_LENGTH = 16384;
    private static final Pattern QUANTITY = Pattern.compile("[0-9]{1,5}");
    private static final Pattern INPUT_BITS = Pattern.compile("[01]{1," + MAX_INPUTS + "}");
    private static final Pattern OUTPUT_BITS = Pattern.compile("[01]{1," + MAX_OUTPUTS + "}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    public static final VerificationRules EMPTY = new VerificationRules(Map.of(), "");

    public record TruthCase(String inputs, String outputs) { }

    public VerificationRules {
        var copy = new EnumMap<Kind, Integer>(Kind.class);
        if (quantities != null) {
            quantities.forEach((kind, count) -> {
                if (kind == null || count == null || count < 0 || count > MAX_QUANTITY) {
                    throw new LearningException(INVALID_RULES);
                }
                copy.put(kind, count);
            });
        }
        quantities = Collections.unmodifiableMap(copy);
        table = table == null ? "" : table.strip();
        if (table.length() > MAX_TABLE_LENGTH || table.codePoints().anyMatch(VerificationRules::unsupportedControl)) {
            throw new LearningException(INVALID_RULES);
        }
    }

    public boolean configured() {
        return !quantities.isEmpty() || !table.isEmpty();
    }

    public static Integer quantity(String value) {
        if (value == null || value.isBlank()) return null;
        if (!QUANTITY.matcher(value.strip()).matches()) throw new LearningException(INVALID_RULES);
        int result = Integer.parseInt(value.strip());
        if (result > MAX_QUANTITY) throw new LearningException(INVALID_RULES);
        return result;
    }

    /** Incomplete truth tables are allowed in a private draft, never in a publication. */
    public List<TruthCase> cases() {
        if (table.isBlank()) return List.of();
        var rows = new TreeMap<String, String>();
        int inputs = 0;
        int outputs = 0;
        for (String line : table.lines().toList()) {
            if (line.isBlank()) continue;
            String[] pair = WHITESPACE.matcher(line).replaceAll("").split("->", -1);
            if (pair.length != 2 || !INPUT_BITS.matcher(pair[0]).matches() || !OUTPUT_BITS.matcher(pair[1]).matches()) {
                throw new LearningException(INVALID_RULES);
            }
            if (inputs == 0) {
                inputs = pair[0].length();
                outputs = pair[1].length();
            }
            if (pair[0].length() != inputs || pair[1].length() != outputs || rows.putIfAbsent(pair[0], pair[1]) != null) {
                throw new LearningException(INVALID_RULES);
            }
        }
        if (inputs == 0 || rows.size() != (1 << inputs)
                || !quantityMatches(Kind.LEVER, inputs) || !quantityMatches(Kind.LAMP, outputs)) {
            throw new LearningException(INVALID_RULES);
        }
        return rows.entrySet().stream().map(entry -> new TruthCase(entry.getKey(), entry.getValue())).toList();
    }

    public String encode() {
        if (!configured()) return "";
        var object = new JsonObject();
        object.addProperty("format", 1);
        var counts = new JsonObject();
        quantities.forEach((kind, count) -> counts.addProperty(kind.name(), count));
        object.add("quantities", counts);
        object.addProperty("table", table);
        return object.toString();
    }

    public static VerificationRules decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return EMPTY;
        try {
            if (encoded.length() > MAX_ENCODED_LENGTH) throw new IllegalArgumentException();
            var object = JsonParser.parseString(encoded).getAsJsonObject();
            if (object.get("format").getAsInt() != 1) throw new IllegalArgumentException();
            var counts = new EnumMap<Kind, Integer>(Kind.class);
            for (var entry : object.getAsJsonObject("quantities").entrySet()) {
                var count = quantity(entry.getValue().getAsString());
                if (count == null) throw new IllegalArgumentException();
                counts.put(Kind.valueOf(entry.getKey()), count);
            }
            return new VerificationRules(counts, object.get("table").getAsString());
        } catch (RuntimeException failure) {
            throw new LearningException(INVALID_RULES, failure);
        }
    }

    private boolean quantityMatches(Kind kind, int actual) {
        Integer required = quantities.get(kind);
        return required == null || required == actual;
    }

    private static boolean unsupportedControl(int character) {
        return Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t';
    }
}

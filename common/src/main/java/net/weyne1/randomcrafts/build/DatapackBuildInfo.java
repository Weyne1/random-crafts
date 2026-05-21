package net.weyne1.randomcrafts.build;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatapackBuildInfo {
    private static final String RESOURCE_PATH = "/randomcrafts-datapack.properties";

    private static final Properties PROPERTIES = load();

    private DatapackBuildInfo() {}

    public static String datapackName() {
        return requiredString("datapack_name");
    }

    public static String datapackDescription() {
        return requiredString("datapack_description");
    }

    public static JsonElement minPackFormat() {
        return parseFormat("min_pack_format");
    }

    public static JsonElement maxPackFormat() {
        return parseFormat("max_pack_format");
    }

    private static JsonElement parseFormat(String key) {
        String raw = requiredString(key);
        try {
            return JsonParser.parseString(raw);
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Invalid JSON format for '" + key + "' in " + RESOURCE_PATH + ": '" + raw + "'", e);
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = DatapackBuildInfo.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing build metadata resource: " + RESOURCE_PATH);
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read build metadata resource: " + RESOURCE_PATH, e);
        }
        return props;
    }

    private static String requiredString(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing '" + key + "' in " + RESOURCE_PATH);
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("Blank '" + key + "' in " + RESOURCE_PATH);
        }
        if (trimmed.contains("${")) {
            throw new IllegalStateException(
                    "Unexpanded placeholder for '" + key + "' in " + RESOURCE_PATH + ": '" + trimmed + "'. " +
                            "Make sure processResources expands randomcrafts-datapack.properties."
            );
        }
        return trimmed;
    }
}
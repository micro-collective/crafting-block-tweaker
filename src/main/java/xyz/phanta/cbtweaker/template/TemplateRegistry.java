package xyz.phanta.cbtweaker.template;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.phanta.cbtweaker.CbtMod;
import xyz.phanta.cbtweaker.util.ConfigException;
import xyz.phanta.cbtweaker.util.helper.DataLoadUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class TemplateRegistry<T> {

    private final String templateTypeName;
    private final Path templateFile;
    private final Resolver<T> resolver;
    private final Map<String, T> templateTable = new HashMap<>();

    public TemplateRegistry(String templateTypeName, Path templateFile, Resolver<T> resolver) {
        this.templateTypeName = templateTypeName;
        this.templateFile = templateFile;
        this.resolver = resolver;
    }

    public void loadTemplates() {
        try {
            if (!Files.isRegularFile(templateFile)) {
                return;
            }
            JsonObject templatesDto = DataLoadUtils.readJsonFile(templateFile).getAsJsonObject();
            for (Map.Entry<String, JsonElement> templateEntry : templatesDto.entrySet()) {
                try {
                    templateTable.put(templateEntry.getKey(), resolver.resolve(templateEntry.getValue()));
                } catch (ConfigException e) {
                    CbtMod.LOGGER.warn("Ignoring bad " + templateTypeName + " template: " + templateEntry.getKey(), e);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load " + templateTypeName + " templates!", e);
        }
    }

    public T resolve(JsonElement dto) {
        if (dto.isJsonPrimitive() && dto.getAsJsonPrimitive().isString()) {
            String s = dto.getAsString();
            if (!s.isEmpty() && s.charAt(0) == '#') {
                T template = templateTable.get(s.substring(1));
                if (template == null) {
                    throw new ConfigException("Unknown " + templateTypeName + " template: " + s);
                }
                return template;
            }
        }
        return resolver.resolve(dto);
    }

    public interface Resolver<T> {

        T resolve(JsonElement dto);

    }

}

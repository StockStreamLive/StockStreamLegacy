package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class JSONUtil {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> Optional<T> deserializeString(final String jsonString,
                                                    final TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(jsonString)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(jsonString, typeReference));
        } catch (IOException e) {
            log.warn("Could not deserialize string {} {}", jsonString, e);
        }

        return Optional.empty();
    }

    public static <T> Optional<T> loadObjectFromFile(final String path, final TypeReference valueType) {
        try {
            T objectInstance = objectMapper.readValue(new File(path), valueType);
            return Optional.of(objectInstance);
        } catch (IOException e) {
            log.warn("{}", e);
        }
        return Optional.empty();
    }

    public static <T> List<T> loadObjectsFromFile(final String path, final Class<T> valueType) {
        try {
            return objectMapper.readValue(new File(path), new TypeReference() {
                @Override
                public int compareTo(Object o) {
                    return 0;
                }
            });
        } catch (IOException e) {
            log.warn("{}", e);
        }
        return Collections.emptyList();
    }

    public static void saveObjectToFile(final String path, final Object object) {
        Optional<String> objectStr = serializeObject(object);
        if (objectStr.isPresent()) {
            try {
                try (PrintWriter out = new PrintWriter(path)) {
                    out.println(objectStr.get());
                }
            } catch (FileNotFoundException e) {
                log.warn("{}", e);
            }
        }
    }

    public static Optional<String> serializeObject(final Object object) {
        return serializeObject(object, false);
    }

    public static Optional<String> serializeObject(final Object object, boolean prettyPrint) {
        final SerializationConfig existingConfig = objectMapper.getSerializationConfig();

        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, prettyPrint);

        try {
            return Optional.of(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage(), e);
            return Optional.empty();
        } finally {
            objectMapper.setConfig(existingConfig);
        }
    }

    public static <T> Optional<T> deserializeObject(final String jsonData, final Class<T> valueType) {
        try {
            return Optional.of(objectMapper.readValue(jsonData, valueType));
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            return Optional.empty();
        }
    }
}

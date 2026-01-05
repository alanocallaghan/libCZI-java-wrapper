package uk.ac.ed.eci.libCZI.document;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.Map;

public class DimensionInfo {
    private final Map<String, DimensionInfoChannel> channels;

    public DimensionInfo(String string) throws JsonProcessingException {
        // todo from JSON string...
        TypeReference<Map<String, DimensionInfoChannel>> typeReference = new TypeReference<Map<String, DimensionInfoChannel>>() {};
        this.channels = new ObjectMapper().
                readValue(string, typeReference);
    }

    public Map<String, DimensionInfoChannel> getChannels() {
        return channels;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DimensionInfoChannel {
        @JsonAlias("attribute_name")
        private String name;
        private String color;
        public String getName() {
            return name;
        }
        public String getColor() {
            return color;
        }
    }
}

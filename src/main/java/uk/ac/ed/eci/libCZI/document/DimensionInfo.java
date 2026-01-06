package uk.ac.ed.eci.libCZI.document;

import com.fasterxml.jackson.core.JsonProcessingException;

public class DimensionInfo {
    // JSON string, consumers can parse it
    private final String string;

    public DimensionInfo(String string) {
        this.string = string;
    }

    public String getJSONString() {
        return string;
    }
}

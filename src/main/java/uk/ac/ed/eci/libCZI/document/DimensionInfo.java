package uk.ac.ed.eci.libCZI.document;

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

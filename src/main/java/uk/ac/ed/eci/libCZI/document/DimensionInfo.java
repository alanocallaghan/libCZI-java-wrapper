package uk.ac.ed.eci.libCZI.document;

public class DimensionInfo {
    private final String string;

    public DimensionInfo(String string) {
        // todo from JSON string...
        this.string = string;
    }

    public String toString() {
        return this.string;
    }
}

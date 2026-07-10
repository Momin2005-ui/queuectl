package org.example.model;


public enum StateWorker {
    STOPPED("stopped"),
    RUNNING("running");

    private final String value;

    StateWorker(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StateWorker fromValue(String value) {
        for (StateWorker s : values()) {
            if (s.value.equals(value)) return s;
        }
        throw new IllegalArgumentException("Unknown job state: " + value);
    }
}

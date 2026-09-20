package com.bestbuy.api.dto;

import java.util.List;

public class ErrorResponse {

    private String name;
    private String message;
    private int code;
    private String className;
    private List<String> errors;

    public ErrorResponse(String name, String message, int code, String className, List<String> errors) {
        this.name = name;
        this.message = message;
        this.code = code;
        this.className = className;
        this.errors = errors;
    }

    public String getName() { return name; }
    public String getMessage() { return message; }
    public int getCode() { return code; }
    public String getClassName() { return className; }
    public List<String> getErrors() { return errors; }
}

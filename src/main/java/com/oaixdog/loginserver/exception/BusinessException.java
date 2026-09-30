package com.oaixdog.loginserver.exception;

public class BusinessException extends RuntimeException {

    public static final int DEFAULT_CODE = 100001;

    private final int code;

    public BusinessException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

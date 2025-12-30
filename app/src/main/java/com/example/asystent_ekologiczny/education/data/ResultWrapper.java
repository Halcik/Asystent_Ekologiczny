package com.example.asystent_ekologiczny.education.data;

public class ResultWrapper<T> {
    private final T data;
    private final Throwable error;

    private ResultWrapper(T data, Throwable error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ResultWrapper<T> success(T data) {
        return new ResultWrapper<>(data, null);
    }

    public static <T> ResultWrapper<T> error(Throwable error) {
        return new ResultWrapper<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public T getData() {
        return data;
    }

    public Throwable getError() {
        return error;
    }
}


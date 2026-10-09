package com.frauddetect.util;

/**
 * RUBRIC: 2 - Collections & Generics
 * Generic Result<T> container class representing either a successful operation
 * producing data of type T, or a failure with a descriptive error message.
 * Avoids returning null and standardizes service layer responses.
 *
 * @param <T> the type of value returned on success
 */
public class Result<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private Result(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /**
     * Creates a successful Result carrying data.
     *
     * @param data the result payload
     * @param <T>  the payload type
     * @return successful Result<T>
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, data, "Operation completed successfully.");
    }

    /**
     * Creates a successful Result carrying data and a custom message.
     *
     * @param data    the result payload
     * @param message descriptive success message
     * @param <T>     the payload type
     * @return successful Result<T>
     */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(true, data, message);
    }

    /**
     * Creates a failed Result with an error message and null data.
     *
     * @param message failure explanation
     * @param <T>     the payload type
     * @return failed Result<T>
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, null, message);
    }

    /**
     * Creates a failed Result with an error message and optional partial data.
     *
     * @param message failure explanation
     * @param data    partial or fallback data
     * @param <T>     the payload type
     * @return failed Result<T>
     */
    public static <T> Result<T> error(String message, T data) {
        return new Result<>(false, data, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("Result[success=%b, message='%s', data=%s]", success, message, data);
    }
}

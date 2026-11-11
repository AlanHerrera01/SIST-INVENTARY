package com.veris.inventory.common.error;

/**
 * Error de API con estado HTTP y código estable. El detalle es seguro para
 * el cliente: nunca incluye SQL, trazas ni datos internos.
 */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    public ApiException(int status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
    public String detail() { return getMessage(); }

    public static ApiException badRequest(String code, String detail) {
        return new ApiException(400, code, detail);
    }

    public static ApiException forbidden(String code, String detail) {
        return new ApiException(403, code, detail);
    }

    public static ApiException notFound(String code, String detail) {
        return new ApiException(404, code, detail);
    }

    public static ApiException conflict(String code, String detail) {
        return new ApiException(409, code, detail);
    }

    public static ApiException unprocessable(String code, String detail) {
        return new ApiException(422, code, detail);
    }

    public static ApiException unavailable(String code, String detail) {
        return new ApiException(503, code, detail);
    }
}

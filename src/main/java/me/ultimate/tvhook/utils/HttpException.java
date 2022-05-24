package me.ultimate.tvhook.utils;

public class HttpException extends Exception {
    private final int status;


    public HttpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

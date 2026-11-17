package io.github.sanket1035.javajudge.service;

/** Thrown when all run slots are taken; the API answers 503 so the client can retry. */
public class ServerBusyException extends RuntimeException {
    public ServerBusyException() {
        super("Server is busy running other submissions. Try again in a few seconds.");
    }
}

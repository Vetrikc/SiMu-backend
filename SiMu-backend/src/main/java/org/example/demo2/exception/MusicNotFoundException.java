package org.example.demo2.exception;
public class MusicNotFoundException extends RuntimeException {
    public MusicNotFoundException(String message) { super(message); }
}

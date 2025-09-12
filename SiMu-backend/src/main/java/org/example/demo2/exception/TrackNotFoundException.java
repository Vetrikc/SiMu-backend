package org.example.demo2.exception;

public class TrackNotFoundException extends RuntimeException {
    public TrackNotFoundException(String message) { super(message); }
}
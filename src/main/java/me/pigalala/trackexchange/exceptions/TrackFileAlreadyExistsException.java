package me.pigalala.trackexchange.exceptions;

// TODO: Remove this pls :)
public class TrackFileAlreadyExistsException extends RuntimeException {

    public TrackFileAlreadyExistsException() {
        super("A TrackExchange file with this name already exists");
    }
}

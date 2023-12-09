package me.pigalala.trackexchange.exceptions;

public class TrackFileAlreadyExistsException extends TrackExchangeException {

    public TrackFileAlreadyExistsException() {
        super("A TrackExchange file with this name already exists");
    }
}

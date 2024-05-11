package pt.tecnico.distledger.namingserver.domain.exceptions;

public class CantRemoveServerException extends IllegalArgumentException {


    public CantRemoveServerException() {
        super("Can't remove server");
    }
}
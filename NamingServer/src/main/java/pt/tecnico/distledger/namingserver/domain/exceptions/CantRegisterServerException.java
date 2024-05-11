package pt.tecnico.distledger.namingserver.domain.exceptions;

public class CantRegisterServerException extends IllegalArgumentException {


    public CantRegisterServerException() {
        super("ServerAlreadyAdded.");
    }
}
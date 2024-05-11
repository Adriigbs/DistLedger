package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.util.VectorClock;

public class Operation {
    private String account;
    private VectorClock prev;
    private VectorClock operationTS;
    private boolean stable;
    public enum type {
        UNS,
        CREATE,
        DELETE,
        TRANSFER

    }

    public Operation(String fromAccount) {
        this.account = fromAccount;
        this.prev = new VectorClock();
        this.operationTS = new VectorClock();
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public type getType() {
        return type.UNS;
    }

    public boolean isStable() {
        return stable;
    }

    public void setStable(boolean stable) {
        this.stable = stable;
    }

    public void setOperationTS(VectorClock operationTS) {
        this.operationTS = operationTS;
    }

    public void setPrev(VectorClock prev) {
        this.prev = prev;
    }

    public VectorClock getOperationTS() {
        return operationTS;
    }

    public VectorClock getPrev() {
        return prev;
    }
}

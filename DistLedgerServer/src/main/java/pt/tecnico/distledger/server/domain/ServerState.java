package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.util.VectorClock;


import java.util.*;

import java.io.IOException;

public class ServerState {
    private List<Operation> ledger;
    private Map<String, Integer> accounts;
    private boolean activated;
    private List<Operation> backupLedger;
    private Map<String, Integer> backupAccounts;

    private VectorClock replicaTS;
    private VectorClock valueTS;

    private int id;

    public ServerState(String qualifier) {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.activated = true;
        this.replicaTS = new VectorClock();
        this.valueTS = new VectorClock();
        accounts.put("broker", 1000);

        switch (qualifier) {
            case "A":
                this.id = 0;
                break;
            case "B":
                this.id = 1;
                break;
        }
    }

    public synchronized void createAccount(Operation op) {

        CreateOp operation = (CreateOp) op;
        String userId = operation.getAccount();
        if(!activated)
            return;
        if (accounts.containsKey(userId))
            return;

        accounts.put(userId, 0);

        valueTS.merge(operation.getOperationTS());

    }

    public synchronized VectorClock addCreateOp(String userId, VectorClock prev) {

        replicaTS.setTS(id, replicaTS.getTS(id) + 1);

        Operation op = new CreateOp(userId);
        op.setPrev(prev.copy());
        op.setOperationTS(replicaTS.copy());
        ledger.add(op);

        if (valueTS.GreaterOrEqual(prev)) {
            op.setStable(true);
            createAccount(op);
        } else {
            op.setStable(false);
        }

        return op.getOperationTS().copy();
    }

    public synchronized void deleteAccount(String userId) throws Exception {

        if(!activated)
            throw new Exception("UNAVAILABLE");
        if (!accounts.containsKey(userId))
            throw new Exception("wrong parameters");
        if (accounts.get(userId) > 0) {
            throw new Exception("Balance need to be 0 to delete account");
        }

        accounts.remove(userId);
    }

    public synchronized int balance(String userId) throws Exception {

        if(!activated)
            throw new Exception("UNAVAILABLE");
        if (!accounts.containsKey(userId))
            throw new Exception("User does not exist.");

        System.out.println("Balance Value:" + accounts.get(userId));
        return accounts.get(userId);
    }

    public synchronized void transferTo(Operation op) {

        TransferOp operation = (TransferOp) op;

        if (!activated)
            return;
        if (!accounts.containsKey(operation.getAccount()) || !accounts.containsKey(operation.getDestAccount())
                || accounts.get(operation.getAccount()) < operation.getAmount() || operation.getAmount() <= 0)
            return;

        int sender_balance = accounts.get(operation.getAccount()) - operation.getAmount();
        accounts.replace(operation.getAccount(), sender_balance);
        int receiver_balance = accounts.get(operation.getDestAccount()) + operation.getAmount();
        accounts.replace(operation.getDestAccount(), receiver_balance);


        valueTS.merge(operation.getOperationTS());

    }

    public synchronized VectorClock addTransferToOp(String accountFrom, String accountTo, int amount, VectorClock prev) {
        replicaTS.setTS(id, replicaTS.getTS(id) + 1);

        Operation op = new TransferOp(accountFrom, accountTo, amount);
        op.setPrev(prev.copy());
        op.setOperationTS(replicaTS.copy());
        ledger.add(op);

        if (valueTS.GreaterOrEqual(prev)) {
            op.setStable(true);
            transferTo(op);
        } else {
            op.setStable(false);
        }

        return op.getOperationTS().copy();
    }

    public synchronized void addOperation(Operation op) {
        ledger.add(op);
    }

    public VectorClock getReplicaTS() {
        return replicaTS.copy();
    }

    public VectorClock getValueTS() {
        return valueTS.copy();
    }

    public synchronized void activate() {
        this.activated = true;
    }
    public synchronized void deactivate() {
        this.activated = false;
    }

    public synchronized List<Operation> getLedgerState() {
        return new ArrayList<Operation>(ledger);
    }

    public synchronized boolean getState() {
        return activated;
    }


    public synchronized void gossip(List<Operation> operations, VectorClock senderReplicaTS) {

        for (Operation op : operations) {
            if (!replicaTS.GreaterOrEqual(op.getOperationTS())) {
                ledger.add(op);
                if (valueTS.GreaterOrEqual(op.getPrev())) {
                    op.setStable(true);
                    switch (op.getType()) {
                        case CREATE:
                            createAccount(op);
                            break;
                        case TRANSFER:
                            transferTo(op);
                            break;
                    }
                    valueTS.merge(op.getOperationTS());
                } else {
                    op.setStable(false);
                }
            }
        }

        replicaTS.merge(senderReplicaTS);

        for (Operation op : ledger) {
            if (valueTS.GreaterOrEqual(op.getPrev()) && !op.isStable()) {
                op.setStable(true);
                switch (op.getType()) {
                    case CREATE:
                        createAccount(op);
                        break;
                    case TRANSFER:
                        transferTo(op);
                        break;
                }
            } else {
                op.setStable(false);

            }
        }

    }


}
package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.util.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.ProtoServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import java.io.IOException;
import java.util.Objects;

public class UserService {


    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceBlockingStub namingServerStub;
    private ManagedChannel channel;

    private VectorClock prev;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    private final String service = "DistLedger";

    private String currentQualifier;

    public UserService(String host, int port) {

        // Create naming server channel
        this.namingServerChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create stub
        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);

        // Initialize vector clock
        prev = new VectorClock();

    }

    public void createAccount(String server, String userId) {


        if (Objects.equals(currentQualifier, server)) {
            if (!isServerAvailable()) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }
            }
        } else {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }


        CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(userId)
                .setPrev(prevToVC()).build();

        System.out.println(prevToVC());

        try {
            CreateAccountResponse response = stub.createAccount(request);
            prev.merge(response.getNew().getTsList());
            System.out.println("OK");
        } catch (StatusRuntimeException e) {

            if (e.getStatus() == Status.UNAVAILABLE) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                }
                try {
                    CreateAccountResponse response = stub.createAccount(request);
                    prev.merge(response.getNew().getTsList());
                    System.out.println("OK");
                } catch (StatusRuntimeException se) {
                    System.out.println("Caught exception with description: " +
                           se.getStatus().getDescription());
                }
            } else {

                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }
        }

    }

    public void deleteAccount(String server, String userId) {

        if (Objects.equals(currentQualifier, server)) {
            if (!isServerAvailable()) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }
            }
        } else {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        DeleteAccountRequest request = DeleteAccountRequest.newBuilder().build();
        try {
            stub.deleteAccount(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {

            if (e.getStatus() == Status.UNAVAILABLE) {   // If current server fails lookUp another and try again
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }

                /* Normal delete account operation execution */
                try {
                    stub.deleteAccount(request);
                    System.out.println("OK");
                } catch (StatusRuntimeException se) {
                    System.out.println("Caught exception with description: " +
                            se.getStatus().getDescription());
                }

            /* If it wasn't server failure, print the normal operation exceptions */
            } else {

                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }
        }

    }

    public void balance(String server, String userId) {
        if (Objects.equals(currentQualifier, server)) {
            if (!isServerAvailable()) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }
            }
        } else {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        BalanceRequest request = BalanceRequest.newBuilder().setUserId(userId).setPrev(prevToVC()).build();

        try {
            BalanceResponse response = stub.balance(request);
            System.out.println("OK");
            System.out.println(response.getValue());
            System.out.println(response.getNew());
            prev.merge(response.getNew().getTsList());
        } catch (StatusRuntimeException e) {

            if (e.getStatus() == Status.UNAVAILABLE) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }

                try {
                    BalanceResponse response = stub.balance(request);
                    prev.merge(response.getNew().getTsList());
                    System.out.println("OK");
                    System.out.println(response.getValue());
                } catch (StatusRuntimeException se) {
                    System.out.println("Caught exception with description: " +
                            se.getStatus().getDescription());
                }
            } else {
                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }
        }

    }

    public void transferTo(String server, String accountFrom, String accountTo, int amount) {


        if (Objects.equals(currentQualifier, server)) {
            if (!isServerAvailable()) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }
            }
        } else {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(accountFrom).setAccountTo(accountTo)
                .setAmount(amount).setPrev(prevToVC()).build();
        try {
            TransferToResponse response = stub.transferTo(request);
            prev.merge(response.getNew().getTsList());
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.UNAVAILABLE) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }
                try {
                    stub.transferTo(request);
                    System.out.println("OK");
                } catch (StatusRuntimeException se) {
                    System.out.println("Caught exception with description: " +
                            se.getStatus().getDescription());
                }
            } else {
                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }
        }

    }


    /* Method that searches for a new server and sets the channel and stub */
    private boolean lookupAndSet(String qualifier) {
        LookupRequest request = LookupRequest.newBuilder()
                .setService(service)
                .setQualifier(qualifier)
                .build();

        try {

            LookupResponse response = namingServerStub.lookup(request);

            if (response.getServerListCount() == 0) {
                return false;
            }

            ProtoServer server = response.getServerList(0);

            channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort())
                    .usePlaintext().build();

            stub = UserServiceGrpc.newBlockingStub(channel);

            currentQualifier = qualifier;

            return true;



        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());

            return false;
        }
    }


    /* Auxiliary method that checks is server is available or not */
    private boolean isServerAvailable() {

        return channel != null &&
                channel.getState(true) != ConnectivityState.IDLE &&
                channel.getState(true) != ConnectivityState.TRANSIENT_FAILURE;

    }

    /* Transforms the prev vector clock into a proto vector clock */
    public DistLedgerCommonDefinitions.VectorClock prevToVC() {

        DistLedgerCommonDefinitions.VectorClock.Builder vectorClock = DistLedgerCommonDefinitions.VectorClock
                .newBuilder();

        for (int i = 0; i < prev.size(); i++) {
            vectorClock.addTs(prev.getTS(i));
        }

        return vectorClock.build();
    }

}
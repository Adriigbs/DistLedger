package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.*;
import pt.tecnico.distledger.adminclient.util.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;


public class AdminService {

    private VectorClock prev;
    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceBlockingStub namingServerStub;
    private ManagedChannel channel;
    private AdminServiceGrpc.AdminServiceBlockingStub stub;

    private final String service = "DistLedger";

    public AdminService(String host, int port) {

        // Create naming server channel
        this.namingServerChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // Create stub
        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);

        prev = new VectorClock();

    }

    public void activate(String server) {

        if (!isServerAvailable()) {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        ActivateRequest request = ActivateRequest.newBuilder().build();
        try {
            stub.activate(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.UNAVAILABLE) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }

                try {
                    stub.activate(request);
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

    public void deactivate(String server) {


        if (!isServerAvailable()) {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        DeactivateRequest request = DeactivateRequest.newBuilder().build();

        try {
            stub.deactivate(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.UNAVAILABLE) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }

                try {
                    stub.deactivate(request);
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

    public void getLedgerState(String server) {

        if (!isServerAvailable()) {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        getLedgerStateRequest request = getLedgerStateRequest.newBuilder().build();

        try {
            getLedgerStateResponse response = stub.getLedgerState(request);
            System.out.println("OK");
            System.out.println(response);
        } catch (StatusRuntimeException e) {
            if (e.getStatus() == Status.UNAVAILABLE) {
                if (!lookupAndSet(server)) {
                    System.out.println("There is no server available");
                    return;
                }
                try {
                    getLedgerStateResponse response = stub.getLedgerState(request);
                    System.out.println("OK");
                    System.out.println(response);
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

    public void gossip(String server) {

        if (!isServerAvailable()) {
            if (!lookupAndSet(server)) {
                System.out.println("There is no server available");
                return;
            }
        }

        try {
            GossipRequest request = GossipRequest.newBuilder().build();
            stub.gossip(request);
            System.out.println("OK");
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
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

            DistLedgerCommonDefinitions.ProtoServer server = response.getServerList(0);

            channel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort())
                    .usePlaintext().build();

            stub = AdminServiceGrpc.newBlockingStub(channel);

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
}

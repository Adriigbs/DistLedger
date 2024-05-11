package pt.tecnico.distledger.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.util.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.ActivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.DeactivateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.getLedgerStateRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipRequest;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.GossipResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private ServerState currentState;
    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingStub;
    private ManagedChannel crossServerChannel;
    private DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub crossServerStub;
    private String qualifier;

    private Map<String,Integer> idMap;


    public AdminServiceImpl(ServerState state, String qualifier) {
        namingServerChannel = ManagedChannelBuilder.forAddress("localhost", 5001).usePlaintext().build();
        namingStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);
        currentState = state;
        this.qualifier = qualifier;
        idMap = new HashMap<>();
        idMap.put("A", 0);
        idMap.put("B", 1);

    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {

        // Set server state to active
        currentState.activate();

        // Create and send response to client
        ActivateResponse response = ActivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {

        // Set server state to inactive
        currentState.deactivate();

        // Create and send response to client
        DeactivateResponse response = DeactivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {

        List<Operation> operations = currentState.getLedgerState();

        getLedgerStateResponse.Builder responseBuilder = getLedgerStateResponse.newBuilder();

        DistLedgerCommonDefinitions.LedgerState.Builder ledgerState = DistLedgerCommonDefinitions.LedgerState
                .newBuilder();

        for (Operation op : operations) {
            DistLedgerCommonDefinitions.Operation ledger = protoOperation(op);
            ledgerState.addLedger(ledger);
        }

        getLedgerStateResponse response = responseBuilder.setLedgerState(ledgerState).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {

        if (!lookupAndSet()) {
            responseObserver.onError(Status.INTERNAL.withDescription("No server to gossip").asRuntimeException());
        }

        List<Operation> operations = currentState.getLedgerState();

        PropagateStateRequest.Builder propagateRequestBuilder = PropagateStateRequest.newBuilder();
        DistLedgerCommonDefinitions.VectorClock.Builder replicaTSBuilder = DistLedgerCommonDefinitions.VectorClock
                .newBuilder();

        VectorClock timeStamps = currentState.getReplicaTS();

        for (int i = 0; i < 2; i++) {
            replicaTSBuilder.addTs(timeStamps.getTS(i));
        }

        DistLedgerCommonDefinitions.VectorClock replicaTS = replicaTSBuilder.build();
        propagateRequestBuilder.setReplicaTS(replicaTS);

        DistLedgerCommonDefinitions.LedgerState.Builder ledgerState = DistLedgerCommonDefinitions.LedgerState
                .newBuilder();

        for (Operation op : currentState.getLedgerState()) {
            DistLedgerCommonDefinitions.Operation ledger = protoOperation(op);
            ledgerState.addLedger(ledger);
        }

        propagateRequestBuilder.setState(ledgerState.build());

        try {
            crossServerStub.propagateState(propagateRequestBuilder.build());
            GossipResponse response = GossipResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Could not gossip state").asRuntimeException());
        }

    }

    private boolean lookupAndSet() {
        String targetQualifier = qualifier;

        switch (qualifier) {
            case "A":
                targetQualifier = "B";
                break;
            case "B":
                targetQualifier = "A";
                break;
        }

        LookupRequest request = LookupRequest.newBuilder().setService("DistLedger").setQualifier(targetQualifier)
                .build();

        try {
            LookupResponse response =  namingStub.lookup(request);

            if  (response.getServerListCount() == 0) {
                return false;
            }

            DistLedgerCommonDefinitions.ProtoServer server = response.getServerList(0);
            crossServerChannel = ManagedChannelBuilder.forAddress(server.getHost(), server.getPort())
                    .usePlaintext().build();
            crossServerStub = DistLedgerCrossServerServiceGrpc.newBlockingStub(crossServerChannel);
            return true;
        } catch (StatusRuntimeException e) {
            return false;
        }
    }


    /* Method that takes a domain Operation object and returns the proto Operation class that
    corresponds to the same operation */
    private DistLedgerCommonDefinitions.Operation protoOperation(Operation op) {

        DistLedgerCommonDefinitions.Operation.Builder ledger = DistLedgerCommonDefinitions
                .Operation.newBuilder();

        switch (op.getType()) {

            case UNS:
                ledger.setType(DistLedgerCommonDefinitions.OperationType.OP_UNSPECIFIED).build();
                ledger.setStable(op.isStable());
                ledger.setPrev(protoVectorClock(op.getPrev()));
                ledger.setTS(protoVectorClock(op.getOperationTS()));
                break;

            case CREATE:
                ledger.setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT);
                ledger.setUserId(op.getAccount()).build();
                ledger.setStable(op.isStable());
                ledger.setPrev(protoVectorClock(op.getPrev()));
                ledger.setTS(protoVectorClock(op.getOperationTS()));
                break;

            case DELETE:
                ledger.setType(DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT);
                ledger.setUserId(op.getAccount()).build();
                ledger.setStable(op.isStable());
                ledger.setPrev(protoVectorClock(op.getPrev()));
                ledger.setTS(protoVectorClock(op.getOperationTS()));
                break;

            case TRANSFER:
                ledger.setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO);
                ledger.setUserId(op.getAccount());
                ledger.setAmount(((TransferOp) op).getAmount());
                ledger.setDestUserId(((TransferOp) op).getDestAccount()).build();
                ledger.setStable(op.isStable());
                ledger.setPrev(protoVectorClock(op.getPrev()));
                ledger.setTS(protoVectorClock(op.getOperationTS()));
                break;
        }

        return ledger.build();
    }

    private DistLedgerCommonDefinitions.VectorClock protoVectorClock(VectorClock v) {
        DistLedgerCommonDefinitions.VectorClock.Builder vectorClock = DistLedgerCommonDefinitions
                .VectorClock
                .newBuilder();

        for (int i = 0; i < 2; i++) {
            vectorClock.addTs(v.getTS(i));
        }

        return vectorClock.build();
    }
}

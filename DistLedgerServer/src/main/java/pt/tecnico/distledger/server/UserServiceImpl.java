package pt.tecnico.distledger.server;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.w3c.dom.stylesheets.LinkStyle;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.util.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.Operation;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.ProtoServer;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.CreateAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.DeleteAccountResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.BalanceResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToRequest;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.TransferToResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    /** Implementation. */
    private final ServerState serverState;
    private final ManagedChannel namingServerChannel;
    private final NamingServerServiceBlockingStub namingStub;
    private ManagedChannel crossServerChannel;
    private DistLedgerCrossServerServiceBlockingStub crossServerStub;

    private int id;

    public UserServiceImpl(ServerState state, String qual) {
        this.serverState = state;
        namingServerChannel = ManagedChannelBuilder.forAddress("localhost", 5001).usePlaintext().build();
        namingStub = NamingServerServiceGrpc.newBlockingStub(namingServerChannel);

        switch (qual) {
            case "A":
                id = 0;
                break;
            case "B":
                id = 1;
                break;
        }
    }

    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {


        VectorClock prev = new VectorClock(request.getPrev().getTsList());
        debug("Received prev vector: " + prev);
        System.out.println("Prev:" + prev);
        VectorClock newTS = serverState.addCreateOp(request.getUserId(), prev);
        System.out.println("NewTS:" + newTS);
        DistLedgerCommonDefinitions.VectorClock.Builder builder = DistLedgerCommonDefinitions.VectorClock.newBuilder();

        for (int i = 0; i < 2; i++) {
            builder.addTs(newTS.getTS(i));
        }

        CreateAccountResponse response = CreateAccountResponse.newBuilder().setNew(builder.build()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();



    }

    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {

        try {
            synchronized (serverState) {
                serverState.deleteAccount(request.getUserId());
                LedgerState ledgerState = LedgerState.newBuilder()
                        .addLedger(Operation.newBuilder().setUserId(request.getUserId()).setType(OperationType.OP_DELETE_ACCOUNT).build())
                        .build();
                PropagateStateRequest propagateRequest = PropagateStateRequest.newBuilder()
                        .setState(ledgerState)
                        .build();
                crossServerStub.propagateState(propagateRequest);

            }
            serverState.deleteAccount(request.getUserId());
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }

    }

    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {


        VectorClock prev = new VectorClock(request.getPrev().getTsList());


        if (!serverState.getValueTS().GreaterOrEqual(prev)) {
            responseObserver.onError(Status.INTERNAL.withDescription("Server state is outdated").asRuntimeException());
        }



        try {

           int balance = serverState.balance(request.getUserId());
           DistLedgerCommonDefinitions.VectorClock.Builder builder = DistLedgerCommonDefinitions.VectorClock.newBuilder();
            VectorClock newTS = serverState.getValueTS();
           for (int i = 0; i < 2; i++) {
               builder.addTs(newTS.getTS(i));
           }
           BalanceResponse response = BalanceResponse.newBuilder().setValue(balance).setNew(builder.build()).build();
           responseObserver.onNext(response);
           responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }



    }

    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {

        VectorClock prev = new VectorClock(request.getPrev().getTsList());
        VectorClock newTS = serverState.addTransferToOp(request.getAccountFrom(), request.getAccountTo()
                , request.getAmount(), prev);
        DistLedgerCommonDefinitions.VectorClock.Builder builder = DistLedgerCommonDefinitions.VectorClock.newBuilder();

        for (int i = 0; i < 2; i++) {
            builder.addTs(newTS.getTS(i));
        }

        TransferToResponse response = TransferToResponse.newBuilder().setNew(builder.build()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();


    }


}
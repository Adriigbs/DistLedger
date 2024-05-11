package pt.tecnico.distledger.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.domain.util.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;

import java.util.ArrayList;
import java.util.List;

public class CrossServerImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {


    /** Implementation. */
    private ServerState serverState;

    public CrossServerImpl(ServerState state) {
        this.serverState = state;
    }



    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {

        List<Operation> operations = new ArrayList<>();

        for (DistLedgerCommonDefinitions.Operation op : request.getState().getLedgerList()) {
            switch (op.getType()) {

                case OP_CREATE_ACCOUNT:
                    CreateOp createOp = new CreateOp(op.getUserId());
                    createOp.setPrev(new VectorClock(op.getPrev().getTsList()));
                    createOp.setOperationTS(new VectorClock(op.getTS().getTsList()));
                    operations.add(createOp);
                    break;
                case OP_TRANSFER_TO:
                    TransferOp transferOp = new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount());
                    transferOp.setPrev(new VectorClock(op.getPrev().getTsList()));
                    transferOp.setOperationTS(new VectorClock(op.getTS().getTsList()));
                    operations.add(transferOp);
                    break;

            }
        }

        serverState.gossip(operations, new VectorClock(request.getReplicaTS().getTsList()));
        PropagateStateResponse response = PropagateStateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void getState(GetStateRequest request, StreamObserver<GetStateResponse> responseObserver) {
        GetStateResponse response = GetStateResponse.newBuilder().setIsActivated(serverState.getState()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}

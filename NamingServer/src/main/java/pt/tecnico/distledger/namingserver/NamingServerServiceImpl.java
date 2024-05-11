package pt.tecnico.distledger.namingserver;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.tecnico.distledger.namingserver.domain.ServiceEntry;
import pt.tecnico.distledger.namingserver.domain.NamingServer;
import pt.tecnico.distledger.namingserver.domain.exceptions.CantRegisterServerException;
import pt.tecnico.distledger.namingserver.domain.exceptions.CantRemoveServerException;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;

import java.util.List;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private NamingServer namingServer;

    public NamingServerServiceImpl() {
        namingServer = new NamingServer();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        try {
            namingServer.register(request.getService(), request.getQualifier(), request.getHost(), request.getPort());
            RegisterResponse response = RegisterResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (CantRegisterServerException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }




    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {

        String service = request.getService();
        String qualifier = request.getQualifier();

        ServiceEntry serviceEntry = namingServer.getServiceEntry(service);
        List<ServerEntry> servers = serviceEntry.getServerEntries();


        LookupResponse.Builder responseBuilder = LookupResponse.newBuilder();
        for (ServerEntry se : servers) {
            if (request.getQualifierBytes().isEmpty() ||
                    (!request.getQualifierBytes().isEmpty() && qualifier.equals(se.getQualifier()))) {
                ProtoServer server = ProtoServer.newBuilder().setHost(se.getHost())
                        .setService(service)
                        .setHost(se.getHost())
                        .setPort(se.getPort()).build();

                responseBuilder.addServerList(server);
            }
        }

        LookupResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            namingServer.removeServerEntry(request.getService(), request.getHost(), request.getPort());
            DeleteResponse response = DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (CantRemoveServerException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage())
                    .asRuntimeException());
        }

    }
}

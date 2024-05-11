package pt.tecnico.distledger.server;

import io.grpc.*;
import pt.tecnico.distledger.server.domain.*;

import pt.tecnico.distledger.server.domain.util.VectorClock;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;

import java.io.IOException;


public class ServerMain {

    public static void main(String[] args) throws InterruptedException, IOException {

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            return;
        }

        final int port = Integer.parseInt(args[0]);
        System.out.println("Port: " + port);
        final String qualifier = args[1];

        ServerState state = new ServerState(qualifier);

        final BindableService userImpl = new UserServiceImpl(state, qualifier);
        final BindableService adminImpl = new AdminServiceImpl(state, qualifier);
        final BindableService crossImpl = new CrossServerImpl(state);


        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(userImpl)
                .addService(adminImpl)
                .addService(crossImpl)
                .build();



        // Start the server
        server.start();

        // Server threads are running in the background.
        System.out.println("Servers started");

        // Create channel and stub to contact the naming server
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 5001).usePlaintext().build();
        NamingServerServiceBlockingStub stub = NamingServerServiceGrpc.newBlockingStub(channel);

        try {
            RegisterRequest request = RegisterRequest.newBuilder().setService("DistLedger")
                    .setQualifier(qualifier)
                    .setHost("localhost")
                    .setPort(port)
                    .build();

            stub.register(request);


            System.out.println("Server added to the naming server");

        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " +
                    e.getStatus().getDescription());
            server.shutdown();
        }


        new Thread(() -> {
            System.out.println("Press enter to shutdown");
            try {
                System.in.read();
            } catch (IOException e) {
                System.out.println("Could not read.");
            }

            try {
                DeleteRequest request = DeleteRequest.newBuilder()
                        .setService("DistLedger")
                        .setHost("localhost")
                        .setPort(port)
                        .build();


                stub.delete(request);

                System.out.println("Server removed from the naming server");

            } catch (StatusRuntimeException e) {
                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }
            server.shutdown();
            channel.shutdown();
            System.exit(0);
        }).start();




        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();





    }

}

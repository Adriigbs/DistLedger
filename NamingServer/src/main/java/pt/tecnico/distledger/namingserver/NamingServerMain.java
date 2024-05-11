package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.namingserver.domain.ServiceEntry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NamingServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            return;
        }

        final int port = Integer.parseInt(args[0]);

        final BindableService NamingImpl = new NamingServerServiceImpl();

        Server namingServer = ServerBuilder.forPort(port).addService(NamingImpl).build();

        namingServer.start();
        System.out.println("Naming server started");

        namingServer.awaitTermination();

    }

}

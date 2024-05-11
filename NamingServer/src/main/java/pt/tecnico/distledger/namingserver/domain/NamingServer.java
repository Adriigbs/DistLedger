package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.domain.exceptions.CantRegisterServerException;
import pt.tecnico.distledger.namingserver.domain.exceptions.CantRemoveServerException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamingServer {
    private Map<String, ServiceEntry> services;

    public NamingServer() {
        services = new HashMap<String, ServiceEntry>();
    }

    public synchronized void register(String serviceName, String qualifier, String host, int port) throws CantRegisterServerException{

        ServiceEntry serviceEntry;

        if (services.containsKey(serviceName)) {
            serviceEntry = services.get(serviceName);
        } else {
            serviceEntry = new ServiceEntry(serviceName);
        }

        ServerEntry serverEntry = new ServerEntry(port, host, qualifier);

        serviceEntry.addEntry(serverEntry);

        services.putIfAbsent(serviceName, serviceEntry);

        System.out.println("Added qualifier: " + qualifier);
        System.out.println("Added host: " + host);
        System.out.println("Added port: " + port);
    }

    public synchronized ServiceEntry getServiceEntry(String service) {
        return services.get(service);
    }

    public synchronized void removeServerEntry(String service, String host, int port) throws CantRemoveServerException {
        if (!services.containsKey(service)) {
            throw new CantRemoveServerException();
        }
        services.get(service).removeServerEntry(host, port);
    }



}

package pt.tecnico.distledger.namingserver.domain;


import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.tecnico.distledger.namingserver.domain.exceptions.CantRegisterServerException;
import pt.tecnico.distledger.namingserver.domain.exceptions.CantRemoveServerException;

import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {

    private String serviceName;
    private List<ServerEntry> serverEntries;

    public ServiceEntry(String name) {
        serverEntries = new ArrayList<ServerEntry>();
        serviceName = name;
    }


    public void addEntry(ServerEntry entry) throws CantRegisterServerException{
        for (ServerEntry se : serverEntries) {
            if (se.equals(entry)) {
                throw new CantRegisterServerException();
            }
        }
        serverEntries.add(entry);
    }

    public List<ServerEntry> getServerEntries() {

        return new ArrayList<ServerEntry>(serverEntries);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String name) {
        serviceName = name;
    }

    public void removeServerEntry(String host, int port) throws CantRemoveServerException {
      if (!serverEntries.removeIf(server -> server.getHost().equals(host) && port == server.getPort())) {
          throw new CantRemoveServerException();
      }
    }
}

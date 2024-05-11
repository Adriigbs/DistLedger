package pt.tecnico.distledger.namingserver.domain;



public class ServerEntry {

    private String qualifier;
    private String host;
    private int port;

    public ServerEntry(int port, String host, String qualifier) {
        this.qualifier = qualifier;
        this.port = port;
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ServerEntry)) {
            return false;
        }

        ServerEntry server = (ServerEntry) o;

        return this.port == server.getPort() &&
                (this.host).equals(server.getHost());
    }
}

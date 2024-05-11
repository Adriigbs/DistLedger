package pt.tecnico.distledger.namingserver.domain.util;

import java.util.ArrayList;

public class VectorClock {

    private final ArrayList<Integer> timeStamps;


    public VectorClock() {
        timeStamps = new ArrayList<>();
    }

    public Integer getTS(Integer i) {
        return timeStamps.get(i);
    }

    public void setTS(Integer i, Integer value) {
        timeStamps.set(i, value);
    }

    public boolean GreaterOrEqual(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (timeStamps.get(i) < v.getTS(i)) {
                return false;
            }
        }

        return true;
    }
}

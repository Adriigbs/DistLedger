package pt.tecnico.distledger.server.domain.util;

import java.util.ArrayList;
import java.util.List;

public class VectorClock {

    private final ArrayList<Integer> timeStamps;


    public VectorClock() {
        timeStamps = new ArrayList<>();
        timeStamps.add(0);
        timeStamps.add(0);
    }

    public VectorClock(List<Integer> timeStamps) {
        this.timeStamps = new ArrayList<>(timeStamps);
    }

    public VectorClock copy() {
        return new VectorClock(timeStamps);
    }

    public Integer getTS(Integer i) {
        return timeStamps.get(i);
    }

    public void setTS(Integer i, Integer value) {
        timeStamps.set(i, value);
    }

    public void merge(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (v.getTS(i) > timeStamps.get(i)) {
                timeStamps.set(i,v.getTS(i));
            }
        }
    }

    public boolean GreaterOrEqual(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (timeStamps.get(i) < v.getTS(i)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public String toString() {
        return timeStamps.toString();
    }

}

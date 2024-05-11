package pt.tecnico.distledger.userclient.util;

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

    public Integer getTS(Integer i) {
        return timeStamps.get(i);
    }

    public int size() {
        return timeStamps.size();
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

    public void merge(List<Integer> TS) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (TS.get(i) > timeStamps.get(i)) {
                setTS(i, TS.get(i));
            }
        }
    }
}

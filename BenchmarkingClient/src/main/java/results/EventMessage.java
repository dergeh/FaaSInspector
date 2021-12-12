package results;

public class EventMessage {

    private String reqID;
    private int billedDuration;
    private int maxMem;
    private int usedMem;

    public String getReqID() {
        return reqID;
    }

    public void setReqID(String reqID) {
        this.reqID = reqID;
    }

    public int getBilledDuration() {
        return billedDuration;
    }

    public void setBilledDuration(int billedDuration) {
        this.billedDuration = billedDuration;
    }

    public int getMaxMem() {
        return maxMem;
    }

    public void setMaxMem(int maxMem) {
        this.maxMem = maxMem;
    }

    public int getUsedMem() {
        return usedMem;
    }

    public void setUsedMem(int usedMem) {
        this.usedMem = usedMem;
    }

    public EventMessage(String reqID, int billedDuration, int maxMem, int usedMem) {
        this.reqID = reqID;
        this.billedDuration = billedDuration;
        this.maxMem = maxMem;
        this.usedMem = usedMem;
    }

    @Override
    public String toString() {
        return reqID + ',' + billedDuration + ',' + maxMem + ',' + usedMem;
    }
}

package ua.drovolskyi.os.scheduling_simulator;

import java.util.Comparator;

public class Process {
    private final int id;
    private int priority;
    private State state = State.READY;

    private int usedTimeBeforeBlocking = 0;
    private final int periodBeforeBlocking;
    public final int blockingPeriod; // period of time, how much process will be blocked for waiting input
    private int awakeTime;
    private int blockingsCount = 0;

    private int usedTime = 0;
    private final int requiredTime;

    public Process(int id, int requiredTime, int periodBeforeBlocking, int periodForBlocking) {
        this.id = id;
        this.periodBeforeBlocking = periodBeforeBlocking;
        this.requiredTime = requiredTime;
        this.blockingPeriod = periodForBlocking;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public int getAwakeTime() {
        return awakeTime;
    }

    public void setAwakeTime(int awakeTime) {
        this.awakeTime = awakeTime;
    }

    // burstDuration bust be > 0
    // we go there on the beginning of current millisecond
    public int execute(int burstDuration){
        int usedBurstTime = 0;

        state = State.RUNNING;
        while(true){
            // this line is for business process really should do

            usedBurstTime++;
            usedTimeBeforeBlocking++;
            usedTime++;

            if(usedTime == requiredTime){ // process is finished
                return usedBurstTime;
            }
            if(usedTimeBeforeBlocking == periodBeforeBlocking){ // process need I/O
                state = State.BLOCKED;
                blockingsCount++;
                return usedBurstTime;
                // awakeTime is set by scheduler
            }
            if(usedBurstTime == burstDuration){ // burst is elapsed
                state = State.READY;
                return usedBurstTime;
            }
        }
    }

    public boolean isDone(){
        return usedTime == requiredTime;
    }

    public boolean isBlocked(){
        return state == State.BLOCKED;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getBlockingPeriod() {
        return blockingPeriod;
    }

    public int getId() {
        return id;
    }

    public int getUsedTime(){
        return this.usedTime;
    }

    public int getRequiredTime(){
        return this.requiredTime;
    }

    public int getBlockingsCount() {
        return blockingsCount;
    }

    public int getUsedTimeBeforeBlocking(){
        return this.usedTimeBeforeBlocking;
    }

    public int getPeriodBeforeBlocking(){
        return this.periodBeforeBlocking;
    }
    public void resetUsedTimeBeforeBlocking(){
        this.usedTimeBeforeBlocking = 0;
    }

    public enum State{
        RUNNING,
        READY,
        BLOCKED
    }

    public static class ComparatorByAwakeTime implements Comparator<Process> {
        @Override
        public int compare(Process a, Process b){
            int result = Integer.compare(a.getAwakeTime(), b.getAwakeTime());
            if(result == 0){
                return Integer.compare(a.getId(), b.getId());
            }
            else{
                return result;
            }

        }
    }
}
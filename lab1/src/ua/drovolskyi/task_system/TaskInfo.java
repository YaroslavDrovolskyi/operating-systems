package ua.drovolskyi.task_system;

public class TaskInfo {
    private Status status = Status.NOT_STARTED;
    private double result = 0;
    private final int MAX_COMPUTATION_ATTEMPTS;

    public TaskInfo (final int MAX_COMPUTATION_ATTEMPTS){
        this.MAX_COMPUTATION_ATTEMPTS = MAX_COMPUTATION_ATTEMPTS;
    }

    public int getMaxComputationAttempts(){
        return MAX_COMPUTATION_ATTEMPTS;
    }

    public void setStatus(Status status){
        this.status = status;
    }

    public Status getStatus(){
        return status;
    }

    public boolean isFinishedSuccessfully(){
        return status == Status.FINISHED_SUCCESSFULLY;
    }

    public void setResult(double result){
        if (status != Status.FINISHED_SUCCESSFULLY){
            throw new IllegalStateException("Can't set result, because illegal state");
        }
        this.result = result;
    }

    public double getResult() {
        if (status != Status.FINISHED_SUCCESSFULLY){
            throw new IllegalStateException("There is no result");
        }
        return result;
    }


    public static enum Status {
        NOT_STARTED,
        STARTED,
        FINISHED_SOFTFAIL,
        FINISHED_HARDFAIL,
        FINISHED_SUCCESSFULLY
    }
}


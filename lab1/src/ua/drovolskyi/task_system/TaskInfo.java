package ua.drovolskyi.task_system;



public class TaskInfo {
    private Status status = Status.NOT_STARTED;
    private double result = 0;


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
        return result;
    }


    public static enum Status {
        NOT_STARTED,
        FINISHED_HARDFAIL,
        FINISHED_SOFTFAIL,
        FINISHED_SUCCESSFULLY
    }
}


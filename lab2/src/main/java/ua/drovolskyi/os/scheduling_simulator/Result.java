package ua.drovolskyi.os.scheduling_simulator;

public class Result {
    private String algorithmType;
    private String algorithmName;
    private int runtime;
    private int finishedProcessesNumber;

    public Result(String algorithmType, String algorithmName, int runtime, int finishedProcessesNumber) {
        this.algorithmType = algorithmType;
        this.algorithmName = algorithmName;
        this.runtime = runtime;
        this.finishedProcessesNumber = finishedProcessesNumber;
    }

    public String getAlgorithmType() {
        return algorithmType;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public int getRuntime() {
        return runtime;
    }

    public int getFinishedProcessesNumber() {
        return finishedProcessesNumber;
    }
}

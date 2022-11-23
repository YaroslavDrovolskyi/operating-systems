// Run() is called from Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

package ua.drovolskyi.os.scheduling_simulator;
import java.util.*;
import java.io.*;



public class SchedulingAlgorithm {
    private final String type = "Interactive (preemptive)";
    private final String name = "Multiple queues";

    private final List<List<Process>> readyProcesses;
    private final TreeSet<Process> blockedProcesses;
    private int doneProcesses = 0;

    // current running millisecond
    private int runtime = 0;

    // Maximum time period system can run. System must finish at the end of (maxRuntime-1)-th millisecond
    private final int maxRuntime;
    private final int quantumDuration = 25;
    private final int numberOfQuantumsForPriority[] = new int[]{1, 2, 4, 8, 16};
    private final int maxPriority = 4;
    private final int resetPrioritiesPeriod = 2000;
    private int nextResetPrioritiesTime = resetPrioritiesPeriod;
    private final String resultFilePath;
    private final String statesFilePath;

    public SchedulingAlgorithm(int maxRuntime, String resultFilePath, String statesFilePath){
        this.maxRuntime = maxRuntime;
        doneProcesses = 0;
        readyProcesses = new ArrayList<>(numberOfQuantumsForPriority.length);
        for(int i = 0; i < numberOfQuantumsForPriority.length; i++){
            readyProcesses.add(new LinkedList<>());
        }
        blockedProcesses = new TreeSet<>(new Process.ComparatorByAwakeTime());

        this.resultFilePath = resultFilePath;
        this.statesFilePath = statesFilePath;
    }

    public Results run(List<Process> initialReadyProcesses) throws FileNotFoundException {
        // init streams to output (debug)
        File resultsFile = new File(resultFilePath);
        if(!resultsFile.exists()){
            try {
                resultsFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        PrintStream resultsOut = new PrintStream(new FileOutputStream(resultsFile));
//        PrintStream statesOut = new PrintStream(new FileOutputStream(statesFilePath));

        // add initial processes to queue
        for(Process p : initialReadyProcesses){
            p.setPriority(maxPriority);
        }
        readyProcesses.get(maxPriority).addAll(initialReadyProcesses);


        Process currentProcess = null;
        while(runtime < maxRuntime){
            awakeBlockedProcesses();

            // reset priorities
            if(runtime == nextResetPrioritiesTime){
                resetPriorities();
                resultsOut.println(runtime + " ms, Reset priority");
            }

            // choose next process and remove it from queue
            try{
                currentProcess = chooseNextProcess();
            } catch(NoSuchElementException e){
                // finish because all processes are done
                resultsOut.println("System finished, because all processes are done");
                break;
            }


            /*
                calculating duration of burst and adjust it to
                - time remaining for end of work (when maxRuntime is elapsed),
                - time remaining for next reset of priority
             */
            int numberOfQuantums = numberOfQuantumsForPriority[currentProcess.getPriority()];
            int burstDuration = numberOfQuantums * quantumDuration;
            burstDuration = Math.min(burstDuration, maxRuntime - runtime);
            burstDuration = Math.min(burstDuration, nextResetPrioritiesTime - runtime);
            printProcessPicked(currentProcess, resultsOut, burstDuration);

            // give process a burst
            // runtime millisecond starts there
            runtime += currentProcess.execute(burstDuration);
            // runtime millisecond end there
            if(currentProcess.isDone()){
                doneProcesses++;
                printProcessDone(currentProcess, resultsOut);
            } else if(currentProcess.isBlocked()){
                // add to blocked processes queue only if process can be run in future
                int awakeTime = runtime + currentProcess.getBlockingPeriod() + 1;
                if(awakeTime < maxRuntime){
                    currentProcess.setAwakeTime(awakeTime);
                    blockedProcesses.add(currentProcess);
                }
                printProcessBlocked(currentProcess, resultsOut);
            } else{ // reduce priority of currentProcess
                int currentPriority = currentProcess.getPriority();
                int nextPriority = Math.max(currentPriority - 1, 0);
                currentProcess.setPriority(nextPriority);
                readyProcesses.get(nextPriority).add(currentProcess);
                printProcessUsedBurst(currentProcess, resultsOut);
            }
            currentProcess = null;
        }
        if(runtime == maxRuntime){ // exit because runtime == maxRuntime
            resultsOut.println("System finished, because max time is exceed");
        }

        resultsOut.close();
//        statesOut.close();


        //////////////////////////////////////////////////////////////////////////// need to return result
        return null;
    }

    // add unblocked processes to queue of ready processes
    private void awakeBlockedProcesses(){
        while(!blockedProcesses.isEmpty()){
            Process p = blockedProcesses.first();
            if(runtime >= p.getAwakeTime()){
                blockedProcesses.remove(p);
                p.setAwakeTime(-1);
                p.setState(Process.State.READY);
                readyProcesses.get(p.getPriority()).add(p);
            }
            else{
                break;
            }
        }
    }

    // set priorities of all ready processes to maxPriority and move them into max priority queue
    private void resetPriorities(){
        List<Process> maxPriorityQueue = readyProcesses.get(maxPriority);
        for(int i = maxPriority - 1; i >= 0; i--){
            Iterator<Process> queueIterator = readyProcesses.get(i).iterator();
            while(queueIterator.hasNext()){
                Process p = queueIterator.next();
                p.setPriority(maxPriority);
                queueIterator.remove();
                maxPriorityQueue.add(p);
            }
        }
        nextResetPrioritiesTime = runtime + resetPrioritiesPeriod;
    }

    // choose next process and remove it from queue
    private Process chooseNextProcess(){
        for(int i = maxPriority; i >= 0; i--){
            List<Process> queue = readyProcesses.get(i);
            if(!queue.isEmpty()){
                return queue.remove(0);
            }
        }
        throw new NoSuchElementException();
    }

    private void printProcessPicked(Process p, PrintStream out, int burstDuration){
        out.print(runtime + " ms, Process " + p.getId() + " picked by scheduler, " +
                "burst: "+ burstDuration + " ms ");
        printProcessInfo(p,out);
    }

    private void printProcessDone(Process p, PrintStream out){
        out.print(runtime + " ms, Process " + p.getId() + " is done ");
        printProcessInfo(p,out);
    }

    private void printProcessUsedBurst(Process p, PrintStream out){
        out.print(runtime + " ms, Process " + p.getId() + " used burst ");
        printProcessInfo(p,out);
    }

    private void printProcessBlocked(Process p, PrintStream out){
        out.print(runtime + " ms, Process " + p.getId() + " I/O blocked ");
        printProcessInfo(p,out);
    }

    private void printProcessInfo(Process p, PrintStream out){
        out.println("( priority: " + p.getPriority() +
                ", used " + p.getUsedTime() + " ms of " + p.getRequiredTime() + " ms, " +
                "blocked " + p.getBlockingsCount() + " times, " +
                "used before blocking: " + p.getUsedTimeBeforeBlocking() + " ms of " +
                p.getPeriodBeforeBlocking() + " ms )");
    }

    public static Results Run(int runtime, Vector processVector, Results result) {
        int i = 0;
        int comptime = 0;
        int currentProcess = 0;
        int previousProcess = 0;
        int size = processVector.size();
        int completed = 0;
        String resultsFile = "Summary-Processes";

        result.schedulingType = "Batch (Nonpreemptive)";
        result.schedulingName = "First-Come First-Served";
        try {
            //BufferedWriter out = new BufferedWriter(new FileWriter(resultsFile));
            //OutputStream out = new FileOutputStream(resultsFile);
            PrintStream out = new PrintStream(new FileOutputStream(resultsFile));
            sProcess process = (sProcess) processVector.elementAt(currentProcess);
            out.println("Process: " + currentProcess + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
            while (comptime < runtime) {
                if (process.cpudone == process.cputime) {
                    completed++;
                    out.println("Process: " + currentProcess + " completed... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
                    if (completed == size) {
                        result.compuTime = comptime;
                        out.close();
                        return result;
                    }
                    for (i = size - 1; i >= 0; i--) {
                        process = (sProcess) processVector.elementAt(i);
                        if (process.cpudone < process.cputime) {
                            currentProcess = i;
                        }
                    }
                    process = (sProcess) processVector.elementAt(currentProcess);
                    out.println("Process: " + currentProcess + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
                }
                if (process.ioblocking == process.ionext) {
                    out.println("Process: " + currentProcess + " I/O blocked... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
                    process.numblocked++;
                    process.ionext = 0;
                    previousProcess = currentProcess;
                    for (i = size - 1; i >= 0; i--) {
                        process = (sProcess) processVector.elementAt(i);
                        if (process.cpudone < process.cputime && previousProcess != i) {
                            currentProcess = i;
                        }
                    }
                    process = (sProcess) processVector.elementAt(currentProcess);
                    out.println("Process: " + currentProcess + " registered... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.cpudone + ")");
                }
                process.cpudone++;
                if (process.ioblocking > 0) {
                    process.ionext++;
                }
                comptime++;
            }
            out.close();
        } catch (IOException e) { /* Handle exceptions */ }
        result.compuTime = comptime;
        return result;
    }
}

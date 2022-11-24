// Run() is called from Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

package ua.drovolskyi.os.scheduling_simulator;
import java.util.*;
import java.io.*;



public class SchedulingAlgorithm {
    private final String type = "Interactive (preemptive)";
    private final String name = "Multiple queues (Round-robin in each queue)";

    private final List<List<Process>> readyProcesses;
    private final TreeSet<Process> blockedProcesses;
    private int doneProcesses = 0;

    // current running millisecond
    private int runtime = 0;

    // Maximum time period system can run. System must finish at the end of (maxRuntime-1)-th millisecond
    private final int maxRuntime;
    private final int quantumDuration; // 25
    private final int numberOfQuantumsForPriority[] = new int[]{16, 8, 4, 2, 1};
    private final int maxPriority = 4;
    private final int resetPrioritiesPeriod; // 2000
    private int nextResetPrioritiesTime;
    private final String resultFilePath;
    private final String statesFilePath;

    public SchedulingAlgorithm(int maxRuntime, int resetPrioritiesPeriod, int quantumDuration,
                               String resultFilePath, String statesFilePath){
        this.maxRuntime = maxRuntime;
        this.quantumDuration = quantumDuration;
        this.resetPrioritiesPeriod = resetPrioritiesPeriod;
        this.nextResetPrioritiesTime = resetPrioritiesPeriod;
        doneProcesses = 0;
        readyProcesses = new ArrayList<>(numberOfQuantumsForPriority.length);
        for(int i = 0; i < numberOfQuantumsForPriority.length; i++){
            readyProcesses.add(new LinkedList<>());
        }
        blockedProcesses = new TreeSet<>(new Process.ComparatorByAwakeTime());

        this.resultFilePath = resultFilePath;
        this.statesFilePath = statesFilePath;
    }

    public Result run(List<Process> initialReadyProcesses) throws FileNotFoundException {
        // init output streams
        PrintStream resultsOut = new PrintStream(new FileOutputStream(resultFilePath));
        PrintStream statesOut = new PrintStream(new FileOutputStream(statesFilePath));

        // add initial processes to queue
        for(Process p : initialReadyProcesses){
            p.setPriority(maxPriority);
        }
        readyProcesses.get(maxPriority).addAll(initialReadyProcesses);


        Process currentProcess = null;
        while(runtime < maxRuntime){
            // awake blocked processes
            awakeBlockedProcesses();

            printState(statesOut);

            // reset priorities if necessary
            if(runtime == nextResetPrioritiesTime){
                resetPriorities();
                resultsOut.println(runtime + " ms, Reset priority");
                statesOut.println(runtime + " ms, Reset priority");
            }

            // choose next process and remove it from queue
            try{
                currentProcess = chooseNextProcess();
            } catch(NoSuchElementException e){
                if(blockedProcesses.isEmpty()){ // finish because all processes are done
                    resultsOut.println("Simulator finished, because all processes are done");
                    statesOut.println("Simulator finished, because all processes are done");
                    break;
                }
                else{
                    if(blockedProcesses.first().getAwakeTime() < maxRuntime){
                        resultsOut.println("No ready processes, waiting for blocked processes...\n");
                        statesOut.println("No ready processes, waiting for blocked processes...\n");
                        runtime = blockedProcesses.first().getAwakeTime();
                        continue;
                    }
                    else{
                        resultsOut.println("Simulator finished, because all processes are done, " +
                                "and blocked processes won't awake before maxRuntime");
                        statesOut.println("Simulator finished, because all processes are done, " +
                                "and blocked processes won't awake before maxRuntime");
                        break;
                    }
                }

            }


            /*
                calculating duration of burst and adjust it to
                - time remaining for end of work (when maxRuntime is used),
                - time remaining for next reset of priority
             */
            int numberOfQuantums = numberOfQuantumsForPriority[currentProcess.getPriority()];
            int burstDuration = numberOfQuantums * quantumDuration;
            burstDuration = Math.min(burstDuration, maxRuntime - runtime);
            burstDuration = Math.min(burstDuration, nextResetPrioritiesTime - runtime);

            printProcessPicked(currentProcess, resultsOut, burstDuration);
            printProcessPicked(currentProcess, statesOut, burstDuration);

            // give process a burst
            runtime += currentProcess.execute(burstDuration);
            if(currentProcess.isDone()){
                doneProcesses++;
                printProcessDone(currentProcess, resultsOut);
                printProcessDone(currentProcess, statesOut);
            } else if(currentProcess.isBlocked()){
                int awakeTime = runtime + currentProcess.getBlockingPeriod();
                currentProcess.setAwakeTime(awakeTime);
                blockedProcesses.add(currentProcess);
                printProcessBlocked(currentProcess, resultsOut);
                printProcessBlocked(currentProcess, statesOut);
                currentProcess.resetUsedTimeBeforeBlocking();
            } else{ // reduce priority of currentProcess
                int currentPriority = currentProcess.getPriority();
                int nextPriority = Math.max(currentPriority - 1, 0);
                currentProcess.setPriority(nextPriority);
                readyProcesses.get(nextPriority).add(currentProcess);
                printProcessUsedBurst(currentProcess, resultsOut);
                printProcessUsedBurst(currentProcess, statesOut);
            }
            currentProcess = null;
        }
        if(runtime == maxRuntime){ // exit because runtime == maxRuntime
            resultsOut.println("System finished, because max time is used");
            statesOut.println("System finished, because max time is used");
        }

        resultsOut.close();
        statesOut.close();

        return new Result(this.type, this.name, this.runtime, this.doneProcesses);
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
        out.println("\n");
    }

    private void printProcessUsedBurst(Process p, PrintStream out){
        out.print(runtime + " ms, Process " + p.getId() + " used burst ");
        printProcessInfo(p,out);
        out.println("\n");
    }

    private void printProcessBlocked(Process p, PrintStream out){
        out.print(runtime + " ms, Process " + p.getId() + " I/O blocked ");
        printProcessInfo(p,out);
        out.println("\n");
    }

    private void printProcessInfo(Process p, PrintStream out){
        out.println("( priority: " + p.getPriority() +
                ", used " + p.getUsedTime() + " ms of " + p.getRequiredTime() + " ms, " +
                "blocked " + p.getBlockingsCount() + " times, " +
                "used before blocking: " + p.getUsedTimeBeforeBlocking() + " ms of " +
                p.getPeriodBeforeBlocking() + " ms )");
    }

    public void printState(PrintStream out){
        out.println("RUNTIME: " + runtime + " ms");
        out.println("Queues:");
        for(int i = maxPriority; i >= 0; i--){
            out.print("Priority " + i + ": ");
            List<Process> queue = readyProcesses.get(i);
            for(int j = 0; j < queue.size(); j++){
                out.print(queue.get(j).getId() + " ");
            }
            out.println();
        }
        out.print("\nBlocked processes: ");
        if(blockedProcesses.isEmpty()){
            out.print("[no processes]");
        }
        for(Process p : blockedProcesses){
            out.print(p.getId() + "("+p.getAwakeTime()+"ms)  ");
        }
        out.println("\n");
    }
}

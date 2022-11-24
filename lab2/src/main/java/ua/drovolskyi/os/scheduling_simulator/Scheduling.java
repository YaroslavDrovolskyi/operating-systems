// This file contains the main() function for the Scheduling
// simulation.  Init() initializes most of the variables by
// reading from a provided file.  SchedulingAlgorithm.Run() is
// called from main() to run the simulation.  Summary-Result
// is where the summary results are written, and Summary-Processes
// is where the process scheduling summary is written.

// Created by Alexander Reeder, 2001 January 06
package ua.drovolskyi.os.scheduling_simulator;


import java.io.*;
import java.util.*;

public class Scheduling {
    private static int numberOfProcesses = 5;
    private static int runtimeAverage = 1000;
    private static int runtimeStandardDeviation = 100;
    private static int resetPrioritiesPeriod = 2000;
    private static int quantumDuration = 25;
    private static int maxRuntime = 1000;
    private static int processIdCount = 0;
    private static List<Process> processes = new LinkedList<>();
    private static String resultsFilePath = "summary-results.txt";

    private static void initialize(String inputFilePath) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(inputFilePath));
            while(scanner.hasNext()){
                String token = scanner.next();
                if(token.equals("//")){
                    scanner.nextLine();
                    continue;
                }
                switch (token){
                    case "number_of_processes":
                        numberOfProcesses = scanner.nextInt();
                        if(numberOfProcesses <= 0){
                            throw new IllegalArgumentException("number_of_processes must be > 0");
                        }
                        break;
                    case "max_runtime":
                        maxRuntime = scanner.nextInt();
                        if(maxRuntime <= 0){
                            throw new IllegalArgumentException("max_runtime must be > 0");
                        }
                        break;
                    case "reset_priorities_period":
                        resetPrioritiesPeriod = scanner.nextInt();
                        if(resetPrioritiesPeriod <= 0){
                            throw new IllegalArgumentException("reset_priorities_period must be > 0");
                        }
                        break;
                    case "quantum_duration":
                        quantumDuration = scanner.nextInt();
                        if(quantumDuration <= 0){
                            throw new IllegalArgumentException("quantum_duration must be > 0");
                        }
                        break;
                    case "runtime_average":
                        runtimeAverage = scanner.nextInt();
                        if(runtimeAverage <= 0){
                            throw new IllegalArgumentException("runtime_average must be > 0");
                        }
                        break;
                    case "runtime_std_deviation":
                        runtimeStandardDeviation = scanner.nextInt();
                        if(runtimeStandardDeviation < 0){
                            throw new IllegalArgumentException("runtime_average must be >= 0");
                        }
                        break;

                    case "process":{
                        double X = Common.R1();
                        while (X == -1.0) {
                            X = Common.R1();
                        }
                        X *= runtimeStandardDeviation;
                        int requiredRuntime = (int) X + runtimeAverage;
                        processes.add(new Process(processIdCount, requiredRuntime, scanner.nextInt(), scanner.nextInt()));
                        processIdCount++;
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally{
            if (scanner != null){
                scanner.close();
            }
        }

        // if user not specified some processes, generate random processes
        while (processes.size() < numberOfProcesses) {
            double X = Common.R1();
            while (X == -1.0) {
                X = Common.R1();
            }
            X *= runtimeStandardDeviation;
            int requiredRuntime = (int) X + runtimeAverage;
            processes.add(new Process(processIdCount, requiredRuntime,
                    processIdCount*100 + 1, processIdCount*10));
            processIdCount++;
        }
    }

    private static void debug() {
        System.out.println("number_of_processes: " + numberOfProcesses);
        System.out.println("max_runtime " + maxRuntime);
        System.out.println("reset_priorities_period " + resetPrioritiesPeriod);
        System.out.println("quantum_duration " + quantumDuration);
        System.out.println("runtime_average " + runtimeAverage);
        System.out.println("runtime_std_deviation: " + runtimeStandardDeviation);

        for(Process p : processes){
            System.out.println("Process " + p.getId() + "  ( " + p.getRequiredTime() + " ms, " +
            p.getPeriodBeforeBlocking() + " ms, " + p.getBlockingPeriod() + " )");
        }
    }

    public static void main(String[] args) {
//        args = new String[]{"src/main/resources/scheduling.conf"};

        if (args.length != 1) {
            System.out.println("Usage: 'java [executable file] <INIT FILE>'");
            System.exit(-1);
        }
        File f = new File(args[0]);
        if (!(f.exists())) {
            System.out.println("Scheduling: error, file '" + f.getName() + "' does not exist.");
            System.exit(-1);
        }
        if (!(f.canRead())) {
            System.out.println("Scheduling: error, read of " + f.getName() + " failed.");
            System.exit(-1);
        }
        System.out.println("Working...");


        initialize(args[0]);

        SchedulingAlgorithm algorithm = new SchedulingAlgorithm(
                maxRuntime, resetPrioritiesPeriod, quantumDuration,
                "summary-processes.txt",
                "debug-processes.txt");

        Result result = null;
        try {
            result = algorithm.run(processes);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        // print summary into summary-results file
        try {
            PrintStream out = new PrintStream(new FileOutputStream(resultsFilePath));
            out.println("Scheduling algorithm Type: " + result.getAlgorithmType());
            out.println("Scheduling algorithm Name: " + result.getAlgorithmName());
            out.println("Simulation Run Time: " + result.getRuntime());
            out.println("Number of done processes: " + result.getFinishedProcessesNumber());
            out.println("requiredRuntime average: " + runtimeAverage);
            out.println("requiredRuntime standard deviation: " + runtimeStandardDeviation);
            out.println("Process ID |\tRequired CPU time |\tUsed CPU time |\tis done |\tPeriod before IO Blocking |\tBlocking count");

            for (Process p : processes) {
                out.println(
                        p.getId() + "\t\t\t\t" +
                        p.getRequiredTime() + " ms \t\t\t\t" +
                        p.getUsedTime() + " ms \t\t\t" + p.isDone() + " \t\t" +
                        p.getPeriodBeforeBlocking() + " ms \t\t" +
                        p.getBlockingsCount() + " times"
                );
            }
            out.close();
        } catch (IOException e) {
            System.out.println("IO error occurred");
            throw new RuntimeException(e);
        }

        System.out.println("Completed.");
    }
}


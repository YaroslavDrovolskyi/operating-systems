package ua.drovolskyi.task_system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.function.Function;
import os.lab1.compfuncs.advanced.DoubleOps;

public class Manager {
    private int x;
    Status status = Status.COMPUTATIONS_NOT_STARTED;
    private Thread cancellationWaitingThread;
    private TaskInfo infoTask1 = new TaskInfo(5);
    private TaskInfo infoTask2 = new TaskInfo(5);
    private Function<Integer, Optional<Optional<Double>>> f;
    private Function<Integer, Optional<Optional<Double>>> g;
    private Pipe pipeTask1;
    private Pipe pipeTask2;


    public Manager(){
        // add termination hook
        Runtime current = Runtime.getRuntime();
        current.addShutdownHook(new Thread(()->{

            try{
                if (pipeTask1 != null){
                    pipeTask1.source().close();
                }
                if (pipeTask2 != null){
                    pipeTask2.source().close();
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }


            switch(status){
                case COMPUTATIONS_NOT_STARTED:{
                    System.out.println("Computations cancelled. They haven't started yet");
                    break;
                }
                case COMPUTATIONS_STARTED: { // calculating is in progress
                    System.out.println("Computations cancelled:");
                    switch (infoTask1.getStatus()){
                        case STARTED:
                            System.out.println("f(x) - not finished");
                            break;
                        case FINISHED_SOFTFAIL:
                            System.out.println("f(x) - soft fail, " +
                                    "max number of attempts reached (" + infoTask1.getMaxComputationAttempts() +")");
                            break;
                        case FINISHED_HARDFAIL:
                            System.out.println("f(x) - hard fail");
                            break;
                        case FINISHED_SUCCESSFULLY:
                            System.out.println("f(x) - computed");
                            break;
                    }

                    switch (infoTask2.getStatus()){
                        case STARTED:
                            System.out.println("g(x) - not finished");
                            break;
                        case FINISHED_SOFTFAIL:
                            System.out.println("g(x) - soft fail, " +
                                    "max number of attempts reached (" + infoTask2.getMaxComputationAttempts() +")");
                            break;
                        case FINISHED_HARDFAIL:
                            System.out.println("g(x) - hard fail");
                            break;
                        case FINISHED_SUCCESSFULLY:
                            System.out.println("g(x) - computed");
                            break;
                    }
                    break;
                }
                case RESULT_CALCULATED_AND_PRINTED:{
                    // nothing need to print, because result is already printed
                    break;
                }
            }
        }));

        // create cancellation-input waiting thread
        cancellationWaitingThread = new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            while(true){
                String input = scanner.next();

                if (input.trim().equalsIgnoreCase("q")){
                    scanner.close();
                    System.exit(0);
                }
            }
        });
        cancellationWaitingThread.setDaemon(true);

        // create functions for tasks
        f = (Integer x) ->{
            try {
                return DoubleOps.trialF(x);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        g = (Integer x) ->{
            try {
                return DoubleOps.trialG(x);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }


    public void run(){
        // possibility of exit is provided both by scanX() and by cancellation-waiting thread
        scanX();
        cancellationWaitingThread.start();


        try {
            Selector selector = Selector.open();
            pipeTask1 = Pipe.open();
            pipeTask2 = Pipe.open();

            Pipe.SourceChannel sourceChannelTask1 = pipeTask1.source();
            Pipe.SourceChannel sourceChannelTask2 = pipeTask2.source();

            sourceChannelTask1.configureBlocking(false);
            sourceChannelTask2.configureBlocking(false);

            SelectionKey keyTask1 = sourceChannelTask1.register(selector, SelectionKey.OP_READ);
            SelectionKey keyTask2 = sourceChannelTask2.register(selector, SelectionKey.OP_READ);

            // attach ID of task
            keyTask1.attach(Integer.valueOf(1));
            keyTask2.attach(Integer.valueOf(2));

            // create and start task threads
            Thread threadTask1 = new Thread(new TaskThread(x, f,
                    infoTask1.getMaxComputationAttempts(), pipeTask1));
            Thread threadTask2 = new Thread(new TaskThread(x, g,
                    infoTask2.getMaxComputationAttempts(), pipeTask2));

            threadTask1.setDaemon(true);
            threadTask2.setDaemon(true);

            infoTask1.setStatus(TaskInfo.Status.STARTED);
            infoTask2.setStatus(TaskInfo.Status.STARTED);
            status = Status.COMPUTATIONS_STARTED;

            threadTask1.start();
            threadTask2.start();


            int finishedTasks = 0;
            boolean stop = false;
            while(!stop && finishedTasks < 2){
                int readyChannels = selector.selectNow();
                if (readyChannels == 0){
                    continue;
                }

                // read messages from all finished tasks
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while(keyIterator.hasNext()) {
                    TaskInfo processedTaskInfo = proceedTaskFinishing(keyIterator.next());
                    keyIterator.remove();
                    finishedTasks++;

                    // if processed task is failed, then we can't calculate result,
                    // so we don't need wait second task
                    if (!processedTaskInfo.isFinishedSuccessfully()){
                        stop = true;
                        break;
                    }
                }
            }

            sourceChannelTask1.close();
            sourceChannelTask2.close();

            calculateAndPrintResult();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanX(){
        System.out.println("Hello! If you want to cancel computations, enter [q] at any time");
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.print("Enter x: ");
            String input = scanner.next();
            if (input.trim().equalsIgnoreCase("q")){
                scanner.close();
                System.exit(0);
            }

            try{
                x = Integer.parseInt(input);
                break;
            } catch (NumberFormatException e){
                System.out.println("Input must be integer value");
            }
        }
    }

    /*
        This function fills taskInfo after task is finished
     */
    private TaskInfo proceedTaskFinishing(SelectionKey key) throws IOException {
        if (!key.isReadable()){
            throw new IllegalArgumentException("Task's key must be readable");
        }

        int taskId = (Integer)(key.attachment());
        TaskInfo currentTaskInfo = taskId == 1 ? infoTask1 : infoTask2;

        ByteBuffer buf = ByteBuffer.allocate(12);

        // read task result into buffer
        Pipe.SourceChannel channel = (Pipe.SourceChannel)(key.channel());
        channel.read(buf);
        buf.flip(); // switch buffer from writing to reading mode

        // process result & fill taskInfo
        int returningCode = buf.getInt();

        if (returningCode == 0){
            currentTaskInfo.setStatus(TaskInfo.Status.FINISHED_SUCCESSFULLY);
            double result = buf.getDouble();
            currentTaskInfo.setResult(result);
        } else if (returningCode == 1){
            currentTaskInfo.setStatus(TaskInfo.Status.FINISHED_SOFTFAIL);
        } else if (returningCode == 2){
            currentTaskInfo.setStatus(TaskInfo.Status.FINISHED_HARDFAIL);
        }

        /*
        System.out.println("Task #" + (Integer)key.attachment());
        System.out.println("Returning code = " + returningCode);
        if (returningCode == 0){
            buf.rewind();
            buf.getDouble();
            System.out.println("Result = " + currentTaskInfo.getResult() + "\n");
        }
         */

        return currentTaskInfo;
    }

    // only this method will print result (except cancellation from user)
    private void calculateAndPrintResult(){
        if (infoTask1.isFinishedSuccessfully() &&
                infoTask2.isFinishedSuccessfully()){
            double resultF = infoTask1.getResult();
            double resultG = infoTask2.getResult();
            double result = resultF + resultG;

            System.out.println("Result = " + result);
        }
        else{
            System.out.println("Computations failed. Reason is: ");
            if (infoTask1.getStatus() == TaskInfo.Status.FINISHED_SOFTFAIL){
                System.out.println("f(x) - soft fail, " +
                        "max number of attempts reached (" + infoTask1.getMaxComputationAttempts() +")");
            }
            else if (infoTask1.getStatus() == TaskInfo.Status.FINISHED_HARDFAIL){
                System.out.println("f(x) - hard fail");
            }
            else if (infoTask2.getStatus() == TaskInfo.Status.FINISHED_SOFTFAIL){
                System.out.println("g(x) - soft fail, " +
                        "max number of attempts reached (" + infoTask2.getMaxComputationAttempts() +")");
            }
            else if (infoTask2.getStatus() == TaskInfo.Status.FINISHED_HARDFAIL){
                System.out.println("g(x) - hard fail");
            }
        }
        status = Status.RESULT_CALCULATED_AND_PRINTED;
    }



    public static enum Status {
        COMPUTATIONS_NOT_STARTED,
        COMPUTATIONS_STARTED,
        RESULT_CALCULATED_AND_PRINTED
    }
}
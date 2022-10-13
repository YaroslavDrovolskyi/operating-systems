package ua.drovolskyi.task_system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class Manager { // need to add operandsComputed, resultComputed
    private TaskInfo infoTask1 = new TaskInfo();
    private TaskInfo infoTask2 = new TaskInfo();
    private boolean isCalculatedTask1 = false;
    private boolean isCalculatedTask2 = false;
    private boolean isCalculatedResult = false;

    public Manager(){
        Runtime current = Runtime.getRuntime();
        current.addShutdownHook(new Thread(()->{
            System.out.println("Program is terminated");
                })
        );


        // also here need to start input thread there
    }


    public void run(){
        try {
            Selector selector = Selector.open();
            Pipe pipe1 = Pipe.open();
            Pipe pipe2 = Pipe.open();

            Pipe.SourceChannel sourceChannelTask1 = pipe1.source();
            Pipe.SourceChannel sourceChannelTask2 = pipe2.source();

            sourceChannelTask1.configureBlocking(false);
            sourceChannelTask2.configureBlocking(false);

            SelectionKey keyTask1 = sourceChannelTask1.register(selector, SelectionKey.OP_READ);
            SelectionKey keyTask2 = sourceChannelTask2.register(selector, SelectionKey.OP_READ);

            // attach ID of task
            keyTask1.attach(Integer.valueOf(1));
            keyTask2.attach(Integer.valueOf(2));

            // create and start threads
            Thread threadTask1 = new Thread(new TaskThread(0, pipe1));
            Thread threadTask2 = new Thread(new TaskThread(0, pipe2));

            threadTask1.setDaemon(true);
            threadTask2.setDaemon(true);

            threadTask1.start();
            threadTask2.start();

            int finishedTasks = 0;
            while(finishedTasks < 2){
                int readyChannels = selector.selectNow();
                if (readyChannels == 0){
                    continue;
                    // there also can be handling of some keys pressed
                }

                // read messages from all finished tasks
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while(keyIterator.hasNext()) {
                    proceedTaskFinishing(keyIterator.next());
                    keyIterator.remove();
                    finishedTasks++;
                }
            }

            // there need calculate result


//            threadTask1.join();
//            threadTask2.join();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }// catch (InterruptedException e) {
        //   throw new RuntimeException(e);
        // }


    }

    private void proceedTaskFinishing(SelectionKey key) throws IOException {
        if (!key.isReadable()){
            return;
        }

        int taskId = (Integer)(key.attachment());
        TaskInfo currentTaskInfo = taskId == 1 ? infoTask1 : infoTask2;

        ByteBuffer buf = ByteBuffer.allocate(16);

        // read task result into buffer
        Pipe.SourceChannel channel = (Pipe.SourceChannel)(key.channel());
        channel.read(buf);
        buf.flip(); // switch buffer from writing to reading mode

        // process result
        double returningCode = buf.getDouble();

        if (returningCode == 0){
            currentTaskInfo.setStatus(TaskInfo.Status.FINISHED_SUCCESSFULLY);
            double result = buf.getDouble();
            currentTaskInfo.setResult(result);
        } else if (returningCode == 1){
            currentTaskInfo.setStatus(TaskInfo.Status.FINISHED_SOFTFAIL);
        } else if (returningCode == 2){
            currentTaskInfo.setStatus(TaskInfo.Status.FINISHED_HARDFAIL);
        }

        ///////
        System.out.println("Task #" + (Integer)key.attachment());
        System.out.println("Returning code = " + returningCode);
        if (returningCode == 0){
            buf.rewind();
            buf.getDouble();
            System.out.println("Result = " + buf.getDouble() + "\n");
        }
        //////
    }

    // only this will print result (except cancellation from user)
    private void calculateResult(){
        if (infoTask1.isFinishedSuccessfully() &&
                infoTask2.isFinishedSuccessfully()){
            double resultF = infoTask1.getResult();
            double resultG = infoTask1.getResult();
            double result = resultF + resultG;

            System.out.println("Result = " + result);
        }
        else{
            System.out.println("Result don't computed because of: ");
            if (infoTask1.getStatus() == TaskInfo.Status.FINISHED_SOFTFAIL){
                System.out.println("f(x) - soft fail, " +
                        "max number of attempts reached (" + infoTask1.getMaxComputationAttempts() +")");
            }
            else if (infoTask1.getStatus() == TaskInfo.Status.FINISHED_HARDFAIL){
                System.out.println("f(x) - hard fail");
            }

            if (infoTask2.getStatus() == TaskInfo.Status.FINISHED_SOFTFAIL){
                System.out.println("g(x) - soft fail, " +
                        "max number of attempts reached (" + infoTask2.getMaxComputationAttempts() +")");
            }
            else if (infoTask2.getStatus() == TaskInfo.Status.FINISHED_HARDFAIL){
                System.out.println("g(x) - hard fail");
            }
        }
    }
}

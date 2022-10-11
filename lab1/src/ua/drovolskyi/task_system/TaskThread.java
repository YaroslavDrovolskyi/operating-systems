package ua.drovolskyi.task_system;

import os.lab1.compfuncs.advanced.DoubleOps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Optional;
import java.util.concurrent.Callable;

public class TaskThread implements Runnable{
    private Pipe pipe;
    private final int x;
    private final int ATTEMPTS_WHEN_SOFT_FAIL = 5;


    public TaskThread(int x, Pipe pipe){
        this.pipe = pipe;
        this.x = x;
    }

    @Override
    public void run(){
        try {
            Optional<Optional<Double>> softOptional = null;

            for (int i = 0; i <= ATTEMPTS_WHEN_SOFT_FAIL; i++){
                softOptional = DoubleOps.trialF(x);
                if (softOptional.isPresent()){
                    break;
                }
            }

            if (softOptional.isPresent()){
                Optional<Double> hardOptional = softOptional.get();

                if (hardOptional.isPresent()){
                    double result = hardOptional.get();
                    reportResult(result);
                }
                else{
                    reportFailure(2);// report about hard fail
                }
            }
            else{
                reportFailure(1); // report about soft fail
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
            // report about hard fail or what????
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private void reportResult(Double result) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.clear();
        buf.putDouble(0);
        buf.putDouble(result);

        buf.flip();

        Pipe.SinkChannel sinkChannel = pipe.sink();
        while(buf.hasRemaining()) {
            sinkChannel.write(buf);
        }
    }

    private void reportFailure(double failureId) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.clear();
        buf.putDouble(failureId);

        buf.flip();

        Pipe.SinkChannel sinkChannel = pipe.sink();
        while(buf.hasRemaining()) {
            sinkChannel.write(buf);
        }
    }

}

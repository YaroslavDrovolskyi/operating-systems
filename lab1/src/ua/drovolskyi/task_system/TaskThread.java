package ua.drovolskyi.task_system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Optional;
import java.util.function.Function;

public class TaskThread implements Runnable{
    private final int x;
    private Function<Integer, Optional<Optional<Double>>> function;
    private final int MAX_COMPUTATION_ATTEMPTS;
    private Pipe pipe;


    public TaskThread(int x, Function<Integer, Optional<Optional<Double>>> function,
                      final int MAX_COMPUTATION_ATTEMPTS, Pipe pipe){
        this.x = x;
        this.function = function;
        this.MAX_COMPUTATION_ATTEMPTS = MAX_COMPUTATION_ATTEMPTS;
        this.pipe = pipe;
    }

    @Override
    public void run(){
        try {
            Optional<Optional<Double>> softOptional = null;

            for (int i = 0; i < MAX_COMPUTATION_ATTEMPTS; i++){
                softOptional = function.apply(x);
                if (softOptional.isPresent()){
                    break;
                }
            }

            if (softOptional.isPresent()){
                Optional<Double> hardOptional = softOptional.get();

                if (hardOptional.isPresent()){
                    double result = hardOptional.get();
                    sendResult(result);
                }
                else{
                    sendFailure(2); // report manager about hard fail
                }
            }
            else{
                sendFailure(1); // report manager about soft fail
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
            // no handling, because f() and g() considered not throwing exceptions
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private void sendResult(Double result) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.clear();
        buf.putInt(0);
        buf.putDouble(result);

        buf.flip();

        Pipe.SinkChannel sinkChannel = pipe.sink();
        while(buf.hasRemaining()) {
            sinkChannel.write(buf);
        }
    }

    private void sendFailure(int failureId) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.clear();
        buf.putInt(failureId);

        buf.flip();

        Pipe.SinkChannel sinkChannel = pipe.sink();
        while(buf.hasRemaining()) {
            sinkChannel.write(buf);
        }
    }

}

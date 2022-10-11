package ua.drovolskyi.task_system;

import os.lab1.compfuncs.basic.DoubleOps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Optional;

public class Program {
    public static void main(String[] args) {
        /*
        try {
            Optional<Double> op = DoubleOps.trialF(0);
            if (op.isPresent()){
                System.out.println("f(x) = " + op.orElse(0.0));
            }
            else{
                System.out.println("Soft fail");
            }
        } catch (InterruptedException e) {
            System.out.println("Exception: hard-fail");
        }
*/

        try {
            Pipe pipe = Pipe.open();
            Pipe.SourceChannel sourceChannel = pipe.source();


            sourceChannel.configureBlocking(false);


            ByteBuffer buf = ByteBuffer.allocate(48);

            Thread th = new Thread(new TaskThread(0, pipe));
            th.start();

            while(sourceChannel.read(buf) == 0){

            }

            double result = buf.getDouble();
            System.out.println("Result = " + result);

            if (result == 0){
                /*
                while(sourceChannel.read(buf) == 0){

                }
                */


                double result1 = buf.getDouble();
                System.out.println("Result1 = " + result);
            }



            th.join();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}

package ua.drovolskyi.task_system;

import os.lab1.compfuncs.basic.DoubleOps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

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


        // need to remove this
        /*
        try {
            Thread.sleep(30000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


         */

        Manager manager = new Manager();

        /*
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        */
        manager.run();

    }
}

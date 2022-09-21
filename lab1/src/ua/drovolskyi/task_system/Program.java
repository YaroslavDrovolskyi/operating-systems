package ua.drovolskyi.task_system;

import os.lab1.compfuncs.basic.DoubleOps;

import java.util.Optional;

public class Program {
    public static void main(String[] args) {
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

    }
}

package lockvis.tools;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Dining philosophers with 1.5 style Lock objects.
 * They also get their own napkin each just to demonstrate Locks with no contention. 
 */
public class DiningPhilosophers {

    private static Lock chopstick1 = new ReentrantLock();
    private static Lock chopstick2 = new ReentrantLock();
    private static Lock chopstick3 = new ReentrantLock();
    private static Lock chopstick4 = new ReentrantLock();

    static class Chopstick {
    }

    static class Philosopher implements Runnable {
        private final String name;

        private final Lock leftChopstick, rightChopstick, napkin;

        public Philosopher(String name, Lock leftChopstick, Lock rightChopstick, Lock napkin) {
            this.name = name;
            this.leftChopstick = leftChopstick;
            this.rightChopstick = rightChopstick;
            this.napkin = napkin;
        }

        public String getName() {
            return this.name;
        }

        public void eat() {
            // randomly take left or right first
            double random = Math.random();

            if (random >= 0.5) {
                eat(leftChopstick, rightChopstick);
            } else {
                eat(rightChopstick, leftChopstick);
            }
        }

        private void eat(Lock first, Lock second) {
            try {
                napkin.lock();
                first.lock();
                System.out.println(name + " has taken a chopstick");
                Thread.sleep(10); // It takes a while to pickup a chopstick
                second.lock();
                System.out.println(name + " has taken a chopstick");
                System.out.println(name + " is eating...");
                Thread.sleep(500); // It takes a while to eat you know.
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                napkin.unlock();
                first.unlock();
                System.out.println(name + " has put down a chopstick");
                second.unlock();
                System.out.println(name + " has put down a chopstick");
            }
        }

        @Override
        public void run() {
            while (true) {
                eat();
            }
        }
    }

    public static void main(String[] args) {
        final Philosopher diner1 = new Philosopher("Descartes", chopstick1, chopstick2, new ReentrantLock());
        final Philosopher diner2 = new Philosopher("Popper", chopstick2, chopstick3, new ReentrantLock());
        final Philosopher diner3 = new Philosopher("Zhang Daoling", chopstick3, chopstick4, new ReentrantLock());
        final Philosopher diner4 = new Philosopher("Ge Hong", chopstick4, chopstick1, new ReentrantLock());
        dine(diner1);
        dine(diner2);
        dine(diner3);
        dine(diner4);
    }

    private static void dine(final Philosopher diner) {
        new Thread(diner, diner.getName()).start();
    }

}

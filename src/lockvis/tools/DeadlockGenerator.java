package lockvis.tools;

/**
 * Dining philosophers with original synchronized style Locks.
 */
public class DeadlockGenerator {

	private static final int PICKUP_WAIT = 20; // max time to wait between picking up sticks

	private static Chopstick chopstick1 = new Chopstick("Chopstick 1");
	private static Chopstick chopstick2 = new Chopstick("Chopstick 2");
	private static Chopstick chopstick3 = new Chopstick("Chopstick 3");
	private static Chopstick chopstick4 = new Chopstick("Chopstick 4");

	static class Chopstick {
		String name;

		public Chopstick(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	static class Philosopher {
		private final String name;
		private Chopstick onLeft;
		private Chopstick onRight;

		public Philosopher(String name, Chopstick onLeft, Chopstick onRight) {
			this.name = name;
			this.onLeft = onLeft;
			this.onRight = onRight;
		}

		public String getName() {
			return this.name;
		}

		public void eat() {
			while (true) {
				try {
					Thread.sleep((int) (Math.random() * PICKUP_WAIT));

					//Decide whether to pickup left-right or right-left
					if (Math.random() > 0.5) {
						synchronized (onLeft) {
							System.out.println(name + " got " + onLeft);
							Thread.sleep((int) (Math.random() * PICKUP_WAIT));
							synchronized (onRight) {
								System.out.println(name + " got " + onRight);
								System.out.println(name + " has eaten");
							}
						}
						System.out.println(name + " has put down the chopsticks");
					} else {
						synchronized (onRight) {
							System.out.println(name + " got " + onRight);
							Thread.sleep((int) (Math.random() * PICKUP_WAIT));
							synchronized (onLeft) {
								System.out.println(name + " got " + onLeft);
								System.out.println(name + " has eaten");
							}
						}
						System.out.println(name + " has put down the chopsticks");
					}
				} catch (InterruptedException e) {
					// don't care. Continue.
				}
			}
		}
	}

	public static void main(String[] args) {
		final Philosopher diner1 = new Philosopher("Descartes", chopstick1, chopstick2);
		final Philosopher diner2 = new Philosopher("Popper", chopstick2, chopstick3);
		final Philosopher diner3 = new Philosopher("Zhang Daoling", chopstick3, chopstick4);
		final Philosopher diner4 = new Philosopher("Ge Hong", chopstick4, chopstick1);
		dine(diner1);
		dine(diner2);
		dine(diner3);
		dine(diner4);
	}

	private static void dine(final Philosopher diner) {
		new Thread(new Runnable() {
			public void run() {
				diner.eat();
			}
		}, diner.getName()).start();
	}
}

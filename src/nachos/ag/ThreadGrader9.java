package nachos.ag;

import nachos.threads.KThread;

import java.util.HashSet;
import java.util.Set;

/**
 * <li>ThreadGrader9: <b>A more extensive src of thread join</b><br>
 * </li>
 * 
 * @author Isaac
 * 
 */
public class ThreadGrader9 extends BasicTestGrader {
	static int total = 0;
	static int count = 0;
	Set<ThreadHandler> set = new HashSet<ThreadHandler>();

	public void run() {

		/* Test ThreadGrader9: Tests priority scheduler without donation */
		total = 400;
		count = 0;
		set.clear();
		for (int i = 0; i < total; ++i)
			set.add(forkNewThread(new a()));
		
		for (ThreadHandler t : set)
			t.thread.join();
		
		assertTrue(count == total,
				"not all threads finished in \nTest ThreadGrader5.a");
		done();
	}

	private class a implements Runnable {
		public void run() {
			KThread.yield();
			++count;
			System.out.println("count = " + count);
		}
	}
}

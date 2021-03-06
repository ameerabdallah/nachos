package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     * transfer priority from waiting threads
     * to the owning thread.
     * @return a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param    thread    the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
        protected ThreadState lockHolder = null;
		private LinkedList<ThreadState> waitQueue;

        PriorityQueue(boolean transferPriority) {
			waitQueue = new LinkedList<>();
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());

            KThread thread = null;

            ThreadState maxPriorityThread = pickNextThread();

            if(lockHolder != null)
            {
                lockHolder.resources.remove(this);
                lockHolder.invalidateEffectivePriorityCache();
                lockHolder = null;
            }
            if(maxPriorityThread != null)
            {
                lockHolder = maxPriorityThread;
                thread = maxPriorityThread.thread;
                waitQueue.removeFirstOccurrence(maxPriorityThread);
            }

            return thread;
        }

        // add to the waitQ
		protected void add(ThreadState threadState)
		{
            waitQueue.add(threadState);
		}

        // waiting on thread 'x'
        protected boolean isEmpty()
        {
            return waitQueue.isEmpty();
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         * return.
         */
        protected ThreadState pickNextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            int maxEffectivePriority = priorityMinimum-1;
            ThreadState maxPriorityThread = null;

            for(final ThreadState threadState: waitQueue)
            {
                if(threadState.getEffectivePriority() > maxEffectivePriority)
                {
                    maxEffectivePriority = threadState.getEffectivePriority();
                    maxPriorityThread = threadState;
                }
                // there won't be any higher priorities after this one so no reason to check the rest.
                if(maxEffectivePriority == priorityMaximum) break;
            }

            return maxPriorityThread;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());

            System.out.println(waitQueue.toString());
        }
    }



    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see    nachos.threads.KThread#schedulingState
     *
     *
     */


    protected class ThreadState {


        /**
         *
         *
         * The thread with which this object is associated.
         *
         *
         */


        protected KThread thread;
        /**
         * The priority of the associated thread.
         */
        protected int priority;
        /**
         * The effective priority of the associated thread
         */
        protected int effectivePriority;
        /**
         * A flag to determine if the effective priority of the associated thread should be recalculated
         */
        protected boolean recalculateEffectivePriority = false;

        // a list of the resources the associated thread is owning
        protected List<PriorityQueue> resources = new ArrayList<>();
        protected ThreadState lockHolder;



        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param    thread    the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            setPriority(priorityDefault);
            effectivePriority = priority;
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param    priority    the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;
            invalidateEffectivePriorityCache();
        }

        protected void invalidateEffectivePriorityCache()
        {
            if(lockHolder != null) {
                lockHolder.invalidateEffectivePriorityCache();
            }
            recalculateEffectivePriority = true;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {

            if(recalculateEffectivePriority)
            {
                if(!resources.isEmpty())
                {
                    this.effectivePriority = priority;
                    for (PriorityQueue priorityQueue : resources)
                    {
                        if(!priorityQueue.isEmpty() && priorityQueue.transferPriority)
                        {
                            this.effectivePriority = Math.max(effectivePriority, priorityQueue.pickNextThread().effectivePriority);
                        }
                    }
                }
                else
                {
                    effectivePriority = priority;
                }

            }

            recalculateEffectivePriority = false;
            return effectivePriority;
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param    waitQueue    the queue that the associated thread is
         * now waiting on.
         * @see    nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            waitQueue.add(this);
            if(waitQueue.transferPriority && waitQueue.lockHolder != null)
            {
                lockHolder = waitQueue.lockHolder;
            }
        }


        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see    nachos.threads.ThreadQueue#acquire
         * @see    nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            Lib.assertTrue(waitQueue.isEmpty());
            if(waitQueue.transferPriority) {
                resources.add(waitQueue);
                waitQueue.lockHolder = getThreadState(this.thread);
            }
        }

        public String toString()
        {
            return this.thread + " | " + this.effectivePriority;
        }
    }

}

package nachos.threads;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {

    private Condition2 speakCond = null, listenCond = null;
    private int numOfListeners = 0;
    private Lock lock = null;
    private Integer word = null;


    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        speakCond = new Condition2(lock);
        listenCond = new Condition2(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {

        lock.acquire();

        while(numOfListeners == 0 || this.word != null)
        {
            speakCond.sleep();
        }

        numOfListeners--;
        this.word = word;
        listenCond.wake();
        lock.release();

    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */
    public int listen() {
        int word;
        lock.acquire();

        numOfListeners++;
        while(this.word == null)
        {
            speakCond.wake();
            listenCond.sleep();
        }

        word = this.word;
        this.word = null;
        listenCond.wake();
        lock.release();

        return word;
    }

}
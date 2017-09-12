package daris.client.task;

import java.util.logging.Logger;

import daris.client.session.MFSession;
import daris.client.util.HasAbortableOperation;

public interface Task extends Runnable, Loggable, HasProgress, HasAbortableOperation {

    public static enum State {
        PENDING, EXECUTING, COMPLETED, FAILED
    }

    State state();

    void execute(MFSession session) throws Throwable;

    String type();

    Logger logger();

    Throwable thrown();

}

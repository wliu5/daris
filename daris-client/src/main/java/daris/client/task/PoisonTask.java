package daris.client.task;

import daris.client.session.MFSession;

public class PoisonTask extends AbstractTask {

    public PoisonTask() {
        super(null, null);
    }

    @Override
    public void execute(MFSession session) throws Throwable {
        throw new AssertionError();
    }

    @Override
    public String type() {
        return null;
    }

}

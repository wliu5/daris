package daris.client.task;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import daris.client.util.LoggingUtils;

public class TaskConsumer implements Runnable {

    private BlockingQueue<Task> _queue;
    private Logger _logger;

    private Task _currentTask;

    public TaskConsumer(BlockingQueue<Task> queue, Logger logger) {
        _queue = queue;
        _logger = logger;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                // wait for task from queue
                _currentTask = _queue.take();
                if (_currentTask instanceof PoisonTask) {
                    LoggingUtils.logInfo(_logger, "Stopping worker thread...");
                    break;
                }
                // execute task
                _currentTask.run();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LoggingUtils.logInfo(_logger, "Interrupted '" + Thread.currentThread().getName() + "' thread(id="
                    + Thread.currentThread().getId() + ").");
        } catch (Throwable e) {
            LoggingUtils.logError(_logger, e);
        }
    }

}

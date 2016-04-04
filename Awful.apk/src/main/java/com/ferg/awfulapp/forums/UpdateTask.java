package com.ferg.awfulapp.forums;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.ferg.awfulapp.network.NetworkUtils;
import com.ferg.awfulapp.task.AwfulRequest;
import com.ferg.awfulapp.util.AwfulError;

import org.jsoup.nodes.Document;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ferg.awfulapp.constants.Constants.DEBUG;

/**
 * Created by baka kaba on 18/04/2016.
 * <p/>
 * <p>Base class for async update tasks.</p>
 * <p/>
 * <p>Subclasses need to provide an initial {@link ForumParseTask} to run. This should add
 * further tasks as necessary using {@link #startTask(ForumParseTask)}. Once all started tasks
 * have ended (or the task times out), the supplied {@link ResultListener} is called with
 * the result status and the collected forums (if successful)</p> *
 */
abstract class UpdateTask {

    protected String TAG = "UpdateTask";

    // give up if the task hasn't completed after this length of time:
    private static final int TIMEOUT = 10;
    private static final TimeUnit TIMEOUT_UNITS = TimeUnit.MINUTES;
    // execution delay for added tasks, used to throttle multiple requests
    protected int taskDelayMillis = 0;

    /**
     * Single thread, basically processes request tasks sequentially, so they can be delayed
     * The bulk of the work is done on a networking thread that the request result comes in on
     */
    private final ScheduledExecutorService taskExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r, "Forum update");
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        }
    });

    /**
     * Counter that tracks the number of outstanding tasks
     */
    private final AtomicInteger openTasks = new AtomicInteger();

    /**
     * flag to ensure the UpdateTask can only be started once
     */
    private volatile boolean executed = false;
    /**
     * flag to ensure the wrap-up and result code can only be called once
     */
    private volatile boolean finishCalled = false;
    /**
     * catch-all failure flag for when something has gone wrong
     */
    private volatile boolean failed = false;

    protected final Context context;
    /**
     * The initial task to run - this may (or may not) add additional tasks to the queue
     */
    protected ForumParseTask initialTask;
    private volatile ResultListener resultListener = null;


    interface ResultListener {
        /**
         * Called when the UpdateTask has finished.
         *
         * @param task           The finished task
         * @param success        Whether the task finished successfully, or hit a fatal error
         * @param forumStructure The forums data produced by the task - will be null if success is false
         */
        void onRefreshCompleted(@NonNull UpdateTask task, boolean success, @Nullable ForumStructure forumStructure);
    }


    UpdateTask(@NonNull Context context) {
        this.context = context;
    }


    /**
     * Start the refresh task.
     * The task and its callback will be executed on a worker thread.
     *
     * @param callback A listener to deliver the result to
     */
    public void execute(@NonNull final ResultListener callback) {
        if (executed) {
            throw new IllegalStateException("Task already executed - you need to create a new one!");
        }
        executed = true;
        resultListener = callback;

        // queue up a timeout task to shut everything down - not counted in #openTasks
        taskExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                fail("timed out");
            }
        }, TIMEOUT, TIMEOUT_UNITS);

        // kick off the updates!
        startTask(initialTask);
    }


    /**
     * Interrupt and cancel this task.
     */
    public void cancel() {
        // set the executed flag in case someone tries cancelling before executing
        executed = true;
        fail("cancel was called");
    }


    /**
     * Finish the UpdateTask, and return a result to the listener.
     */
    private synchronized void finish() {
        if (finishCalled) {
            return;
        }
        finishCalled = true;
        taskExecutor.shutdownNow();

        boolean success = !failed;
        Log.d(TAG, String.format("finish() called, success: %s, outstanding tasks: %d", success ? "true" : "false", openTasks.get()));
        // only build the structure if we succeeded (otherwise it'll be incomplete)
        ForumStructure forumStructure = null;
        if (success) {
            forumStructure = buildForumStructure();
            // always treat zero forums as a failure (this is definitely bad data)
            if (forumStructure.getNumberOfForums() == 0) {
                Log.w(TAG, "All tasks completed successfully, but got 0 forums!");
                success = false;
            }
        }
        resultListener.onRefreshCompleted(this, success, forumStructure);

        // print some debug infos about what was produced
        if (DEBUG && success && forumStructure != null) {
            List<Forum> allForums = forumStructure.getAsList().formatAs(ForumStructure.FLAT).build();
            Log.w(TAG, String.format("Forums parsed! %d sections found:\n\n", allForums.size()));
            for (String line : printForums(forumStructure).split("\\n")) {
                Log.w(TAG, line);
            }
        }

    }


    /**
     * Called on success - this is where the task should create the final forum structure.
     *
     * @return The complete parsed forums hierarchy
     */
    @NonNull
    protected abstract ForumStructure buildForumStructure();


    /**
     * Mark the task as failed.
     * Call this to end the task and return a failed result - every failure condition
     * (timeout, failed network request, interruption etc) should call this!
     */
    void fail(@NonNull String reason) {
        failed = true;
        Log.w(TAG, "Forum update task failed! (" + reason + ")");
        finish();
    }


    /**
     * Add a new parse task to the queue, incrementing the number of pending tasks.
     * This call will be ignored if the main task has already been flagged as failed,
     * so it can wind down without generating new (pointless) work.
     * New tasks will be run with a delay of {@link #taskDelayMillis}
     *
     * @param requestTask The task to queue
     */
    protected void startTask(@NonNull final ForumParseTask requestTask) {
        if (failed) {
            Log.d(TAG, "Forum update has failed - dropping new task");
            return;
        }
        openTasks.incrementAndGet();
        try {
            taskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    // run the task with any required delay - immediately fail it if interrupted
                    try {
                        Thread.sleep(taskDelayMillis);
                        NetworkUtils.queueRequest(requestTask.build());
                    } catch (InterruptedException e) {
                        finishTask(false);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            // executor has probably been shut down between the failure check and submitting the new task
            e.printStackTrace();
            finishTask(false);
        }
    }


    /**
     * Remove a finished task from the outstanding list, and handle its result.
     * Every task initiated with {@link #startTask(ForumParseTask)}  must call this!
     * If called with success = false, the main task will be flagged as failed and terminate.
     *
     * @param success true if the task completed, false if it failed somehow
     */
    private void finishTask(boolean success) {
        int remaining = openTasks.decrementAndGet();
        if (!success) {
            fail("one of this update's tasks failed");
        } else if (remaining <= 0) {
            finish();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Requests
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Abstract superclass ensuring {@link #finishTask(boolean)} is always called appropriately
     */
    @WorkerThread
    protected abstract class ForumParseTask extends AwfulRequest<Void> {

        /**
         * The url of the page to retrieve, which is returned in the handle* methods
         */
        String url;


        public ForumParseTask() {
            super(context, null);
        }


        @Override
        protected String generateUrl(Uri.Builder urlBuilder) {
            return url;
        }

        // TODO: request errors aren't being handled properly, e.g. failed page loads when you're not logged in don't call here, and the task times out


        @Override
        protected final Void handleResponse(Document doc) throws AwfulError {
            onRequestSucceeded(doc);
            finishTask(true);
            return null;
        }


        @Override
        protected final boolean handleError(AwfulError error, Document doc) {
            onRequestFailed(error);
            finishTask(false);
            return false;
        }


        abstract protected void onRequestSucceeded(Document doc);

        abstract protected void onRequestFailed(AwfulError error);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Output stuff for logging
    ///////////////////////////////////////////////////////////////////////////


    private String printForums(ForumStructure forums) {
        StringBuilder sb = new StringBuilder();
        List<Forum> forumList = forums.getAsList().formatAs(ForumStructure.FULL_TREE).build();
        for (Forum section : forumList) {
            printForum(section, 0, sb);
        }
        return sb.toString();
    }


    private void printForum(Forum forum, int depth, StringBuilder sb) {
        appendPadded(sb, forum.title, depth).append(":\n");
        if (!"".equals(forum.subtitle)) {
            appendPadded(sb, forum.subtitle, depth).append("\n");
        }
        for (Forum subforum : forum.subforums) {
            printForum(subforum, depth + 1, sb);
        }
    }


    private StringBuilder appendPadded(StringBuilder sb, String message, int pad) {
        for (int i = 0; i < pad; i++) {
            sb.append("-");
        }
        sb.append(message);
        return sb;
    }

}

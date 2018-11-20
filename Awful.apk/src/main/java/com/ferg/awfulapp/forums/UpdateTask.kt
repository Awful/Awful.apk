package com.ferg.awfulapp.forums

import android.content.Context
import android.net.Uri
import android.support.annotation.WorkerThread
import com.ferg.awfulapp.constants.Constants.DEBUG
import com.ferg.awfulapp.forums.UpdateTask.ResultListener
import com.ferg.awfulapp.network.NetworkUtils
import com.ferg.awfulapp.task.AwfulRequest
import com.ferg.awfulapp.util.AwfulError
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by baka kaba on 18/04/2016.
 *
 * Base class for async update tasks.
 *
 * Subclasses need to set an [initialTask] to run. This should add further tasks as necessary
 * using [startTask]. Once all started tasks have ended (or the task times out),
 * the supplied [ResultListener] is called with the result status and the collected forums (if successful)
 *
 * Task processing can be throttled by passing a value for [taskDelayMillis], which will delay each
 * added task by this amount of time, allowing you to space out requests and avoid hitting the site
 * too heavily.
 */
internal abstract class UpdateTask(protected val context: Context, private val taskDelayMillis: Int = 0) {

    /**
     * Single thread, basically processes request tasks sequentially, so they can be delayed
     * The bulk of the work is done on a networking thread that the request result comes in on
     */
    private val taskExecutor = Executors.newScheduledThreadPool(1) { r ->
        Thread(r, "Forum update").apply { priority = Thread.MIN_PRIORITY }
    }

    /** Counter that tracks the number of outstanding tasks */
    private val openTasks = AtomicInteger()

    /** flag to ensure the UpdateTask can only be started once */
    @Volatile
    private var executed = false

    /** flag to ensure the wrap-up and result code can only be called once */
    @Volatile
    private var finishCalled = false

    /** catch-all failure flag for when something has gone wrong */
    @Volatile
    private var failed = false

    /** The initial task to run - this may (or may not) add additional tasks to the queue */
    protected abstract val initialTask: ForumParseTask

    @Volatile
    private var resultListener: ResultListener? = null


    internal interface ResultListener {
        /**
         * Called when the UpdateTask has finished.
         *
         * @param task           The finished task
         * @param success        Whether the task finished successfully, or hit a fatal error
         * @param forumStructure The forums data produced by the task - will be null if success is false
         */
        fun onRefreshCompleted(task: UpdateTask, success: Boolean, forumStructure: ForumStructure?)
    }


    /**
     * Start the refresh task.
     * The task and its callback will be executed on a worker thread.
     *
     * @param callback A listener to deliver the result to
     */
    fun execute(callback: ResultListener) {
        check(!executed) { "Task already executed - you need to create a new one!" }
        executed = true
        resultListener = callback

        // queue up a timeout task to shut everything down - not counted in #openTasks
        taskExecutor.schedule({ fail("timed out") }, TIMEOUT.toLong(), TIMEOUT_UNITS)

        // kick off the updates!
        startTask(initialTask)
    }


    /**
     * Interrupt and cancel this task.
     */
    fun cancel() {
        // set the executed flag in case someone tries cancelling before executing
        executed = true
        fail("cancel was called")
    }


    /**
     * Finish the UpdateTask, and return a result to the listener.
     */
    @Synchronized
    private fun finish() {
        if (finishCalled) return

        finishCalled = true
        taskExecutor.shutdownNow()

        var success = !failed
        Timber.d("finish() called, success: $success, outstanding tasks: ${openTasks.get()}")
        // only build the structure if we succeeded (otherwise it'll be incomplete)
        var forumStructure: ForumStructure? = null
        if (success) {
            forumStructure = buildForumStructure()
            // always treat zero forums as a failure (this is definitely bad data)
            if (forumStructure.numberOfForums == 0) {
                Timber.w("All tasks completed successfully, but got 0 forums!")
                success = false
            }
        }
        resultListener!!.onRefreshCompleted(this, success, forumStructure)

        // print some debug infos about what was produced
        if (DEBUG && forumStructure != null) {
            val allForums = forumStructure.asList.formatAs(ForumStructure.FLAT).build()
            Timber.w("Forums parsed! ${allForums.size} sections found:\n\n")
            for (line in printForums(forumStructure).split("\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                Timber.w(line)
            }
        }

    }


    /**
     * Called on success - this is where the task should create the final forum structure.
     *
     * @return The complete parsed forums hierarchy
     */
    protected abstract fun buildForumStructure(): ForumStructure


    /**
     * Mark the task as failed.
     * Call this to end the task and return a failed result - every failure condition
     * (timeout, failed network request, interruption etc) should call this!
     */
    fun fail(reason: String) {
        failed = true
        Timber.w("Forum update task failed! ($reason)")
        finish()
    }


    /**
     * Add a new parse task to the queue, incrementing the number of pending tasks.
     * This call will be ignored if the main task has already been flagged as failed,
     * so it can wind down without generating new (pointless) work.
     * New tasks will be run with a delay of [taskDelayMillis]
     *
     * @param requestTask The task to queue
     */
    protected fun startTask(requestTask: ForumParseTask) {
        if (failed) {
            Timber.d("Forum update has failed - dropping new task")
            return
        }
        openTasks.incrementAndGet()
        try {
            taskExecutor.execute {
                // run the task with any required delay - immediately fail it if interrupted
                try {
                    Thread.sleep(taskDelayMillis.toLong())
                    NetworkUtils.queueRequest(requestTask.build())
                } catch (e: InterruptedException) {
                    finishTask(false)
                }
            }
        } catch (e: RejectedExecutionException) {
            // executor has probably been shut down between the failure check and submitting the new task
            e.printStackTrace()
            finishTask(false)
        }

    }


    /**
     * Remove a finished task from the outstanding list, and handle its result.
     * Every task initiated with [.startTask]  must call this!
     * If called with success = false, the main task will be flagged as failed and terminate.
     *
     * @param success true if the task completed, false if it failed somehow
     */
    private fun finishTask(success: Boolean) {
        val remaining = openTasks.decrementAndGet()
        if (!success) {
            fail("one of this update's tasks failed")
        } else if (remaining <= 0) {
            finish()
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Requests
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Abstract superclass ensuring [.finishTask] is always called appropriately
     */
    @WorkerThread
    protected abstract inner class ForumParseTask : AwfulRequest<Void?>(context, null) {

        /**
         * The url of the page to retrieve, which is returned in the handle* methods
         */
        abstract val url: String


        override fun generateUrl(urlBuilder: Uri.Builder?): String = url

        // TODO: request errors aren't being handled properly, e.g. failed page loads when you're not logged in don't call here, and the task times out


        override fun handleResponse(doc: Document): Void? {
            onRequestSucceeded(doc)
            finishTask(true)
            return null
        }


        override fun handleError(error: AwfulError, doc: Document): Boolean {
            onRequestFailed(error)
            finishTask(false)
            return true
        }


        protected abstract fun onRequestSucceeded(doc: Document)

        protected abstract fun onRequestFailed(error: AwfulError)
    }


    ///////////////////////////////////////////////////////////////////////////
    // Output stuff for logging
    ///////////////////////////////////////////////////////////////////////////


    private fun printForums(forums: ForumStructure): String {
        val topLevelForums = forums.asList.formatAs(ForumStructure.FULL_TREE).build()
        return buildString {
            topLevelForums.forEach { printForum(it, 0) }
        }
    }


    private fun StringBuilder.printForum(forum: Forum, depth: Int) {
        with(forum) {
            appendPadded(title, depth)
            if (subtitle.isNotBlank()) appendPadded(subtitle, depth)
            subforums.forEach { printForum(it, depth + 1) }
        }
    }


    private fun StringBuilder.appendPadded(message: String, pad: Int) {
        repeat(pad) { append('-') }
        appendln(message)
    }

    companion object {
        // give up if the task hasn't completed after this length of time:
        private const val TIMEOUT = 10
        private val TIMEOUT_UNITS = TimeUnit.MINUTES
    }

}

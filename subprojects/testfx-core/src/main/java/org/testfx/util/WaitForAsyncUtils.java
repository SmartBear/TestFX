/*
 * Copyright 2013-2014 SmartBear Software
 * Copyright 2014-2018 The TestFX Contributors
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package org.testfx.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Provides static methods for handling execution on different threads. The
 * "Test Thread" is usually running on the main thread, while the GUI runs on the
 * "FX Application Thread". Additionally, tasks may also be started on different
 * asynchronous threads.
 * <p>
 * <strong>General Convention (Method Names)</strong>
 * <ul>
 * <li>{@code async} methods without a suffix refer to some unknown thread in a thread pool.
 * <li>Methods ending with the suffix {@code Fx} refer to the "FX application thread".
 * </ul>
 * <p>
 * <strong>Exception Handling</strong>
 * <p>
 * As exceptions on different threads are thrown the caller is usually not aware
 * of these exceptions. Exceptions returned directly from this framework are wrapped
 * in {@link RuntimeException}s.
 * <p>
 * There are two ways this class notifies the user of exceptions:
 * <ul>
 * <li>The returned {@link Future}.
 * <li>The internal exception stack.
 * </ul>
 * <p>
 * Usually exceptions are forwarded to the {@code Future} returned by the methods
 * of this class. When the calling thread calls {@link Future#get()} on the {@code Future}
 * any exceptions encountered during execution will be thrown. All {@code waitFor} methods
 * acquire the value of the {@code Future} and accordingly throw the same exceptions.
 * <p>
 * The <b>internal exception stack</b> stores all unhandled exceptions thrown during
 * direct calls to the {@code async} methods. As this class can not guarantee that
 * exceptions in these methods are handled properly, it will internally store
 * these exceptions. The exceptions will be in the stack, until they are handled
 * somewhere in the application. If the field {@code autoCheckException} is set to
 * {@literal true}, any subsequent calls to one of the {@code async} methods will
 * throw one of those exceptions.
 */
public final class WaitForAsyncUtils {

    //// default iming constants ////
    static final long CONDITION_SLEEP_IN_MILLIS_DEFAULT = 10;
    static final long SEMAPHORE_SLEEP_IN_MILLIS_DEFAULT = 10;
    static final int SEMAPHORE_LOOPS_COUNT_DEFAULT = 5;
    static final int PULSE_LOOPS_COUNT_DEFAULT = 2;
    static final long FX_TIMEOUT_CONDITION_DEFAULT = 5000;
    

    static final long CONDITION_SLEEP_IN_MILLIS_AGRESSIVE = 0;
    static final long SEMAPHORE_SLEEP_IN_MILLIS_AGRESSIVE = 0;
    static final int SEMAPHORE_LOOPS_COUNT_AGRESSIVE = 2;
    static final int PULSE_LOOPS_COUNT_AGRESSIVE = 1;
    static final long FX_TIMEOUT_CONDITION_AGRESSIVE = 5000;

    static final long CONDITION_SLEEP_IN_MILLIS_DEBUG = 10;
    static final long SEMAPHORE_SLEEP_IN_MILLIS_DEBUG = 10;
    static final int SEMAPHORE_LOOPS_COUNT_DEBUG = 10;
    static final int PULSE_LOOPS_COUNT_DEBUG = 5;
    static final long FX_TIMEOUT_CONDITION_DEBUG = 5000;
    
    static long CONDITION_SLEEP_IN_MILLIS = CONDITION_SLEEP_IN_MILLIS_DEFAULT;
    static long SEMAPHORE_SLEEP_IN_MILLIS = SEMAPHORE_SLEEP_IN_MILLIS_DEFAULT;
    static int SEMAPHORE_LOOPS_COUNT = SEMAPHORE_LOOPS_COUNT_DEFAULT;
    static int PULSE_LOOPS_COUNT = PULSE_LOOPS_COUNT_DEFAULT;
    static long FX_TIMEOUT_CONDITION = FX_TIMEOUT_CONDITION_DEFAULT;
    
    
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(new DefaultThreadFactory());
    
    public static boolean debugTestTiming;

    private static Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    /**
     * If {@literal true} any exceptions encountered during execution of the
     * {@code async} methods will be printed to stderr.
     * The exceptions will be printed at the time they occur (not when fetched).
     */
    public static boolean printException = true;

    /**
     * If {@literal true} any call to an {@code async} method will check for
     * the occurrence of unhandled exceptions.
     */
    public static boolean autoCheckException = true;

    /**
     * If {@literal true} any call to an {@code async} method will check for
     * the occurrence of unhandled exceptions in any {@link Thread}.
     */
    public static boolean checkAllExceptions = true;

    /**
     * If {@literal true} exceptions will be printed when they are fetched by a caller.
     * Even when they are handled properly. This field is mainly for development debug purposes.
     */
    private static final boolean TRACE_FETCH = false;

    /*
     * Static initialization of WaitForAsyncUtils.
     * Should be initialized with the FXToolkit, but the static initialization ensures
     * that it is setup before the first use.
     */
    static {
        setup();
    }

    /**
     * Needs to be called to setup WaitForAsyncUtils before the first use.
     * Currently it installs/removes the handler for uncaught exceptions depending on the flag
     * {@link #checkAllExceptions}.
     */
    private static void setup() {
        if (checkAllExceptions) {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> registerException(e));
        } else {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {});
        }
    }
    /**
     * Sets all timing relevant values to the defined default values
     */
    public static void setDefaultTiming() {
        CONDITION_SLEEP_IN_MILLIS = CONDITION_SLEEP_IN_MILLIS_DEFAULT;
        SEMAPHORE_SLEEP_IN_MILLIS = SEMAPHORE_SLEEP_IN_MILLIS_DEFAULT;
        SEMAPHORE_LOOPS_COUNT = SEMAPHORE_LOOPS_COUNT_DEFAULT;
        PULSE_LOOPS_COUNT = PULSE_LOOPS_COUNT_DEFAULT;
        FX_TIMEOUT_CONDITION = FX_TIMEOUT_CONDITION_DEFAULT;
    }
    /**
     * Sets all timing relevant values to be very fast. Timing may not be guaranteed in all cases,
     * violations may occur. This setup shouldn't generally be used. It is mainly used for testing. 
     */
    public static void setAggressiveTiming() {
        CONDITION_SLEEP_IN_MILLIS = CONDITION_SLEEP_IN_MILLIS_AGRESSIVE;
        SEMAPHORE_SLEEP_IN_MILLIS = SEMAPHORE_SLEEP_IN_MILLIS_AGRESSIVE;
        SEMAPHORE_LOOPS_COUNT = SEMAPHORE_LOOPS_COUNT_AGRESSIVE;
        PULSE_LOOPS_COUNT = PULSE_LOOPS_COUNT_AGRESSIVE;
        FX_TIMEOUT_CONDITION = FX_TIMEOUT_CONDITION_AGRESSIVE;
    }
    /**
     * Sets all timing relevant values to a value, that allows the user to follow the test
     * on screen for debugging. This option may also be used to identify timing issues in 
     * a test
     */
    public static void setDebugTiming() {
        CONDITION_SLEEP_IN_MILLIS = CONDITION_SLEEP_IN_MILLIS_DEBUG;
        SEMAPHORE_SLEEP_IN_MILLIS = SEMAPHORE_SLEEP_IN_MILLIS_DEBUG;
        SEMAPHORE_LOOPS_COUNT = SEMAPHORE_LOOPS_COUNT_DEBUG;
        PULSE_LOOPS_COUNT = PULSE_LOOPS_COUNT_DEBUG;
        FX_TIMEOUT_CONDITION = FX_TIMEOUT_CONDITION_DEBUG;
    }
    

    /**
     * Runs the given {@link Runnable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param runnable the {@code Runnable} to run
     * @return the {@code Future} result of the {@code Runnable}
     */
    public static Future<Void> async(Runnable runnable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        Callable<Void> call = new ASyncFXCallable<>(runnable, true);
        return EXECUTOR_SERVICE.submit(call);
    }

    /**
     * Runs the given {@link Runnable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param runnable the {@code Runnable} to run
     * @param throwExceptions whether or not to throw exceptions on the executing
     *      thread
     * @return the {@code Future} result of the runnable
     */
    public static Future<Void> async(Runnable runnable, boolean throwExceptions) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        Callable<Void> call = new ASyncFXCallable<>(runnable, throwExceptions);
        return EXECUTOR_SERVICE.submit(call);
    }

    /**
     * Calls the given {@link Callable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param callable the {@code Callable} to run
     * @param <T> the return type of the {@code Callable}
     * @return the {@code Future} result of the {@code Callable}
     */
    public static <T> Future<T> async(Callable<T> callable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        ASyncFXCallable<T> call = new ASyncFXCallable<>(callable, true);
        EXECUTOR_SERVICE.submit((Runnable) call); // exception handling not guaranteed
        return call;
    }

    /**
     * Calls the given {@link Callable} on a new {@link Thread} and returns a
     * {@link Future} that is set on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle exceptions
     * after the task has finished.
     *
     * @param callable the {@code Callable} to run
     * @param throwExceptions whether or not to throw exceptions on the executing
     *      thread
     * @param <T> the return type of the {@code Callable}
     * @return the {@code Future} result of the {@code Callable}
     */
    public static <T> Future<T> async(Callable<T> callable, boolean throwExceptions) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        Callable<T> call = new ASyncFXCallable<>(callable, throwExceptions);
        return EXECUTOR_SERVICE.submit(call); // exception handling not guaranteed
    }

    /**
     * Runs the given {@link Runnable} on the JavaFX Application Thread at some
     * unspecified time in the future and returns a {@link Future} that is set
     * on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle
     * exceptions after the task has finished.
     *
     * @param runnable the {@code Runnable} to run
     * @return the {@code Future} result of the {@code Runnable}
     */
    public static Future<Void> asyncFx(Runnable runnable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        ASyncFXCallable<Void> call = new ASyncFXCallable<>(runnable, true);
        runOnFxThread(call);
        return call;
    }

    /**
     * Calls the given {@link Callable} on the JavaFX Application Thread at some
     * unspecified time in the future and returns a {@link Future} that is set
     * on finish or error.
     * <p>
     * You need to evaluate the returned {@code Future} via ({@link Future#get()})
     * for exceptions or call the {@link #checkException()} method to handle
     * exceptions after the task has finished.
     *
     * @param callable the {@code Callable}
     * @param <T> the {@code Callable} type
     * @return the {@code Future} result of the {@code Callable}
     */
    public static <T> Future<T> asyncFx(Callable<T> callable) {
        if (autoCheckException) {
            checkExceptionWrapped();
        }
        ASyncFXCallable<T> call = new ASyncFXCallable<>(callable, true);
        runOnFxThread(call);
        return call;
    }

    /**
     * Waits for the given {@link Future} to be set and then returns the
     * future result of type {@code T}.
     *
     * @param future the {@code Future} to wait for to be set
     * @param <T> the type of the {@code Future}
     * @return the result of the {@code Future}
     */
    public static <T> T waitFor(Future<T> future) {
        try {
            return future.get();
        }
        catch (ExecutionException exception) {
            // if the computation threw an exception.
            throw new RuntimeException(exception.getCause());
        }
        catch (InterruptedException ignore) {
            // if the current thread was interrupted while waiting.
            return null;
        }
    }

    /**
     * Waits for given {@link Future} to be set and returns {@code T}, otherwise times out
     * with a {@link TimeoutException}.
     *
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param future the {@code Future} to wait for to be set
     * @param <T> the type of the {@code Future}
     * @return the result of the {@code Future}
     * @throws TimeoutException if the wait timed out
     */
    public static <T> T waitFor(long timeout, TimeUnit timeUnit, Future<T> future) throws TimeoutException {
        try {
            return future.get(timeout, timeUnit);
        }
        catch (ExecutionException exception) {
            // if the computation threw an exception.
            throw new RuntimeException(exception.getCause());
        }
        catch (InterruptedException ignore) {
            // if the current thread was interrupted while waiting.
            return null;
        }
    }

    /**
     * Waits for given {@link Callable} to return {@literal true} otherwise times out with
     * a {@link TimeoutException}. The condition will be evaluated at least once. This method
     * will wait for the last condition to finish after a timeout.
     *
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param condition the condition to wait for to be {@literal true}
     * @throws TimeoutException if the wait timed out
     */
    public static void waitFor(long timeout, TimeUnit timeUnit, Callable<Boolean> condition)
            throws TimeoutException {
        Instant start = Instant.now();
        while (!callConditionAndReturnResult(condition)) {
            sleep(CONDITION_SLEEP_IN_MILLIS, MILLISECONDS);
            if (Duration.between(start, Instant.now()).compareTo(Duration.of(timeout, chronoUnit(timeUnit))) >= 0) {
                throw new TimeoutException();
            }
        }
    }

    /**
     * Waits for given {@link ObservableBooleanValue} to return {@literal true} otherwise
     * times out with a {@link TimeoutException}.
     *
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param booleanValue the observable
     * @throws TimeoutException if the wait timed out
     */
    public static void waitFor(long timeout, TimeUnit timeUnit, ObservableBooleanValue booleanValue)
            throws TimeoutException {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ChangeListener<Boolean> changeListener = (observable, oldValue, newValue) -> {
            if (newValue) {
                future.complete(null);
            }
        };
        booleanValue.addListener(changeListener);
        if (!booleanValue.get()) {
            waitFor(timeout, timeUnit, future);
        }
        booleanValue.removeListener(changeListener);
    }

    /**
     * Waits for the given condition to become true. The conditions are evaluated on the Fx-Thread, so
     * GUI-Parameters can be accessed safely. The method will return, when all of the given conditions are true,
     * or the defined timeout has elapsed.
     * @param timeout the timeout to wait for
     * @param timeUnit the time unit {@code timeout} is in
     * @param conditions the conditions to wait for as BooleanSuppliers
     */
    public static void waitForFxCondition(long timeout, TimeUnit timeUnit, BooleanSupplier... conditions) {
        if (Platform.isFxApplicationThread()) {
            throw new RuntimeException(
                    "Waiting for events on the 'JavaFX Application Thread' is not possible (nor advisable). " +
                    "Instead, call 'waitForFxEvents' on a test thread (or any other background thread). " +
                    "See stacktrace(s) below to find the bad call to 'waitForFxEvents'");
        }
        //TODO#615 reduce sleep to 0
        FxConditionWaiter waiter = new FxConditionWaiter(timeUnit.toMillis(timeout), 10, conditions);
        waiter.waitFor();
    }
    
    
    /**
     * Waits for the event queue of the "JavaFX Application Thread" to be completed,
     * as well as any new events triggered in it.
     */
    public static void waitForFxEvents() {
        waitForFxEvents(SEMAPHORE_LOOPS_COUNT, PULSE_LOOPS_COUNT);
    }
    public static void waitForFxEvents(int attemptsCount, int pulses) {
        waitForFxEvents(attemptsCount, SEMAPHORE_SLEEP_IN_MILLIS, pulses, FX_TIMEOUT_CONDITION);
    }

    /**
     * Waits up to {@code attemptsCount} attempts for the event queue of the
     * "JavaFX Application Thread" to be completed, as well as any new events
     * triggered on it.
     *
     * @param attemptsCount number of events to enqueue to the JavaFx Application Thread
     * @param fxSleep the time to sleep between the events
     * @param pulses the number of rendering pulses to wait for
     */
    public static void waitForFxEvents(int attemptsCount, long fxSleep, int pulses, long timeout) {
        if (Platform.isFxApplicationThread()) {
            throw new RuntimeException(
                    "Waiting for events on the 'JavaFX Application Thread' is not possible (nor advisable). " +
                    "Instead, call 'waitForFxEvents' on a test thread (or any other background thread). " +
                    "See stacktrace(s) below to find the bad call to 'waitForFxEvents'");
        }
        FxConditionWaiter waiter = null;
        if (pulses > 0 && attemptsCount > 0) {
            waiter = new FxConditionWaiter(timeout, fxSleep,
                new FxEventCounter(attemptsCount), new FxRenderCondition(pulses));
        } else if (attemptsCount > 0) {
            waiter = new FxConditionWaiter(timeout, fxSleep, 
                new FxEventCounter(attemptsCount));
        } else if (pulses > 0) {
            waiter = new FxConditionWaiter(timeout, fxSleep, 
                new FxRenderCondition(pulses));
        } else {
            return; // nothing to wait for...
        }
        waiter.waitFor();
    }

    /**
     * Sleeps the current thread for the given duration.
     *
     * @param duration the duration to sleep
     * @param timeUnit the time unit {@code duration} is in
     */
    public static void sleep(long duration, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(duration));
        }
        catch (InterruptedException ignore) {
        }
    }

    /**
     * Runs the given {@link Runnable} on a new {@link Thread} and waits {@code millis}
     * milliseconds for it to finish, otherwise times out with a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param runnable the {@code Runnable} to run
     */
    public static void waitForAsync(long millis, Runnable runnable) {
        // exceptions handled in wait are safe
        Future<Void> future = async(runnable, false);
        waitForMillis(millis, future);
    }

    /**
     * Calls the given {@link Callable} on a new {@link Thread} and waits {@code millis}
     * milliseconds for it to finish. If finished, returns the future result of type
     * {@code T}, otherwise times out with a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param callable the {@code Callable} to call
     * @param <T> the type returned by the {@code Callable}
     * @return the result returned by the {@code Callable}
     */
    public static <T> T waitForAsync(long millis, Callable<T> callable) {
        Future<T> future = async(callable, false); //exceptions handled in wait --> safe
        return waitForMillis(millis, future);
    }

    /**
     * Runs the given {@link Runnable} on the JavaFX Application Thread at some unspecified time
     * in the future and waits {@code millis} milliseconds for it to finish, otherwise times out with
     * a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param runnable the {@code Runnable} to run
     */
    public static void waitForAsyncFx(long millis, Runnable runnable) {
        Future<Void> future = asyncFx(runnable);
        waitForMillis(millis, future);
    }

    /**
     * Calls the given {@link Callable} on the JavaFX Application Thread at some unspecified time
     * in the future and waits {@code millis} milliseconds for it to finish. If finished, returns
     * {@code T} otherwise times out with a {@link TimeoutException}.
     *
     * @param millis number of milliseconds to wait
     * @param callable the {@code Callable} to call
     * @param <T> the type returned by the {@code Callable}
     * @return the result returned by the {@code Callable}
     */
    public static <T> T waitForAsyncFx(long millis, Callable<T> callable) {
        Future<T> future = asyncFx(callable);
        return waitForMillis(millis, future);
    }

    /**
     * Checks if an exception in an async task occurred that has not been checked currently.
     * If so, the first exception will be removed and thrown by this method.
     *
     * @throws Throwable if an exception has occurred in an async task
     */
    public static void checkException() throws Throwable {
        waitForFxEvents();
        Throwable throwable = getCheckException();
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Clears all unhandled exceptions.
     */
    public static void clearExceptions() {
        exceptions.clear();
    }

    /**
     * Used to add an exception on the stack. Used by the global exception handler.
     * @param throwable the throwable to add on the local exception buffer.
     */
    private static void registerException(Throwable throwable) {
        if (checkAllExceptions) {
            // TODO more general version of filter after refactoring
            if (printException) {
                printException(throwable, null);
            }
            // Add exception to stack of occurred exceptions
            exceptions.add(new RuntimeException(throwable));
        }
    }

    /**
     * Internal function that throws Exceptions. It does not require handling
     * of the Exceptions.
     */
    private static void checkExceptionWrapped() {
        Throwable throwable = getCheckException();
        if (throwable instanceof RuntimeException) {
            if (TRACE_FETCH) {
                printException(throwable, Thread.currentThread().getStackTrace());
            }
            throw (RuntimeException) throwable;
        } else if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }

    /**
     * Pops an exception from the stack and adds an entry in the stack trace
     * to notify the user that this is not the original place of the exception.
     *
     * @return the exception or {@literal null} if none in stack
     */
    private static Throwable getCheckException() {
        if (exceptions.peek() != null) {
            Throwable throwable = exceptions.poll();
            StackTraceElement stackTraceElement = new StackTraceElement(WaitForAsyncUtils.class.getName(),
                    "---- Delayed Exception: (See Trace Below) ----",
                    WaitForAsyncUtils.class.getSimpleName() + ".java", 0);
            StackTraceElement[] stackTrace = new StackTraceElement[1];
            stackTrace[0] = stackTraceElement;
            throwable.setStackTrace(stackTrace);
            return throwable;
        } else {
            return null;
        }
    }

    private static <T> T waitForMillis(long millis, Future<T> future) {
        try {
            // exceptions are thrown on current thread
            return waitFor(millis, MILLISECONDS, future);
        }
        catch (TimeoutException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private static boolean callConditionAndReturnResult(Callable<Boolean> condition) {
        try {
            return condition.call();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
    
    
    private static class FxEventCounter implements BooleanSupplier {
        int n = 1;

        public FxEventCounter(int n) {
            this.n = n;
        }

        @Override
        public boolean getAsBoolean() {
            if (n != 0) {
                --n;
                if (debugTestTiming) {
                    System.out.println("event counter " + n);
                }
            }
            return n <= 0;
        }
    }


    /**
     * Waits for a condition on the Fx-Thread to become true. The condition is
     * evaluated periodically on the Fx-Thread with
     * <code>SEMAPHORE_SLEEP_IN_MILLIS</code> ms delay between calls.
     */
    static class FxConditionWaiter extends ConditionWaiter {
        final BooleanSupplier[] fxCondition;

        public FxConditionWaiter(long timeoutMS, long sleepMS, BooleanSupplier... fxCondition) {
            super(timeoutMS, sleepMS);
            this.fxCondition = fxCondition;
        }

        @Override
        public Boolean call() throws Exception {
            if (debugTestTiming) {
                System.out.println("Check wait conditions on Fx-Thread");
            }
            boolean done = checkCondition();
            if (debugTestTiming) {
                System.out.println("Is " + done);
            }
            return done;
        }
        
        @Override
        protected boolean checkCondition() {
            boolean done = false;
            if (fxCondition == null) {
                done = true;
            } else {
                boolean tmpDone = true;
                for (int i = 0; i < fxCondition.length; i++) {
                    if (!fxCondition[i].getAsBoolean()) {
                        tmpDone = false;
                        break;
                    }
                }
                done = tmpDone;
            }
            return done;
        }
        
        
        @Override
        protected void onInterrupted() {
            System.err.println("WaitForAsyncUtils -> Interrupt was requested while waiting");
            // StackTrace on Fx-Thread
            Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
            for (Thread t : traces.keySet()) {
                if (t.getName().indexOf("Application") > -1) {
                    System.err.println("----- Thread info " + t.getName() + "-----");
                    System.err.println("State: " + t.getState());
                    StackTraceElement[] trace = traces.get(t);
                    for (StackTraceElement se : trace) {
                        System.err.println(se);
                    }
                }
            }
            System.err.flush();
        }
        
        @Override
        protected void onTimeout() {
            // StackTrace on Fx-Thread
            //TODO#615 remove trace
            System.err.println("Fx Thread state at Timeout");
            Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
            for (Thread t : traces.keySet()) {
                if (t.getName().indexOf("Application") > -1) {
                    System.err.println("----- Thread info " + t.getName() + "-----");
                    System.err.println("State: " + t.getState());
                    StackTraceElement[] trace = traces.get(t);
                    for (StackTraceElement se : trace) {
                        System.err.println(se);
                    }
                }
            }
            System.err.flush();
            throw new TestFxTimeoutException("Timelimit for waiting for Fx-Thread exceeded." +
                    " Operation took longer than " + timeoutMS + " ms");
        }
    }
    
    abstract static class ConditionWaiter implements Callable<Boolean> {
        long startMS;
        final long timeoutMS; 
        final long sleepMS; 

        public ConditionWaiter(long timeoutMS, long sleepMS) {
            this.timeoutMS = timeoutMS;
            this.sleepMS = sleepMS;
        }

        @Override
        public Boolean call() throws Exception {
            if (debugTestTiming) {
                System.out.println("Check wait conditions on Fx-Thread " + this.getClass().getSimpleName());
            }
            boolean done = checkCondition();
            return done;
        }
        
        /**
         * This method will be called on the Fx Thread, to evaluate, if the condition the instance is waiting for
         * is true.
         * @return true, if the condition the instance waits for is true
         */
        protected abstract boolean checkCondition();

        /**
         * Waits for the condition to become true on the JavaFx Application Thread.
         */
        public void waitFor() {
            if (debugTestTiming) {
                System.out.println("----- waitFor ------ (" + this.getClass().getSimpleName() + ")");
            }

            startMS = System.currentTimeMillis();
            boolean done = false;
            while (!done) {
                if (System.currentTimeMillis() - startMS > timeoutMS) {
                    onTimeout();
                    return;
                }
                try {
                    // any exception will be thrown up the tree
                    if (sleepMS > 0) {
                        Thread.sleep(sleepMS);
                    }
                    ASyncFXCallable<Boolean> call = new ASyncFXCallable<>(this, true);
                    runOnFxThread(call);
                    long timeout = timeoutMS - (System.currentTimeMillis() - startMS);
                    timeout = Math.max(1, timeout);
                    done = call.get(timeout, TimeUnit.MILLISECONDS);
                } 
                catch (InterruptedException e) {
                    onInterrupted();
                    return; // Interrupt requested
                } 
                catch (TimeoutException e) {
                    if (debugTestTiming) {
                        System.out.println("Timeout after " + (System.currentTimeMillis() - startMS) + "ms");
                    }
                    onTimeout();
                    return;
                }
                catch (Exception e) {
                    if (debugTestTiming) {
                        System.out.println("Exception during waitForFx " + this.getClass().getSimpleName());
                        e.printStackTrace();
                    }
                    throw new RuntimeException("Exception during waiting for contdition to become true on Fx-Thread",
                            e);
                }
            }
            if (debugTestTiming) {
                System.out.println("Waiting for events took " + (System.currentTimeMillis() - startMS) + " ms " +
                        this.getClass().getSimpleName());
            }
        }
        
        /**
         * This method is called, if a interrupt occurred during waiting
         */
        protected abstract void onInterrupted();
        /**
         * This method is called, if a timeout occurred during waiting
         */
        protected abstract void onTimeout();
        
    }

    private static void printException(Throwable e, StackTraceElement[] trace) {
        StringBuilder out = new StringBuilder("--- Exception in Async Thread ---\n");
        out.append(e.getClass().getName()).append(": ").append(e.getMessage()).append('\n');
        StackTraceElement[] st = e.getStackTrace();
        out.append(printTrace(st));
        Throwable cause = e.getCause();
        while (cause != null) {
            out.append(cause.getClass().getName()).append(": ").append(cause.getMessage()).append('\n');
            st = cause.getStackTrace();
            out.append(printTrace(st));
            cause = cause.getCause();
        }
        if (trace != null) {
            out.append("--- Trace of caller of unhandled exception in Async Thread ---\n");
            out.append(printTrace(trace));
        }
        System.err.println(out.toString());
    }

    /**
     * Returns a {@code String} containing the printed stacktrace.
     *
     * @param st the stacktrace
     * @return a {@code String} containing the printed stacktrace
     */
    private static String printTrace(StackTraceElement[] st) {
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement ste : st) {
            stackTrace.append("\t").append(ste.toString()).append("\n");
        }
        return stackTrace.toString();
    }

    private static ChronoUnit chronoUnit(TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        switch (unit) {
            case NANOSECONDS:
                return ChronoUnit.NANOS;
            case MICROSECONDS:
                return ChronoUnit.MICROS;
            case MILLISECONDS:
                return ChronoUnit.MILLIS;
            case SECONDS:
                return ChronoUnit.SECONDS;
            case MINUTES:
                return ChronoUnit.MINUTES;
            case HOURS:
                return ChronoUnit.HOURS;
            case DAYS:
                return ChronoUnit.DAYS;
            default:
                throw new IllegalArgumentException("unknown TimeUnit constant: " + unit);
        }
    }

    /**
     * Internally used {@code Callable} that handles all the async stuff. All external
     * Callables/Runnables must be wrapped in this class.
     * <p>
     * <em>Note:</em> This is a single call object. Do not use twice!
     *
     * @param <X> the return type of the callable
     */
    private static class ASyncFXCallable<X> extends FutureTask<X> implements Callable<X> {

        /**
         * If {@literal true}, exceptions will be added to the internal stack.
         */
        private final boolean throwException;

        /**
         * Holds the stacktrace of the caller, for printing, if an Exception occurs.
         */
        private final StackTraceElement[] trace;

        /**
         * The unhandled exception.
         */
        private Throwable exception;

        public ASyncFXCallable(Runnable runnable, boolean throwException) {
            super(runnable, null);
            this.throwException = throwException;
            trace = Thread.currentThread().getStackTrace();
        }

        public ASyncFXCallable(Callable<X> callable, boolean throwException) {
            super(callable);
            this.throwException = throwException;
            trace = Thread.currentThread().getStackTrace();
        }

        /**
         * Called to handle exceptions during run().
         */
        @Override
        protected void setException(Throwable throwable) {
            if (throwException) {
                if (printException) {
                    printException(throwable, trace);
                }
                exception = transformException(throwable);
                // Add exception to stack of occurred exceptions
                exceptions.add(exception);
            }
            super.setException(throwable);
        }

        /**
         * Transforms am exception to be a throwable. Basically wraps the exception
         * in a RuntimeException, if it is not already one.
         *
         * @param exception the exception to transform
         * @return the throwable exception
         */
        private Throwable transformException(Throwable exception) {
            if (exception instanceof ExecutionException) {
                // unwind one ExecutionException
                return exception.getCause();
            }
            else if (exception instanceof RuntimeException || exception instanceof Error) {
                return exception;
            } else {
                return new RuntimeException(exception);
            }
        }

        @Override
        public X call() throws Exception {
            run();
            return get();
        }

        @Override
        public X get() throws InterruptedException, ExecutionException {
            try {
                return super.get();
            }
            catch (Exception e) { // exception is thrown to caller, so remove it from stack
                if (exception != null) {
                    exceptions.remove(exception);
                    exception = null;
                }
                if (TRACE_FETCH) {
                    printException(e, Thread.currentThread().getStackTrace());
                }
                throw e;
            }
        }

        @Override
        public X get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return super.get(timeout, unit);
            }
            catch (Exception e) { // exception is thrown to caller, so remove it from stack
                if (exception != null) {
                    exceptions.remove(exception);
                    exception = null;
                }
                if (TRACE_FETCH) {
                    printException(e, Thread.currentThread().getStackTrace());
                }
                throw e;
            }
        }

    }

    private static class DefaultThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName(String.format("testfx-async-pool-thread-%d", threadCount.getAndIncrement()));
            return thread;
        }
    }
}

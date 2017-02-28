/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressReporter extends Thread {

    public ProgressReporter(long totalWork, String title) {
        this.totalWork = totalWork;
        this.title = title;
        this.startTime = System.currentTimeMillis();
        this.workDone.set(0L);
        this.exitThread.set(false);

        if (!Pbrt.options.Quiet) {
            // We need to temporarily disable the profiler before launching
            // the update thread here, through the time the thread calls
            // ProfilerWorkerThreadInit(). Otherwise, there's a potential
            // deadlock if the profiler interrupt fires in the progress
            // reporter's thread and we try to access the thread-local
            // ProfilerState variable in the signal handler for the first
            // time. (Which in turn calls malloc, which isn't allowed in a
            // signal handler.)
            Stats.SuspendProfiler();
            barrier = new CyclicBarrier(2);
            this.start();

            // Wait for the thread to get past the ProfilerWorkerThreadInit()
            // call.
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            Stats.ResumeProfiler();
        }
    }

    protected void finalize() {
        if (!Pbrt.options.Quiet) {
            workDone.set(totalWork);
            exitThread.set(true);
            try {
                this.join();
            } catch (InterruptedException intr) {

            }
            System.out.println("");
        }
    }
    public void Update(long num) {
        if (num == 0 || Pbrt.options.Quiet) return;
        workDone.getAndAdd(num);
    }

    public float ElapsedMS() {
        long now = System.currentTimeMillis();
        long elapsedMs = now - startTime;
        return (float)elapsedMs;
    }

    public void Done() {
        workDone.set(totalWork);
    }

    @Override
    public void run() {
        Stats.ProfilerWorkerThreadInit();
        Stats.ProfilerState = 0;
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        PrintBar();
    }

    private void PrintBar() {
        int barLength = TerminalWidth() - 28;
        int totalPlusses = Math.max(2, barLength - title.length());
        int plussesPrinted = 0;

        // Initialize progress string
        final int bufLen = title.length() + totalPlusses + 64;
        String buf = String.format("\r%s: [", title);
        int curSpace = buf.length();
        int s = curSpace;
        for (int i = 0; i < totalPlusses; ++i) buf += ' ';
        buf += ']';
        buf += ' ';
        System.out.print(buf);
        System.out.flush();

        long sleepDuration = 250;
        int iterCount = 0;
        while (!exitThread.get()) {
            try {
                this.sleep(sleepDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Periodically increase sleepDuration to reduce overhead of
            // updates.
            ++iterCount;
            if (iterCount == 10)
                // Up to 0.5s after ~2.5s elapsed
                sleepDuration *= 2;
            else if (iterCount == 70)
                // Up to 1s after an additional ~30s have elapsed.
                sleepDuration *= 2;
            else if (iterCount == 520)
                // After 15m, jump up to 5s intervals
                sleepDuration *= 5;

            float percentDone = (float)(workDone.get()) / (float)(totalWork);
            int plussesNeeded = (int)Math.round(totalPlusses * percentDone);
            while (plussesPrinted < plussesNeeded) {
            //    curSpace++ = '+';
                ++plussesPrinted;
            }
            //fputs(buf.get(), stdout);

            // Update elapsed time and estimated time to completion
            float seconds = ElapsedMS() / 1000.f;
            float estRemaining = seconds / percentDone - seconds;
            if (percentDone == 1.f)
                System.out.format(" (%.1fs)       ", seconds);
            else if (!Float.isInfinite(estRemaining))
                System.out.format(" (%.1fs|%.1fs)  ", seconds, Math.max(0, estRemaining));
            else
                System.out.format(" (%.1fs|?s)  ", seconds);

            System.out.flush();
        }
    }

    private final long totalWork;
    private final String title;
    private final long startTime;
    private AtomicLong workDone = new AtomicLong();
    private AtomicBoolean exitThread = new AtomicBoolean();
    private CyclicBarrier barrier;
    private static int TerminalWidth() {
        return 80;// System.console();
    }
}
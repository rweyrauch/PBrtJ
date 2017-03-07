/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

public class Stats {

    public static class STAT_COUNTER implements Consumer<StatsAccumulator> {
        public STAT_COUNTER(String title) {
            this.title = title;
            this.STATS_REG = new StatRegisterer(this);
            this.var.set(0L);
        }

        public void STATS_FUNC(StatsAccumulator accum) {
            accum.ReportCounter(title, var.get());
            var.set(0L);
        }

        public StatRegisterer STATS_REG;
        private final String title;
        public ThreadLocal<Long> var = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            STATS_FUNC(accum);
        }

        public void increment() {
            var.set(var.get()+1);
        }
    }

    public static class STAT_MEMORY_COUNTER implements Consumer<StatsAccumulator> {
        public STAT_MEMORY_COUNTER(String title) {
            this.title = title;
            this.STATS_REG = new StatRegisterer(this);
            var.set(0L);
        }

        public void STATS_FUNC(StatsAccumulator accum) {
            accum.ReportMemoryCounter(title, var.get());
            var.set(0L);
        }

        public StatRegisterer STATS_REG;
        private final String title;
        public ThreadLocal<Long> var = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            STATS_FUNC(accum);
        }

        public void increment(long value) {
            var.set(var.get()+value);
        }
    }

    public static class STAT_PERCENT implements Consumer<StatsAccumulator> {
        public STAT_PERCENT(String title) {
            this.title = title;
            this.STATS_REG = new StatRegisterer(this);
            numVar.set(0L);
            denomVar.set(0L);
        }

        public void STATS_FUNC(StatsAccumulator accum) {
            accum.ReportPercentage(title, numVar.get(), denomVar.get());
            numVar.set(0L);
            denomVar.set(0L);
        }

        public void incrementNumer(int value) {
            numVar.set(numVar.get()+value);
        }
        public void incrementDenom(int value) {
            denomVar.set(denomVar.get()+value);
        }

        public StatRegisterer STATS_REG;
        private final String title;
        public ThreadLocal<Long> numVar = new ThreadLocal<>();
        public ThreadLocal<Long> denomVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            STATS_FUNC(accum);
        }
    }

    public static class STAT_RATIO implements Consumer<StatsAccumulator> {
        public STAT_RATIO(String title) {
            this.title = title;
            this.STATS_REG = new StatRegisterer(this);
            numVar.set(0L);
            denomVar.set(0L);
        }

        public void STATS_FUNC(StatsAccumulator accum) {
            accum.ReportRatio(title, numVar.get(), denomVar.get());
            numVar.set(0L);
            denomVar.set(0L);
        }

        public void incrementNumer(int value) {
            numVar.set(numVar.get()+value);
        }
        public void incrementDenom(int value) {
            denomVar.set(denomVar.get()+value);
        }

        public StatRegisterer STATS_REG;
        private final String title;
        public ThreadLocal<Long> numVar = new ThreadLocal<>();
        public ThreadLocal<Long> denomVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            STATS_FUNC(accum);
        }
    }

    public static class STAT_INT_DISTRIBUTION implements Consumer<StatsAccumulator> {
        public STAT_INT_DISTRIBUTION(String title) {
            this.title = title;
            this.STATS_REG = new StatRegisterer(this);
            sumVar.set(0L);
            countVar.set(0L);
            minVar.set(Long.MIN_VALUE);
            maxVar.set(Long.MAX_VALUE);
        }

        public void STATS_FUNC(StatsAccumulator accum) {
            accum.ReportIntDistribution(title, sumVar.get(), countVar.get(), minVar.get(), maxVar.get());
            sumVar.set(0L);
            countVar.set(0L);
            minVar.set(Long.MAX_VALUE);
            maxVar.set(Long.MIN_VALUE);
        }

        public void ReportValue(long value) {
            sumVar.set(sumVar.get()+value);
            countVar.set(countVar.get()+1);
            minVar.set(Math.min(minVar.get(), value));
            maxVar.set(Math.max(maxVar.get(), value));
        }

        public StatRegisterer STATS_REG;
        private final String title;
        public ThreadLocal<Long> sumVar = new ThreadLocal<>();
        public ThreadLocal<Long> countVar = new ThreadLocal<>();
        public ThreadLocal<Long> minVar = new ThreadLocal<>();
        public ThreadLocal<Long> maxVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            STATS_FUNC(accum);
        }
    }

    public static class STAT_FLOAT_DISTRIBUTION implements Consumer<StatsAccumulator> {
        public STAT_FLOAT_DISTRIBUTION(String title) {
            this.title = title;
            this.STATS_REG = new StatRegisterer(this);
            sumVar.set(0.0);
            countVar.set(0L);
            minVar.set(Double.MIN_VALUE);
            maxVar.set(Double.MAX_VALUE);
        }

        public void STATS_FUNC(StatsAccumulator accum) {
            accum.ReportFloatDistribution(title, sumVar.get(), countVar.get(), minVar.get(), maxVar.get());
            sumVar.set(0.0);
            countVar.set(0L);
            minVar.set(Double.MAX_VALUE);
            maxVar.set(Double.MIN_VALUE);
        }

        public void ReportValue(double value) {
            sumVar.set(sumVar.get()+value);
            countVar.set(countVar.get()+1);
            minVar.set(Math.min(minVar.get(), value));
            maxVar.set(Math.max(maxVar.get(), value));
        }

        public StatRegisterer STATS_REG;
        private final String title;
        public ThreadLocal<Double> sumVar = new ThreadLocal<>();
        public ThreadLocal<Long> countVar = new ThreadLocal<>();
        public ThreadLocal<Double> minVar = new ThreadLocal<>();
        public ThreadLocal<Double> maxVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            STATS_FUNC(accum);
        }
    }

    public enum Prof {
        SceneConstruction,
        AccelConstruction,
        TextureLoading,
        MIPMapCreation,

        IntegratorRender,
        SamplerIntegratorLi,
        SPPMCameraPass,
        SPPMGridConstruction,
        SPPMPhotonPass,
        SPPMStatsUpdate,
        BDPTGenerateSubpath,
        BDPTConnectSubpaths,
        LightDistribLookup,
        LightDistribSpinWait,
        LightDistribCreation,
        DirectLighting,
        BSDFEvaluation,
        BSDFSampling,
        BSDFPdf,
        BSSRDFEvaluation,
        BSSRDFSampling,
        PhaseFuncEvaluation,
        PhaseFuncSampling,
        AccelIntersect,
        AccelIntersectP,
        LightSample,
        LightPdf,
        MediumSample,
        MediumTr,
        TriIntersect,
        TriIntersectP,
        CurveIntersect,
        CurveIntersectP,
        ShapeIntersect,
        ShapeIntersectP,
        ComputeScatteringFuncs,
        GenerateCameraRay,
        MergeFilmTile,
        SplatFilm,
        AddFilmSample,
        StartPixel,
        GetSample,
        TexFiltTrilerp,
        TexFiltEWA,
        NumProfCategories
    }

    private static StatsAccumulator statsAccumulator = new StatsAccumulator();

    public static void PrintStats(java.io.Writer file) {
        statsAccumulator.Print(file);
    }
    public static void ClearStats() {
        statsAccumulator.Clear();
    }
    public static void ReportThreadStats() {
        StatRegisterer.CallCallbacks(statsAccumulator);
    }

    public static long ProfToBits(Prof p) { return 1L << p.ordinal(); }

    public static long ProfilerState;
    public static long CurrentProfilerState() { return ProfilerState; }

    private static AtomicBoolean profilerRunning = new AtomicBoolean(false);
    private static long profileStartTime;

    // For a given profiler state (i.e., a set of "on" bits corresponding to
    // profiling categories that are active), ProfileSample stores a count of
    // the number of times that state has been active when the timer interrupt
    // to record a profiling sample has fired.
    private static class ProfileSample {
        AtomicLong profilerState = new AtomicLong(0);
        AtomicLong count = new AtomicLong(0);
    }

    // We use a hash table to keep track of the profiler state counts. Because
    // we can't do dynamic memory allocation in a signal handler (and because
    // the counts are updated in a signal handler), we can't easily use
    // std::unordered_map.  We therefore allocate a fixed size hash table and
    // use linear probing if there's a conflict.
    private static final int profileHashSize = 256;
    private static ProfileSample[] profileSamples = new ProfileSample[profileHashSize];

    public static void InitProfiler() {
        assert (!profilerRunning.get());

        // Access the per-thread ProfilerState variable now, so that there's no
        // risk of its first access being in the signal handler (which in turn
        // would cause dynamic memory allocation, which is illegal in a signal
        // handler).
        ProfilerState = ProfToBits(Prof.SceneConstruction);

        for (int i = 0; i < profileSamples.length; i++)
            profileSamples[i] = new ProfileSample();

        ClearProfiler();

        profileStartTime = System.currentTimeMillis();
        profilerRunning.set(true);
    }

    private static AtomicInteger profilerSuspendCount = new AtomicInteger(0);

    public static void SuspendProfiler() {
        profilerSuspendCount.incrementAndGet();
    }
    public static void ResumeProfiler() {
        profilerSuspendCount.decrementAndGet();
    }

    public static void ProfilerWorkerThreadInit() {

    }

    private static char[] spaces = new char[]{' '};

    public static void ReportProfilerResults(java.io.Writer file) {
        long now = System.currentTimeMillis();

        int NumProfCategories = Prof.NumProfCategories.ordinal();
        long overallCount = 0;
        long[] eventCount = new long[NumProfCategories];
        int used = 0;
        for (ProfileSample ps : profileSamples) {
            if (ps.count.get() > 0) {
                overallCount += ps.count.get();
                ++used;
                for (int b = 0; b < NumProfCategories; ++b)
                    if ((ps.profilerState.get() & (1L << b)) != 0) eventCount[b] += ps.count.get();
            }
        }
        Api.logger.info("Used %d / %d  entries in profiler hash table", used, profileHashSize);

        HashMap<String, Long> flatResults = new HashMap<>();
        HashMap<String, Long> hierarchicalResults = new HashMap<>();
        for (ProfileSample ps : profileSamples) {
            if (ps.count.get() == 0) continue;

            String s = new String();
            for (Prof prof : Prof.values()) {
                int b = prof.ordinal();
                if ((ps.profilerState.get() & (1L << b)) != 0) {
                    if (!s.isEmpty()) {
                        // contribute to the parents...
                        hierarchicalResults.put(s, hierarchicalResults.getOrDefault(s, 0L)+ ps.count.get());
                        s += "/";
                    }
                    s += prof.toString();
                }
            }
            hierarchicalResults.put(s, hierarchicalResults.getOrDefault(s, 0L) + ps.count.get());

            int nameIndex = Pbrt.Log2Int((int)ps.profilerState.get());
            assert (nameIndex < Prof.NumProfCategories.ordinal());
            Prof prof = Prof.values()[nameIndex];
            flatResults.put(prof.toString(), flatResults.getOrDefault(prof.toString(), 0L) + ps.count.get());
        }

        try {
            file.write("  Profile\n");
            for (HashMap.Entry<String, Long> r : hierarchicalResults.entrySet()) {
                float pct = (100.f * r.getValue()) / overallCount;
                int indent = 4;
                int slashIndex = r.getKey().lastIndexOf("/");
                if (slashIndex >= 0)
                    indent += 2 * StringUtils.countMatches(r.getKey(), '/');
                String toPrint = r.getKey().substring(slashIndex + 1);
                file.write(String.format("%s%s%s %5.2f%% (%s)\n", new String(spaces, 0, indent), toPrint,
                        new String(spaces, 0, Math.max(0, (67 - toPrint.length() - indent))), pct,
                        timeString(pct, now)));
            }

            // Sort the flattened ones by time, longest to shortest.
            ArrayList<ImmutablePair<String, Long>> flatVec = new ArrayList<>();
            for (HashMap.Entry<String, Long> r : flatResults.entrySet())
                flatVec.add(new ImmutablePair<>(r.getKey(), r.getValue()));

            flatVec.sort((ImmutablePair<String, Long> a, ImmutablePair<String, Long> b) -> a.getRight().compareTo(b.getRight()) );

            file.write("  Profile (flattened)\n");
            for (ImmutablePair<String, Long> r : flatVec) {
                float pct = (100.f * r.getRight()) / overallCount;
                int indent = 4;
                String toPrint = r.getLeft();
                file.write(String.format("%s%s%s %5.2f%% (%s)\n", new String(spaces, 0, indent), toPrint,
                        new String(spaces, 0, Math.max(0, (67 - toPrint.length() - indent))), pct,
                        timeString(pct, now)));
            }
            file.write("\n");

        } catch (IOException e) {

        }
    }

    public static void ClearProfiler() {
        for (ProfileSample ps : profileSamples) {
            ps.profilerState.set(0);
            ps.count.set(0);
        }
    }

    public static void CleanupProfiler() {
        assert (profilerRunning.get());
        profilerRunning.set(false);
    }

    public static class ProfilePhase {

        public ProfilePhase(Prof p) {
            this.categoryBit = ProfToBits(p);
            this.reset = (ProfilerState & this.categoryBit) == 0;
            ProfilerState |= this.categoryBit;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            if (reset) ProfilerState &= ~categoryBit;
        }

        private boolean reset;
        private long categoryBit;
    }

    public static class StatRegisterer {

        // StatRegisterer Public Methods
        public StatRegisterer(Consumer<StatsAccumulator> func) {
            if (funcs == null)
                funcs = new ArrayList<>();
            funcs.add(func);
        }
        public static void CallCallbacks(StatsAccumulator accum) {
            for (Consumer<StatsAccumulator> func : funcs) {
                func.accept(accum);
            }
        }

        private static ArrayList<Consumer<StatsAccumulator>> funcs;
    }

    public static class StatsAccumulator {
        
        // StatsAccumulator Public Methods
        public void ReportCounter(String name, long val) {
            counters.put(name, counters.getOrDefault(name, 0L) + val);
        }
        public void ReportMemoryCounter(String name, long val) {
            memoryCounters.put(name, memoryCounters.getOrDefault(name, 0L) + val);
        }
        public void ReportIntDistribution(String name, long sum, long count, long min, long max) {
            intDistributionSums.put(name, intDistributionSums.getOrDefault(name, 0L) + sum);
            intDistributionCounts.put(name, intDistributionCounts.getOrDefault(name, 0L) + count);
            if (!intDistributionMins.containsKey(name))
                intDistributionMins.put(name, min);
            else
                intDistributionMins.put(name, Math.min(intDistributionMins.get(name), min));
            if (!intDistributionMaxs.containsKey(name))
                intDistributionMaxs.put(name, max);
            else
                intDistributionMaxs.put(name, Math.max(intDistributionMaxs.get(name), max));
        }

        public void ReportFloatDistribution(String name, double sum, long count, double min, double max) {
            floatDistributionSums.put(name, floatDistributionSums.getOrDefault(name, 0.) + sum);
            floatDistributionCounts.put(name, floatDistributionCounts.getOrDefault(name, 0L) + count);
            if (!floatDistributionMins.containsKey(name))
                floatDistributionMins.put(name, min);
            else
                floatDistributionMins.put(name, Math.min(floatDistributionMins.get(name), min));
            if (!floatDistributionMaxs.containsKey(name))
                floatDistributionMaxs.put(name, max);
            else
                floatDistributionMaxs.put(name, Math.max(floatDistributionMaxs.get(name), max));
        }
        public void ReportPercentage(String name, long num, long denom) {
            if (!percentages.containsKey(name)) {
                percentages.put(name, new PairLL(num, denom));
            }
            else {
                PairLL pair = percentages.get(name);
                pair.first += num;
                pair.second += denom;
                percentages.put(name, pair);
            }
        }
        public void ReportRatio(String name, long num, long denom) {
            if (!ratios.containsKey(name)) {
                ratios.put(name, new PairLL(num, denom));
            }
            else {
                PairLL pair = ratios.get(name);
                pair.first += num;
                pair.second += denom;
                ratios.put(name, pair);
            }
        }

        public void Print(java.io.Writer file) {
            try {
                file.write("Statistics:\n");
                HashMap<String, ArrayList<String>> toPrint = new HashMap<>();

                for (HashMap.Entry<String, Long> counter : counters.entrySet()) {
                    if (counter.getValue() == 0) continue;
                    String[] categoryTitle = {null, null};
                    categoryTitle = getCategoryAndTitle(counter.getKey(), categoryTitle);
                    if (!toPrint.containsKey(categoryTitle[0])) toPrint.put(categoryTitle[0], new ArrayList<>(1));
                    toPrint.get(categoryTitle[0]).add(String.format("%-42s               %12d", categoryTitle[1], counter.getValue()));
                }
                for (HashMap.Entry<String, Long> counter : memoryCounters.entrySet()){
                    if (counter.getValue() == 0) continue;
                    String[] categoryTitle = {null, null};
                    categoryTitle = getCategoryAndTitle(counter.getKey(), categoryTitle);
                    if (!toPrint.containsKey(categoryTitle[0])) toPrint.put(categoryTitle[0], new ArrayList<>(1));
                    float kb = (float)counter.getValue() / 1024.0f;
                    if (kb < 1024.0f)
                        toPrint.get(categoryTitle[0]).add(String.format("%-42s                  %9.2f kB", categoryTitle[1], kb));
                    else {
                        float mib = kb / 1024.0f;
                        if (mib < 1024.0f)
                            toPrint.get(categoryTitle[0]).add(String.format("%-42s                  %9.2f MiB", categoryTitle[1], mib));
                        else {
                            float gib = mib / 1024.0f;
                            toPrint.get(categoryTitle[0]).add(String.format("%-42s                  %9.2f GiB", categoryTitle[1], gib));
                        }
                    }
                }
                for (HashMap.Entry<String, Long> distributionSum : intDistributionSums.entrySet()){
                    String name = distributionSum.getKey();
                    if (intDistributionCounts.get(name) == 0) continue;
                    String[] categoryTitle = {null, null};
                    categoryTitle = getCategoryAndTitle(distributionSum.getKey(), categoryTitle);
                    if (!toPrint.containsKey(categoryTitle[0])) toPrint.put(categoryTitle[0], new ArrayList<>(1));
                    double avg = (double) distributionSum.getValue() / (double)intDistributionCounts.get(name);
                    toPrint.get(categoryTitle[0]).add(
                            String.format("%-42s                      %.3f avg [range %d - %d]",
                                    categoryTitle[1], avg, intDistributionMins.get(name), intDistributionMaxs.get(name)));
                }
                for (HashMap.Entry<String, Double> distributionSum : floatDistributionSums.entrySet()){
                    String  name = distributionSum.getKey();
                    if (floatDistributionCounts.get(name) == 0) continue;
                    String[] categoryTitle = {null, null};
                    categoryTitle = getCategoryAndTitle(distributionSum.getKey(), categoryTitle);
                    if (!toPrint.containsKey(categoryTitle[0])) toPrint.put(categoryTitle[0], new ArrayList<>(1));
                    double avg = distributionSum.getValue() / (double)floatDistributionCounts.get(name);
                    toPrint.get(categoryTitle[0]).add(
                            String.format("%-42s                      %.3f avg [range %f - %f]",
                                    categoryTitle[1], avg, floatDistributionMins.get(name), floatDistributionMaxs.get(name)));
                }
                for (HashMap.Entry<String, PairLL> percentage : percentages.entrySet()){
                    if (percentage.getValue().second == 0) continue;
                    long num = percentage.getValue().first;
                    long denom = percentage.getValue().second;
                    String[] categoryTitle = {null, null};
                    categoryTitle = getCategoryAndTitle(percentage.getKey(), categoryTitle);
                    if (!toPrint.containsKey(categoryTitle[0])) toPrint.put(categoryTitle[0], new ArrayList<>(1));
                    toPrint.get(categoryTitle[0]).add(
                            String.format("%-42s%12d / %12d (%.2f%%)",
                                    categoryTitle[1], num, denom, (100.f * num) / denom));
                }
                for (HashMap.Entry<String, PairLL> ratio : ratios.entrySet()){
                    if (ratio.getValue().second == 0) continue;
                    long num = ratio.getValue().first;
                    long denom = ratio.getValue().second;
                    String[] categoryTitle = {null, null};
                    categoryTitle = getCategoryAndTitle(ratio.getKey(), categoryTitle);
                    if (!toPrint.containsKey(categoryTitle[0])) toPrint.put(categoryTitle[0], new ArrayList<>(1));
                    toPrint.get(categoryTitle[0]).add(String.format(
                            "%-42s%12d / %12d (%.2fx)", categoryTitle[1], num, denom, (double) num / (double) denom));
                }

                for (HashMap.Entry<String, ArrayList<String>> categories : toPrint.entrySet()){
                    file.write("  " + categories.getKey() + "\n");
                    for (String item : categories.getValue())
                    file.write("    " + item + "\n");
                }
            } catch (IOException e) {

            }
        }

        public void Clear() {
            counters.clear();
            memoryCounters.clear();
            intDistributionSums.clear();
            intDistributionCounts.clear();
            intDistributionMins.clear();
            intDistributionMaxs.clear();
            floatDistributionSums.clear();
            floatDistributionCounts.clear();
            floatDistributionMins.clear();
            floatDistributionMaxs.clear();
            percentages.clear();
            ratios.clear();
        }

        // StatsAccumulator Private Data
        private static class PairLL {
            public PairLL(long first, long second) {
                this.first = first;
                this.second = second;
            }
            long first, second;
        }
        private HashMap<String, Long> counters = new HashMap<>();
        private HashMap<String, Long> memoryCounters = new HashMap<>();
        private HashMap<String, Long> intDistributionSums = new HashMap<>();
        private HashMap<String, Long> intDistributionCounts = new HashMap<>();
        private HashMap<String, Long> intDistributionMins = new HashMap<>();
        private HashMap<String, Long> intDistributionMaxs = new HashMap<>();
        private HashMap<String, Double> floatDistributionSums = new HashMap<>();
        private HashMap<String, Long> floatDistributionCounts = new HashMap<>();
        private HashMap<String, Double> floatDistributionMins = new HashMap<>();
        private HashMap<String, Double> floatDistributionMaxs = new HashMap<>();
        private HashMap<String, PairLL> percentages = new HashMap<>();
        private HashMap<String, PairLL> ratios = new HashMap<>();
    }

    private static String[] getCategoryAndTitle(String str, String[] categoryTitle) {
        int slash = str.indexOf('/');
        if (slash < 0)
            categoryTitle[1] = str;
        else {
            categoryTitle[0] = str.substring(0, slash);
            categoryTitle[1] = str.substring(slash+1);
        }
        return categoryTitle;
    }

    private static String timeString(float pct, long now) {
        pct /= 100.;  // remap passed value to to [0,1]
        // milliseconds for this category
        long ms = now - profileStartTime;
        // Peel off hours, minutes, seconds, and remaining milliseconds.
        long h = ms / (3600 * 1000);
        ms -= h * 3600 * 1000;
        long m = ms / (60 * 1000);
        ms -= m * (60 * 1000);
        long s = ms / 1000;
        ms -= s * 1000;
        ms /= 10;  // only printing 2 digits of fractional seconds
        return String.format("%4d:%02d:%02d.%02d", h, m, s, ms);
    }

}

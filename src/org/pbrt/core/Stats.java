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

    public static class Counter implements Consumer<StatsAccumulator> {
        public Counter(String title) {
            this.title = title;
            this.statRegisterer = new StatRegisterer(this);
            this.var.set(0L);
        }

        void report(StatsAccumulator accum) {
            accum.ReportCounter(title, var.get());
            var.set(0L);
        }

        StatRegisterer statRegisterer;
        private final String title;
        ThreadLocal<Long> var = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            report(accum);
        }

        public void increment() {
            var.set(var.get()+1);
        }
        public void increment(long val) {
            var.set(var.get()+val);
        }
    }

    public static class MemoryCounter implements Consumer<StatsAccumulator> {
        public MemoryCounter(String title) {
            this.title = title;
            this.statRegisterer = new StatRegisterer(this);
            var.set(0L);
        }

        void report(StatsAccumulator accum) {
            accum.ReportMemoryCounter(title, var.get());
            var.set(0L);
        }

        StatRegisterer statRegisterer;
        private final String title;
        ThreadLocal<Long> var = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            report(accum);
        }

        public void increment(long value) {
            var.set(var.get()+value);
        }
    }

    public static class Percent implements Consumer<StatsAccumulator> {
        public Percent(String title) {
            this.title = title;
            this.statRegisterer = new StatRegisterer(this);
            numVar.set(0L);
            denomVar.set(0L);
        }

        void report(StatsAccumulator accum) {
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

        StatRegisterer statRegisterer;
        private final String title;
        ThreadLocal<Long> numVar = new ThreadLocal<>();
        ThreadLocal<Long> denomVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            report(accum);
        }
    }

    public static class Ratio implements Consumer<StatsAccumulator> {
        public Ratio(String title) {
            this.title = title;
            this.statRegisterer = new StatRegisterer(this);
            numVar.set(0L);
            denomVar.set(0L);
        }

        void report(StatsAccumulator accum) {
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

        StatRegisterer statRegisterer;
        private final String title;
        ThreadLocal<Long> numVar = new ThreadLocal<>();
        ThreadLocal<Long> denomVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            report(accum);
        }
    }

    public static class IntegerDistribution implements Consumer<StatsAccumulator> {
        public IntegerDistribution(String title) {
            this.title = title;
            this.statRegisterer = new StatRegisterer(this);
            sumVar.set(0L);
            countVar.set(0L);
            minVar.set(Long.MIN_VALUE);
            maxVar.set(Long.MAX_VALUE);
        }

        void report(StatsAccumulator accum) {
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

        StatRegisterer statRegisterer;
        private final String title;
        ThreadLocal<Long> sumVar = new ThreadLocal<>();
        ThreadLocal<Long> countVar = new ThreadLocal<>();
        ThreadLocal<Long> minVar = new ThreadLocal<>();
        ThreadLocal<Long> maxVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            report(accum);
        }
    }

    public static class FloatDistribution implements Consumer<StatsAccumulator> {
        public FloatDistribution(String title) {
            this.title = title;
            this.statRegisterer = new StatRegisterer(this);
            sumVar.set(0.0);
            countVar.set(0L);
            minVar.set(Double.MIN_VALUE);
            maxVar.set(Double.MAX_VALUE);
        }

        void report(StatsAccumulator accum) {
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

        StatRegisterer statRegisterer;
        private final String title;
        ThreadLocal<Double> sumVar = new ThreadLocal<>();
        ThreadLocal<Long> countVar = new ThreadLocal<>();
        ThreadLocal<Double> minVar = new ThreadLocal<>();
        ThreadLocal<Double> maxVar = new ThreadLocal<>();

        @Override
        public void accept(StatsAccumulator accum) {
            report(accum);
        }
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
}

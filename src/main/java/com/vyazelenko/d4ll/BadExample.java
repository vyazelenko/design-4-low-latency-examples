package com.vyazelenko.d4ll;

import org.agrona.collections.Hashing;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class BadExample
{
    private static final int SIZE = 1024 * 1024;
    private int next;
    private long[] values;
    private HashMap<Long, Integer> cache;

    @Setup
    public void setup()
    {
        values = new long[SIZE];
        cache = new HashMap<>(SIZE);
        ArrayList<Long> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++)
        {
            values[i] = ThreadLocalRandom.current().nextLong(1_000_000_000_001L, Long.MAX_VALUE);
            list.add(values[i]);
        }
        Collections.shuffle(list);
        for (final Long value : list)
        {
            cache.put(value, Hashing.hash(value));
        }
    }

    @Benchmark
    public Integer cached()
    {
        final int index = (next++ & (SIZE - 1));
        return cache.get(values[index]);
    }

    @Benchmark
    public int calculated()
    {
        final int index = (next++ & (SIZE - 1));
        return Hashing.hash(values[index]);
    }
}

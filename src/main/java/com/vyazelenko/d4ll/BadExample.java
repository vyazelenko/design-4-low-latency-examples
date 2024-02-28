package com.vyazelenko.d4ll;

import org.agrona.BitUtil;
import org.agrona.collections.Hashing;
import org.openjdk.jmh.annotations.*;

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
    @Param({ "1048576" })
    private int size;
    private int next;
    private long[] values;
    private HashMap<Long, Integer> cache;

    @Setup
    public void setup()
    {
        if (!BitUtil.isPowerOfTwo(size))
        {
            throw new IllegalArgumentException("Size must be a power of two, got: " + size);
        }
        values = new long[size];
        cache = new HashMap<>(size);
        for (int i = 0; i < size; i++)
        {
            values[i] = ThreadLocalRandom.current().nextLong(1_000_000_000_001L, Long.MAX_VALUE);
            cache.put(values[i], Hashing.hash(values[i]));
        }
    }

    @Benchmark
    public Integer cached()
    {
        final int index = (next++ & (size - 1));
        return cache.get(values[index]);
    }

    @Benchmark
    public int calculated()
    {
        final int index = (next++ & (size - 1));
        return Hashing.hash(values[index]);
    }
}

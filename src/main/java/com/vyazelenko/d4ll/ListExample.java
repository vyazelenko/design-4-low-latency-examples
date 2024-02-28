package com.vyazelenko.d4ll;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class ListExample
{
    private static final int SIZE = 4096;
    private int cursor;
    private int[] keys;
    private long[] values;
    private ArrayList<Long> list;

    @Setup
    public void setup()
    {
        final Random random = new Random(0xDEADBEEF);
        keys = new int[SIZE];
        values = new long[SIZE];
        list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++)
        {
            keys[i] = random.nextInt(SIZE);
            values[i] = random.nextLong();
            list.add(Long.MIN_VALUE);
        }
        for (int i = 0; i < SIZE; i++)
        {
            list.set(keys[i], values[i]);
        }
    }

    @Benchmark
    public long array()
    {
        final int index = keys[cursor++ & (SIZE - 1)];
        return values[index];
    }

    @Benchmark
    public Long list()
    {
        final int index = keys[cursor++ & (SIZE - 1)];
        return list.get(index);
    }
}

package com.vyazelenko.d4ll;

import org.agrona.collections.Hashing;
import org.agrona.collections.Long2LongCounterMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class ParseExample
{
    private static final int SIZE = 1024 * 1024;
    private static final int NUM_UNIQUE_KEYS = 50_000;

    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SIZE * 50);
    private final HashMap<Long, Long> hashMap = new HashMap<>(NUM_UNIQUE_KEYS * 2);
    private final UnsafeBuffer unsafeBuffer = new UnsafeBuffer();
    private final Long2LongCounterMap counterMap = new Long2LongCounterMap(NUM_UNIQUE_KEYS * 2, Hashing.DEFAULT_LOAD_FACTOR, 0);
    private final int[] lengths = new int[SIZE];
    private final int[] offsets = new int[SIZE];
    private int next;

    @Setup
    public void setup()
    {
        unsafeBuffer.wrap(byteBuffer);
        final Random r = new Random(-4623848);
        final long[] keys = r.longs(NUM_UNIQUE_KEYS).toArray();
        int offset = 0;
        for (int i = 0; i < SIZE; i++)
        {
            offsets[i] = offset;
            int written = unsafeBuffer.putStringWithoutLengthAscii(offset, "{key:");
            final long key = keys[i % keys.length];
            written += unsafeBuffer.putLongAscii(offset + written, key);
            written += unsafeBuffer.putStringWithoutLengthAscii(offset + written, ",value:");
            written += unsafeBuffer.putNaturalIntAscii(offset + written, r.nextInt(100_000, 1_000_000));
            written += unsafeBuffer.putStringWithoutLengthAscii(offset + written, "}");
            lengths[i] = written;
            offset += written;
            hashMap.put(key, key);
            counterMap.put(key, key);
        }
    }

    @Benchmark
    public long jdk()
    {
        final int index = (next++ & (SIZE - 1));
        final int offset = offsets[index];
        final int length = lengths[index];

        final byte[] bytes = new byte[length];
        byteBuffer.position(offset).limit(offset + length).get(bytes);

        final String json = new String(bytes, StandardCharsets.US_ASCII);

        final long key = Long.parseLong(json, 5, length - 14, 10);
        final int value = Integer.parseInt(json, length - 7, length - 1, 10);

        final Long oldValue = hashMap.get(key);
        hashMap.put(key, null != oldValue ? oldValue + value : (long)value);
        return null != oldValue ? oldValue : 0;
    }

    @Benchmark
    public long agrona()
    {
        final int index = (next++ & (SIZE - 1));
        final int offset = offsets[index];
        final int length = lengths[index];

        unsafeBuffer.wrap(byteBuffer, offset, length);

        final long key = unsafeBuffer.parseLongAscii(5, length - 19);
        final int value = unsafeBuffer.parseNaturalIntAscii(length - 7, 6);

        return counterMap.getAndAdd(key, value);
    }
}

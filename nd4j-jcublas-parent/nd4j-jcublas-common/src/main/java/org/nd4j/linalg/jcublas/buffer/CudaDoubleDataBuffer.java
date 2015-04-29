/*
 * Copyright 2015 Skymind,Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.nd4j.linalg.jcublas.buffer;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.jcublas.JCublas;
import jcuda.runtime.JCuda;
import jcuda.runtime.cudaMemcpyKind;
import jcuda.utils.KernelLauncher;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.util.ArrayUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

/**
 * Cuda double  buffer
 *
 * @author Adam Gibson
 */
public class CudaDoubleDataBuffer extends BaseCudaDataBuffer {

    private double[] data;

    /**
     * Base constructor
     *
     * @param length the length of the buffer
     */
    public CudaDoubleDataBuffer(int length) {
        super(length, Sizeof.DOUBLE);
    }

    /**
     * Instantiate based on the given data
     *
     * @param data the data to instantiate with
     */
    public CudaDoubleDataBuffer(double[] data) {
        this(data.length);
        setData(data);
    }


    @Override
    public void assign(int[] indices, float[] data, boolean contiguous, int inc) {
        ensureNotFreed();

        if (indices.length != data.length)
            throw new IllegalArgumentException("Indices and data length must be the same");
        if (indices.length > getLength())
            throw new IllegalArgumentException("More elements than space to assign. This buffer is of length " + getLength() + " where the indices are of length " + data.length);

        if (contiguous) {
            int offset = indices[0];
            Pointer p = Pointer.to(data);
            set(offset, data.length, p, inc);

        } else
            throw new UnsupportedOperationException("Non contiguous is not supported");

    }

    @Override
    public void assign(int[] indices, double[] data, boolean contiguous, int inc) {
        if (indices.length != data.length)
            throw new IllegalArgumentException("Indices and data length must be the same");
        if (indices.length > getLength())
            throw new IllegalArgumentException("More elements than space to assign. This buffer is of length " + getLength() + " where the indices are of length " + data.length);

        if (contiguous) {
            int offset = indices[0];
            Pointer p = Pointer.to(data);
            set(offset, data.length, p, inc);
        } else
            throw new UnsupportedOperationException("Non contiguous is not supported");

    }

    @Override
    public double[] getDoublesAt(int offset, int length) {
        return getDoublesAt(offset, 1, length);
    }

    @Override
    public float[] getFloatsAt(int offset, int length) {
        return ArrayUtil.toFloats(getDoublesAt(offset, length));
    }

    @Override
    public double[] getDoublesAt(int offset, int inc, int length) {
        ensureNotFreed();

        if (offset + length > getLength())
            length -= offset;

        double[] ret = new double[length];
        ByteBuffer buf = getBuffer(offset);
        DoubleBuffer buf2 = buf.asDoubleBuffer();
        for(int i = 0; i < length; i++) {
            ret[i] = buf2.get(i * inc);
        }
        return ret;
    }

    @Override
    public float[] getFloatsAt(int offset, int inc, int length) {
        ensureNotFreed();
        return ArrayUtil.toFloats(getDoublesAt(offset, 1, length));
    }

    @Override
    public void assign(Number value, int offset) {
        ensureNotFreed();
        ByteBuffer buf = getBuffer(offset);
        DoubleBuffer buf2 = buf.asDoubleBuffer();
        for (int i = offset; i < getLength(); i++)
            buf2.put(i,value.doubleValue());
    }

    @Override
    public void setData(int[] data) {
        setData(ArrayUtil.toDoubles(data));
    }

    @Override
    public void setData(float[] data) {
        setData(ArrayUtil.toDoubles(data));
    }

    @Override
    public void setData(double[] data) {
        ensureNotFreed();
        this.data = data;
        if (data.length != length)
            throw new IllegalArgumentException("Unable to set vector, must be of length " + getLength() + " but found length " + data.length);

    }

    @Override
    public byte[] asBytes() {
        return hostBuffer.array();
    }

    @Override
    public int dataType() {
        return DataBuffer.DOUBLE;
    }



    @Override
    public double getDouble(int i) {
        //ensureNotFreed();
        return data[i];
    }

    @Override
    public float getFloat(int i) {
        return (float) getDouble(i);
    }

    @Override
    public Number getNumber(int i) {
        return getDouble(i);
    }


    @Override
    public void put(int i, float element) {
        put(i, (double) element);
    }

    @Override
    public void put(int i, double element) {
        ensureNotFreed();
        data[i] = element;
    }

    @Override
    public void put(int i, int element) {
        put(i, (double) element);
    }


    @Override
    public int getInt(int ix) {
        return (int) getDouble(ix);
    }

    @Override
    public DataBuffer dup() {
        ensureNotFreed();

        CudaDoubleDataBuffer buffer = new CudaDoubleDataBuffer(getLength());
        copyTo(buffer);
        return buffer;
    }


    private void writeObject(java.io.ObjectOutputStream stream)
            throws java.io.IOException {
        stream.defaultWriteObject();

        if (getHostPointer() == null) {
            stream.writeInt(0);
        } else {
            double[] arr = this.asDouble();

            stream.writeInt(arr.length);
            for (int i = 0; i < arr.length; i++) {
                stream.writeDouble(arr[i]);
            }
        }
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws java.io.IOException, ClassNotFoundException {
        stream.defaultReadObject();

        int n = stream.readInt();
        double[] arr = new double[n];

        for (int i = 0; i < n; i++) {
            arr[i] = stream.readDouble();
        }
        setData(arr);
    }


}

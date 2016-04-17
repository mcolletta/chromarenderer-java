package net.chromarenderer.math;

import java.util.Arrays;

/**
* @author steinerb
*/
public class ImmutableVector32 implements Vector3 {

    private final float[] values;

    public ImmutableVector32() {
        values = new float[]{0.f,0.f,0.f};
    }

    public ImmutableVector32(float x, float y, float z) {
        values = new float[]{x,y,z};
    }

    public ImmutableVector32(Vector3 input) {
        values = new float[]{input.getX(), input.getY(),input.getZ()};
    }

    public ImmutableVector32 mult(float val){
        return new ImmutableVector32(
                values[0] * val,
                values[1] * val,
                values[2] * val
        );
    }

    public float dot(Vector3 input){
        return values[0]*input.getX() + values[1]*input.getY() + values[2]*input.getZ();
    }


    @Override
    public float[] internalGetValues() {
        return Arrays.copyOf(values, 3);
    }


    public ImmutableVector32 plus(Vector3 input){
        return new ImmutableVector32(
                values[0] + input.getX(),
                values[1] + input.getY(),
                values[2] + input.getZ()
        );
    }

    public ImmutableVector32 plus(float x, float y, float z){
        return new ImmutableVector32(
                this.values[0] + x,
                this.values[1] + y,
                this.values[2] + z
        );
    }

    public ImmutableVector32 div(float val) {
        float div = 1.0f / val;
        return mult(div);
    }

    @Override
    public ImmutableVector32 minus(Vector3 input) {
        return new ImmutableVector32(
                values[0] - input.getX(),
                values[1] - input.getY(),
                values[2] - input.getZ()
        );
    }

    public ImmutableVector32 minus(float x, float y, float z) {
        return new ImmutableVector32(
                this.values[0] - x,
                this.values[1] - y,
                this.values[2] - z
        );
    }

    public float getX() {
        return values[0];
    }

    public float getY() {
        return values[1];
    }

    public float getZ() {
        return values[2];
    }

    public Vector3 mult(Vector3 project) {
        return new ImmutableVector32(values[0] * project.getX(), values[1] * project.getY(), values[2] * project.getZ());
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(Vector3 input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(float x, float y, float z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableVector32 crossProduct(Vector3 input) {
        return new ImmutableVector32(
                this.getY() * input.getZ() - this.getZ() * input.getY(),
                this.getZ() * input.getX() - this.getX() * input.getZ(),
                this.getX() * input.getY() - this.getY() * input.getX());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableVector32 vector3 = (ImmutableVector32) o;

        if (Float.compare(vector3.values[0], values[0]) != 0) return false;
        if (Float.compare(vector3.values[1], values[1]) != 0) return false;
        if (Float.compare(vector3.values[2], values[2]) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (values[0] != +0.0f ? Float.floatToIntBits(values[0]) : 0);
        result = 31 * result + (values[1] != +0.0f ? Float.floatToIntBits(values[1]) : 0);
        result = 31 * result + (values[2] != +0.0f ? Float.floatToIntBits(values[2]) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Vector3{" +
                "x=" + values[0] +
                ", y=" + values[1] +
                ", z=" + values[2] +
                '}';
    }

    public ImmutableVector32 normalize() {
        float recLen = 1.f / length();
        return new ImmutableVector32(getX() * recLen, getY() * recLen, getZ() * recLen);
    }

    @Override
    public ImmutableVector32 abs() {
        return new ImmutableVector32(Math.abs(values[0]), Math.abs(values[1]), Math.abs(values[2]));
    }


    @Override
    public float getScalar(int splitDimIndex) {
        return values[splitDimIndex];
    }


    public ImmutableVector32 inverse() {
        return new ImmutableVector32(1.f / this.getX(), 1.f/this.getY(), 1.f/this.getZ()).normalize();
    }
}
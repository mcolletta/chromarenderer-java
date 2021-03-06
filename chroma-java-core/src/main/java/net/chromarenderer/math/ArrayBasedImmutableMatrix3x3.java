package net.chromarenderer.math;

import java.util.Arrays;

/**
 * @author steinerb
 */
public class ArrayBasedImmutableMatrix3x3 {

    ImmutableVector3[] columns;

    public ArrayBasedImmutableMatrix3x3(Vector3 col1, Vector3 col2, Vector3 col3) {
        this.columns = new ImmutableVector3[3];
        columns[0] = new ImmutableVector3(col1);
        columns[1] = new ImmutableVector3(col2);
        columns[2] = new ImmutableVector3(col3);
    }

    public ArrayBasedImmutableMatrix3x3(float m11, float m12, float m13, float m21, float m22, float m23, float m31, float m32, float m33) {
        this(new ImmutableVector3(m11, m21, m31), new ImmutableVector3(m12, m22, m32), new ImmutableVector3(m13, m23, m33));
    }


    public ArrayBasedImmutableMatrix3x3 invert() {

        float temp[] = new float[9];
        float invDet = 1.f/(col1().getX()*col2().getY()*col3().getZ() - col1().getX()*col3().getY()*col2().getZ() - col2().getX()*col1().getY()*col3().getZ()
                + col2().getX()*col3().getY()*col1().getZ() + col3().getX()*col1().getY()*col2().getZ() - col3().getX()*col2().getY()*col1().getZ());

        temp[0] = col2().getY()*col3().getZ() - col3().getY()*col2().getZ();
        temp[1] = col3().getX()*col2().getZ() - col2().getX()*col3().getZ();
        temp[2] = col3().getX()*col3().getY() - col3().getX()*col2().getY();
        temp[3] = col3().getY()*col1().getZ() - col3().getZ()*col1().getY();
        temp[4] = col1().getX()*col3().getZ() - col3().getX()*col1().getZ();
        temp[5] = col2().getX()*col1().getZ() - col1().getX()*col2().getZ();
        temp[6] = col1().getY()*col2().getZ() - col1().getZ()*col2().getY();
        temp[7] = col3().getX()*col1().getY() - col1().getX()*col3().getY();
        temp[8] = col1().getX()*col2().getY() - col2().getX()*col1().getY();

        ImmutableVector3 newCol1 = new ImmutableVector3(invDet * temp[0], invDet * temp[3], invDet * temp[6]);
        ImmutableVector3 newCol2 = new ImmutableVector3(invDet * temp[1], invDet * temp[4], invDet * temp[7]);
        ImmutableVector3 newCol3 = new ImmutableVector3(invDet * temp[2], invDet * temp[5], invDet * temp[8]);

        return new ArrayBasedImmutableMatrix3x3(newCol1, newCol2, newCol3);
    }

    public ArrayBasedImmutableMatrix3x3 orthogonalize() {
        ImmutableVector3 tempA2, tempA3;
        ImmutableVector3 newCol1 = col1().normalize();
        tempA2 = col2().minus(newCol1.mult(newCol1.dot(col2())));
        ImmutableVector3 newCol2 = tempA2.normalize();
        ImmutableVector3 projNewCol1 = newCol1.mult(newCol1.dot(col3()));
        ImmutableVector3 projNewCol2 = newCol2.mult(newCol2.dot(col3()));
        tempA3 = col3().minus(projNewCol1).minus(projNewCol2);
        ImmutableVector3 newCol3 = tempA3.normalize();      
        return new ArrayBasedImmutableMatrix3x3(newCol1, newCol2, newCol3);
    }

    public ArrayBasedImmutableMatrix3x3 transpose() {
        return new ArrayBasedImmutableMatrix3x3(row1(), row2(), row3());
    }

    public ArrayBasedImmutableMatrix3x3 mult(ArrayBasedImmutableMatrix3x3 input) {

        ImmutableVector3 row1 = row1();
        ImmutableVector3 row2 = row2();
        ImmutableVector3 row3 = row3();

        return new ArrayBasedImmutableMatrix3x3(new ImmutableVector3(row1.dot(input.col1()),
                                                           row2.dot(input.col1()),
                                                           row3.dot(input.col1())),
                                                             new ImmutableVector3(row1.dot(input.col2()),
                                                                                  row2.dot(input.col2()),
                                                                                  row3.dot(input.col2())),
                                                                                    new ImmutableVector3(row1.dot(input.col3()),
                                                                                                         row2.dot(input.col3()),
                                                                                                         row3.dot(input.col3())));
    }

    public ImmutableVector3 mult(ImmutableVector3 input) {
        return new ImmutableVector3( row1().dot(input), row2().dot(input), row3().dot(input) );
    }

    private ImmutableVector3 row1() {
        return new ImmutableVector3(columns[0].getX(), columns[1].getX(), columns[2].getX());
    }

    private ImmutableVector3 row2() {
        return new ImmutableVector3(columns[0].getY(), columns[1].getY(), columns[2].getY());
    }

    private ImmutableVector3 row3() {
        return new ImmutableVector3(columns[0].getZ(), columns[1].getZ(), columns[2].getZ());
    }

    private ImmutableVector3 col1() {
        return columns[0];
    }

    private ImmutableVector3 col2() {
        return columns[1];
    }

    private ImmutableVector3 col3() {
        return columns[2];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArrayBasedImmutableMatrix3x3 that = (ArrayBasedImmutableMatrix3x3) o;

        if (!Arrays.equals(columns, that.columns)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(columns);
    }

    @Override
    public String toString() {
        return "ImmutableMatrix3x3{" +
                "columns=" + Arrays.toString(columns) +
                '}';
    }
}

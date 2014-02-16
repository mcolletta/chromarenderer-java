package net.chroma.math.geometry;

import net.chroma.math.ImmutableArrayMatrix3x3;
import net.chroma.math.ImmutableVector3;
import net.chroma.math.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * @author steinerb
 */
public class SceneFactory {

    public static List<Geometry> cornellBox(ImmutableVector3 center, float halfDimension){

        List<Geometry> result = new ArrayList<>(10);

        Vector3 shiftX = new ImmutableVector3(halfDimension, 0.0f, 0.0f);
        Vector3 shiftY = new ImmutableVector3(0.0f, halfDimension, 0.0f);
        Vector3 shiftZ = new ImmutableVector3(0.0f, 0.0f, halfDimension);

        Vector3 minusCenter = center.mult(-1f);

        ImmutableArrayMatrix3x3 rotationY90 = new ImmutableArrayMatrix3x3(0, 0, 1,
                                                                          0, 1, 0,
                                                                         -1, 0, 0);

        ImmutableArrayMatrix3x3 rotationZ90 = new ImmutableArrayMatrix3x3( 0,-1, 0,
                                                                           1, 0, 0,
                                                                           0, 0, 1);


        ImmutableVector3 p0x1 = new ImmutableVector3(center.subtract(shiftX).subtract(shiftY).plus(shiftZ));
        ImmutableVector3 p1x1 = new ImmutableVector3(center.subtract(shiftX).subtract(shiftY).subtract(shiftZ));
        ImmutableVector3 p2x1 = new ImmutableVector3(center.subtract(shiftX).plus(shiftY).subtract(shiftZ));
        ImmutableVector3 p3x1 = new ImmutableVector3(center.subtract(shiftX).plus(shiftY).plus(shiftZ));

        Triangle t0 = new Triangle(p0x1, p1x1, p2x1);
        Triangle t1 = new Triangle(p3x1, p0x1, p2x1);

        //left
        result.add(t0);
        result.add(t1);

        //back
        result.add(t0.transpose(minusCenter).rotate(rotationY90).transpose(center));
        result.add(t1.transpose(minusCenter).rotate(rotationY90).transpose(center));

        //floor
        Geometry t2 = t0.transpose(minusCenter).rotate(rotationZ90).transpose(center);
        Geometry t3 = t1.transpose(minusCenter).rotate(rotationZ90).transpose(center);
        result.add(t2);
        result.add(t3);

        //left
        Geometry t4 = t2.transpose(minusCenter).rotate(rotationZ90).transpose(center);
        Geometry t5 = t3.transpose(minusCenter).rotate(rotationZ90).transpose(center);
        result.add(t4);
        result.add(t5);

        //top
        result.add(t4.transpose(minusCenter).rotate(rotationZ90).transpose(center));
        result.add(t5.transpose(minusCenter).rotate(rotationZ90).transpose(center));

        return result;
    }
}

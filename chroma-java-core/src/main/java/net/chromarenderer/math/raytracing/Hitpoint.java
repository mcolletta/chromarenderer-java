package net.chromarenderer.math.raytracing;

import net.chromarenderer.math.Constants;
import net.chromarenderer.math.ImmutableVector3;
import net.chromarenderer.math.VectorUtils;
import net.chromarenderer.math.geometry.Geometry;
import net.chromarenderer.renderer.core.ChromaThreadContext;
import net.chromarenderer.renderer.shader.MaterialType;
import org.apache.commons.math3.util.FastMath;


public class Hitpoint {

    public static final Hitpoint INFINITY = new Hitpoint(null, Float.MAX_VALUE, null, null);

    private final Geometry hitGeometry;
    private float distance;
    private final ImmutableVector3 point;
    private final ImmutableVector3 hitpointNormal;
    private final float inverseSampleWeight;


    public Hitpoint(Geometry hitGeometry, float distance, ImmutableVector3 point, ImmutableVector3 hitpointNormal) {
        this.hitGeometry = hitGeometry;
        this.distance = distance;
        this.point = point;
        this.hitpointNormal = hitpointNormal;
        this.inverseSampleWeight = 1.0f;
    }


    public Hitpoint(Geometry hitGeometry, ImmutableVector3 point, ImmutableVector3 hitpointNormal, float inverseSampleWeight) {
        this.hitGeometry = hitGeometry;
        this.point = point;
        this.hitpointNormal = hitpointNormal;
        this.inverseSampleWeight = inverseSampleWeight;
    }


    public ImmutableVector3 getPoint() {
        return point;
    }


    public ImmutableVector3 getHitpointNormal() {
        return hitpointNormal;
    }


    public Geometry getHitGeometry() {
        return hitGeometry;
    }


    public CoordinateSystem getCoordinateSystem() {
        return VectorUtils.buildCoordSystem(hitpointNormal);
    }


    public ImmutableVector3 getUniformHemisphereSample() {
        float u = ChromaThreadContext.randomFloatClosedOpen();
        float v = ChromaThreadContext.randomFloatClosedOpen();

        float sqrtOneMinusU = (float) FastMath.sqrt(1.0f - u * u); // sin² + cos² = 1 -> sin = sqrt(1-cos²)
        float vTwoPi = v * Constants.TWO_PI_f;

        ImmutableVector3 unitSphereSample = new ImmutableVector3((float) (FastMath.cos(vTwoPi) * sqrtOneMinusU), (float) (sqrtOneMinusU * FastMath.sin(vTwoPi)), u);
        CoordinateSystem coordinateSystem = getCoordinateSystem();
        return coordinateSystem.getT1().mult(unitSphereSample.getX()).plus(coordinateSystem.getT2().mult(unitSphereSample.getY())).plus(coordinateSystem.getN().mult(unitSphereSample.getZ())).normalize();
    }


    public boolean hit() {
        return hitGeometry != null;
    }


    public float getInverseSampleWeight() {
        return inverseSampleWeight;
    }


    public boolean isOn(MaterialType materialType) {
        return hit() && getHitGeometry().getMaterial().getType().equals(materialType);
    }


    public float getDistance() {
        return distance;
    }


    public void setDistance(float distance) {
        this.distance = distance;
    }
}

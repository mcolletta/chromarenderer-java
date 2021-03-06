package net.chromarenderer.math.geometry;

import net.chromarenderer.math.ImmutableMatrix3x3;
import net.chromarenderer.math.ImmutableVector3;
import net.chromarenderer.math.Vector3;
import net.chromarenderer.math.raytracing.Ray;
import net.chromarenderer.renderer.shader.Material;

/**
 * @author bensteinert
 */
public class PhotonFountain extends AbstractGeometry {

    private final ImmutableVector3 point;
    private final float power;

    public PhotonFountain(ImmutableVector3 point, float power) {
        super(Material.createEmittingMaterial(Vector3.ONE, power));
        this.power = power;
        this.point = point;
    }

    @Override
    public float intersect(Ray ray) {
        return 0;
    }

    @Override
    public PhotonFountain transpose(Vector3 transpose) {
        return new PhotonFountain(point.plus(transpose), power);
    }

    @Override
    public Geometry rotate(ImmutableMatrix3x3 rotationY) {
        return new PhotonFountain(point, power);
    }

    @Override
    public ImmutableVector3 getNormal(ImmutableVector3 hitpoint) {
        return hitpoint.minus(point).normalize();
    }

    @Override
    public boolean isPlane() {
        return true;
    }


    @Override
    public float getArea() {
        return 0.001f;
    } // no energy without area, even for PLS ;)

    @Override
    public ImmutableVector3 getUnifDistrSample() {
        return point;
    }

    @Override
    public ImmutableVector3 getSpatialMinimum() {
        return point;
    }

    @Override
    public ImmutableVector3 getSpatialMaximum() {
        return point;
    }
}

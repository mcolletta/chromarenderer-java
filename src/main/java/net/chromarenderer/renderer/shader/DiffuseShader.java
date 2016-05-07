package net.chromarenderer.renderer.shader;

import net.chromarenderer.main.ChromaSettings;
import net.chromarenderer.math.COLORS;
import net.chromarenderer.math.Constants;
import net.chromarenderer.math.ImmutableVector3;
import net.chromarenderer.math.MutableVector3;
import net.chromarenderer.math.Vector3;
import net.chromarenderer.math.raytracing.CoordinateSystem;
import net.chromarenderer.math.raytracing.Hitpoint;
import net.chromarenderer.math.raytracing.Ray;
import net.chromarenderer.renderer.core.ChromaThreadContext;
import net.chromarenderer.renderer.scene.ChromaScene;
import net.chromarenderer.renderer.scene.Radiance;
import org.apache.commons.math3.util.FastMath;

/**
 * @author steinerb
 */
class DiffuseShader {

    static ChromaScene scene;


    static Radiance getDirectRadianceSample(Hitpoint hitpoint, float pathWeight, ChromaSettings settings) {

        if (settings.isDirectLightEstimationEnabled()) {
            return ptdl(hitpoint, pathWeight);
        } else {
            return pt(hitpoint, pathWeight);
        }
    }


    /**
     * Simple path tracing with uniform hemisphere samples to determine path.
     * Ls(ω) = ∫ Li * ρ(ωi, ωo) * cosθ dω
     * <p>
     * MC computation with one sample replaces ∫:
     * Ls(ω) = (Li * ρ(ωi, ωo) * cosθ) / p(ω)
     * <p>
     * where p(ω) = cos(θ)/π (sample weight for a uniform sample on the hemisphere - PT)
     * Ls(ω) = (Li * ρ(ωi, ωo)) * π
     * <p>
     * Why can I neglect *π? (missing in the code)
     * Assume Li = 1, Ls = 0,8: (80% get reflected)
     * 0,8 = ρ(ωi, ωo) * ∫ cosθ dω
     * 0,8 = ρ(ωi, ωo) * π
     * ρ(ωi, ωo) = 0,8 / π
     * <p>
     * For diffuse surfaces where p const, you can write:
     * Ls(ω) = (Li * ρ(ωi, ωo)/π) * π
     * Ls(ω) = Li * ρ(ωi, ωo)
     */
    private static Radiance pt(Hitpoint hitpoint, float pathWeight) {
        ImmutableVector3 direction = hitpoint.getUniformHemisphereSample();
        Ray ray = new Ray(hitpoint.getPoint(), direction);
        Hitpoint lightSourceSample = scene.intersect(ray);

        if (lightSourceSample.hit() && lightSourceSample.isOn(MaterialType.EMITTING)) {
            return new Radiance(lightSourceSample.getHitGeometry().getMaterial().getEmittance().mult(pathWeight), ray);
        } else {
            return new Radiance(COLORS.BLACK, ray);
        }
    }


    private static Radiance ptdl(Hitpoint hitpoint, float pathWeight) {
        ImmutableVector3 point = hitpoint.getPoint();
        Hitpoint lightSourceSample = scene.getLightSourceSample();
        ImmutableVector3 direction = lightSourceSample.getPoint().minus(point);

        float distToLight = direction.length();
        Ray shadowRay = new Ray(point, direction.normalize(), Constants.FLT_EPSILON, distToLight - Constants.FLT_EPSILON);

        float cosThetaSceneHit = direction.dot(hitpoint.getHitpointNormal());

        //lightSource hit from correct side?
        if (cosThetaSceneHit > 0.0f) {
            if (scene.isObstructed(shadowRay)) {
                return new Radiance(COLORS.BLACK, shadowRay);
            }
        } else {
            return new Radiance(COLORS.BLACK, shadowRay);
        }

        float geomTerm = (cosThetaSceneHit) / (distToLight * distToLight);
        Vector3 rhoDiffuse = hitpoint.getHitGeometry().getMaterial().getColor();
        float precisionBound = 10.0f / (rhoDiffuse.getMaxValue());      // bound can include brdf which can soften the geometric term
        Material emittingMaterial = lightSourceSample.getHitGeometry().getMaterial();
        Vector3 lightSourceEmission = emittingMaterial.getEmittance();
        Vector3 result = lightSourceEmission.mult(rhoDiffuse.div(Constants.PI_f).mult(FastMath.min(precisionBound, geomTerm)).mult(lightSourceSample.getInverseSampleWeight()));

        return new Radiance(result.mult(pathWeight), shadowRay);
    }


    static Ray getRecursiveRaySample(Hitpoint hitpoint) {
        float u = ChromaThreadContext.randomFloatClosedOpen();
        float v = ChromaThreadContext.randomFloatClosedOpen();
        float sqrtU = (float) FastMath.sqrt(u);
        float v2pi = v * Constants.TWO_PI_f;

        float sampleX = (float) FastMath.cos(v2pi) * sqrtU;
        float sampleY = (float) FastMath.sin(v2pi) * sqrtU;
        float sampleZ = (float) FastMath.sqrt(1.0f - u);
        CoordinateSystem coordinateSystem = hitpoint.getCoordinateSystem();

        Vector3 newDirection = new MutableVector3(coordinateSystem.getT1()).mult(sampleX)
                .plus(coordinateSystem.getT2().mult(sampleY))
                .plus(coordinateSystem.getN().mult(sampleZ)).normalize();

        return new Ray(hitpoint.getPoint(), new ImmutableVector3(newDirection));
    }
}

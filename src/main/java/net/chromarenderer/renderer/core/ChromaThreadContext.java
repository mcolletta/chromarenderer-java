package net.chromarenderer.renderer.core;

import net.chromarenderer.math.random.MersenneTwisterFast;

/**
 * @author steinerb
 */
public class ChromaThreadContext {

    private static ThreadLocal<Integer> currentX = new ThreadLocal<>() ;
    private static ThreadLocal<Integer> currentY = new ThreadLocal<>() ;
    private static ThreadLocal<MersenneTwisterFast> mt = new ThreadLocal<>();

    public static void init() {
        //TODO: remove static seed!
        mt.set(new MersenneTwisterFast(13499));
    }

    public static void setX(int x) {
        currentX.set(x);
    }

    public static Integer getX() {
        return currentX.get();
    }

    public static void setY(int y) {
        currentY.set(y);
    }

    public static Integer getY() {
        return currentY.get();
    }


    public static float randomFloat() {
        return mt.get().nextFloat();
    }
}
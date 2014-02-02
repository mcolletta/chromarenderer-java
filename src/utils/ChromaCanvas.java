package utils;

import net.chroma.math.MutableVector3;
import net.chroma.math.Vector3;

/**
 * @author steinerb
 */
public class ChromaCanvas {

    //TODO: Write post on JVM Array performance: one dimensional vs. two-dimensional: The latter one is 8 times slower ;)
    protected final Vector3[] pixels;
    protected final int width;
    protected final int height;


    public ChromaCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new Vector3[width * height];
        for (int i = 0; i < width * height; i++) {
            pixels[i] = new MutableVector3();
        }
        cleanCanvas();
    }


    public void cleanCanvas() {
        for (int i = 0; i < width * height; i++) {
            pixels[i].reset();
        }
    }


    public byte[] toByteImage() {
        int pixelCount = width * height;
        byte[] result = new byte[pixelCount * 3];
        for (int i = 0, j = 0; i < pixelCount; i+=1, j+=3) {
            result[j] = (byte) (pixels[i].getX());
            result[j + 1] = (byte) (pixels[i].getY());
            result[j + 2] = (byte) (pixels[i].getZ());
        }

        return result;
    }

    public Vector3[] getPixels() {
        return pixels;
    }
}
package net.chroma.renderer.cores;

import net.chroma.renderer.Renderer;

/**
 * @author steinerb
 */
public class WhittedRayTracer implements Renderer {

    @Override
    public void renderNextImage(int imgWidth, int imgHeight) {

    }

    @Override
    public boolean isContinuous() {
        return false;
    }

    @Override
    public byte[] get8BitRGBSnapshot() {
        return new byte[0];
    }
}

package net.chromarenderer.main;

import net.chromarenderer.renderer.ChromaRenderMode;

/**
 * @author steinerb
 */
public class ChromaSettings {

    private final int threadCount;
    private final int imgWidth;
    private final int imgHeight;
    private final ChromaRenderMode mode;
    private final boolean forceContinuousRender;
    private final int maxRayDepth = 3;


    public ChromaSettings(int imgWidth, int imgHeight, ChromaRenderMode mode, boolean forceContinuousRender, int threadCount) {
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.mode = mode;
        this.forceContinuousRender = forceContinuousRender;
        this.threadCount = threadCount;
    }


    public ChromaSettings(ChromaSettings settings) {
        this.imgWidth = settings.getImgWidth();
        this.imgHeight = settings.getImgHeight();
        this.mode = settings.getMode();
        this.forceContinuousRender = settings.isForceContinuousRender();
        this.threadCount = settings.threadCount;
    }


    public int getImgWidth() {
        return imgWidth;
    }


    public int getImgHeight() {
        return imgHeight;
    }


    public ChromaRenderMode getMode() {
        return mode;
    }


    public boolean isForceContinuousRender() {
        return forceContinuousRender;
    }


    public ChromaSettings changeMode(ChromaRenderMode mode) {
        return new ChromaSettings(imgWidth, imgHeight, mode, forceContinuousRender, threadCount);
    }


    public ChromaSettings changeContinuousRender(boolean isContinuousRender) {
        return new ChromaSettings(imgWidth, imgHeight, mode, isContinuousRender, threadCount);

    }

    public int getMaxRayDepth() {
        return maxRayDepth;
    }


    public int getThreadCount() {
        return threadCount;
    }
}

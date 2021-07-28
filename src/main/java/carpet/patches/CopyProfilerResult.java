package carpet.patches;

import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.ProfilerTiming;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class CopyProfilerResult implements ProfileResult {
    int startI, endI;
    long startL, endL;
    public CopyProfilerResult(int startI, long startL, int endI, long endL)
    {
        this.startI = startI;
        this.startL = startL;
        this.endI = endI;
        this.endL = endL;
    }
    @Override
    public List<ProfilerTiming> getTimings(String parentPath) {
        return Collections.emptyList();
    }

    @Override
    public boolean save(Path path) {
        return false;
    }

    @Override
    public long getStartTime() {
        return startL;
    }

    @Override
    public int getStartTick() {
        return startI;
    }

    @Override
    public long getEndTime() {
        return endL;
    }

    @Override
    public int getEndTick() {
        return endI;
    }

    @Override
    public String getRootTimings() {
        return "";
    }
}

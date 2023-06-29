package carpet.patches;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;

public class CopyProfilerResult implements ProfileResults {
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
    public List<ResultField> getTimes(String parentPath) {
        return Collections.emptyList();
    }

    @Override
    public boolean saveResults(Path path) {
        return false;
    }

    @Override
    public long getStartTimeNano() {
        return startL;
    }

    @Override
    public int getStartTimeTicks() {
        return startI;
    }

    @Override
    public long getEndTimeNano() {
        return endL;
    }

    @Override
    public int getEndTimeTicks() {
        return endI;
    }

    @Override
    public String getProfilerResults() {
        return "";
    }
}

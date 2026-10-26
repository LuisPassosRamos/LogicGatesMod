package sisso.logicsim.classroom.learning;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Optional server configuration. Absence uses conservative defaults; invalid values fail closed. */
public record LearningLimits(int width,int height,int depth,int contentBytes,long classroomBytes,int queue,long selectionMillis) {
    public static final LearningLimits DEFAULT=new LearningLimits(32,16,32,8*1024*1024,1024L*1024*1024,8,15*60*1000L);
    public LearningLimits {
        if(width<1 || height<1 || depth<1 || width>64 || height>64 || depth>64
                || (long)width*height*depth>262144 || contentBytes<1024 || contentBytes>32*1024*1024
                || classroomBytes<contentBytes || queue<1 || queue>32 || selectionMillis<1000 || selectionMillis>3600000)
            throw new IllegalArgumentException("Invalid Classroom learning limits");
    }
    public static LearningLimits load(Path file) throws IOException {
        if(!Files.exists(file)) return DEFAULT;
        var p=new Properties(); try(var in=Files.newInputStream(file)) { p.load(in); }
        try { return new LearningLimits(Math.toIntExact(value(p,"maxWidth",32)),Math.toIntExact(value(p,"maxHeight",16)),Math.toIntExact(value(p,"maxDepth",32)),
                Math.toIntExact(value(p,"maxContentBytes",DEFAULT.contentBytes)),value(p,"maxClassroomBytes",DEFAULT.classroomBytes),
                Math.toIntExact(value(p,"queueCapacity",8)),value(p,"selectionMillis",DEFAULT.selectionMillis)); }
        catch(RuntimeException e) { throw new IOException("Invalid Classroom learning configuration",e); }
    }
    private static long value(Properties p,String key,long fallback) { return Long.parseLong(p.getProperty(key,Long.toString(fallback))); }
}

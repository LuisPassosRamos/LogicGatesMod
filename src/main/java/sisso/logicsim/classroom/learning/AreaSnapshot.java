package sisso.logicsim.classroom.learning;

import java.util.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;

/** Portable asset names and intent, never runtime asset indices or live world references. */
public record AreaSnapshot(int format,int width,int height,int depth,List<Block> blocks) {
    public record Block(int x,int y,int z,String asset,int rotation,int filler,Boolean leverOn) { }
    public AreaSnapshot { blocks=List.copyOf(blocks); }
    public void validate(LearningLimits limits) {
        if(format!=1 || width<1 || height<1 || depth<1 || width>limits.width() || height>limits.height() || depth>limits.depth()
                || blocks.size()>(long)width*height*depth) throw new LearningException(AREA_LIMIT);
        if(blocks.isEmpty()) throw new LearningException(AREA_EMPTY);
        var seen=new HashSet<Long>();
        for(var b:blocks) {
            if(b.x<0 || b.x>=width || b.y<0 || b.y>=height || b.z<0 || b.z>=depth || b.asset==null
                    || b.asset.isBlank() || b.asset.length()>256 || b.rotation<0 || b.rotation>255 || b.filler<0
                    || !seen.add(((long)b.x*height+b.y)*depth+b.z)) throw new LearningException(CORRUPT_CONTENT);
        }
    }
}

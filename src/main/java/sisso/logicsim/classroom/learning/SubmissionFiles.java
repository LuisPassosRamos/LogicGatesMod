package sisso.logicsim.classroom.learning;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;

/** Private immutable content; atomic file publication precedes the metadata transaction. */
public final class SubmissionFiles {
    public record Content(UUID id,String checksum,long bytes,AreaSnapshot snapshot) { }
    private final Path root;
    private final LearningLimits limits;
    private final Gson gson=new Gson();
    public SubmissionFiles(Path root,LearningLimits limits) { this.root=root.toAbsolutePath().normalize(); this.limits=limits; }
    private Path file(UUID id) { return root.resolve(id+".json.gz"); }
    public Content write(AreaSnapshot snapshot) {
        snapshot.validate(limits);
        byte[] json=gson.toJson(snapshot).getBytes(StandardCharsets.UTF_8);
        if(json.length>limits.contentBytes()) throw new LearningException(AREA_LIMIT);
        UUID id=UUID.randomUUID(); Path target=file(id),temp=root.resolve(id+".tmp");
        try {
            Files.createDirectories(root);
            try(var out=new GZIPOutputStream(Files.newOutputStream(temp,StandardOpenOption.CREATE_NEW))) { out.write(json); }
            try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)) { channel.force(true); }
            byte[] bytes=Files.readAllBytes(temp);
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
            return new Content(id,hash(bytes),bytes.length,snapshot);
        } catch(Exception e) {
            try { Files.deleteIfExists(temp); } catch(IOException cleanup) { e.addSuppressed(cleanup); }
            throw new LearningException(CORRUPT_CONTENT,e);
        }
    }
    public AreaSnapshot read(UUID id,String checksum) {
        try {
            Path path=file(id);
            if(Files.size(path)>limits.contentBytes()+65536L || !Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid content size/type");
            byte[] bytes=Files.readAllBytes(path);
            if(!hash(bytes).equals(checksum)) throw new IOException("Content checksum mismatch");
            byte[] json;
            try(var in=new GZIPInputStream(new ByteArrayInputStream(bytes))) { json=in.readNBytes(limits.contentBytes()+1); }
            if(json.length>limits.contentBytes()) throw new IOException("Content exceeds expansion limit");
            var snapshot=gson.fromJson(new String(json,StandardCharsets.UTF_8),AreaSnapshot.class);
            snapshot.validate(limits); return snapshot;
        } catch(Exception e) { throw new LearningException(CORRUPT_CONTENT,e); }
    }
    /** Only the uncommitted content created by the caller may be removed through this method. */
    public void discardUncommitted(UUID id) { try { Files.deleteIfExists(file(id)); } catch(IOException e) { throw new LearningException(CORRUPT_CONTENT,e); } }
    /** Startup only, before accepting requests. Unknown names and directories are never touched. */
    public void recover(Set<UUID> referenced) {
        if(!Files.exists(root))return;
        try(var entries=Files.list(root)) {
            for(var path:entries.toList()) {
                if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))continue;
                String name=path.getFileName().toString();
                if(!name.matches("[a-f0-9-]{36}\\.(?:tmp|json\\.gz)"))continue;
                UUID id;try{id=UUID.fromString(name.substring(0,36));}catch(IllegalArgumentException unknown){continue;}
                if(name.endsWith(".tmp") || !referenced.contains(id))Files.delete(path);
            }
        } catch(IOException failure) { throw new LearningException(CORRUPT_CONTENT,failure); }
    }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
}

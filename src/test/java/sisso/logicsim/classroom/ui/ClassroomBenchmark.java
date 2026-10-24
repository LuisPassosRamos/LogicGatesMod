package sisso.logicsim.classroom.ui;

import com.google.gson.GsonBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.ClassroomUiFixture;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.util.BuildInfo;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.lang.reflect.*;

/** Isolated server/transport measurements, not real players, client frames or world tick latency. */
public final class ClassroomBenchmark {
    private static final int WARMUP=10, SAMPLES=25;
    private static Map<String,Object> measure(String scenario,int size,Runnable work) {
        for(int i=0;i<WARMUP;i++) work.run();
        long[] nanos=new long[SAMPLES];
        for(int i=0;i<SAMPLES;i++) { long start=System.nanoTime(); work.run(); nanos[i]=System.nanoTime()-start; }
        Arrays.sort(nanos);
        var row=new LinkedHashMap<String,Object>(); row.put("scenario",scenario); row.put("size",size);
        row.put("samples",SAMPLES); row.put("medianMs",nanos[SAMPLES/2]/1e6); row.put("p95Ms",nanos[(int)Math.ceil(SAMPLES*.95)-1]/1e6);
        return row;
    }
    private static final class Transport {
        private final Object cache;
        private final Method packet;
        Transport() throws Exception {
            // Allows the same harness to measure the archived rc.7 classes before the optimization.
            Class<?> type;
            try { type=Class.forName("sisso.logicsim.classroom.ui.ClassroomPageUpdates"); }
            catch(ClassNotFoundException oldVersion) { cache=null; packet=null; return; }
            var constructor=type.getDeclaredConstructor(); constructor.setAccessible(true); cache=constructor.newInstance();
            packet=type.getDeclaredMethod("packet",String.class,CustomPageLifetime.class,UICommandBuilder.class,UIEventBuilder.class,List.class);
            packet.setAccessible(true);
        }
        CustomPage packet(UICommandBuilder commands,UIEventBuilder events,List<?> rows) {
            if(cache==null) return new CustomPage(ClassroomPage.class.getName(),false,false,CustomPageLifetime.CanDismiss,commands.getCommands(),events.getEvents());
            try { return (CustomPage)packet.invoke(cache,ClassroomPage.class.getName(),CustomPageLifetime.CanDismiss,commands,events,rows); }
            catch(ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
    }
    public static void main(String[] args) throws Exception {
        try { run(args); }
        catch(Throwable failure) { failure.printStackTrace(System.err); System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        Path output=Path.of(args[0]).toAbsolutePath(); Files.createDirectories(output.getParent());
        Path database=output.getParent().resolve("classroom-benchmark-data").resolve(UUID.randomUUID().toString());
        UUID teacher=UUID.randomUUID(), student=UUID.randomUUID();
        ClassroomUiFixture.create(database,teacher,student);
        var rows=new ArrayList<Map<String,Object>>(); var failures=new CopyOnWriteArrayList<Throwable>();
        var allowed=new AtomicBoolean(true); var checks=new AtomicLong();
        try(var runtime=new ClassroomRuntime(database,id->{checks.incrementAndGet(); return allowed.get();},failures::add)) {
            long start=System.nanoTime(); runtime.home(teacher).join(); double initializationMs=(System.nanoTime()-start)/1e6;
            var cold=new LinkedHashMap<String,Object>(); cold.put("scenario","first-query-after-ready");
            for(Kind kind:List.of(Kind.MEMBERS,Kind.INTAKES,Kind.PLAYERS,Kind.SETTINGS)) {
                start=System.nanoTime(); runtime.screen(teacher,kind,"",0).join(); cold.put(kind.name()+"Ms",(System.nanoTime()-start)/1e6);
                rows.add(measure("query-"+kind.name(),25,()->runtime.screen(teacher,kind,"",0).join()));
            }
            cold.put("runtimeInitializationAndHomeMs",initializationMs); rows.addFirst(cold);
            for(int count:new int[]{25,100,250}) {
                var waits=new long[count]; var futures=new ArrayList<CompletableFuture<?>>(); var rejected=new AtomicInteger();
                start=System.nanoTime();
                for(int i=0;i<count;i++) {
                    int index=i; long submitted=System.nanoTime();
                    futures.add(runtime.screen(teacher,Kind.MEMBERS,"",0).whenComplete((screen,error)->{
                        waits[index]=System.nanoTime()-submitted; if(error!=null) rejected.incrementAndGet();
                    }).exceptionally(error->null));
                }
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join(); Arrays.sort(waits);
                var row=new LinkedHashMap<String,Object>(); row.put("scenario","simultaneous-screen-requests"); row.put("size",count);
                row.put("totalMs",(System.nanoTime()-start)/1e6); row.put("p95Ms",waits[(int)Math.ceil(count*.95)-1]/1e6);
                row.put("failures",rejected.get()); rows.add(row);
            }
            var screen=runtime.screen(teacher,Kind.MEMBERS,"",0).join(); var page=new ClassroomPage(null,null,null);
            page.model.apply(screen,true); var transport=new Transport(); var packet=new AtomicReference<CustomPage>(); var toggle=new AtomicInteger();
            for(boolean selection:new boolean[]{false,true}) {
                var row=measure(selection?"member-selection":"unchanged-member-refresh",20,()->{
                    if(selection) page.model.select(ClassroomPageModel.id(screen.rows().items().get(toggle.getAndIncrement()%2)).toString());
                    var c=new UICommandBuilder(); var e=new UIEventBuilder(); page.render(c,e,false);
                    packet.set(transport.packet(c,e,screen.rows().items()));
                });
                row.put("commands",packet.get().commands.length); row.put("bindings",packet.get().eventBindings.length);
                row.put("packetBytes",packet.get().computeSize()); rows.add(row);
            }
            for(int count:new int[]{10,100,500,1000}) {
                var subscriptions=new ArrayList<AutoCloseable>(); var callbacks=new AtomicLong(); UUID target=null;
                for(int i=0;i<count;i++) { UUID owner=UUID.randomUUID(); target=owner; subscriptions.add(runtime.listenAuthorization(owner,value->callbacks.incrementAndGet())); }
                UUID actor=target;
                for(String scenario:List.of("individual-permission-change","unchanged-group-permission","changed-group-permission")) {
                    Runnable action=()->{
                        if(!scenario.startsWith("unchanged")) allowed.set(!allowed.get());
                        if(scenario.startsWith("individual")) runtime.authorizationChanged(actor); else runtime.authorizationChangedForOpenPages();
                    };
                    for(int i=0;i<WARMUP;i++) action.run();
                    long beforeChecks=checks.get(), beforeCallbacks=callbacks.get();
                    var row=measure(scenario,count,action);
                    row.put("permissionChecksPerEvent",(checks.get()-beforeChecks)/(WARMUP+SAMPLES));
                    row.put("callbacksPerEvent",(callbacks.get()-beforeCallbacks)/(WARMUP+SAMPLES)); rows.add(row);
                }
                for(var subscription:subscriptions) subscription.close();
            }
            Method filtered;
            try { filtered=ClassroomRuntime.class.getMethod("listen",UUID.class,Predicate.class,Consumer.class); }
            catch(NoSuchMethodException oldVersion) { filtered=null; }
            for(int count:new int[]{10,100,500,1000}) {
                var callbacks=new AtomicLong(); var subscriptions=new ArrayList<AutoCloseable>(); UUID actor=UUID.randomUUID();
                for(int i=0;i<count;i++) {
                    UUID owner=i==0?actor:UUID.randomUUID(); Consumer<ClassroomRuntime.Change> callback=change->callbacks.incrementAndGet();
                    subscriptions.add(filtered==null ? runtime.listen(owner,callback)
                            : (AutoCloseable)filtered.invoke(runtime,owner,(Predicate<ClassroomRuntime.Change>)change->owner.equals(change.player()),callback));
                }
                var row=measure("unrelated-page-data-change",count,()->runtime.remember(actor,"Benchmark player").join());
                long before=callbacks.get(); runtime.remember(actor,"Benchmark player").join();
                row.put("callbacksPerChange",callbacks.get()-before); rows.add(row);
                for(var subscription:subscriptions) subscription.close();
            }
            if(!failures.isEmpty()) throw new AssertionError(failures);
            var result=new LinkedHashMap<String,Object>(); result.put("buildId",BuildInfo.BUILD_ID); result.put("at",Instant.now().toString());
            result.put("java",System.getProperty("java.version")); result.put("os",System.getProperty("os.name"));
            result.put("transport",transport.cache==null?"rc7-full":"incremental");
            result.put("scope","synthetic subscriptions, isolated SQLite with 25 members, server builders and packet size; excludes client, network and world ticks");
            result.put("cases",rows);
            Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");
            System.out.println(output);
            for(var row:rows) System.out.println(row);
        }
    }
}

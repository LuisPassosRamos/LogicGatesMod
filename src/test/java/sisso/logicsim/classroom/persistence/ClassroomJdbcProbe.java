package sisso.logicsim.classroom.persistence;

import com.google.gson.GsonBuilder;
import org.sqlite.JDBC;
import sisso.logicsim.classroom.ClassroomUiFixture;
import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.domain.ClassroomViews.Kind;
import sisso.logicsim.util.BuildInfo;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/** Test-only JDBC observation, also runnable against the preserved rc.9 JAR. No production counters or injection hooks. */
public final class ClassroomJdbcProbe implements AutoCloseable {
    @FunctionalInterface interface SqlHook { void accept(String sql) throws SQLException; }
    private final Driver delegate=new JDBC();
    private final List<Driver> originals=new ArrayList<>();
    final AtomicInteger connections=new AtomicInteger(),closed=new AtomicInteger(),selects=new AtomicInteger();
    final AtomicInteger begins=new AtomicInteger(),commits=new AtomicInteger(),rollbacks=new AtomicInteger(),active=new AtomicInteger();
    volatile SqlHook before=sql->{};
    volatile Runnable afterSelect=()->{};
    private final Driver observer=new Driver() {
        public Connection connect(String url,Properties properties) throws SQLException {
            if (!acceptsURL(url)) return null;
            var connection=delegate.connect(url,properties); connections.incrementAndGet(); active.incrementAndGet();
            return wrap(connection,Connection.class,null);
        }
        public boolean acceptsURL(String url) throws SQLException { return delegate.acceptsURL(url); }
        public DriverPropertyInfo[] getPropertyInfo(String url,Properties properties) throws SQLException { return delegate.getPropertyInfo(url,properties); }
        public int getMajorVersion() { return delegate.getMajorVersion(); }
        public int getMinorVersion() { return delegate.getMinorVersion(); }
        public boolean jdbcCompliant() { return delegate.jdbcCompliant(); }
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
    };
    public ClassroomJdbcProbe() throws SQLException {
        for(var driver:Collections.list(DriverManager.getDrivers())) if(driver.acceptsURL("jdbc:sqlite::memory:")) {
            originals.add(driver); DriverManager.deregisterDriver(driver);
        }
        DriverManager.registerDriver(observer);
    }
    void reset() {
        if(active.get()!=0) throw new AssertionError("Cannot reset with open connections");
        for(var count:List.of(connections,closed,selects,begins,commits,rollbacks)) count.set(0);
    }
    Map<String,Integer> counts() {
        return Map.of("connections",connections.get(),"closed",closed.get(),"selects",selects.get(),
                "begins",begins.get(),"commits",commits.get(),"rollbacks",rollbacks.get(),"active",active.get());
    }
    @SuppressWarnings("unchecked")
    private <T> T wrap(T target,Class<T> api,String prepared) {
        boolean[] finished={false}, selected={false};
        return (T)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{api},(proxy,method,args)->{
            String name=method.getName();
            if(name.startsWith("execute")) {
                String sql=(prepared!=null?prepared:args!=null && args.length>0 && args[0] instanceof String value?value:"").stripLeading();
                before.accept(sql);
                if(sql.startsWith("SELECT")) { selects.incrementAndGet(); selected[0]=true; }
                if(sql.startsWith("BEGIN")) begins.incrementAndGet();
                if(sql.equals("COMMIT")) commits.incrementAndGet();
                if(sql.equals("ROLLBACK")) rollbacks.incrementAndGet();
            }
            Object result;
            try { result=method.invoke(target,args); }
            catch(InvocationTargetException failure) { throw failure.getCause(); }
            if(target instanceof Connection) {
                if(name.equals("prepareStatement")) return wrap((PreparedStatement)result,PreparedStatement.class,(String)args[0]);
                if(name.equals("createStatement")) return wrap((Statement)result,Statement.class,null);
                if(name.equals("close") && !finished[0]) { finished[0]=true; closed.incrementAndGet(); active.decrementAndGet(); }
            } else if(name.equals("close") && selected[0]) { selected[0]=false; afterSelect.run(); }
            return result;
        });
    }
    @Override public void close() throws SQLException {
        DriverManager.deregisterDriver(observer);
        for(var driver:originals) DriverManager.registerDriver(driver);
        if(active.get()!=0) throw new AssertionError("JDBC connections leaked: "+active.get());
    }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]).toAbsolutePath(); Files.createDirectories(output.getParent());
        Path data=output.getParent().resolve("read-probe-data").resolve(UUID.randomUUID().toString());
        UUID teacher=UUID.randomUUID(),student=UUID.randomUUID(),observer=UUID.randomUUID();
        var cases=new LinkedHashMap<String,Map<String,Integer>>();
        try(var probe=new ClassroomJdbcProbe()) {
            ClassroomUiFixture.create(data,teacher,student);
            var service=new ClassroomService(new SqliteClassroomRepository(data.resolve("classroom.db")),id->true);
            probe.reset(); service.home(teacher); cases.put("home-teacher",probe.counts());
            probe.reset(); service.home(student); cases.put("home-student",probe.counts());
            for(var kind:List.of(Kind.MEMBERS,Kind.INTAKES,Kind.PLAYERS,Kind.SETTINGS)) {
                probe.reset(); service.screen(teacher,kind,"",0); cases.put("teacher-"+kind,probe.counts());
            }
            probe.reset(); service.screen(student,Kind.DETAIL,"",0); cases.put("student-DETAIL",probe.counts());
            for(var kind:List.of(Kind.BROWSE,Kind.INTAKES,Kind.SETTINGS)) {
                probe.reset(); service.screen(observer,kind,"",0); cases.put("unlinked-"+kind,probe.counts());
            }
        }
        var report=Map.of("buildId",BuildInfo.BUILD_ID,"cases",cases,"scope","test-only JDBC counts; fixture with 25 members; no client or real player data");
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report)+System.lineSeparator());
        System.out.println(output);
    }
}

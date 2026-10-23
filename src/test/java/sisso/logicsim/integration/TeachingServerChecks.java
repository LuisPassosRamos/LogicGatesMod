package sisso.logicsim.integration;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.diagnostics.GateInspection;
import sisso.logicsim.diagnostics.GateInspector;
import sisso.logicsim.interactions.ToggleLogicLeverInteraction;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Exercises native parser/permissions/translations and snapshot inspection without a player client. */
final class TeachingServerChecks {
    private static final BlockPosition GATE = new BlockPosition(2,64,20);
    private final List<String> passed;
    private Map<String,Object> measurements = Map.of();
    TeachingServerChecks(List<String> passed) { this.passed=passed; }
    Map<String,Object> measurements() { return measurements; }

    CompletableFuture<Void> run(World world, boolean create) {
        if(create) {
            for(var position:List.of(GATE,new BlockPosition(2,64,19),new BlockPosition(2,64,21))) {
                place(world,new BlockPosition(position.x(),63,position.z()),"Rock_Stone");
            }
            place(world,GATE,LogicBlockTypes.AND_ID);
            place(world,new BlockPosition(2,64,19),LogicBlockTypes.LEVER_ID);
            ToggleLogicLeverInteraction.toggle(world,new BlockPosition(2,64,19));
            place(world,new BlockPosition(2,64,21),LogicBlockTypes.CABLE_ID);
            EnergyPropagationSystem.recalculate(world);
            var before=inspect(world);
            check(!before.outputOn() && before.inputs().get(1).state()==GateInspection.InputState.OFF,"connected B=0 is visible to inspection");
            place(world,new BlockPosition(2,64,21),"Empty");
            EnergyPropagationSystem.recalculate(world);
            passed.add("gate inspector distinguishes connected input 0 from removed input and follows automatic output");
        }
        check(inspect(world).outputOn(),"snapshot restores AND with only A=1, including after restart");
        var i18n=I18nModule.get();
        check("Porta Lógica XOR".equals(i18n.getMessage("pt-BR","server.items.logic_xor_gate.name")),"Portuguese item translation loaded");
        check(i18n.getMessage("pt-BR","server.logicgates.help.optional").contains("Entrada vazia"),"Portuguese teaching translation loaded");
        check(i18n.getMessage("en-US","server.logicgates.help.optional").contains("Empty inputs"),"English teaching translation loaded");
        passed.add("native I18n loads matching English and Brazilian Portuguese teaching/item messages");
        var state=world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
        var timing=state.metrics.summary();
        check(timing.samples()>0 && timing.lastMs()>=timing.solverMs(),"native timing captures complete evaluation and solver");
        measurements=Map.of("nodes",state.nodeCount,"evaluationSamples",timing.samples(),"lastEvaluationMs",timing.lastMs(),
                "meanEvaluationMs",timing.meanMs(),"p95EvaluationMs",timing.p95Ms(),"monitoringMeanMs",timing.monitoringMeanMs());
        passed.add("native monitoring and evaluation timings are recorded with a bounded window");
        var sender=new Receiver(true);
        var commands=CommandManager.get();
        return commands.handleCommand(sender,"logichelp --gate=XOR --inputs=3").thenCompose(ignored->{
            check(sender.count("server.logicgates.help.row")==8,"native help parser produces eight truth-table rows");
            return commands.handleCommand(sender,"logicinspect 2 64 20 --world="+world.getName());
        }).thenCompose(ignored->{
            check(sender.count("server.logicgates.inspect.header")==1,"native inspection parser chooses requested world");
            check(sender.count("server.logicgates.inspect.input")==3,"native inspection reports A/B/C");
            return commands.handleCommand(sender,"logicenergy");
        }).thenCompose(ignored->{
            check(sender.count("server.logicgates.energy.timing")>0,"native diagnostic contains measured timings");
            passed.add("native commands parse help/inspection arguments and publish translated diagnostics");
            var denied=new Receiver(false);
            return commands.handleCommand(denied,"logicinspect 2 64 20 --world="+world.getName()).thenRun(()->{
                check(denied.count("server.logicgates.inspect.header")==0,"inspection requires its permission");
                passed.add("native inspection permission prevents unauthorized gate inspection");
            });
        });
    }

    private GateInspection inspect(World world) {
        var state=world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
        return GateInspector.inspect(state.lastSnapshot,GATE).orElseThrow();
    }
    private void place(World world,BlockPosition p,String id) {
        var type=BlockType.getAssetMap().getAsset(id);
        BlockOperations.setBlock(world.getChunkStore(),BlockComponentAccess.sectionIfLoaded(world,p),p.x(),p.y(),p.z(),
                BlockType.getAssetMap().getIndex(id),type,RotationTuple.NONE_INDEX,0,0);
    }
    private void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }

    private static final class Receiver implements CommandSender {
        private final boolean allowed;
        private final List<Message> messages=Collections.synchronizedList(new ArrayList<>());
        Receiver(boolean allowed) { this.allowed=allowed; }
        public String getUsername() { return "LogicGatesIntegration"; }
        public UUID getUuid() { return new UUID(0,0); }
        public boolean hasPermission(String permission) { return allowed; }
        public boolean hasPermission(String permission,boolean fallback) { return allowed; }
        public void sendMessage(Message message) { messages.add(message); }
        long count(String key) { synchronized(messages) { return messages.stream().filter(m->key.equals(m.getMessageId())).count(); } }
    }
}

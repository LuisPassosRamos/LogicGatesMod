package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.CircuitNode.Kind;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VerificationRulesTest {
    @Test void quantitiesAreExactOptionalAndImmutableAndRoundTripWithoutLosingZero(){
        assertNull(VerificationRules.quantity(" "));assertEquals(0,VerificationRules.quantity("0"));assertEquals(12,VerificationRules.quantity("0012"));
        for(String invalid:List.of("-1","1.5","1e2","16385","100000","garbage"))assertThrows(LearningException.class,()->VerificationRules.quantity(invalid));
        var mutable=new EnumMap<Kind,Integer>(Kind.class);mutable.put(Kind.AND,0);
        var rules=new VerificationRules(mutable,SubmissionVerifierTest.AND);mutable.put(Kind.AND,3);
        assertEquals(0,rules.quantities().get(Kind.AND));assertThrows(UnsupportedOperationException.class,()->rules.quantities().clear());
        assertEquals(rules,VerificationRules.decode(rules.encode()));assertEquals(VerificationRules.EMPTY,VerificationRules.decode(""));
    }
    @Test void malformedOrIncompleteTablesStayPrivateAndPublicationValidationIsStrict(){
        for(String invalid:List.of("0 -> 1","0 -> 1\n0 -> 0","0 -> 1\n1 -> xx","00 -> 0\n1 -> 1","0000000 -> 0")){
            var rules=new VerificationRules(Map.of(),invalid);assertEquals(invalid,rules.table());
            assertThrows(LearningException.class,rules::cases);
        }
        assertThrows(LearningException.class,()->new VerificationRules(Map.of(Kind.LEVER,1),SubmissionVerifierTest.AND).cases());
        assertThrows(LearningException.class,()->new VerificationRules(Map.of(Kind.LAMP,2),SubmissionVerifierTest.AND).cases());
        assertEquals(4,new VerificationRules(Map.of()," 11 -> 1\n10 -> 0\n01 -> 0\n00 -> 0 ").cases().size());
        assertThrows(LearningException.class,()->VerificationRules.decode("{broken"));
        assertThrows(LearningException.class,()->VerificationRules.decode("{\"format\":99}"));
    }
    @Test void maximumTableHasSixtyFourCasesAndFourOutputs(){
        var table=new StringBuilder();for(int i=0;i<64;i++)table.append(String.format("%6s",Integer.toBinaryString(i)).replace(' ','0')).append(" -> 0000\n");
        assertEquals(64,new VerificationRules(Map.of(Kind.LEVER,6,Kind.LAMP,4),table.toString()).cases().size());
    }
}

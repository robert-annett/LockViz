package lockvis.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestSimpleGraphExtractor {

    @Test
    /* Two threads locking and waiting on a mutex with a third, independent thread. 
     * ThreadA --> mutex <-- ThreadB,  ThreadC
     */     
    public void testExtractSimpleGraphs() {
        MutexFactory factory = new MutexFactory();
        List<ThreadInfo> all = new ArrayList<ThreadInfo>();
        
        ThreadInfo a = new ThreadInfo("\"Thread A\"");
        ThreadInfo b = new ThreadInfo("\"Thread B\"");
        ThreadInfo c = new ThreadInfo("\"Thread C\"");
        
        ThreadLocation abl = new ThreadLocation("A", a); 
        a.addLocation(abl);
        Mutex muAB = factory.getMutex("AB", "type", "waiting on");
        MutexAction ma = new MutexAction(a, abl, muAB, "state");
        muAB.addReference(ma);
        abl.addMutexAction(ma);
        a.addMutexAction(ma);
        
        ThreadLocation bal = new ThreadLocation("B", b);
        b.addLocation(bal);
        MutexAction ma1 = new MutexAction(b, bal, muAB, "state");
        bal.addMutexAction(ma1);
        b.addMutexAction(ma1);
        
        all.add(a);
        all.add(b);
        all.add(c);
        
        
        SimpleGraphExtractor sge = new SimpleGraphExtractor();
        List<ThreadInfoSet> extractSimpleGraphs = sge.extractSimpleGraphs(all);
        
        Assert.assertEquals("There should be two entanglements", 2, extractSimpleGraphs.size());
        ThreadInfoSet firstSet = extractSimpleGraphs.get(0);
        Assert.assertEquals("The first should have A and B", 2, firstSet.size());
        Assert.assertTrue("The first should have A and B", firstSet.contains(a));
        Assert.assertTrue("The first should have A and B", firstSet.contains(b));
        ThreadInfoSet secondSet = extractSimpleGraphs.get(1);
        Assert.assertTrue("The second should have A and B", secondSet.contains(c));
       
    }

}

/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.routing.mta.comparison;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.opentripplanner.routing.mta.comparison.GenerateTestODPairsFromRunningInstance;

import java.io.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunnerIT {

    @Test
    public void a_ODGen_QA_NoAccessible() throws IOException, Exception {    	
		
    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/index/stops?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t.setMTAOnly(true);

    	t.setPairsToGenerate(100);
    	t.setAccessibilityPercent(0.0);
    	t.setMax("MTA", 25);
    	t.setMax("MTASBWY", 25);
    	t.setMax("LI", 25);
    	t.setMax("MNR", 25);
    	
    	t.setOutputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t.run();
    }
    
    @Test    
    public void b_ODGen_QA_Accessible() throws IOException, Exception {    	

    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/index/stops?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t.setMTAOnly(true);

    	t.setPairsToGenerate(100);
    	t.setAccessibilityPercent(1.0);
    	t.setMax("MTA", 75);
    	t.setMax("MTASBWY", 75);
    	t.setMax("LI", 75);
    	t.setMax("MNR", 75);
    	
    	t.setOutputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t.run();
    }
    
    @Test  	
    public void c_Baseline_QA_NoAccessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/plan?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t2.setInputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_qa_0_accessible.txt");
    	t2.run();    	
    }
    
    @Test
    public void d_Baseline_QA_Accessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/plan?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t2.setInputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_qa_100_accessible.txt");
    	t2.run();    	
    }
    
 	
    @Test
    public void e_Baseline_Prod_NoAccessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-prod.camsys-apps.com/otp/routers/default/plan?apikey=Z276E3rCeTzOQEoBPPN4JCEc6GfvdnYE");
    	t2.setInputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_prod_0_accessible.txt");
    	t2.run();    	
    }

    @Test
    public void f_Baseline_Prod_Accessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-prod.camsys-apps.com/otp/routers/default/plan?apikey=Z276E3rCeTzOQEoBPPN4JCEc6GfvdnYE");
    	t2.setInputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_prod_100_accessible.txt");
    	t2.run();    	
    }
    
    @Test
    public void g_Test_Dev_NoAccessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t2.setInputFile("src/test/resources/mta/test_pairs_0_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_dev_0_accessible.txt");
    	t2.run();    	
    }
    
    
    @Test
    public void h_Test_Dev_Accessible() throws IOException, Exception {    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t2.setInputFile("src/test/resources/mta/test_pairs_100_accessible.txt");
    	t2.setOutputFile("src/test/resources/mta/test_dev_100_accessible.txt");
    	t2.run();    	
    }
        
    @Test
    public void k1_QAXProd_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                    PROD   vs   QA");
    	System.out.println("             ***************************************************************");

    	System.out.println("");
    	System.out.println("NO ACCESSIBLE QUERIES");
    	System.out.println("");

      	CompareODResults t3 = new CompareODResults();
    	t3.setBaselineResultsFile("src/test/resources/mta/test_prod_0_accessible.txt");
    	t3.setTestResultsFile("src/test/resources/mta/test_qa_0_accessible.txt");
    	t3.run();

    }
    
    @Test
    public void k2_QAXProd_Results() throws IOException, Exception {    	

    	System.out.println("");
    	System.out.println("ACCESSIBLE QUERIES");
    	System.out.println("");

      	CompareODResults t4 = new CompareODResults();
    	t4.setBaselineResultsFile("src/test/resources/mta/test_prod_100_accessible.txt");
    	t4.setTestResultsFile("src/test/resources/mta/test_qa_100_accessible.txt");
    	t4.run();
    }    	
    	
    
    @Test
    public void l1_DevXQA_Results() throws IOException, Exception {    	
    	System.out.println("             ***************************************************************");
    	System.out.println("                                    QA   vs   DEV");
    	System.out.println("             ***************************************************************");

    	System.out.println("");
    	System.out.println("NO ACCESSIBLE QUERIES");
    	System.out.println("");


      	CompareODResults t3 = new CompareODResults();
    	t3.setBaselineResultsFile("src/test/resources/mta/test_qa_0_accessible.txt");
    	t3.setTestResultsFile("src/test/resources/mta/test_dev_0_accessible.txt");
    	t3.run();
    }
    
    @Test
    public void l2_DevXQA_Results() throws IOException, Exception {    	
    	
    	System.out.println("");
    	System.out.println("ACCESSIBLE QUERIES");
    	System.out.println("");
    	
      	CompareODResults t4 = new CompareODResults();
    	t4.setBaselineResultsFile("src/test/resources/mta/test_qa_100_accessible.txt");
    	t4.setTestResultsFile("src/test/resources/mta/test_dev_100_accessible.txt");
    	t4.run();
    }    		
       
}


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
import java.io.*;

public class RunnerIT {

	@BeforeClass
    public static void QA_ODGen() throws IOException, Exception {    	

    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/index/stops?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t.setMTAOnly(false);
    	t.setPairsToGenerate(250);
    	t.run();
    	
    	
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/plan?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t2.setOutputFile("src/test/resources/mta/test_qa_baseline.txt");
    	t2.run();

    }    
    
  /*  
    @Test
    public void Demo_QA() throws IOException, Exception {    	

//    	BlockUntilReleaseFinished t = new BlockUntilReleaseFinished();
//    	t.setOTPURL("http://otp-mta-demo.camsys-apps.com/otp/routers/default/version?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnt");
//    	t.run();
 
    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-demo.camsys-apps.com/otp/routers/default/plan?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnt");
    	t2.setOutputFile("src/test/resources/mta/test_demo_otp.txt");
    	t2.run();

    	System.out.println("*************************************");
    	System.out.println("BASELINE = QA   DEV = DEMO");
    	System.out.println("*************************************");

    	CompareODResults t3 = new CompareODResults();
    	t3.setBaselineResultsFile("src/test/resources/mta/test_qa_baseline.txt");
    	t3.setDevResultsFile("src/test/resources/mta/test_demo_otp.txt");
    	t3.run();

    }
   */
      
        
    @Test
    public void Dev_QA() throws IOException, Exception {    	

//    	BlockUntilReleaseFinished t = new BlockUntilReleaseFinished();
//    	t.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/version?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
//    	t.run();

    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-dev.camsys-apps.com/otp/routers/default/plan?apikey=hAR0VMP2Ufxk542WrtTW8ToBmi4N3UUp");
    	t2.setOutputFile("src/test/resources/mta/test_dev_otp.txt");
    	t2.run();

    	System.out.println("*************************************");
    	System.out.println("BASELINE = QA   DEV = DEV");
    	System.out.println("*************************************");

      	CompareODResults t3 = new CompareODResults();
    	t3.setBaselineResultsFile("src/test/resources/mta/test_qa_baseline.txt");
    	t3.setDevResultsFile("src/test/resources/mta/test_dev_otp.txt");
    	t3.run();

    }
      
       
}

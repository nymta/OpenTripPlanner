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
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runners.MethodSorters;

import java.io.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunnerIT {
	
	/*
	 * This class just runs the other tests. JUnit 5 supports ordering
	 * and parent/child relationships for tests, but JUnit 4 doesn't, so we
	 * hack something similar up here. 
	 */

    @Test
    public void a_Wait() throws IOException, Exception {

    	BlockUntilReleaseFinished t = new BlockUntilReleaseFinished();
    	t.run();
    
    }

    @Test
    public void e_CompareToQA_ODGen() throws IOException, Exception {    	

    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setMTAOnly(false);
    	t.run();
    }

    @Test
    public void f_CompareToQA_OTP1() throws IOException, Exception {    	

    	RunODPairsWithOTP t1 = new RunODPairsWithOTP();
    	t1.setOTPURL("http://otp-mta-demo.camsys-apps.com/otp/routers/default/plan?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnt");
    	t1.setOutputFile("src/test/resources/mta/test1_otp_results.txt");
    	t1.run();
    }
    
    @Test
    public void g_CompareToQA_OTP2() throws IOException, Exception {    	

    	RunODPairsWithOTP t2 = new RunODPairsWithOTP();
    	t2.setOTPURL("http://otp-mta-qa.camsys-apps.com/otp/routers/default/plan?apikey=EQVQV8RM6R4o3Dwb6YNWfg6OMSR7kT9L");
    	t2.setOutputFile("src/test/resources/mta/test1_otp2_results.txt");
    	t2.run();
    }
    
    @Test
    public void h_CompareToQA_Compare() throws IOException, Exception {    	

    	CompareODResults t3 = new CompareODResults();
    	t3.setOTPResultsFile("src/test/resources/mta/test1_otp_results.txt");
    	t3.setBrandXResultsFile("src/test/resources/mta/test1_otp2_results.txt");
    	t3.run();
    }
    

    @Test
    public void j_CompareToATIS_ODGen() throws IOException, Exception {    	
  	
    	GenerateTestODPairsFromRunningInstance t = new GenerateTestODPairsFromRunningInstance();
    	t.setMTAOnly(true);
    	t.run();

    }
    
    @Test
    public void k_CompareToATIS_OTP() throws IOException, Exception {    	

    	RunODPairsWithOTP t1 = new RunODPairsWithOTP();
    	t1.setOTPURL("http://otp-mta-demo.camsys-apps.com/otp/routers/default/plan?apikey=z6odKJINMNQww8M1zWfFoTMCUPcfbKnt");
    	t1.setOutputFile("src/test/resources/mta/test2_otp_results.txt");
    	t1.run();
    }
    
    @Test
    public void l_CompareToATIS_ATIS() throws IOException, Exception {    	

    	RunODPairsWithATIS t2 = new RunODPairsWithATIS();
    	t2.setOutputFile("src/test/resources/mta/test2_atis_results.txt");
    	t2.run();
    }
    
    @Test
    public void m_CompareToATIS_Compare() throws IOException, Exception {    	

    	CompareODResults t3 = new CompareODResults();
    	t3.setOTPResultsFile("src/test/resources/mta/test2_otp_results.txt");
    	t3.setBrandXResultsFile("src/test/resources/mta/test2_atis_results.txt");
    	t3.run();
    }
    
}

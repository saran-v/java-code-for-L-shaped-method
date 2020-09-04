package com.project.optimizer;


import com.project.optimizer.busplan.Master;
import com.project.optimizer.busplan.Sub;

public class Algorithm {

	
	public Algorithm() throws Exception
	{
	}
	

	public void CreateLPObjects(String fileName)
	{
		Master.readMPSFile(fileName);
		Master.getNames();
		Master.getMasterModel();
		
		if(Optimizer.debug)
			Master.writeLPFile("lp",-1);
		
		Sub.readMPSFile(fileName);
		Sub.getSubModel();
		
		if(Optimizer.debug)		
			Sub.getLPFile("",-1,-1);		
	
		Sub.getIndicesValues(Master.getIndices(), Master.getValues());
	}	
    
}

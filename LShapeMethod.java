package com.project.optimizer;

import java.util.ArrayList;
import java.util.Map;

import com.project.optimizer.beans.RowValBean;
import com.project.optimizer.beans.StoFileBean;
import com.project.optimizer.busplan.Master;
import com.project.optimizer.busplan.Sub;

public class LShapeMethod {

	public static double lb = 0;
	public static double ub = 0;

	public LShapeMethod() {

		super();

		try {
			Algorithm();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(" Error: L-shaped Algorithm:");
		}
		// TODO Auto-generated constructor stub

//		System.exit(0);
	}


	public static void Algorithm() throws Exception
	{
		if(Optimizer.debug)
			Master.writeLPFile("ls",100); // test

		Master.solve();
		Master.addColumn();

		if(Optimizer.debug)
			Master.writeLPFile("ls",101); // test

		double lBound,tUbound = 0;
		double uBound = Double.MAX_VALUE;
		int iter = 0;
 	    lBound = Master.getObjValue();

		do
		{
			Sub.createAlphaBeta(Master.getxValues().length);
	 	    tUbound = Master.getFSOj(iter);

			int spN=0;
	 	    for (Map.Entry<String, ArrayList<RowValBean>> entry : StoFileBean.stoHash.entrySet())
		    {
			   	ArrayList<RowValBean> rwBeanList = entry.getValue();
			   	Sub.chgRhs(rwBeanList);
			   	Sub.adjustT(Master.getxValues());
			   	Sub.solve(false);

			   	if(Optimizer.debug)
			   		Sub.getLPFile("",spN++, iter);

			   	Sub.calculateAlpha(StoFileBean.sceProb.get(entry.getKey()));
			   	Sub.calculateBeta(Master.getxValues().length, StoFileBean.sceProb.get(entry.getKey()));
			   	tUbound += (Sub.getObj()*StoFileBean.sceProb.get(entry.getKey()));
			}

			if(Optimizer.debug)
				Master.writeLPFile("ls",102); // test

	 	    Master.addBendersCut(Sub.getBetaValue(), Sub.getAlphaValue());

			if(Optimizer.debug)
				Master.writeLPFile("ls",iter);

	 	    Master.solve();
	 	    lBound = Master.getObjValue();

	 	    if(tUbound < uBound)
	 	    	uBound = tUbound;


			System.out.println("LB: " + lBound + " UB: " + uBound + " Iter: " + iter);


	 	    iter++;

		}while(uBound - lBound > 0.001 && (System.currentTimeMillis() - Optimizer.stTime)/1000 < Optimizer.runTime); // && iter < 10);

		lb = lBound;
		ub = uBound;


	}

}

package com.project.optimizer.busplan;

import ilog.concert.IloConversion;
import ilog.concert.IloCopyable;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloObjective;
import ilog.concert.IloObjectiveSense;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.project.optimizer.Optimizer;
import com.project.optimizer.beans.RowValBean;
import com.project.optimizer.beans.TimeFileBean;

public class Sub {

	private static IloCplex    subCplex;
	private static IloLPMatrix subLp;
//	private static IloLPMatrix subLp1;
	private static IloNumVar[] subVars;
	private static IloRange[]  subRngs;

	private static IloNumVar[] rsubVars;
	private static IloRange[]  rsubRngs;

	private static IloConversion relax;

	private static ArrayList<String>  rrowNames = new
			ArrayList<String>();

	private static int    rVars;
	private static int    rCons;
	private static double obj;

	private static double[] rhs;
	public  static double[] oRhs; // accessed by Fenchel cut model
	private static double[] vUB;

	private static double[] duals;
	public  static double[] soln;
	private static double   alphaValue;
	private static double[] betaValue;

	private static int[][]     indices;
	private static double[][]  values;

	private static boolean oStored = false;
	public static int     nFCuts;
	private static IloObjective objF;

	public Sub()
	{
	  try
	  {

	  }
	  catch (Exception e)
	  {
		e.printStackTrace();
		System.out.println("Error: MP Creator Constructor");
		// TODO: handle exception
	  }
	}

	public static void readMPSFile(String fileName)
	{
		try
		{
			Sub.subCplex = new IloCplex();
			Sub.subCplex.importModel(fileName+".mps");
			Sub.subLp   = (IloLPMatrix)subCplex.LPMatrixIterator().next();
			Sub.subVars = Sub.subLp.getNumVars();
			Sub.subRngs = Sub.subLp.getRanges();

			Sub.subCplex.setOut(null);

		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: MP MPS Reader");
		}
	}

	public static double getAlphaValue() {
		return alphaValue;
	}

	public static void setAlphaValue(double alphaValue) {
		Sub.alphaValue = alphaValue;
	}

	public static double[] getBetaValue() {
		return betaValue;
	}

	public static void setBetaValue(double[] betaValue) {
		Sub.betaValue = betaValue;
	}

	public static void getSubModel()
	{
		int enVMastIndex = TimeFileBean.getMpsVarNames().indexOf(TimeFileBean.getSsVar());
		int enCMastIndex = TimeFileBean.getMpsConNames().indexOf(TimeFileBean.getSsCon());

		try
		{
			System.out.println(" enVMastIndex:" + enVMastIndex);
			System.out.println(" enCMastIndex:" + enCMastIndex);

			Sub.subLp.removeCols(0, enVMastIndex);
			Sub.subLp.removeRows(0, enCMastIndex);

			IloCopyable[] vars = new IloCopyable[enVMastIndex];

			for(int i=0; i < enVMastIndex; i++)
				vars[i] = Sub.subVars[i];

			Sub.subCplex.delete(vars);

			Sub.rVars = Sub.subCplex.getNcols();
			Sub.rCons = Sub.subCplex.getNrows();

			Sub.rsubVars = Sub.subLp.getNumVars();

			addColsRows(); // adding the cons to cplex object
			Sub.rsubRngs = Sub.subLp.getRanges();

			for(int i=0; i < Sub.rsubRngs.length; i++)
				rrowNames.add(Sub.rsubRngs[i].getName());

            relax = Sub.subCplex.conversion(Sub.subLp.getNumVars(),
                    IloNumVarType.Float);

            Sub.subCplex.add(relax);

		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: MasterModel");
		}
	}

	public static void getLPObject()
	{
		Sub.subLp   = (IloLPMatrix)subCplex.LPMatrixIterator().next();
	}



	public static void addColsRows() throws IloException
	{
		ArrayList<Double> rhsArr = new ArrayList<Double>();

		int index = 0;
		for(IloNumVar v:Sub.rsubVars)
		{
			if(v.getUB() > 0) // initially it was 1
			{
				int[] ind    = new int[1];
				double[] val = new double[1];

				ind[0] = index;
				val[0] = 1.0;

				IloLinearNumExpr cons = Sub.subCplex.linearNumExpr();
				cons.addTerm(v, 1);

				IloRange r = Sub.subCplex.addLe(cons, v.getUB(), "v_UB_" + index);
				Sub.subLp.addRow(r);
//				SPBean.subLp.addRow(-0, v.getUB(), ind, val);
				v.setUB(Double.MAX_VALUE);
				rhsArr.add(v.getUB());
			}

			index++;
		}

		vUB =  new double[rhsArr.size()];

		index = 0;
		for(Double d:rhsArr)
			vUB[index++] = d;

//		if(rhsArr.size() > 0)
//		{
//			double[] t = new double[rhs.length];
//			System.arraycopy(rhs, 0, t, 0, rhs.length);
//
//			rhs = new double[rhs.length + rhsArr.size()];
//			System.arraycopy(t, 0, rhs, 0, t.length);
//
//			for(int i=0;i<rhsArr.size();i++)
//				rhs[t.length+i] = rhsArr.get(i);
//		}

	}

	public static void getLPFile(String fName, int insNo, int iter)
	{
		try {
			String filename = fName + "sub_" + insNo + "_" + iter + ".lp";

			if(Optimizer.debug)
				System.out.println(" fileName: " + filename);

			Sub.subCplex.exportModel(filename);
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: GetLPFile");
		}
	}

	public static void chgRhs(ArrayList<RowValBean> rwValBeanArr) throws IloException
	{
		// As of now all the constrainsts for T have to be stored
		//  change it to orhs to rhs. to make it generic
		for(RowValBean rvBean : rwValBeanArr)
		{
			int index = rrowNames.indexOf(rvBean.getrName());

			if(Sub.subLp.getRange(index).getLB() > -Double.MAX_VALUE )
				Sub.subLp.getRange(index).setLB(rvBean.getRhsVal());

			if(Sub.subLp.getRange(index).getUB() < Double.MAX_VALUE )
				Sub.subLp.getRange(index).setUB(rvBean.getRhsVal());

			if(Sub.oStored)
			{
				oRhs[index] = rvBean.getRhsVal(); // rhs changes for the changes scenario
			}
		}

		rhs = new double[Sub.rsubRngs.length];

		int i = 0;
		// ub for <= constraints  and lb for >= constraints
		for(IloRange ir : Sub.rsubRngs)
			if(ir.getUB() < Double.MAX_VALUE)
				rhs[i++] = ir.getUB();
			else
				rhs[i++] = ir.getLB();

		if(!Sub.oStored)
		{
			oRhs = new double[Sub.rsubRngs.length];
			System.arraycopy(rhs, 0, oRhs, 0, oRhs.length); // source to dest
			Sub.oStored = true;
		}
	}

	public static void adjustT(double[] xSol) throws IloException
	{
//		System.out.println(" adjustT: ");
//		System.out.println(" SPBean.rCons: " + SPBean.rCons);

		for(int i=0; i < Sub.rCons; i++)
		{
			double adjValue = 0;
			for(int j=0; j < Sub.indices[i].length; j++)
			{
				if(Sub.indices[i][j] < TimeFileBean.getSsVarIndex())
				{
					/*
					System.out.println("j: " + j + " SPBean.indices[i][j]: " + Sub.indices[i][j]
							+ " SPBean.values[i][j]:" + Sub.values[i][j]
									+ " xSol[SPBean.indices[i][j]]: " + xSol[Sub.indices[i][j]]);
					*/
					adjValue += Sub.values[i][j]*xSol[Sub.indices[i][j]];
				}
			}

//			if(adjValue != 0)
//			{
				double rhs;
				rhs = Sub.oRhs[i];
//				rhs = SPBean.subLp.getRange(i).getUB();
				rhs -= adjValue;

//				System.out.println(" rhs: " + rhs + " Sub.subLp.getRange(i).getLB() : "
//						+ Sub.subLp.getRange(i).getLB() );

				if(Sub.subLp.getRange(i).getLB() > -Double.MAX_VALUE )
					Sub.subLp.getRange(i).setLB(rhs);
				
				Sub.subLp.getRange(i).setUB(rhs);


//				System.out.println(" rhs: " + rhs + " Sub.oRhs[i]: " + Sub.oRhs[i]);
//			}
		}
	}

	public static void solve(boolean isMIP)
	{
		try
		{

			Sub.subCplex.setParam(IloCplex.IntParam.RootAlg,
                    IloCplex.Algorithm.Dual);
			Sub.subCplex.setParam(IloCplex.BooleanParam.PreInd, false);

			Sub.subCplex.solve();
			obj   = Sub.subCplex.getObjValue();
			soln  = Sub.subCplex.getValues(rsubVars);

			if(!isMIP)
				duals = Sub.subCplex.getDuals(Sub.subLp);

			if(Optimizer.debug)
				for(int i = 0; i< duals.length; i++)
					System.out.println(" i: " + i + " duals[i]: " + duals[i]);

		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: MasterSolve");
		}
	}


	public static int getrVars() {
		return rVars;
	}

	public static void setrVars(int rVars) {
		Sub.rVars = rVars;
	}

	public static double getObj() {
		return obj;
	}


	public static void setObj(double obj) {
		Sub.obj = obj;
	}


	public static double[] getDuals() {
		return duals;
	}


	public static void setDuals(double[] duals) {
		Sub.duals = duals;
	}


	public static  void calculateAlpha(double prb)
	{
		try {
//			System.out.println(" xxxxxxxxxxxxxxxxxxxxxxxxxx:");
//			System.out.println(" SPBean.alphaValue:" + Sub.alphaValue + " duals.length: " + duals.length);
			for(int i=0; i < duals.length; i++)
			{
//				System.out.println(" duals[i]:" + duals[i] + " oRhs[i]:" + oRhs[i] + " prb:" + prb);
				Sub.alphaValue += (duals[i]*oRhs[i]*prb);
//				System.out.println(" SPBean.alphaValue:" + Sub.alphaValue);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}


	public static  void calculateAlphaFc(double prb, ArrayList<Double> rhsValArr)
	{
		try
		{
			if(Optimizer.debug)
				System.out.println("start: Sub.alphaValue: " + Sub.alphaValue + " rhsValArr.size(): " + rhsValArr.size());

			double tAlphaValue  = 0;
			for(int i=0; i < oRhs.length; i++)
			{
				tAlphaValue += (duals[i]*oRhs[i]);

				if(Optimizer.debug)
					System.out.println("tAlphaValue : " + tAlphaValue +
							" duals[i]: " + duals[i] + " oRhs[i]: " + oRhs[i] + " prb: " +  prb);
			}

			for(int i = oRhs.length; i < oRhs.length + rhsValArr.size(); i++)
			{
				tAlphaValue += (duals[i]*rhsValArr.get(i - oRhs.length));

				if(Optimizer.debug)
					System.out.println("tAlphaValue : " + tAlphaValue + " duals[i]: " + duals[i] + " rhsValArr.get(i - oRhs.length): "
							+ rhsValArr.get(i - oRhs.length) +
							" duals[i]*rhsValArr.get(i - oRhs.length): " + duals[i]*rhsValArr.get(i - oRhs.length) + " prb: " +  prb);
			}

			Sub.alphaValue += (tAlphaValue*prb);

			if(Optimizer.debug)
				System.out.println("end: Sub.alphaValue: " + Sub.alphaValue);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}


	public static  void calculateBeta(int xArray, double prb) throws IloException
	{
		for(int i=0; i < Sub.rCons; i++)
		{
			for(int j=0; j < Sub.indices[i].length; j++)
			{
//				if(SPBean.indices[i][j] == 5)
//				{
//					System.out.println(" xxxxxxxxxxxxxxxxxxxxxxxxxx:");
//					System.out.println(" SPBean.betaValue[ind[i][j]]:" + SPBean.betaValue[SPBean.indices[i][j]]);
//				}

				if(Sub.indices[i][j] < TimeFileBean.getSsVarIndex())
					Sub.betaValue[Sub.indices[i][j]] += (Sub.values[i][j]*duals[i]*prb);
//					SPBean.betaValue[SPBean.indices[i][j]] += (SPBean.values[i][SPBean.indices[i][j]]*duals[i]*prb);

//				if(SPBean.indices[i][j] == 5)
//				{
//					System.out.println(" i:" + i + " ind[i][j]:" + SPBean.indices[i][j] + " duals[i]:"
//							+ duals[i] + " prb:" + prb + " val[i][ind[i][j]]:" + SPBean.values[i][SPBean.indices[i][j]]);
//
//					System.out.println(" SPBean.betaValue[SPBean.indices[i][j]]:" + SPBean.betaValue[SPBean.indices[i][j]]);
//				}
			}
		}
	}

	public static void getIndicesValues(int[][] ind, double[][] val)
	{
		Sub.indices = new int[Sub.subRngs.length][];
		Sub.values  = new double[Sub.subRngs.length][];

//		System.out.println(" Red-Sub-Ranges:" );
//		System.out.println(" SPBean.subRngs.length:" + SPBean.subRngs.length);
//		System.out.println(" SPBean.rsubRngs.length:" + SPBean.rsubRngs.length);

//		System.out.println(" TimeFileBean.getSsConIndex():" + TimeFileBean.getSsConIndex());

		for(int i=TimeFileBean.getSsConIndex(); i < ind.length; i++)
		{
			Sub.indices[i-TimeFileBean.getSsConIndex()] = new int[ind[i].length];
			Sub.values[i-TimeFileBean.getSsConIndex()]  = new double[val[i].length];

//			System.out.println(" ind[i].length:" + ind[i].length + " val[i].length: "
//					+ val[i].length );

			for(int j=0; j < ind[i].length; j++)
			{
//				System.out.println(" j:" + j);
//				System.out.println(" i-TimeFileBean.getSsCo	nIndex():" + (i-TimeFileBean.getSsConIndex()));
//				System.out.println(" SPBean.indices[i-TimeFileBean.getSsConIndex()][j]:" + SPBean.indices[i-TimeFileBean.getSsConIndex()][j]);
//				System.out.println(" ind[i][j]:" + ind[i][j] + " val[i][j]:" + val[i][j]);
				Sub.indices[i-TimeFileBean.getSsConIndex()][j] = ind[i][j];
				Sub.values[i-TimeFileBean.getSsConIndex()][j]  = val[i][j];
			}
		}
	}

	public static int[][] getIndices() {
		return indices;
	}

	public static void setIndices(int[][] indices) {
		Sub.indices = indices;
	}

	public static double[][] getValues() {
		return values;
	}

	public static void setValues(double[][] values) {
		Sub.values = values;
	}

	public static double[] getSoln() {
		return soln;
	}

	public static void setSoln(double[] soln) {
		Sub.soln = soln;
	}

	public static  void createAlphaBeta(int xSize)
	{
		Sub.betaValue  = new double[xSize];
		Sub.alphaValue = 0;
	}

	public static void removeRelax() throws Exception
	{
        Sub.subCplex.remove(relax);
	}

	public static double getSubInt() throws Exception
	{
        Sub.subCplex.remove(relax);

        if(Optimizer.debug)
        	Sub.subCplex.exportModel("sub_Int.lp");

		System.out.println(" Solving Sub: ");
		Sub.subCplex.solve();

		if(Optimizer.debug)
			System.out.println(" Objective: " + Sub.subCplex.getObjValue());

		return Sub.subCplex.getObjValue();
	}


}

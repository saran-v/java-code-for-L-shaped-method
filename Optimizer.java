package com.project.optimizer;


import com.project.optimizer.dao.StoFileDAO;
import com.project.optimizer.dao.TimeFileDAO;

public class Optimizer {

	public static String fileName;	
	public static boolean debug        = false;
	public static boolean useReduction = false;
	
	public static int runTime = 0;
	public static long stTime = 0;	
	
	public Optimizer() throws Exception
	{

	}
	
    public static void main(String[] args) {

    	try 
    	{
    		fileName = args[0];
    		runTime  = Integer.parseInt(args[2]);

    		new TimeFileDAO(args[0]);
    		new StoFileDAO(args[0]);
    		
    		long t1 = System.currentTimeMillis();
    		Algorithm alg = new Algorithm();
    		alg.CreateLPObjects(args[0]);

    		if(Integer.parseInt(args[1]) == 1)
    			useReduction = true;

    		stTime = System.currentTimeMillis();    		
    		new LShapeMethod();  		
    		
    		System.out.println( " Total Time Taken (Secs): "
    				+ (System.currentTimeMillis() - t1)/1000);    		
		} 
    	catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
    }
    
    
    
}

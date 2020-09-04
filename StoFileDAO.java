package com.project.optimizer.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.project.optimizer.beans.RowValBean;
import com.project.optimizer.beans.StoFileBean;

public class StoFileDAO
{
    public StoFileDAO(String fileName)
    {
        try
        {	        	
            String strFileName = fileName + ".sto";            
	        BufferedReader in  = new BufferedReader(new FileReader(strFileName));
	        
	        String str;
	        String[] readVal = new String[5];
	        int index = 0;
	        String sName = null;
	        double sPrb  = 0;
	        
	        ArrayList<RowValBean>  rValues = new ArrayList<RowValBean>(); 
	        while ((str = in.readLine()) != null)
	        {
	     	    //skip the first row - column header        	   
	       	    if(index++ == 0)
	    	    	continue;	       	   
	     	   
	            int arrIndex = 0;	             

	            
	            StringBuffer strB = new StringBuffer();		    
			    
	            boolean fChar = false;
	            for(int i=0; i< str.length(); i++)
	            {		            
	            	if(str.charAt(i) == '	' || str.charAt(i) == ' ')
	            	{            		
	            		if((arrIndex != 0 || (fChar && arrIndex == 0)) && isLetterDigit(strB))
	            		{
	            			readVal[arrIndex] = strB.toString();
				            
	            			strB = new StringBuffer();
	            			arrIndex ++;
	            			
	            			if(fChar)
	            			  fChar = false;
	            		}	            			
	            			
	            	}
	            	else
	            	{
	            		strB.append(str.charAt(i));
	            		if ( i > 0)
	            			fChar = true;
	            	}
	            }
				readVal[arrIndex] = strB.toString();	            
	            
             
	            if(readVal[0].equals("SC"))
	            { 	 
	            	 
	            	 if(sName != null)
	            	 {
	            		 StoFileBean.stoHash.put(sName, rValues);
	            		 StoFileBean.sceProb.put(sName, sPrb);
	            	 }
	            		 
	            	 sName = readVal[1];
	            	 sPrb  = Double.valueOf(readVal[3]).doubleValue();
	            	 rValues = new ArrayList<RowValBean>();
	            }
	            else if( readVal[0].equals("RHS"))
	            {
	            	 rValues.add(new RowValBean(readVal[1],Double.valueOf(readVal[2]).doubleValue())); 
	            }           
            
	        }
	        StoFileBean.stoHash.put(sName, rValues);
	        StoFileBean.sceProb.put(sName, sPrb);			
	        in.close();
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        	System.out.println("Error in VinsDAO");
            System.exit(0);
        }		
    }
    
    
    public static boolean isLetterDigit(StringBuffer sb)
    {
    	String str = sb.toString();
    	
        for(int i=0; i< str.length(); i++)
        {
        	if(Character.isLetterOrDigit(str.charAt(i)))
        		return true;
        }
        
        return false;   	
    	
    }
}
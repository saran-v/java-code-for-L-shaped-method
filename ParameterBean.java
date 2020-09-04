package com.project.optimizer.beans;

import java.util.ArrayList;
import java.util.HashMap;

public class ParameterBean 
{
	private static String fileName = "";
	
	private static String  inputFolderName   = "";
	private static String  outputFolderName  = "";
	private static String  fieldSeperator    = ",";
	private static boolean lpFileGenerate    = true;
	
	private static int     nPeriod           = 10;
	
	public static String getFileName() {
		return fileName;
	}
	public static void setFileName(String fileName) {
		ParameterBean.fileName = fileName;
	}	
	public static int getnPeriod() {
		return nPeriod;
	}
	public static void setnPeriod(int nPeriod) {
		ParameterBean.nPeriod = nPeriod;
	}
	public static String getInputFolderName() {
		return inputFolderName;
	}
	public static void setInputFolderName(String inputFolderName) {
		ParameterBean.inputFolderName = inputFolderName;
	}
	public static String getOutputFolderName() {
		return outputFolderName;
	}
	public static void setOutputFolderName(String outputFolderName) {
		ParameterBean.outputFolderName = outputFolderName;
	}
	public static String getFieldSeperator() {
		return fieldSeperator;
	}
	public static void setFieldSeperator(String fieldSeperator) {
		ParameterBean.fieldSeperator = fieldSeperator;
	}
	public static boolean isLpFileGenerate() {
		return lpFileGenerate;
	}
	public static void setLpFileGenerate(boolean lpFileGenerate) {
		ParameterBean.lpFileGenerate = lpFileGenerate;
	}	
	
}

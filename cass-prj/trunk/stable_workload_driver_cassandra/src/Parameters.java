package org.fi;

public class Parameters{
	public final static int BREAK_EXP_NUMBER = Integer.parseInt(System.getProperty("BREAK_EXP_NUMBER", "100000"));
	public final static int MAX_FSN = Integer.parseInt(System.getProperty("MAX_FSN", "2"));
	public final static boolean enableFailure = Boolean.parseBoolean(System.getProperty("enableFailure", "true"));
	public final static boolean enableCoverage = Boolean.parseBoolean(System.getProperty("enableCoverage", "true"));
	public final static boolean debug = Boolean.parseBoolean(System.getProperty("debug", "true"));
    public final static String filter = System.getProperty("filter", "default");
    public final static String clevel = System.getProperty("consistencyLevel", "all");
}

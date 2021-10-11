package hmm;

public interface Alphabet 
{
	public ProcessedString[] getProcessedStrings(String[] p_strings);
	public int size();
	public Integer[] getAllIDs();
	public Object lookupID(Integer p_id);
	
	public static boolean isTerminator(Object p_c) 
	{
		// TODO Auto-generated method stub
		return false;
	}
	public ProcessedString getProcessedString(String p_string);
}

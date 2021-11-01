package hmm;

public interface Alphabet
{
	public static boolean isTerminator(final Object p_c)
	{
		// TODO Auto-generated method stub
		return false;
	}

	public Integer[] getAllIDs();

	public ProcessedString getProcessedString(String p_string);

	public ProcessedString[] getProcessedStrings(String[] p_strings);

	public Object lookupID(Integer p_id);

	public int size();
}

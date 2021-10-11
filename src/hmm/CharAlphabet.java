package hmm;

import java.util.TreeMap;

public class CharAlphabet implements Alphabet 
{
	private TreeMap<Object, Integer> m_outputs;
	private TreeMap<Integer, Object> m_inverse;
	private int m_index = 0;
	private static final char TERMINATOR = '\uE000';
	
	public static void main(String[] p_args)
	{
		CharAlphabet a = new CharAlphabet();
		String test = "abcdefgabcdefg";
		Integer[] idSeq = a.getIDs(test);
		for(int i: idSeq)
		{
			System.out.println(i);
		}
	}
	
	public CharAlphabet()
	{
		m_outputs = new TreeMap<Object, Integer>();
		m_inverse = new TreeMap<Integer, Object>();
		ensureID(TERMINATOR);
	} 
	
	public int size()
	{
		return m_outputs.size();
	}
	
	public static boolean isTerminator(char p_char)
	{
		return p_char == TERMINATOR;
	}
	
	public Object lookupID(Integer p_ID)
	{
		return m_inverse.get(p_ID);
	}
	
	private void ensureID(Object p_char)
	{
		if(!m_outputs.keySet().contains(p_char))
		{
			m_outputs.put(p_char, m_index);
			m_inverse.put(m_index, p_char);
			
			m_index++;
		}
	}
	
	public int getID(Object p_char)
	{
		ensureID(p_char);
		return m_outputs.get(p_char);
	}
	
	public Integer[] getIDs(String p_input)
	{
		Integer[] ret = new Integer[p_input.length() + 1];
		for(int i = 0; i < p_input.length(); i++)
		{
			char c = p_input.charAt(i);
			ret[i] = getID(c);
		}
		
		ret[p_input.length()] = getID(TERMINATOR);
		return ret;
	}
	
	public void ensureIDs(String p_string)
	{
		getIDs(p_string);
	}
	
	public Integer[] getAllIDs()
	{
		return m_outputs.values().toArray(new Integer[0]);
	}
	
	public ProcessedString getProcessedString(String p_string)
	{
		Integer[] ids = getIDs(p_string);
		ProcessedString ret = new ProcessedString(p_string, ids);
		return ret;
	}
	
	public ProcessedString[] getProcessedStrings(String[] p_strings)
	{
		ProcessedString[] ret = new ProcessedString[p_strings.length];
		
		for(int i = 0; i < ret.length; i++)
		{
			ret[i] = getProcessedString(p_strings[i]);
		}
		
		return ret;
	}
}

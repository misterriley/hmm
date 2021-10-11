package hmm;

import java.util.Arrays;

public class ProcessedString 
{
	private String m_string;
	private Integer[] m_IDs;
	private LogNum[] m_weights;
	
	public ProcessedString(String p_string, Integer[] p_IDs)
	{
		m_string = p_string;
		m_IDs = p_IDs;
		m_weights = new LogNum[m_IDs.length];
		initializeWeights();
	}
	
	private void initializeWeights()
	{
		Arrays.fill(m_weights, LogNum.ONE);
	}
	
	public String getString()
	{
		return m_string;
	}
	
	public Integer getID(int p_index)
	{
		return m_IDs[p_index];
	}
	
	public LogNum getWeight(int p_index)
	{
		return m_weights[p_index];
	}
	
	public void setWeight(int p_index, LogNum p_switchNumber)
	{
		m_weights[p_index] = p_switchNumber;
	}
	
	public int length()
	{
		return m_IDs.length;
	}
	
	public void sanityCheck()
	{
		if(m_string.length() != m_IDs.length || m_string.length() != m_weights.length)
		{
			throw new RuntimeException("Lengths are off");
		}
	}
}

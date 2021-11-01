package hmm;

import java.util.Arrays;

public class ProcessedString
{
	private final String	m_string;
	private final Integer[]	m_IDs;
	private final LogNum[]	m_weights;

	public ProcessedString(final String p_string, final Integer[] p_IDs)
	{
		m_string = p_string;
		m_IDs = p_IDs;
		m_weights = new LogNum[m_IDs.length];
		initializeWeights();
	}

	public Integer getID(final int p_index)
	{
		return m_IDs[p_index];
	}

	public String getString()
	{
		return m_string;
	}

	public LogNum getWeight(final int p_index)
	{
		return m_weights[p_index];
	}

	public int length()
	{
		return m_IDs.length;
	}

	public void sanityCheck()
	{
		if (m_string.length() != m_IDs.length || m_string.length() != m_weights.length)
		{
			throw new RuntimeException("Lengths are off");
		}
	}

	public void setWeight(final int p_index, final LogNum p_switchNumber)
	{
		m_weights[p_index] = p_switchNumber;
	}

	private void initializeWeights()
	{
		Arrays.fill(m_weights, LogNum.ONE);
	}
}

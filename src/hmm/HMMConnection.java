package hmm;

public class HMMConnection implements SimplexUnit
{
	private HMMNode m_from;
	private HMMNode m_to;
	private LogNum m_likelihood;
	
	public HMMConnection(HMMNode p_from, HMMNode p_to, LogNum connProbs)
	{
		m_from = p_from;
		m_to = p_to;
		m_likelihood = connProbs;
	}
	
	public HMMNode getFromNode()
	{
		return m_from;
	}
	
	public HMMNode getToNode()
	{
		return m_to;
	}
	
	public LogNum getLikelihood()
	{
		return m_likelihood;
	}
	
	public void setLikelihood(LogNum p_likelihood)
	{
		m_likelihood = p_likelihood;
	}
}

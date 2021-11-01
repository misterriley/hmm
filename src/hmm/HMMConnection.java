package hmm;

public class HMMConnection implements SimplexUnit
{
	private final HMMNode	m_from;
	private final HMMNode	m_to;
	private LogNum			m_likelihood;

	public HMMConnection(final HMMNode p_from, final HMMNode p_to, final LogNum connProbs)
	{
		m_from = p_from;
		m_to = p_to;
		m_likelihood = connProbs;
	}

	public HMMNode getFromNode()
	{
		return m_from;
	}

	@Override
	public LogNum getLikelihood()
	{
		return m_likelihood;
	}

	public HMMNode getToNode()
	{
		return m_to;
	}

	@Override
	public void setLikelihood(final LogNum p_likelihood)
	{
		m_likelihood = p_likelihood;
	}
}

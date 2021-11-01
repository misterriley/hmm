package hmm;

public class HMMEmission implements SimplexUnit
{
	private final Integer	m_id;
	private LogNum			m_likelihood;

	public HMMEmission(final Integer p_id, final LogNum p_likelihood)
	{
		m_id = p_id;
		m_likelihood = p_likelihood;
	}

	public Integer getID()
	{
		return m_id;
	}

	@Override
	public LogNum getLikelihood()
	{
		return m_likelihood;
	}

	@Override
	public void setLikelihood(final LogNum p_likelihood)
	{
		m_likelihood = p_likelihood;
	}
}

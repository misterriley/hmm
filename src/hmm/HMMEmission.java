package hmm;

public class HMMEmission implements SimplexUnit
{
	private Integer m_id;
	private LogNum m_likelihood;
	
	public HMMEmission(Integer p_id, LogNum p_likelihood)
	{
		m_id = p_id;
		m_likelihood = p_likelihood;
	}
	
	public Integer getID()
	{
		return m_id;
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

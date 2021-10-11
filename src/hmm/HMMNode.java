package hmm;

import java.util.Collection;
import java.util.HashMap;

public class HMMNode 
{	
	private HashMap<HMMNode, HMMConnection> m_connections;
	private HashMap<Integer, HMMEmission> m_emissions;
	
	public HMMNode()
	{
		m_connections = new HashMap<HMMNode, HMMConnection>();
		m_emissions = new HashMap<Integer, HMMEmission>();
	}
	
	public void sanityCheck()
	{		
		correctBadSimplex(m_emissions.values());
		correctBadSimplex(m_connections.values());
		
		//Utilities.checkAllNonzero(m_emissions.values());
		//Utilities.checkAllNonzero(m_connections.values());
	}

	private void correctBadSimplex(Collection<? extends SimplexUnit> p_coll) 
	{
		if(!Utilities.checkSumToOne(p_coll))
		{
			LogNum sum = Utilities.sum(p_coll);
			if(sum.isZero())
			{
				LogNum newTarget = new LogNum(1.0/p_coll.size());
				for(SimplexUnit su: p_coll)
				{
					su.setLikelihood(newTarget);
				}
			}
			else
			{
				for(SimplexUnit su: m_emissions.values())
				{
					su.setLikelihood(LogNum.divide(su.getLikelihood(), sum));
				}
			}
		}
	}
	
	public void addConnection(HMMNode p_to, LogNum connProbs)
	{
		HMMConnection conn = new HMMConnection(this, p_to, connProbs);
		m_connections.put(p_to, conn);
	}
	
	public void addEmission(Integer p_id, LogNum p_likelihood)
	{
		HMMEmission em = new HMMEmission(p_id, p_likelihood);
		m_emissions.put(p_id, em);
	}
	
	public HMMConnection getConnectionTo(HMMNode p_to)
	{
		return m_connections.get(p_to);
	}

	public HMMEmission getEmissionByID(Integer p_id)
	{
		return m_emissions.get(p_id);
	}
	
	public HMMConnection selectRandomConnection()
	{
		return (HMMConnection) Utilities.selectSimplexUnit(m_connections.values());
	}
	
	public HMMEmission selectRandomEmission()
	{
		return (HMMEmission) Utilities.selectSimplexUnit(m_emissions.values());
	}
}

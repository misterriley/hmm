package hmm;

import java.util.Collection;
import java.util.HashMap;

public class HMMNode
{
	private final HashMap<HMMNode, HMMConnection>	m_connections;
	private final HashMap<Integer, HMMEmission>		m_emissions;

	public HMMNode()
	{
		m_connections = new HashMap<>();
		m_emissions = new HashMap<>();
	}

	public void addConnection(final HMMNode p_to, final LogNum connProbs)
	{
		final HMMConnection conn = new HMMConnection(this, p_to, connProbs);
		m_connections.put(p_to, conn);
	}

	public void addEmission(final Integer p_id, final LogNum p_likelihood)
	{
		final HMMEmission em = new HMMEmission(p_id, p_likelihood);
		m_emissions.put(p_id, em);
	}

	public HMMConnection getConnectionTo(final HMMNode p_to)
	{
		return m_connections.get(p_to);
	}

	public HMMEmission getEmissionByID(final Integer p_id)
	{
		return m_emissions.get(p_id);
	}

	public void sanityCheck()
	{
		correctBadSimplex(m_emissions.values());
		correctBadSimplex(m_connections.values());

		//Utilities.checkAllNonzero(m_emissions.values());
		//Utilities.checkAllNonzero(m_connections.values());
	}

	public HMMConnection selectRandomConnection()
	{
		return (HMMConnection) Utilities.selectSimplexUnit(m_connections.values());
	}

	public HMMEmission selectRandomEmission()
	{
		return (HMMEmission) Utilities.selectSimplexUnit(m_emissions.values());
	}

	private void correctBadSimplex(final Collection<? extends SimplexUnit> p_coll)
	{
		if (!Utilities.checkSumToOne(p_coll))
		{
			final LogNum sum = Utilities.sum(p_coll);
			if (sum.isZero())
			{
				final LogNum newTarget = new LogNum(1.0 / p_coll.size());
				for (final SimplexUnit su : p_coll)
				{
					su.setLikelihood(newTarget);
				}
			}
			else
			{
				for (final SimplexUnit su : m_emissions.values())
				{
					su.setLikelihood(LogNum.divide(su.getLikelihood(), sum));
				}
			}
		}
	}
}

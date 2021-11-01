package hmm;

import java.util.Arrays;

public class QHMM
{
	public static void main(final String[] p_args)
	{
		final String[] strings = DataLoader.loadStrings(5);
		final HMM hmm = new HMM(2, AlphabetType.CHAR);
		hmm.train(strings);

		final QHMM qhmm = new QHMM(hmm);
		for (int i = 0; i < 10; i++)
		{
			qhmm.buildString();
		}
	}

	private final HMM	m_base;
	private LogNum[]	m_stateDistribution;
	private LogNum[]	m_nextOutputProbs;

	private double m_weight;

	//[currentNode][outputID]
	private LogNum[][] m_outputProbs;

	public QHMM(final HMM p_base)
	{
		m_base = p_base;
		m_weight = 1;
		initialize();
		buildOutputMatrix();
	}

	public String buildString()
	{
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 140; i++)
		{
			final LogNum[] outputProbs = getNextOutputProbs();
			final int outputIndex = Utilities.selectSimplexUnitIndex(outputProbs);
			final Object c = m_base.getAlphabet().lookupID(outputIndex);
			if (Alphabet.isTerminator(c))
			{
				break;
			}

			sb.append(c);
			updateStateBasedOnOutput(outputIndex);
		}

		return new String(sb);
	}

	public Alphabet getAlphabet()
	{
		return m_base.getAlphabet();
	}

	public LogNum[] getNextOutputProbs()
	{
		Arrays.fill(m_nextOutputProbs, LogNum.ZERO);

		for (int currentNodeIndex = 0; currentNodeIndex < m_base.numNodes(); currentNodeIndex++)
		{
			final LogNum nodeProbability = m_stateDistribution[currentNodeIndex];
			for (int outputIndex = 0; outputIndex < m_base.getAlphabet().size(); outputIndex++)
			{
				final LogNum outputProbability = m_outputProbs[currentNodeIndex][outputIndex];
				final LogNum product = LogNum.multiply(nodeProbability, outputProbability);
				m_nextOutputProbs[outputIndex] = LogNum.add(m_nextOutputProbs[outputIndex], product);
			}
		}

		Utilities.checkSumToOne(m_nextOutputProbs);

		return m_nextOutputProbs;
	}

	public double getWeight()
	{
		return m_weight;
	}

	public void initialize()
	{
		m_stateDistribution = new LogNum[m_base.numNodes()];
		m_stateDistribution = m_base.getPriors();
		m_nextOutputProbs = new LogNum[m_base.getAlphabet().size()];
	}

	public void setWeight(final double p_weight)
	{
		m_weight = p_weight;
	}

	public void updateStateBasedOnOutput(final int p_outputID)
	{
		final LogNum priorOutputProb = m_nextOutputProbs[p_outputID];
		final boolean noInformationUpdate = priorOutputProb.isZero();

		final LogNum[] posteriorStateProbs = new LogNum[m_base.numNodes()];

		for (int nextNodeIndex = 0; nextNodeIndex < m_base.numNodes(); nextNodeIndex++)
		{
			final HMMNode nextNode = m_base.getNode(nextNodeIndex);
			final HMMEmission he = nextNode.getEmissionByID(p_outputID);
			final LogNum emProb = he.getLikelihood();

			LogNum summation = LogNum.ZERO;
			for (int currentNodeIndex = 0; currentNodeIndex < m_base.numNodes(); currentNodeIndex++)
			{
				final LogNum currentNodeProb = m_stateDistribution[currentNodeIndex];

				final HMMNode currentNode = m_base.getNode(currentNodeIndex);
				final HMMConnection conn = currentNode.getConnectionTo(nextNode);
				final LogNum transProb = conn.getLikelihood();

				final LogNum product = LogNum.multiply(currentNodeProb, transProb);
				summation = LogNum.add(summation, product);
			}

			final LogNum numerator = noInformationUpdate ? summation : LogNum.multiply(emProb, summation);
			LogNum value;
			if (numerator.isZero())
			{
				value = LogNum.ZERO;
			}
			else
				if (noInformationUpdate)
				{
					//Only happens when there is no chance of this output - means that the trained HMM thinks this is impossible,
					//so we're in uncharted territory.  Update the node distribution as if no information is gained.
					if (!emProb.isZero())
					{
						throw new RuntimeException();
					}
					value = numerator;
				}
				else
				{
					value = LogNum.divide(numerator, priorOutputProb);
				}

			posteriorStateProbs[nextNodeIndex] = value;
		}

		m_stateDistribution = posteriorStateProbs;
	}

	private void buildOutputMatrix()
	{
		final int numNodes = m_base.numNodes();
		final int numOutputs = m_base.getAlphabet().size();
		m_outputProbs = new LogNum[numNodes][numOutputs];
		for (int currentNodeIndex = 0; currentNodeIndex < numNodes; currentNodeIndex++)
		{
			Arrays.fill(m_outputProbs[currentNodeIndex], LogNum.ZERO);

			final HMMNode currentNode = m_base.getNode(currentNodeIndex);
			for (int nextNodeIndex = 0; nextNodeIndex < numNodes; nextNodeIndex++)
			{
				final HMMNode nextNode = m_base.getNode(nextNodeIndex);
				final HMMConnection conn = currentNode.getConnectionTo(nextNode);
				final LogNum transProb = conn.getLikelihood();
				for (int outputIndex = 0; outputIndex < numOutputs; outputIndex++)
				{
					final HMMEmission he = nextNode.getEmissionByID(outputIndex);
					final LogNum emProb = he.getLikelihood();
					final LogNum product = LogNum.multiply(transProb, emProb);
					m_outputProbs[currentNodeIndex][outputIndex] =
						LogNum.add(m_outputProbs[currentNodeIndex][outputIndex], product);
				}
			}
		}
	}
}
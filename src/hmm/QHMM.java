package hmm;

import java.util.Arrays;

public class QHMM 
{
	private HMM m_base;
	private LogNum[] m_stateDistribution;
	private LogNum[] m_nextOutputProbs;
	private double m_weight;
	
	//[currentNode][outputID]
	private LogNum[][] m_outputProbs;
	
	public static void main(String[] p_args)
	{
		String[] strings = DataLoader.loadStrings(5);
		HMM hmm = new HMM(2, AlphabetType.CHAR);
		hmm.train(strings);
		
		QHMM qhmm = new QHMM(hmm);
		for(int i = 0; i < 10; i++)
		{
			qhmm.buildString();
		}
	}
	
	public Alphabet getAlphabet()
	{
		return m_base.getAlphabet();
	}
	
	public QHMM(HMM p_base)
	{
		m_base = p_base;
		m_weight = 1;
		initialize();
		buildOutputMatrix();
	}

	public void initialize() 
	{
		m_stateDistribution = new LogNum[m_base.numNodes()];
		m_stateDistribution = m_base.getPriors();
		m_nextOutputProbs = new LogNum[m_base.getAlphabet().size()];
	}
	
	public String buildString()
	{		
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < 140; i++)
		{
			LogNum[] outputProbs = getNextOutputProbs();
			int outputIndex = Utilities.selectSimplexUnitIndex(outputProbs);
			Object c = m_base.getAlphabet().lookupID(outputIndex);
			if(Alphabet.isTerminator(c))
			{
				break;
			}
			
			sb.append(c);
			updateStateBasedOnOutput(outputIndex);
		}
		
		return new String(sb);
	}

	private void buildOutputMatrix() 
	{
		int numNodes = m_base.numNodes();
		int numOutputs = m_base.getAlphabet().size();
		m_outputProbs = new LogNum[numNodes][numOutputs];
		for(int currentNodeIndex = 0; currentNodeIndex < numNodes; currentNodeIndex++)
		{
			Arrays.fill(m_outputProbs[currentNodeIndex], LogNum.ZERO);
			
			HMMNode currentNode = m_base.getNode(currentNodeIndex);
			for(int nextNodeIndex = 0; nextNodeIndex < numNodes; nextNodeIndex++)
			{
				HMMNode nextNode = m_base.getNode(nextNodeIndex);
				HMMConnection conn = currentNode.getConnectionTo(nextNode);
				LogNum transProb = conn.getLikelihood();
				for (int outputIndex = 0; outputIndex < numOutputs; outputIndex++)
				{
					HMMEmission he = nextNode.getEmissionByID(outputIndex);
					LogNum emProb = he.getLikelihood();
					LogNum product = LogNum.multiply(transProb, emProb);
					m_outputProbs[currentNodeIndex][outputIndex] = LogNum.add(m_outputProbs[currentNodeIndex][outputIndex], product);
				}
			}
		}
	}
	
	public LogNum[] getNextOutputProbs()
	{
		Arrays.fill(m_nextOutputProbs, LogNum.ZERO);
		
		for(int currentNodeIndex = 0; currentNodeIndex < m_base.numNodes(); currentNodeIndex++)
		{
			LogNum nodeProbability = m_stateDistribution[currentNodeIndex];
			for(int outputIndex = 0; outputIndex < m_base.getAlphabet().size(); outputIndex++)
			{
				LogNum outputProbability = m_outputProbs[currentNodeIndex][outputIndex];
				LogNum product = LogNum.multiply(nodeProbability,  outputProbability);
				m_nextOutputProbs[outputIndex] = LogNum.add(m_nextOutputProbs[outputIndex], product);
			}
		}
		
		Utilities.checkSumToOne(m_nextOutputProbs);
		
		return m_nextOutputProbs;
	}
	
	public void updateStateBasedOnOutput(int p_outputID)
	{
		LogNum priorOutputProb = m_nextOutputProbs[p_outputID];
		boolean noInformationUpdate = (priorOutputProb.isZero());
		
		LogNum[] posteriorStateProbs = new LogNum[m_base.numNodes()];
		
		for(int nextNodeIndex = 0; nextNodeIndex < m_base.numNodes(); nextNodeIndex++)
		{
			HMMNode nextNode = m_base.getNode(nextNodeIndex);
			HMMEmission he = nextNode.getEmissionByID(p_outputID);
			LogNum emProb = he.getLikelihood();
			
			LogNum summation = LogNum.ZERO;
			for(int currentNodeIndex = 0; currentNodeIndex < m_base.numNodes(); currentNodeIndex++)
			{
				LogNum currentNodeProb = m_stateDistribution[currentNodeIndex];
				
				HMMNode currentNode = m_base.getNode(currentNodeIndex);
				HMMConnection conn = currentNode.getConnectionTo(nextNode);
				LogNum transProb = conn.getLikelihood();
				
				LogNum product = LogNum.multiply(currentNodeProb, transProb);
				summation = LogNum.add(summation, product);
			}
			
			LogNum numerator = noInformationUpdate ? summation : LogNum.multiply(emProb, summation);
			LogNum value;
			if(numerator.isZero())
			{
				value = LogNum.ZERO;
			}
			else if(noInformationUpdate)
			{
				//Only happens when there is no chance of this output - means that the trained HMM thinks this is impossible,
				//so we're in uncharted territory.  Update the node distribution as if no information is gained.
				if(!emProb.isZero())
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

	public double getWeight() 
	{
		return m_weight;
	}

	public void setWeight(double p_weight) 
	{
		m_weight = p_weight;
	}
}
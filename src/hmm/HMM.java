package hmm;

import java.util.Arrays;

public class HMM 
{
	private LogNum[] m_priors;
	private HMMNode[] m_nodes;
	private Alphabet m_alphabet;
	private ProcessedString[] m_trainingSet;
	
	private static final double CON_TOL = .0001;
	private static final int MAX_ITER = -1;
	
	public static void main(String[] p_args)
	{
		String[] strings = DataLoader.loadStrings(5);
		
		for(int i = 0; i < 50; i++)
		{
			System.out.println("training HMM singleton " + i);
			HMM hmm = new HMM(3, AlphabetType.CHAR);
			System.out.println(hmm.train(strings, false));
		}
	}
	
	public Alphabet getAlphabet()
	{
		return m_alphabet;
	}
	
	public HMM(int p_numNodes, Alphabet p_alphabet)
	{
		m_nodes = new HMMNode[p_numNodes];
		m_alphabet = p_alphabet;
	}
	
	public HMM(int p_numNodes, AlphabetType p_type)
	{
		this(p_numNodes, AlphabetFactory.newAlphabet(p_type));
	}
	
	public HMMNode getNode(int p_nodeIndex)
	{
		return m_nodes[p_nodeIndex];
	}
	
	public int numNodes()
	{
		return m_nodes.length;
	}
	
	public LogNum[] getPriors()
	{
		return m_priors;
	}
	
	public void printRandomStrings(int p_numStrings)
	{
		for(int i = 0; i < p_numStrings; i++)
		{
			System.out.println(buildString());
		}
	}
	
	public void setTrainingSet(String[] p_strings)
	{
		m_trainingSet = m_alphabet.getProcessedStrings(p_strings);
	}
	
	public double train(ProcessedString[] p_psArray, boolean p_verbose)
	{
		m_trainingSet = p_psArray;
		return train(p_verbose);
	}

	//assumes m_trainingSet is valid, returns last log likelihood
	private double train(boolean p_verbose) 
	{
		randomlyInitialize();
		
		double lastLogLik = Double.NaN;
		for(int i = 0; i < MAX_ITER || MAX_ITER <= 0; i++)
		{
			double logLik = bwIteration(p_verbose);
			if(Utilities.isWithinTolerance(logLik, lastLogLik, CON_TOL))
			{
				if(p_verbose)
				{
					System.out.println("Converged");
				}
				break;
			}
			lastLogLik = logLik;
		}
		return lastLogLik;
	}
	
	public double train(String[] p_strings, boolean p_verbose)
	{
		m_trainingSet = m_alphabet.getProcessedStrings(p_strings);
		return train(p_verbose);
 	}
	
	//trains with verbose = true
	public double train(String[] p_strings)
	{
		return train(p_strings, true);
	}

	private double bwIteration(boolean p_verbose) 
	{
		double sum = 0;
		
		LogNum[][][] allGammas = new LogNum[m_trainingSet.length][][];
		LogNum[][][][] allEpsilons = new LogNum[m_trainingSet.length][][][];
		for(int i = 0; i < m_trainingSet.length; i++)
		{
			ProcessedString ps = m_trainingSet[i];
			
			LogNum[][] alphas = getAlphas(ps);
			double logLik = getLogLikelihood(alphas);
			sum += logLik;
			
			LogNum[][] betas = getBetas(ps);
			LogNum[][] gammas = getGammas(alphas, betas);
			LogNum[][][] epsilons = getEpsilons(alphas, betas, ps);
			
			allGammas[i] = gammas;
			allEpsilons[i] = epsilons;
		}
		
		setPriors(allGammas);
		setTransProbs(allGammas, allEpsilons);
		setEmissionProbs(allGammas);
		
		fullSanityCheck();
		
		if(p_verbose)
		{
			System.out.println(sum);
		}
		return sum;
	}
	
	private void fullSanityCheck() 
	{	
		Utilities.checkSumToOne(m_priors);
		
		for(HMMNode node: m_nodes)
		{
			node.sanityCheck();
		}
	}

	private double getLogLikelihood(LogNum[][] p_alphas) 
	{
		LogNum sum = LogNum.ZERO;
		for(int i = 0; i < m_nodes.length; i++)
		{
			sum = LogNum.add(sum, p_alphas[p_alphas.length - 1][i]);
		}
		
		return sum.logValue();
	}

	private void setEmissionProbs(LogNum[][][] allGammas) 
	{
		for(int i = 0; i < m_nodes.length; i++)
		{
			LogNum[] numerators = new LogNum[m_alphabet.size()];
			Arrays.fill(numerators,  LogNum.ZERO);
			
			LogNum denominator = LogNum.ZERO;
			for(int stringIndex = 0; stringIndex < m_trainingSet.length; stringIndex++)
			{
				LogNum[][] gammas = allGammas[stringIndex];
				ProcessedString ps = m_trainingSet[stringIndex];
				for(int t = 0; t < ps.length(); t++)
				{
					LogNum weight = ps.getWeight(t);
					LogNum gamma = gammas[t][i];
					LogNum weightedGamma = LogNum.multiply(weight, gamma);
					denominator = LogNum.add(denominator, weightedGamma);
					
					Integer charID = ps.getID(t);
					numerators[charID] = LogNum.add(numerators[charID], weightedGamma);
				}
			}
			
			HMMNode node = m_nodes[i];
			for(int outputIndex = 0; outputIndex < m_alphabet.size(); outputIndex++)
			{
				HMMEmission he = node.getEmissionByID(outputIndex);
				LogNum newLikelihood = LogNum.divide(numerators[outputIndex], denominator);
				he.setLikelihood(newLikelihood);
			}
		}
	}

	private void setTransProbs(LogNum[][][] p_allGammas, LogNum[][][][] p_allEpsilons) 
	{
		for(int fromNodeIndex = 0; fromNodeIndex < m_nodes.length; fromNodeIndex++)
		{
			for(int toNodeIndex = 0; toNodeIndex < m_nodes.length; toNodeIndex++)
			{
				LogNum numerator = LogNum.ZERO;
				LogNum denominator = LogNum.ZERO;
				for(int stringIndex = 0; stringIndex < m_trainingSet.length; stringIndex++)
				{
					LogNum[][] gammas = p_allGammas[stringIndex];
					LogNum[][][] epsilons = p_allEpsilons[stringIndex];
					ProcessedString ps = m_trainingSet[stringIndex];
					for(int t = 0; t < ps.length() - 1; t++)
					{
						LogNum epsilon = epsilons[t][fromNodeIndex][toNodeIndex];
						LogNum gamma = gammas[t][fromNodeIndex];
						
						numerator = LogNum.add(numerator, epsilon);
						denominator = LogNum.add(denominator, gamma);
					}
				}
				
				HMMNode fromNode = m_nodes[fromNodeIndex];
				HMMNode toNode = m_nodes[toNodeIndex];
				HMMConnection conn = fromNode.getConnectionTo(toNode);
				
				if(numerator.equals(LogNum.ZERO))
				{
					conn.setLikelihood(LogNum.ZERO);
				}
				else
				{
					conn.setLikelihood(LogNum.divide(numerator, denominator));
				}
			}
		}
	}

	private void setPriors(LogNum[][][] allGammas) 
	{
		LogNum denominator = new LogNum(allGammas.length);
		
		for(int i = 0; i < m_nodes.length; i++)
		{
			LogNum numerator = LogNum.ZERO;
			for(int stringIndex = 0; stringIndex < allGammas.length; stringIndex++)
			{
				LogNum[][] gammas = allGammas[stringIndex];
				numerator = LogNum.add(numerator, gammas[0][i]);
			}
			
			m_priors[i] = LogNum.divide(numerator, denominator);
		}
	}

	public void randomlyInitialize()
	{
		for(int i = 0; i < m_nodes.length; i++)
		{
			m_nodes[i] = new HMMNode();
		}
		
		Integer[] alphabetIDs = m_alphabet.getAllIDs();
		for(HMMNode from: m_nodes)
		{
			LogNum[] connProbs = Utilities.randBDSimplex(m_nodes.length);
			for(int i = 0; i < m_nodes.length; i++)
			{
				from.addConnection(m_nodes[i], connProbs[i]);
			}
			
			LogNum[] emProbs = Utilities.randBDSimplex(alphabetIDs.length);
			for(int i = 0; i < alphabetIDs.length; i++)
			{
				from.addEmission(alphabetIDs[i], emProbs[i]);
			}
		}
		
		m_priors = Utilities.randBDSimplex(m_nodes.length);
		
		fullSanityCheck();
	}
	
	public String buildString()
	{
		int startIndex = Utilities.selectSimplexUnitIndex(m_priors);
		HMMNode currentNode = m_nodes[startIndex];
		
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < 140; i++)
		{
			HMMEmission he = currentNode.selectRandomEmission();
			Object c = m_alphabet.lookupID(he.getID());
			if(Alphabet.isTerminator(c))
			{
				break;
			}
			
			sb.append(c);
			currentNode = currentNode.selectRandomConnection().getToNode();
		}
		
		return new String(sb);
	}
	
	/**
	 * The forward algorithm.  The return value is a 2D array of decimals, output index by node index.
	 * Values are the likelihood of control sequence that passes through that index at that time conditional
	 * on all previous (t <= output index) emissions in the string.  For the initial element in the string,
	 * all nodes have forward algorithm values equal to prior probabilities.  
	 * @param p_ps
	 * @return
	 */
	
	private LogNum[][] m_alphasBuffer;
	public LogNum[][] getAlphas(ProcessedString p_ps)
	{
		if(m_alphasBuffer == null || m_alphasBuffer.length != p_ps.length())
		{
			m_alphasBuffer = new LogNum[p_ps.length()][m_nodes.length];
		}
		else
		{
			for(LogNum[] arr: m_alphasBuffer)
			{
				Arrays.fill(arr, null);
			}
		}
		
		for(int thisNodeIndex = 0; thisNodeIndex < m_nodes.length; thisNodeIndex++)
		{
			Integer firstEmissionID = p_ps.getID(0);
			HMMNode node = m_nodes[thisNodeIndex];
			HMMEmission he = node.getEmissionByID(firstEmissionID);
			LogNum emProb = he.getLikelihood();
			LogNum value = LogNum.multiply(m_priors[thisNodeIndex], emProb);
			m_alphasBuffer[0][thisNodeIndex] = value;
		}
		
		for(int t = 1; t < p_ps.length(); t++)
		{
			Integer emissionID = p_ps.getID(t);
			
			for(int thisNodeIndex = 0; thisNodeIndex < m_nodes.length; thisNodeIndex++)
			{
				HMMNode thisNode = m_nodes[thisNodeIndex];
				
				LogNum summation = LogNum.ZERO;
				for(int lastNodeIndex = 0; lastNodeIndex < m_nodes.length; lastNodeIndex++)
				{
					HMMNode lastNode = m_nodes[lastNodeIndex];
					HMMConnection conn = lastNode.getConnectionTo(thisNode);
					LogNum lastNodeProb = m_alphasBuffer[t - 1][lastNodeIndex];
					LogNum transitionProb = conn.getLikelihood();
					summation = LogNum.add(summation, LogNum.multiply(lastNodeProb, transitionProb));
				}
				
				HMMEmission he = thisNode.getEmissionByID(emissionID);
				m_alphasBuffer[t][thisNodeIndex] = LogNum.multiply(summation, he.getLikelihood());
			}
		}
		
		return m_alphasBuffer;
	}
	
	/**
	 * The backward algorithm.  The return value is a 2D array of decimals, output index by node index.
	 * Values are the likelihood of control sequence that passes through that index at that time conditional
	 * on all remaining (t > output index) emissions in the string.  For the final element in the string,
	 * all nodes have backward algorithm values of 1.  
	 * @param p_IDs
	 * @return
	 */
	
	private LogNum[][] m_betasBuffer;
	public LogNum[][] getBetas(ProcessedString p_ps)
	{
		if(m_betasBuffer == null || m_betasBuffer.length != p_ps.length())
		{
			m_betasBuffer = new LogNum[p_ps.length()][m_nodes.length];
		}
		else
		{
			for(LogNum[] arr: m_betasBuffer)
			{
				Arrays.fill(arr, null);
			}
		}
		
		for(int thisNodeIndex = 0; thisNodeIndex < m_nodes.length; thisNodeIndex++)
		{
			m_betasBuffer[p_ps.length() - 1][thisNodeIndex] = LogNum.ONE;
		}
		
		for(int t = p_ps.length() - 2; t >= 0; t--)
		{
			Integer nextID = p_ps.getID(t + 1);
			for(int thisNodeIndex = 0; thisNodeIndex < m_nodes.length; thisNodeIndex++)
			{
				HMMNode thisNode = m_nodes[thisNodeIndex];
				
				LogNum summation = LogNum.ZERO;
				for(int nextNodeIndex = 0; nextNodeIndex < m_nodes.length; nextNodeIndex++)
				{
					HMMNode nextNode = m_nodes[nextNodeIndex];
					HMMConnection conn = thisNode.getConnectionTo(nextNode);
					HMMEmission he = nextNode.getEmissionByID(nextID);
					LogNum emissionProb = he.getLikelihood();
					LogNum nextNodeProb = m_betasBuffer[t + 1][nextNodeIndex];
					LogNum transitionProb = conn.getLikelihood();
					
					LogNum product = LogNum.multiply(emissionProb, LogNum.multiply(nextNodeProb, transitionProb));
					summation = LogNum.add(summation, product);
				}
				
				m_betasBuffer[t][thisNodeIndex] = summation;
			}
		}
		
		return m_betasBuffer;
	}
	
	/**
	 * Returns a 2D double matrix[t][i] that is the probability of being in a state i at a time t.  This is conditional
	 * on the current state of the model and the observation sequence.  
	 * @param alphas
	 * @param betas
	 * @return
	 */
	
	private LogNum[][] m_gammasBuffer;
	public LogNum[][] getGammas(LogNum[][] p_alphas, LogNum[][] p_betas)
	{
		if(m_gammasBuffer == null || m_gammasBuffer.length != p_alphas.length)
		{
			m_gammasBuffer = new LogNum[p_alphas.length][p_alphas[0].length];
		}
		else
		{
			for(LogNum[] arr: m_gammasBuffer)
			{
				Arrays.fill(arr, null);
			}
		}
		
		for(int t = 0; t < m_gammasBuffer.length; t++)
		{
			LogNum runningTotal = LogNum.ZERO;
			for(int i = 0; i < m_nodes.length; i++)
			{
				LogNum numerator = getGammaNumerator(p_alphas, p_betas, t, i);
				runningTotal = LogNum.add(runningTotal, numerator);
			}
			
			for(int i = 0; i < m_nodes.length; i++)
			{
				LogNum numerator = getGammaNumerator(p_alphas, p_betas, t, i);
				m_gammasBuffer[t][i] = LogNum.divide(numerator, runningTotal);
			}
		}
		
		return m_gammasBuffer;
	}
	
	private LogNum getGammaNumerator(LogNum[][] p_alphas, 
			LogNum[][] p_betas, 
			int p_timeIndex, 
			int p_nodeIndex)
	{
		LogNum alpha = p_alphas[p_timeIndex][p_nodeIndex];
		LogNum beta = p_betas[p_timeIndex][p_nodeIndex];
		return LogNum.multiply(alpha, beta);
	}
	
	/**
	 * Returns a 3D double matrix[t][i][j] that gives the probability of being in state j at time t, having 
	 * transitioned from state i.  This is conditional on the observation sequence and the current state of the
	 * model.  
	 * @param p_alphas
	 * @param p_betas
	 * @param p_ps
	 * @return
	 */
	
	LogNum[][][] m_epsilonsBuffer;
	public LogNum[][][] getEpsilons(LogNum[][] p_alphas, LogNum[][] p_betas, ProcessedString p_ps)
	{
		if(m_epsilonsBuffer == null || m_epsilonsBuffer.length != p_ps.length() - 1)
		{
			m_epsilonsBuffer = new LogNum[p_ps.length() - 1][m_nodes.length][m_nodes.length];
		}
		else
		{
			for(LogNum[][] arrarr: m_epsilonsBuffer)
			{
				for(LogNum[] arr: arrarr)
				{
					Arrays.fill(arr, null);
				}
			}
		}
		
		for(int t = 0; t < m_epsilonsBuffer.length; t++)
		{
			LogNum runningTotal = LogNum.ZERO;
			for(int i = 0; i < m_nodes.length; i++)
			{
				for(int j = 0; j < m_nodes.length; j++)
				{
					LogNum numerator = getEpsilonNumerator(p_alphas, p_betas, t, i, j, p_ps);
					runningTotal = LogNum.add(runningTotal, numerator);
				}
			}
			
			for(int i = 0; i < m_nodes.length; i++)
			{
				for(int j = 0; j < m_nodes.length; j++)
				{
					LogNum numerator = getEpsilonNumerator(p_alphas, p_betas, t, i, j, p_ps);
					m_epsilonsBuffer[t][i][j] = LogNum.divide(numerator, runningTotal);
				}
			}
		}
		
		return m_epsilonsBuffer;
	}
	
	private LogNum getEpsilonNumerator(LogNum[][] alphas, 
			LogNum[][] betas, 
			int p_timeIndex, 
			int p_fromNodeIndex, 
			int p_toNodeIndex, 
			ProcessedString p_ps)
	{
		LogNum alphaCurrent = alphas[p_timeIndex][p_fromNodeIndex];
		
		HMMNode from = m_nodes[p_fromNodeIndex];
		HMMNode to = m_nodes[p_toNodeIndex];
		HMMConnection conn = from.getConnectionTo(to);
		LogNum transProb = conn.getLikelihood();
		
		LogNum betaNext = betas[p_timeIndex + 1][p_toNodeIndex];
		
		Integer nextEmissionID = p_ps.getID(p_timeIndex + 1);
		HMMEmission em = to.getEmissionByID(nextEmissionID);
		LogNum emProb = em.getLikelihood();
		
		LogNum productOne = LogNum.multiply(alphaCurrent, transProb);
		LogNum productTwo = LogNum.multiply(emProb, betaNext);
		
		LogNum numerator = LogNum.multiply(productOne, productTwo);
		return numerator;
	}
}

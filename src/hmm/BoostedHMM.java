package hmm;

public class BoostedHMM 
{
	private YokedQHMMSet m_yokedSet;
	private Alphabet m_alphabet;
	private int m_numStumps;
	private int m_nodesPerStump;
	private ProcessedString[] m_trainingSet;
	private double m_gradientMultiplier;
	
	public static void main(String[] p_args)
	{
		String[] strings = DataLoader.loadStrings(50);
		BoostedHMM bh = new BoostedHMM(16, 5, strings, .01, AlphabetType.CHAR);
		bh.train(true);
	}
	
	public BoostedHMM(int p_numStumps, int p_nodesPerStump, String[] p_trainingSet, double p_gradientMultiplier, AlphabetType p_type)
	{
		if(p_numStumps < 1 || p_nodesPerStump < 1 || p_trainingSet == null || p_trainingSet.length == 0)
		{
			throw new RuntimeException();
		}
		
		m_yokedSet = new YokedQHMMSet();
		m_numStumps = p_numStumps;
		m_nodesPerStump = p_nodesPerStump;
		m_alphabet = AlphabetFactory.newAlphabet(p_type);
		m_trainingSet = m_alphabet.getProcessedStrings(p_trainingSet);
		m_gradientMultiplier = p_gradientMultiplier;
	}
	
	public void train(boolean p_verbose)
	{
		for(int i = 0; i < m_numStumps; i++)
		{
			System.out.println("training stump " + i);
			trainOnce(p_verbose);
		}
		
		for(int i = 0; i < 20; i++)
		{
			System.out.println(m_yokedSet.buildString());
		}
	}

	private void trainOnce(boolean p_verbose) 
	{
		trainAndAddStump(p_verbose);
		resetWeights();
	}

	private void trainAndAddStump(boolean p_verbose) 
	{
		HMM stumpBase = new HMM(m_nodesPerStump, m_alphabet);
		stumpBase.train(m_trainingSet, p_verbose);
		
		QHMM stump = new QHMM(stumpBase);
		double weight = calculateStumpWeight(stump);
		
		stump.setWeight(weight);
		m_yokedSet.addQHMM(stump);
	}

	private double calculateStumpWeight(QHMM stump) 
	{
		YokedQHMMSet testSet = new YokedQHMMSet();
		testSet.addQHMM(stump);
		double PPLL = testSet.getPOLL(m_trainingSet);
		System.out.println("Stump PPLL: " + PPLL);
		double weight = Math.exp(PPLL);
		return weight;
	}
	
	private void resetWeights()
	{
		//[stringIndex][timeIndex]
		double[][] allLogLiks = new double[m_trainingSet.length][];
		for(int stringIndex = 0; stringIndex < m_trainingSet.length; stringIndex++)
		{
			double[] logLiks = m_yokedSet.getLogLikelihoods(m_trainingSet[stringIndex]);
			allLogLiks[stringIndex] = logLiks;
		}
		double gMeanLog = Utilities.average(allLogLiks);
		//double logLikTotal = Utilities.sum(allLogLiks);
		System.out.println("Yoked PPLL: " + m_yokedSet.getPOLL(m_trainingSet));
		
		for(int stringIndex = 0; stringIndex < m_trainingSet.length; stringIndex++)
		{
			double[] logLiks = allLogLiks[stringIndex];
			ProcessedString ps = m_trainingSet[stringIndex];
			for(int timeIndex = 0; timeIndex < logLiks.length; timeIndex++)
			{
				double logLik = logLiks[timeIndex];
				double newWeightLog = (gMeanLog - logLik)*m_gradientMultiplier;
				ps.setWeight(timeIndex, LogNum.newFromLog(newWeightLog));
			}
		}
	}
}
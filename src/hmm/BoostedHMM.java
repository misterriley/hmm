package hmm;

public class BoostedHMM
{
	public static void main(final String[] p_args)
	{
		final String[] strings = DataLoader.loadStrings(50);
		final BoostedHMM bh = new BoostedHMM(16, 5, strings, .01, AlphabetType.CHAR);
		bh.train(true);
	}

	private final YokedQHMMSet		m_yokedSet;
	private final Alphabet			m_alphabet;
	private final int				m_numStumps;
	private final int				m_nodesPerStump;
	private final ProcessedString[]	m_trainingSet;

	private final double m_gradientMultiplier;

	public BoostedHMM(
		final int p_numStumps,
		final int p_nodesPerStump,
		final String[] p_trainingSet,
		final double p_gradientMultiplier,
		final AlphabetType p_type)
	{
		if (p_numStumps < 1 || p_nodesPerStump < 1 || p_trainingSet == null || p_trainingSet.length == 0)
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

	public void train(final boolean p_verbose)
	{
		for (int i = 0; i < m_numStumps; i++)
		{
			System.out.println("training stump " + i);
			trainOnce(p_verbose);
		}

		for (int i = 0; i < 20; i++)
		{
			System.out.println(m_yokedSet.buildString());
		}
	}

	private double calculateStumpWeight(final QHMM stump)
	{
		final YokedQHMMSet testSet = new YokedQHMMSet();
		testSet.addQHMM(stump);
		final double PPLL = testSet.getPOLL(m_trainingSet);
		System.out.println("Stump PPLL: " + PPLL);
		final double weight = Math.exp(PPLL);
		return weight;
	}

	private void resetWeights()
	{
		//[stringIndex][timeIndex]
		final double[][] allLogLiks = new double[m_trainingSet.length][];
		for (int stringIndex = 0; stringIndex < m_trainingSet.length; stringIndex++)
		{
			final double[] logLiks = m_yokedSet.getLogLikelihoods(m_trainingSet[stringIndex]);
			allLogLiks[stringIndex] = logLiks;
		}
		final double gMeanLog = Utilities.average(allLogLiks);
		//double logLikTotal = Utilities.sum(allLogLiks);
		System.out.println("Yoked PPLL: " + m_yokedSet.getPOLL(m_trainingSet));

		for (int stringIndex = 0; stringIndex < m_trainingSet.length; stringIndex++)
		{
			final double[] logLiks = allLogLiks[stringIndex];
			final ProcessedString ps = m_trainingSet[stringIndex];
			for (int timeIndex = 0; timeIndex < logLiks.length; timeIndex++)
			{
				final double logLik = logLiks[timeIndex];
				final double newWeightLog = (gMeanLog - logLik) * m_gradientMultiplier;
				ps.setWeight(timeIndex, LogNum.newFromLog(newWeightLog));
			}
		}
	}

	private void trainAndAddStump(final boolean p_verbose)
	{
		final HMM stumpBase = new HMM(m_nodesPerStump, m_alphabet);
		stumpBase.train(m_trainingSet, p_verbose);

		final QHMM stump = new QHMM(stumpBase);
		final double weight = calculateStumpWeight(stump);

		stump.setWeight(weight);
		m_yokedSet.addQHMM(stump);
	}

	private void trainOnce(final boolean p_verbose)
	{
		trainAndAddStump(p_verbose);
		resetWeights();
	}
}
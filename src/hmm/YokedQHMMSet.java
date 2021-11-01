package hmm;

import java.util.ArrayList;
import java.util.Arrays;

public class YokedQHMMSet
{
	public static void main(final String[] p_args)
	{
		final String[] strings = DataLoader.loadStrings(50);
		final YokedQHMMSet weightedYokedSet = new YokedQHMMSet();
		final YokedQHMMSet unweightedYokedSet = new YokedQHMMSet();
		final Alphabet a = AlphabetFactory.newAlphabet(AlphabetType.CHAR);
		final ProcessedString[] psArray = a.getProcessedStrings(strings);

		for (int i = 0; i < 1; i++)
		{
			System.out.println("training HMM " + i);
			final HMM hmm = new HMM(60, a);
			hmm.train(psArray, true);

			final QHMM weightedQhmm = new QHMM(hmm);
			final QHMM unweightedQhmm = new QHMM(hmm);
			final YokedQHMMSet singleton = new YokedQHMMSet();
			singleton.addQHMM(unweightedQhmm);
			final double ppll = singleton.getPOLL(psArray);
			final double weight = Math.exp(ppll);
			weightedQhmm.setWeight(weight);

			weightedYokedSet.addQHMM(weightedQhmm);
			unweightedYokedSet.addQHMM(unweightedQhmm);

			System.out.println("PPLL: " + ppll);
			System.out.println("Weighted Yoked POLL: " + weightedYokedSet.getPOLL(psArray));
			System.out.println("Unweighted Yoked POLL: " + unweightedYokedSet.getPOLL(psArray));
		}

		//yokedSet.printRandomStrings(20);
	}

	ArrayList<QHMM> m_qhmms;

	public YokedQHMMSet()
	{
		m_qhmms = new ArrayList<>();
	}

	public void addQHMM(final QHMM p_QHMM)
	{
		m_qhmms.add(p_QHMM);
	}

	public String buildString()
	{
		final StringBuilder sb = new StringBuilder();

		//[qIndex][outputIndex]
		final LogNum[][] allOutputProbs = new LogNum[m_qhmms.size()][];
		for (int j = 0; j < 140; j++)
		{
			for (int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
			{
				allOutputProbs[qIndex] = m_qhmms.get(qIndex).getNextOutputProbs();
			}

			final LogNum[] simplex = buildSimplex(allOutputProbs);

			final int outputIndex = Utilities.selectSimplexUnitIndex(simplex);
			final Object c = m_qhmms.get(0).getAlphabet().lookupID(outputIndex);
			if (Alphabet.isTerminator(c))
			{
				break;
			}

			sb.append(c);

			for (final QHMM m_qhmm : m_qhmms)
			{
				m_qhmm.updateStateBasedOnOutput(outputIndex);
			}
		}

		return new String(sb);
	}

	public LogNum getLikelihood(final ProcessedString p_ps)
	{
		final LogNum[] likelihoods = getLikelihoods(p_ps);

		LogNum ret = LogNum.ONE;
		for (final LogNum likelihood : likelihoods)
		{
			ret = LogNum.multiply(ret, likelihood);
		}

		return ret;
	}

	public LogNum getLikelihood(final String p_string)
	{
		return getLikelihood(getProcessedString(p_string));
	}

	public LogNum[] getLikelihoods(final ProcessedString p_ps)
	{
		final LogNum[] ret = new LogNum[p_ps.length()];

		final LogNum[][] allOutputProbs = new LogNum[m_qhmms.size()][];
		for (int charIndex = 0; charIndex < p_ps.length(); charIndex++)
		{
			for (int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
			{
				allOutputProbs[qIndex] = m_qhmms.get(qIndex).getNextOutputProbs();
			}

			final LogNum[] simplex = buildSimplex(allOutputProbs);
			final int outputIndex = p_ps.getID(charIndex);
			ret[charIndex] = simplex[outputIndex];

			for (final QHMM m_qhmm : m_qhmms)
			{
				m_qhmm.updateStateBasedOnOutput(outputIndex);
			}
		}

		return ret;
	}

	public LogNum[] getLikelihoods(final String p_string)
	{
		final ProcessedString ps = getProcessedString(p_string);
		return getLikelihoods(ps);
	}

	public double getLogLikelihood(final ProcessedString p_ps)
	{
		final LogNum likelihood = getLikelihood(p_ps);
		return likelihood.logValue();
	}

	public double getLogLikelihood(final ProcessedString[] p_psArray)
	{
		double ret = 0;
		for (final ProcessedString ps : p_psArray)
		{
			ret += getLogLikelihood(ps);
		}
		return ret;
	}

	public double getLogLikelihood(final String p_string)
	{
		return getLogLikelihood(getProcessedString(p_string));
	}

	public double[] getLogLikelihoods(final ProcessedString p_ps)
	{
		final LogNum[] likelihoods = getLikelihoods(p_ps);
		final double[] ret = new double[likelihoods.length];
		for (int i = 0; i < likelihoods.length; i++)
		{
			ret[i] = likelihoods[i].logValue();
		}
		return ret;
	}

	public double[] getLogLikelihoods(final String p_string)
	{
		return getLogLikelihoods(getProcessedString(p_string));
	}

	//per observation log likelihood - equals log(geometric mean)
	public double getPOLL(final ProcessedString[] p_psArray)
	{
		double logLikelihood = 0;
		int size = 0;
		for (final ProcessedString ps : p_psArray)
		{
			final double[] logLikelihoods = getLogLikelihoods(ps);
			for (final double d : logLikelihoods)
			{
				if (!Double.isInfinite(d))
				{
					size++;
					logLikelihood += d;
				}
			}
		}
		return logLikelihood / size;
	}

	public int size()
	{
		return m_qhmms.size();
	}

	private LogNum[] buildSimplex(final LogNum[][] p_allOutputProbs)
	{
		final double weightSum = getQHMMWeightSum();
		final int numLetters = m_qhmms.get(0).getAlphabet().size();
		final LogNum[] ret = new LogNum[numLetters];
		Arrays.fill(ret, LogNum.ONE);

		for (int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
		{
			final double qhmmWeight = m_qhmms.get(qIndex).getWeight();
			for (int outputIndex = 0; outputIndex < numLetters; outputIndex++)
			{
				final LogNum outputProb = p_allOutputProbs[qIndex][outputIndex];
				final LogNum root = LogNum.pow(outputProb, qhmmWeight / weightSum);
				ret[outputIndex] = LogNum.multiply(ret[outputIndex], root);
			}
		}

		LogNum sum = LogNum.ZERO;
		for (int outputIndex = 0; outputIndex < numLetters; outputIndex++)
		{
			sum = LogNum.add(sum, ret[outputIndex]);
		}

		for (int outputIndex = 0; outputIndex < numLetters; outputIndex++)
		{
			if (sum.isZero())
			{
				ret[outputIndex] = new LogNum(1.0 / numLetters);
			}
			else
			{
				ret[outputIndex] = LogNum.divide(ret[outputIndex], sum);
			}
		}

		return ret;
	}

	private ProcessedString getProcessedString(final String p_string)
	{
		return m_qhmms.get(0).getAlphabet().getProcessedString(p_string);
	}

	private double getQHMMWeightSum()
	{
		double ret = 0;
		for (final QHMM qhmm : m_qhmms)
		{
			ret += qhmm.getWeight();
		}
		return ret;
	}

	@SuppressWarnings("unused")
	private void printRandomStrings(final int p_numStrings)
	{
		for (int i = 0; i < p_numStrings; i++)
		{
			System.out.println(buildString());
		}
	}
}

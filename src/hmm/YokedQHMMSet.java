package hmm;

import java.util.ArrayList;
import java.util.Arrays;

public class YokedQHMMSet 
{
	ArrayList<QHMM> m_qhmms;
	
	public static void main(String[] p_args)
	{
		String[] strings = DataLoader.loadStrings(50);
		YokedQHMMSet weightedYokedSet = new YokedQHMMSet();
		YokedQHMMSet unweightedYokedSet = new YokedQHMMSet();
		Alphabet a = AlphabetFactory.newAlphabet(AlphabetType.CHAR);
		ProcessedString[] psArray = a.getProcessedStrings(strings);
		
		for(int i = 0; i < 1; i++)
		{
			System.out.println("training HMM " + i);
			HMM hmm = new HMM(60, a);
			hmm.train(psArray, true);
			
			QHMM weightedQhmm = new QHMM(hmm);
			QHMM unweightedQhmm = new QHMM(hmm);
			YokedQHMMSet singleton = new YokedQHMMSet();
			singleton.addQHMM(unweightedQhmm);
			double ppll = singleton.getPOLL(psArray);
			double weight = Math.exp(ppll);
			weightedQhmm.setWeight(weight);
			
			weightedYokedSet.addQHMM(weightedQhmm);
			unweightedYokedSet.addQHMM(unweightedQhmm);
			
			System.out.println("PPLL: " + ppll);
			System.out.println("Weighted Yoked POLL: " + weightedYokedSet.getPOLL(psArray));
			System.out.println("Unweighted Yoked POLL: " + unweightedYokedSet.getPOLL(psArray));
		}
		
		//yokedSet.printRandomStrings(20);
	}
	
	@SuppressWarnings("unused")
	private void printRandomStrings(int p_numStrings) 
	{
		for(int i = 0; i < p_numStrings; i++)
		{
			System.out.println(buildString());
		}
	}

	public YokedQHMMSet()
	{
		m_qhmms = new ArrayList<QHMM>();
	}
	
	public int size()
	{
		return m_qhmms.size();
	}
	
	public LogNum[] getLikelihoods(ProcessedString p_ps)
	{
		LogNum[] ret = new LogNum[p_ps.length()];
		
		LogNum[][] allOutputProbs = new LogNum[m_qhmms.size()][];
		for(int charIndex = 0; charIndex < p_ps.length(); charIndex++)
		{
			for(int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
			{
				allOutputProbs[qIndex] = m_qhmms.get(qIndex).getNextOutputProbs();
			}
			
			LogNum[] simplex = buildSimplex(allOutputProbs);
			int outputIndex = p_ps.getID(charIndex);
			ret[charIndex] = simplex[outputIndex];
			
			for(int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
			{
				m_qhmms.get(qIndex).updateStateBasedOnOutput(outputIndex);
			}
		}
		
		return ret;
	}
	
	//per observation log likelihood - equals log(geometric mean)
	public double getPOLL(ProcessedString[] p_psArray)
	{
		double logLikelihood = 0;
		int size = 0;
		for(ProcessedString ps: p_psArray)
		{
			double[] logLikelihoods = getLogLikelihoods(ps);
			for(double d: logLikelihoods)
			{
				if(!Double.isInfinite(d))
				{
					size++;
					logLikelihood += d;
				}
			}
		}
		return logLikelihood/size;
	}
	
	public LogNum[] getLikelihoods(String p_string)
	{
		ProcessedString ps = getProcessedString(p_string);
		return getLikelihoods(ps);
	}
	
	private ProcessedString getProcessedString(String p_string)
	{
		return m_qhmms.get(0).getAlphabet().getProcessedString(p_string);
	}
	
	public double getLogLikelihood(ProcessedString[] p_psArray)
	{
		double ret = 0;
		for(ProcessedString ps: p_psArray)
		{
			ret += getLogLikelihood(ps);
		}
		return ret;
	}
	
	public LogNum getLikelihood(ProcessedString p_ps)
	{
		LogNum[] likelihoods = getLikelihoods(p_ps);
		
		LogNum ret = LogNum.ONE;
		for(int i = 0; i < likelihoods.length; i++)
		{
			ret = LogNum.multiply(ret, likelihoods[i]);
		}
		
		return ret;
	}
	
	public LogNum getLikelihood(String p_string)
	{
		return getLikelihood(getProcessedString(p_string));
	}
	
	public double[] getLogLikelihoods(ProcessedString p_ps)
	{
		LogNum[] likelihoods = getLikelihoods(p_ps);
		double[] ret = new double[likelihoods.length];
		for(int i = 0; i < likelihoods.length; i++)
		{
			ret[i] = likelihoods[i].logValue();
		}
		return ret;
	}
	
	public double[] getLogLikelihoods(String p_string)
	{
		return getLogLikelihoods(getProcessedString(p_string));
	}
	
	public double getLogLikelihood(ProcessedString p_ps)
	{
		LogNum likelihood = getLikelihood(p_ps);
		return likelihood.logValue();
	}
	
	public double getLogLikelihood(String p_string)
	{
		return getLogLikelihood(getProcessedString(p_string));
	}
	
	public void addQHMM(QHMM p_QHMM)
	{
		m_qhmms.add(p_QHMM);
	}
	
	private double getQHMMWeightSum()
	{
		double ret = 0;
		for(QHMM qhmm: m_qhmms)
		{
			ret += qhmm.getWeight();
		}
		return ret;
	}
	
	public String buildString()
	{
		StringBuilder sb = new StringBuilder();
		
		//[qIndex][outputIndex]
		LogNum[][] allOutputProbs = new LogNum[m_qhmms.size()][];
		for(int j = 0; j < 140; j++)
		{
			for(int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
			{
				allOutputProbs[qIndex] = m_qhmms.get(qIndex).getNextOutputProbs();
			}
			
			LogNum[] simplex = buildSimplex(allOutputProbs);
			
			int outputIndex = Utilities.selectSimplexUnitIndex(simplex);
			Object c = m_qhmms.get(0).getAlphabet().lookupID(outputIndex);
			if(Alphabet.isTerminator(c))
			{
				break;
			}
			
			sb.append(c);
			
			for(int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
			{
				m_qhmms.get(qIndex).updateStateBasedOnOutput(outputIndex);
			}
		}
		
		return new String(sb);
	}
	
	private LogNum[] buildSimplex(LogNum[][] p_allOutputProbs)
	{	
		double weightSum = getQHMMWeightSum();
		int numLetters = m_qhmms.get(0).getAlphabet().size();
		LogNum[] ret = new LogNum[numLetters];
		Arrays.fill(ret, LogNum.ONE);
		
		for(int qIndex = 0; qIndex < m_qhmms.size(); qIndex++)
		{
			double qhmmWeight = m_qhmms.get(qIndex).getWeight();
			for(int outputIndex = 0; outputIndex < numLetters; outputIndex++)
			{
				LogNum outputProb = p_allOutputProbs[qIndex][outputIndex];
				LogNum root = LogNum.pow(outputProb, qhmmWeight/weightSum);
				ret[outputIndex] = LogNum.multiply(ret[outputIndex], root);
			}
		}
		
		LogNum sum = LogNum.ZERO;
		for(int outputIndex = 0; outputIndex < numLetters; outputIndex++)
		{
			sum = LogNum.add(sum, ret[outputIndex]);
		}
		
		for(int outputIndex = 0; outputIndex < numLetters; outputIndex++)
		{
			if(sum.isZero())
			{
				ret[outputIndex] = new LogNum(1.0/numLetters);
			}
			else
			{
				ret[outputIndex] = LogNum.divide(ret[outputIndex], sum);
			}
		}
		
		return ret;
	}
}

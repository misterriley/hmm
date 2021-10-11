package hmm;

public class LogNum 
{
	public static LogNum ONE = new LogNum(1);
	public static LogNum ZERO;
	
	private double m_logValue;
	
	static
	{
		ZERO = new LogNum();
		ZERO.m_logValue = Double.NEGATIVE_INFINITY;
	}
	
	public static LogNum newFromLog(double p_log)
	{
		LogNum ret = new LogNum();
		ret.setLogValue(p_log);
		return ret;
	}
	
	private void setLogValue(double p_log)
	{
		if(Double.isInfinite(p_log))
		{
			throw new RuntimeException();
		}
		m_logValue = p_log;
	}
	
	public LogNum(double p_value)
	{
		if(p_value <= 0)
		{
			throw new RuntimeException();
		}
		
		setLogValue( Math.log(p_value));
	}
	
	private LogNum()
	{
		
	}
	
	public double logValue()
	{
		return m_logValue;
	}

	public double doubleValue()
	{
		return Math.exp(m_logValue);
	}
	
	public boolean isZero()
	{
		return this == ZERO;
	}
	
	public static LogNum add(LogNum p_a, LogNum p_b)
	{
		if(p_a.isZero())
		{
			return p_b;
		}
		
		if(p_b.isZero())
		{
			return p_a;
		}
		
		double min = Math.min(p_a.m_logValue, p_b.m_logValue);
		double max = Math.max(p_a.m_logValue, p_b.m_logValue);
		
		double exp = Math.exp(min - max);
		LogNum ret = new LogNum();
		ret.setLogValue(max + (Math.log(exp + 1)));
		return ret;
	}
	
	public String toString()
	{
		return Double.toString(doubleValue());
	}
	
	public static LogNum multiply(LogNum p_a, LogNum p_b)
	{
		if(p_a.isZero() || p_b.isZero())
		{
			return ZERO;
		}
		
		LogNum ret = new LogNum();
		ret.setLogValue(p_a.m_logValue + p_b.m_logValue);
		
		if(Double.isNaN(ret.doubleValue()))
		{
			throw new RuntimeException();
		}
		
		return ret;
	}
	
	public static LogNum divide(LogNum p_a, LogNum p_b)
	{
		if(p_a.isZero() && !p_b.isZero())
		{
			return ZERO;
		}
		
		if(p_b.isZero())
		{
			throw new RuntimeException();
		}
		
		LogNum ret = new LogNum();
		ret.setLogValue(p_a.m_logValue - p_b.m_logValue);
		return ret;
	}
	
	public static LogNum pow(LogNum p_a, double p_exp)
	{
		if(p_a.isZero())
		{
			return ZERO;
		}
		
		LogNum ret = new LogNum();
		ret.setLogValue(p_a.m_logValue * p_exp);
		return ret;
	}
	
	public int compareTo(LogNum p_other)
	{
		if(m_logValue > p_other.m_logValue)
		{
			return 1;
		}
		
		if(m_logValue < p_other.m_logValue)
		{
			return -1;
		}
		
		return 0;
	}
}

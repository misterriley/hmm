package hmm;

public class LogNum
{
	public static LogNum	ONE	= new LogNum(1);
	public static LogNum	ZERO;

	static
	{
		ZERO = new LogNum();
		ZERO.m_logValue = Double.NEGATIVE_INFINITY;
	}

	public static LogNum add(final LogNum p_a, final LogNum p_b)
	{
		if (p_a.isZero())
		{
			return p_b;
		}

		if (p_b.isZero())
		{
			return p_a;
		}

		final double min = Math.min(p_a.m_logValue, p_b.m_logValue);
		final double max = Math.max(p_a.m_logValue, p_b.m_logValue);

		final double exp = Math.exp(min - max);
		final LogNum ret = new LogNum();
		ret.setLogValue(max + Math.log(exp + 1));
		return ret;
	}

	public static LogNum divide(final LogNum p_a, final LogNum p_b)
	{
		if (p_a.isZero() && !p_b.isZero())
		{
			return ZERO;
		}

		if (p_b.isZero())
		{
			throw new RuntimeException();
		}

		final LogNum ret = new LogNum();
		ret.setLogValue(p_a.m_logValue - p_b.m_logValue);
		return ret;
	}

	public static LogNum multiply(final LogNum p_a, final LogNum p_b)
	{
		if (p_a.isZero() || p_b.isZero())
		{
			return ZERO;
		}

		final LogNum ret = new LogNum();
		ret.setLogValue(p_a.m_logValue + p_b.m_logValue);

		if (Double.isNaN(ret.doubleValue()))
		{
			throw new RuntimeException();
		}

		return ret;
	}

	public static LogNum newFromLog(final double p_log)
	{
		final LogNum ret = new LogNum();
		ret.setLogValue(p_log);
		return ret;
	}

	public static LogNum pow(final LogNum p_a, final double p_exp)
	{
		if (p_a.isZero())
		{
			return ZERO;
		}

		final LogNum ret = new LogNum();
		ret.setLogValue(p_a.m_logValue * p_exp);
		return ret;
	}

	private double m_logValue;

	public LogNum(final double p_value)
	{
		if (p_value <= 0)
		{
			throw new RuntimeException();
		}

		setLogValue(Math.log(p_value));
	}

	private LogNum()
	{

	}

	public int compareTo(final LogNum p_other)
	{
		if (m_logValue > p_other.m_logValue)
		{
			return 1;
		}

		if (m_logValue < p_other.m_logValue)
		{
			return -1;
		}

		return 0;
	}

	public double doubleValue()
	{
		return Math.exp(m_logValue);
	}

	public boolean isZero()
	{
		return this == ZERO;
	}

	public double logValue()
	{
		return m_logValue;
	}

	@Override
	public String toString()
	{
		return Double.toString(doubleValue());
	}

	private void setLogValue(final double p_log)
	{
		if (Double.isInfinite(p_log))
		{
			throw new RuntimeException();
		}
		m_logValue = p_log;
	}
}

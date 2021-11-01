package hmm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class DataLoader
{
	private static final String DATA_DIR = "data";

	public static final String[] loadStrings(final int p_max)
	{
		final File dir = new File(DATA_DIR);
		final ArrayList<String> ret = new ArrayList<>();

		final File[] files = dir.listFiles();
		for (final File file : files)
		{
			if (!file.getName().toLowerCase().endsWith(".json"))
			{
				continue;
			}

			loadStringsFromOneFile(file, ret, p_max);
			if (ret.size() == p_max)
			{
				break;
			}
		}

		return ret.toArray(new String[0]);
	}

	public static void main(final String[] p_args)
	{
		loadStrings(100);
	}

	private static final void loadStringsFromOneFile(final File p_file, final ArrayList<String> p_list, final int p_max)
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(p_file);
			final JSONTokener jt = new JSONTokener(fis);
			while (true)
			{
				final JSONArray ja = (JSONArray) jt.nextValue();
				if (ja == null)
				{
					break;
				}

				for (int i = 0; i < ja.length(); i++)
				{
					final JSONObject jo = (JSONObject) ja.get(i);
					p_list.add(jo.getString("text"));

					if (p_list.size() == p_max)
					{
						break;
					}
				}

				if (p_list.size() == p_max)
				{
					break;
				}
			}
		}
		catch (final FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (final JSONException e)
		{
			//borked files will throw this - who cares?
		}
		finally
		{
			try
			{
				fis.close();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}

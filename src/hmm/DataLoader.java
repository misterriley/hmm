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
	
	public static void main(String[] p_args)
	{
		loadStrings(100);
	}
	
	public static final String[] loadStrings(int p_max)
	{
		File dir = new File(DATA_DIR);
		ArrayList<String> ret = new ArrayList<String>();

		File[] files = dir.listFiles();
		for(File file: files)
		{
			if(!file.getName().toLowerCase().endsWith(".json"))
			{
				continue;
			}
			
			loadStringsFromOneFile(file, ret, p_max);
			if(ret.size() == p_max)
			{
				break;
			}
		}
		
		return ret.toArray(new String[0]);
	}
	
	private static final void loadStringsFromOneFile(File p_file, ArrayList<String> p_list, int p_max)
	{
		FileInputStream fis = null;
		try 
		{
			fis = new FileInputStream(p_file);
			JSONTokener jt = new JSONTokener(fis);
			while(true)
			{
				JSONArray ja = (JSONArray)jt.nextValue();
				if(ja == null)
				{
					break;
				}
				
				for(int i = 0; i < ja.length(); i++)
				{
					JSONObject jo = (JSONObject) ja.get(i);
					p_list.add(jo.getString("text"));
					
					if(p_list.size() == p_max)
					{
						break;
					}
				}
				
				if(p_list.size() == p_max)
				{
					break;
				}
			}
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (JSONException e) 
		{
			//borked files will throw this - who cares?
		}
		finally
		{
			try 
			{
				fis.close();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
}

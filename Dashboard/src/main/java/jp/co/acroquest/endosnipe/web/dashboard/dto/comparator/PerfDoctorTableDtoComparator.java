package jp.co.acroquest.endosnipe.web.dashboard.dto.comparator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import jp.co.acroquest.endosnipe.web.dashboard.dto.PerfDoctorTableDto;


public class PerfDoctorTableDtoComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		PerfDoctorTableDto dto1 = (PerfDoctorTableDto) o1;
		PerfDoctorTableDto dto2 = (PerfDoctorTableDto) o2;
		Date date1 = null;
		Date date2 = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		try
		{
			date1 = sdf.parse(dto1.getDate());
			date2 = sdf.parse(dto2.getDate());

			if (date1.before(date2))
			{
				return 1;
			}
			else if (date1.after(date2))
			{
				return -1;
			}
			else
			{
				return dto1.getDescription().compareTo(dto2.getDescription());
			}
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
		return 0;
	}
}

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.omg.CORBA.*;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.CosNotification.EventBatchHolder;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.StructuredEvent;
import com.ericsson.irp.AlarmIRPSystem.GetAlarmList;
import com.ericsson.irp.AlarmIRPSystem.InvalidParameter;
import com.ericsson.irp.AlarmIRPSystem.NextAlarmInformations;
import com.ericsson.irp.AlarmIRPSystem.ParameterNotSupported;


public class AlarmClient {

	private com.ericsson.irp.AlarmIRPSystem._AlarmIRPOperations _alarmIrp = null;
	
	public static void main(String[] args) {
		AlarmClient ac = new AlarmClient();
		ac.createAlarmObj();
		ac.getActiveAlarms();
	}
	
	private String readIOR() {
        File f = new File("/var/opt/ericsson/blkcm/data/bulkcm.nameservice");
        BufferedReader br;
        String iorContents = null;
		try {
			br = new BufferedReader(new FileReader(f));
	        iorContents = br.readLine();
	        br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return iorContents;
	}
	
	public void createAlarmObj(){
		org.omg.CORBA.Object rootObj = null;
		NamingContextExt rootNameCon = null;
		Properties props = new Properties();
		props.put("org.omg.CORBA.ORBInitRef", "NameService=" + readIOR());
		org.omg.CORBA.ORB orb = ORB.init(new String[0], props);
		// Resolve the CORBA Naming Service 
		try {
			rootObj = orb.resolve_initial_references("NameService");
			rootNameCon = NamingContextExtHelper.narrow(rootObj);
			String s = "com/ericsson/nms/fm_cirpagent/AlarmIRP";
			//Locate Alarm IRP
			rootObj = rootNameCon.resolve(rootNameCon.to_name(s));
			_alarmIrp = com.ericsson.irp.AlarmIRPSystem._AlarmIRPOperationsHelper.narrow(rootObj);
			//System.out.println(_alarmIrp);
		} catch (InvalidName | NotFound | CannotProceed | org.omg.CosNaming.NamingContextPackage.InvalidName e) {
			e.printStackTrace();
		}
	}

	public void getActiveAlarms(){
		BooleanHolder flag = new BooleanHolder(false);  // false for iteration
		com.ericsson.irp.AlarmIRPSystem.AlarmInformationIteratorHolder iter = new com.ericsson.irp.AlarmIRPSystem.AlarmInformationIteratorHolder();
		try {
			_alarmIrp.get_alarm_list("", flag, iter);
			EventBatchHolder alarmInformation = new EventBatchHolder();
			short alarmSize = 100;
			List<StructuredEvent> alarms = new ArrayList<StructuredEvent>();
			boolean haveMoreAlarms = false;
			do{
				if (iter.value != null) {
					haveMoreAlarms = iter.value.next_alarmInformations(alarmSize, alarmInformation);
					alarms.addAll(Arrays.asList(alarmInformation.value));
				}
			}while (haveMoreAlarms);
			if (iter.value != null) {
				for (StructuredEvent alarm: alarms) {
					alarmPrint(alarm);
				}
			}
		} catch (GetAlarmList | ParameterNotSupported | InvalidParameter | NextAlarmInformations e) {
			e.printStackTrace();
		}
	}
	
	private void alarmPrint(StructuredEvent alarm){
		String result = "";
		if (alarm.filterable_data != null) {
			for (Property filterableData: alarm.filterable_data) {
				String fieldName = filterableData.name;
				switch (fieldName){
					case "f":
						result = result + filterableData.value.extract_string() + ";";
						break;
					case "i":
						result = result + filterableData.value.extract_string();
						break;
				}
			}
		}
		System.out.println(result);
	}
}
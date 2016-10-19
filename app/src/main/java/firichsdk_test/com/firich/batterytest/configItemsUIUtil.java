package firichsdk_test.com.firich.batterytest;

import android.util.Log;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.StringWriter;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by brianyeh on  2016/10/19.
 */
public class configItemsUIUtil {
    private boolean bDebugOn = true;
    String strTagUtil = "configItemsUIUtil.";

    private String fectest_config_path = "/data/fec_config/battery_test_config.xml";
    private boolean parseFileOK = false;
    boolean IsExist = false;

    class configItemUI{
        public String name;
        public int update_frequency; //seconds
        public boolean write_log;
        public int discharging_upper_limit;
        public int discharging_lower_limit;
    }
    configItemUI configItemUIObj;

    Hashtable<String, configItemUI> hashtableConfigUI;


    public boolean getParseOK()
    {
        return  parseFileOK;
    }
    public configItemsUIUtil()
    {
        hashtableConfigUI = new Hashtable<String, configItemUI>();
    }
    public configItemsUIUtil(String configPath)
    {
        hashtableConfigUI = new Hashtable<String, configItemUI>();
        fectest_config_path = configPath;
    }
    private void dump_trace( String bytTrace)
    {
        if (bDebugOn)
            Log.d(strTagUtil, bytTrace);
    }
    public Hashtable<String, configItemUI>  getHashtableConfigUI(){
        return hashtableConfigUI;
    }
    public configItemUI getConfigItemUI(String strName)
    {
        configItemUI configUIObject= new configItemUI();


        IsExist = hashtableConfigUI.containsKey(strName);
        if (IsExist){
            configUIObject = hashtableConfigUI.get(strName);
        }
        return  configUIObject;
    }
    boolean IsConfigExist()
    {
        return  IsExist;
    }
    public void dom4jXMLParser()
    {
        String strBaudRate="";
        int i=1; //  android:id="@+id/linearLayout_test_item_1". index from 1.
        String id="1";
        StringWriter xmlWriter = new StringWriter();
        SAXReader reader = new SAXReader();
        File file = new File(fectest_config_path);

        try {

            Document document = reader.read(file);
            Element root = document.getRootElement();
            List<Element> childElements = root.elements();
            for (Element child : childElements) {
                //已知属性名情况下
                dump_trace("name: " + child.attributeValue("name"));
                configItemUIObj = new configItemUI();
                /*
                public int update_frequency; //seconds
                public boolean write_log;
                public int discharging_upper_limit;
                public int discharging_lower_limit;
                 */
                configItemUIObj.name = child.attributeValue("name");
                configItemUIObj.update_frequency = Integer.parseInt(child.attributeValue("update_frequency"));
                configItemUIObj.write_log = Boolean.parseBoolean(child.attributeValue("write_log"));
                configItemUIObj.discharging_upper_limit = Integer.parseInt(child.attributeValue("discharging_upper_limit"));
                configItemUIObj.discharging_lower_limit = Integer.parseInt(child.attributeValue("discharging_lower_limit"));
                hashtableConfigUI.put(configItemUIObj.name, configItemUIObj);
                //Debug
            }
            parseFileOK = true;
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

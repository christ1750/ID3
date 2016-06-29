import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 *author: christ
 *data: 2015��9��16������2:17:38
 *function:
 */

public class ID3 {
	//Attribute�洢һ���ж�������
	private List<String>Attribute = new ArrayList<String>();
	//ÿ�����Ե�����ֵ�����ж�����浽һ��List������ �ڸ���AttributeValue;
	//��i�����Ե�����ֵ����Щ
	private List<List<String>>AttributeValue = new ArrayList<List<String>>(); 
	//data �洢���ݼ�¼��ÿ����¼��һ��String[],ÿ�������ֶ���String[0]
	private List<String[]>data = new ArrayList<String[]>();
	/*
	 * �����洢��Ϣֻ�У�ÿ�����ݶ�����ͨ��������Ϣ�ҵ���
	 * ͬʱ �����Ļ����õ�һ��List<Integer>subset��������ʾ�Ӽ���������Ŀ�ı�ţ���������Ϊid����
	 * ͨ�������ı�ţ��������ҵ�ÿ�����ݵĸ���������Ϣ��
	 * ���� data.get(subset.get(3))[3]��ʾ�Ӽ����е�����λ�ô�ŵ�id����data���ҵ���id��������ݣ������ݵĵ�3+1���ֶ�
	 * 
	 */
	private int decision_data;     //�����־�ֶ�
	private static final String petternString = "@attribute(.*)[{](.*)[}]";  //ƥ��ģʽ
	
	Document doc;
	Element root;
	
	public ID3(){
		doc = DocumentHelper.createDocument();
		root = DocumentHelper.createElement("������");
		doc.setRootElement(root);
	}
	
	//���÷����־��Ϣ
	public void setDec(String name){
		int n = Attribute.indexOf("play");   //�ҵ������Զ�Ӧ���ֶ�
		if(n < 0 || n > Attribute.size()){
			System.out.println("����ָ������");
		}
		decision_data = n;
	}
	public static void main(String[] args) throws FileNotFoundException {
		ID3 decision_true = new ID3();
		File file = new File("./data.ARFF");
		
		decision_true.readArff(file);
		decision_true.setDec("play");
		/*
		 * subset �洢�Ӽ��ϵ�id,��ĳһ�����ֶ�ȷ����ǰ���£��ҵ�������ֵ��Ӧ��������Ϊ�Ӽ�
		 */
		List<Integer>subset = new ArrayList<Integer>(); 
		for(int i = 0; i < decision_true.data.size(); i++){
			subset.add(i);
		}
		decision_true.BuildXmlTree(subset, -1, decision_true.root);
		decision_true.writer();
	}
	public void readArff(File file) throws FileNotFoundException{
		FileReader reader = new FileReader(file);
		BufferedReader buff = new BufferedReader(reader);
		String line;
		try {
			line = buff.readLine();
			Pattern pattern = Pattern.compile(petternString);
			while(line!=null){
				
				Matcher matcher = pattern.matcher(line);
				if(matcher.find()){
					Attribute.add(matcher.group(1).trim());
					List<String>temp_att = new ArrayList<String>(matcher.group().length());
					String []value_att = matcher.group(2).split(",");
					for(String value:value_att){
						temp_att.add(value.trim());
					}
					AttributeValue.add(temp_att);
				}else if(line.startsWith("@data")){
					line = buff.readLine();
					while(line != null){
						String []temp_data = line.split(",");
						data.add(temp_data);
						line = buff.readLine();
					}
				}
				line = buff.readLine();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public double getEntropy(int []arr){
		double entropy = 0.0;
		int sum = 0;
		for(int i = 0; i < arr.length; i++){
			entropy -= arr[i]*Math.log(arr[i]+Double.MIN_VALUE)/Math.log(2);
			sum += arr[i];
		}
		entropy += sum*Math.log(sum+Double.MIN_VALUE)/Math.log(2);
		entropy = entropy / (sum+Double.MIN_VALUE);
		
		return entropy;
	}
	
	//����һ��ԭʼ���ݵ��Ӽ���Ҳ����ȥ���ж����Եļ��ϣ����Լ�Ҫ�����ص�����
	public double getNodeEntropy(List<Integer>subset,int index){
		int num = subset.size();        //�������ܵ����ݸ���
		double entropy = 0.0;
		int [][]info = new int[AttributeValue.get(index).size()][];
		for(int i = 0; i < AttributeValue.get(index).size(); i++){
			info[i] = new int[AttributeValue.get(decision_data).size()];
		}
		int []count = new int[AttributeValue.get(index).size()]; //�����������Զ�Ӧ��ÿ��ȡֵ�ĸ���
		//����ÿ�����ݽ���ͳ��  info[i][j] ��¼�˶�ά��index�����е�i������ֵ��Ӧ��decision_data�����е�j�����ĸ���
		//info[sunny][yes] = 3
		for(int i = 0; i < num; i++){
			String indexnode = data.get(subset.get(i))[index];
			int indexnode_id = AttributeValue.get(index).indexOf(indexnode);//indexnode��index�����еı��
			count[indexnode_id]++;
			String decisionnode = data.get(subset.get(i))[decision_data];
			int decisionnode_id = AttributeValue.get(decision_data).indexOf(decisionnode);
			info[indexnode_id][decisionnode_id]++;
		}
		
		for(int i = 0; i < info.length; i++){
			entropy += getEntropy(info[i])*count[i]/num;
		}
		
		return entropy;
	}
	
	//����xml��
	public void BuildXmlTree(List<Integer>subset,int index,Element root){  //index��ʾ��һ�����������Եı��
		double entroy_Min = Double.MAX_VALUE;
		int Min_id = -1;
		//����Ӽ��ϵ��б�����ֵ��ͬ����Ѹ��Ӽ����б�Ԫ�ص�ֵд�����Ԫ�ؼ���������� �õݹ����
		if(subsame(subset)){
			root.setText(data.get(subset.get(0))[decision_data]);
			return;
		}
		/*
		 * ����Ӽ�ֵ����ͬ���ų�֮ǰ�ĸ��ڵ��Ӧ����������ֵ�Լ�������������ֵ���ֱ����ʣ���Ǹ����Ե���Ϣ����С������Ϣ�������
		 * ��������Ϣ����С������������Min_id��
		 * �Ӽ��϶�Ӧ�ĸ����Ե�ֵ������ͬ�ģ����Ը����Ե���Ϣ��
		 */
		
		for(int i = 0; i < Attribute.size(); i++){
			if(i == index || i == decision_data)
				continue;
			else{
				double entroy = getNodeEntropy(subset, i);
				if(entroy < entroy_Min){
					entroy_Min = entroy;
					Min_id = i;
				}
			}
		}
		//�ҵ��б�����ÿ��ֵ��Ӧ�ĵ��Ӽ�
		for(int i = 0; i < AttributeValue.get(Min_id).size(); i++){
			List<Integer>sub = new ArrayList<Integer>();
			String value = AttributeValue.get(Min_id).get(i);
			Element ele = root.addElement(Attribute.get(Min_id));
			ele.addAttribute("value", value);
			for(int size = 0; size < subset.size(); size++){
				if(value.equals(data.get(subset.get(size))[Min_id]))
					sub.add(subset.get(size));
			}
			
			BuildXmlTree(sub,Min_id,ele);
		}
	}
	
	public boolean subsame(List<Integer>sub){
		int size = sub.size();
		String value = data.get(sub.get(0))[decision_data];
		for(int i = 1; i < size; i++){
			String val = data.get(sub.get(i))[decision_data];
			if(!value.equals(val))
				return false;
		}
		return true;
	}
	
	public void writer(){
		try {
			OutputFormat format = new OutputFormat("	",true,"GBK");
			FileOutputStream fos = new FileOutputStream("ID3.xml");
			XMLWriter writer = new XMLWriter(fos,format);
			writer.write(doc);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XMLWriter writer = new XMLWriter();
		try {
			writer.write(doc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}


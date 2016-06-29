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
 *data: 2015年9月16日下午2:17:38
 *function:
 */

public class ID3 {
	//Attribute存储一共有多少属性
	private List<String>Attribute = new ArrayList<String>();
	//每种属性的属性值可能有多个，存到一个List集合中 在赋给AttributeValue;
	//第i个属性的属性值有哪些
	private List<List<String>>AttributeValue = new ArrayList<List<String>>(); 
	//data 存储数据记录，每条记录是一个String[],每个属性字段是String[0]
	private List<String[]>data = new ArrayList<String[]>();
	/*
	 * 这样存储信息只有，每个数据都可以通过索引信息找到。
	 * 同时 ，后文还会用到一个List<Integer>subset：用来表示子集和数据条目的编号（可以想象为id）。
	 * 通过这样的编号，我们能找到每条数据的各个属性信息。
	 * 比如 data.get(subset.get(3))[3]表示子集合中第三个位置存放的id，在data中找到该id代表的数据，该数据的第3+1个字段
	 * 
	 */
	private int decision_data;     //分类标志字段
	private static final String petternString = "@attribute(.*)[{](.*)[}]";  //匹配模式
	
	Document doc;
	Element root;
	
	public ID3(){
		doc = DocumentHelper.createDocument();
		root = DocumentHelper.createElement("决策树");
		doc.setRootElement(root);
	}
	
	//设置分类标志信息
	public void setDec(String name){
		int n = Attribute.indexOf("play");   //找到该属性对应的字段
		if(n < 0 || n > Attribute.size()){
			System.out.println("变量指定错误");
		}
		decision_data = n;
	}
	public static void main(String[] args) throws FileNotFoundException {
		ID3 decision_true = new ID3();
		File file = new File("./data.ARFF");
		
		decision_true.readArff(file);
		decision_true.setDec("play");
		/*
		 * subset 存储子集合的id,在某一属性字段确定的前提下，找到该属性值对应的数据作为子集
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
	
	//给定一个原始数据的子集（也就是去掉判定属性的集合），以及要计算熵的属性
	public double getNodeEntropy(List<Integer>subset,int index){
		int num = subset.size();        //集合中总的数据个数
		double entropy = 0.0;
		int [][]info = new int[AttributeValue.get(index).size()][];
		for(int i = 0; i < AttributeValue.get(index).size(); i++){
			info[i] = new int[AttributeValue.get(decision_data).size()];
		}
		int []count = new int[AttributeValue.get(index).size()]; //计数特征属性对应的每种取值的个数
		//遍历每条数据进行统计  info[i][j] 记录了二维表。index属性中第i个类别的值对应的decision_data属性中第j个类别的个数
		//info[sunny][yes] = 3
		for(int i = 0; i < num; i++){
			String indexnode = data.get(subset.get(i))[index];
			int indexnode_id = AttributeValue.get(index).indexOf(indexnode);//indexnode在index属性中的标号
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
	
	//构建xml树
	public void BuildXmlTree(List<Integer>subset,int index,Element root){  //index表示上一次做分类属性的编号
		double entroy_Min = Double.MAX_VALUE;
		int Min_id = -1;
		//如果子集合的判别属性值相同，则把该子集的判别元素的值写入其根元素几点的内容中 该递归结束
		if(subsame(subset)){
			root.setText(data.get(subset.get(0))[decision_data]);
			return;
		}
		/*
		 * 如果子集值不相同，排除之前的根节点对应的属性索引值以及分类属性索引值，分别计算剩下那个属性的信息熵最小，即信息增益最大
		 * 并保存信息熵最小的属性索引（Min_id）
		 * 子集合对应的根属性的值都是相同的，所以根属性的信息熵
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
		//找到判别属性每个值对应的的子集
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


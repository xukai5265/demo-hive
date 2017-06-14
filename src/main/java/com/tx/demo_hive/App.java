package com.tx.demo_hive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject; 
 
/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

    	List<Map<String,Object>> list_aa=new ArrayList<Map<String,Object>>();
    	Map<String,Object> map1=new HashMap<String,Object>();
    	map1.put("a", "this is a");
    	map1.put("b", "this is b");
    	Map<String,String> map2=new HashMap<String,String>();
    	map2.put("a2", "this is a2");
    	map2.put("b2", "this is b2");
    	list_aa.add(map1);
//    	list_aa.add(map2);
    	map1.put("map2", map2);
    	System.out.println(JSON.toJSONString(list_aa));
    	//[{"a":"this is a","b":"this is b"},{"b2":"this is b2","a2":"this is a2"}]
    	String str="abc:def:gh";
    	System.out.println(str.substring(0, str.indexOf(":")));
    	System.out.println(str.substring(str.indexOf(":")));
    	String str1=JSON.toJSONString(map1);
    	String str2=JSON.toJSONString(map2);
    	List<String> list2=new ArrayList<String>();
    	list2.add(str1);
    	list2.add(str2);
    	System.out.println(str1);
    	System.out.println(str2);
    	System.out.println(list2);
    	System.out.println(JSON.toJSONString(list2));
    	
    	System.out.println(JSON.toJSONString(JSON.parseObject(str2)));
    	System.out.println("===============");
//    	list2.clear();
//    	List<JSONObject> list3=new ArrayList<JSONObject>();
//    	list3.add(JSON.parseObject("abc"));
//    	System.out.println(list3);
        String st=JSON.toJSONString(JSON.parseObject(str2));
        Map<String,Object> ma=new HashMap<String,Object>();
        ma.put("1", JSON.parseObject(st));
        ma.put("2", "2");
        System.out.println(JSON.toJSONString(ma));
    }
}

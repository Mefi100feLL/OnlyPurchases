package com.PopCorp.Purchases.Loaders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Data.Shop;
import com.PopCorp.Purchases.Utilites.InternetConnection;

public class LoaderShopesFromInternet {
	
	public static String getFirstPage(String city){
		InternetConnection connection = null;
		StringBuilder page = null;
		try{
			connection = new InternetConnection("http://mestoskidki.ru/?city=" + city);
			page = connection.getPageInStringBuilder();
		} catch(IOException e) {
			return null;
		} finally {
			if (connection!=null){
				connection.disconnect();
			}
		}
		return page.toString();
	}
	
	public static ArrayList<Shop> getLinksForShops(String page, String cityId){
		ArrayList<String> linksForImage = new ArrayList<>();
		Matcher matcherLinks = Pattern.compile("src='img/[.[^']]+'").matcher(page);
		while (matcherLinks.find()) {
			String tmpString = matcherLinks.group();
			String url = SD.BASE_URL + tmpString.substring(5, tmpString.length()-1);
			linksForImage.add(url);
		}
		
		int i=0;
		Matcher matcherBegin = Pattern.compile(" <div class=\'left_text2\'><a href=\'[.[^&']]+&shop=[0-9]+\' class='left_links2'>[.[^<]]+").matcher(page);
		ArrayList<Shop> shops = new ArrayList<>();
		while (matcherBegin.find()) {
			String tmpString = matcherBegin.group();
			String keyShop = "";
			String nameShop = "";
			String countSalesShop = "";
			Matcher matcherForKey = Pattern.compile("shop=[0-9]+").matcher(tmpString);
			if (matcherForKey.find()) {
				keyShop = matcherForKey.group().substring(5);
			}
			Matcher matcherForName = Pattern.compile("links2\'>[.[^(]]+").matcher(tmpString);
			if (matcherForName.find()) {
				nameShop = matcherForName.group().substring(8, matcherForName.group().length()-1);
			}
			Matcher matcherForCount = Pattern.compile("\\([0-9]+\\)").matcher(tmpString);
			if (matcherForCount.find()) {
				countSalesShop = matcherForCount.group().substring(1, matcherForCount.group().length()-1);
			}
			shops.add(new Shop(cityId, keyShop, nameShop, linksForImage.get(i++), countSalesShop, "false"));
		}
		return shops;
	}
}

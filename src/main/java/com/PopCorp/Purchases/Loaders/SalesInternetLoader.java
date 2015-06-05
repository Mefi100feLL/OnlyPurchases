package com.PopCorp.Purchases.Loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.preference.PreferenceManager;

import com.PopCorp.Purchases.Data.Sale;
import com.PopCorp.Purchases.Utilites.InternetConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SalesInternetLoader extends AsyncTaskLoader<ArrayList<Sale>> {

	private final String shop;
	private final String cityId;

	public SalesInternetLoader(Context context, String shop) {
		super(context);
		this.shop = shop;
		cityId = PreferenceManager.getDefaultSharedPreferences(context).getString("city", "1");
	}

	@Override
	public ArrayList<Sale> loadInBackground() {
		ArrayList<Sale> result = new ArrayList<>();
		int countOfPages = getPageCount();

		InternetConnection connection = null;
		StringBuilder allpage = null;
		ArrayList<String> linksSale = new ArrayList<>();
		ArrayList<String> titlesSale = new ArrayList<>();
		ArrayList<String> imagesSale = new ArrayList<>();
		ArrayList<String> imagesIdsSale = new ArrayList<>();
		ArrayList<String> periodesSale = new ArrayList<>();

		for (int page = 1; page < countOfPages + 1; page++) {
			try {
				connection = new InternetConnection("http://mestoskidki.ru/view_shop.php?city=" + cityId + "&shop=" + shop + "&page=" + String.valueOf(page));
				allpage = connection.getPageInStringBuilder();
			} catch (IOException e){
				return null;
			} finally{
				if (connection!=null){
					connection.disconnect();
				}
			}
			Matcher matcherForLinkSale = Pattern.compile("&id=[.[^']]+").matcher(allpage.toString());
			while (matcherForLinkSale.find()) {
				linksSale.add(matcherForLinkSale.group().substring(4, matcherForLinkSale.group().length()));
			}
			Matcher matcherForTitleSale = Pattern.compile("alt='[.[^']]+").matcher(allpage.toString());
			while (matcherForTitleSale.find()) {
				titlesSale.add(matcherForTitleSale.group().substring(5, matcherForTitleSale.group().length()));
			}
			Matcher matcherForImageSale = Pattern.compile("src='http:[^']+").matcher(allpage.toString());
			while (matcherForImageSale.find()) {
				String imageUrl = matcherForImageSale.group().substring(5, matcherForImageSale.group().length());
				String id = imageUrl.substring(imageUrl.length()-10, imageUrl.length()-4);
				imageUrl = imageUrl.replaceFirst("s[0-9]+", id);
				imagesSale.add(imageUrl);
				imagesIdsSale.add(id);
			}
			Matcher matcherForPeriodSale = Pattern.compile("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}[0-9\\-\\.]*").matcher(allpage.toString());
			while (matcherForPeriodSale.find()) {
				periodesSale.add(matcherForPeriodSale.group());
			}
		}

		for (int i=0; i<linksSale.size(); i++){
			String periodBegin;
			String periodEnd;
			if (periodesSale.get(i).length()<=10){
				periodBegin = periodesSale.get(i);
				periodEnd = periodesSale.get(i);
			} else{
				String[] periodes = periodesSale.get(i).split("-");
				periodBegin = periodes[0];
				periodEnd = periodes[1];
			}

			Sale sale = new Sale(-1, linksSale.get(i), titlesSale.get(i), "", "", "", "", imagesSale.get(i), imagesIdsSale.get(i), shop, periodBegin, periodEnd);
			result.add(sale);
		}
		return result;
	}

	private int getPageCount(){
		InternetConnection connection = null;
		StringBuilder allpage = null;
		try {
			connection = new InternetConnection("http://mestoskidki.ru/view_shop.php?city=" + cityId + "&shop=" + shop);
			allpage = connection.getPageInStringBuilder();
		} catch(IOException e){
			return 1;
		} finally{
			if (connection!=null){
				connection.disconnect();
			}
		}

		Matcher matcher = Pattern.compile("([0-9]+)><b>&#062;&#062;").matcher(allpage.toString());
		if (matcher.find()) {
			Matcher matcher1 = Pattern.compile("[.[^>]]+").matcher(matcher.group());
			if (matcher1.find()) {
				return Integer.valueOf(matcher1.group());
			}
		}
		return 1;
	}

	public static String[] getPeriod(String page){
		String[] result = new String[2];
		Matcher matcher = Pattern.compile("Период акции:<br><br><font color='red'>[.[^<]]+").matcher(page);
		if (matcher.find()) {
			String period = matcher.group().substring(39, matcher.group().length());
			if (period.length()==10){
				result[0] = period;
				result[1] = period;
			} else{
				result[0] = period.substring(0, 10);
				result[1] = period.substring(13);
			}
			return result;
		}

		Matcher matcher1 = Pattern.compile("Период акции:<br><br>[.[^<]]+").matcher(page);
		if (matcher1.find()) {// ���������� ������, ���� ������� �������
			String period = matcher1.group().substring(21, matcher1.group().length());
			if (period.length()==10){
				result[0] = period;
				result[1] = period;
			} else{
				result[0] = period.substring(0, 10);
				result[1] = period.substring(13);
			}
			return result;
		}
		return null;
	}

	public static String getCoast(String page){
		Matcher matcher = Pattern.compile("Цена: [.[^<]]+").matcher(page);
		if (matcher.find()) {// ���������� ����
			return matcher.group().substring(6);
		}
		return null;
	}
	
	public static String getCount(String page){
		Matcher matcher = Pattern.compile("(Вес|ъем):[^<]+").matcher(page);
		if (matcher.find()) {
			return matcher.group().substring(5);
		}
		return "";
	}
	
	public static String getCoastFor(String page){
		Matcher matcher = Pattern.compile("Цена за [.[^:]]+: [.[^<]]+").matcher(page);
		if (matcher.find()) {
			Matcher matcher2 = Pattern.compile(": [.[^<]]+").matcher(matcher.group());
			if (matcher2.find()) {
				return matcher2.group().substring(2);
			}
		}
		return "";
	}

	public static String getTitle(String page){
		String title = null;
		Matcher matcher = Pattern.compile("<p class='larger'><strong>[.[^<]]+").matcher(page);
		if (matcher.find()) {
			title = matcher.group().substring(26).trim();
		}
		return title;
	}
	
	public static String getSubTitle(String page){
		String subTitle = "";
		Matcher matcher = Pattern.compile("<p class='larger'><strong>[^<]*</strong><br>[^<]+").matcher(page);
		if (matcher.find()) {
			Matcher matcher2 = Pattern.compile("<br>[^<]+").matcher(matcher.group());
			if (matcher2.find()) {
				subTitle=matcher2.group().substring(4).trim();
			}
		}
		return subTitle;
	}
	
	public static String getImageId(String page){
		String imageId = null;
		Matcher matcher = Pattern.compile("src='http://mestoskidki.ru/skidki/[.[^[.jpg]]]+.jpg").matcher(page);
		if (matcher.find()) {
			String finded = matcher.group();
			imageId = finded.substring(finded.length()-10, finded.length()-4);
		}
		return imageId;
	}

	public static String getImageUrl(String page){
		String imageUrl = null;
		Matcher matcher = Pattern.compile("src='http://mestoskidki.ru/skidki/[.[^[.jpg]]]+.jpg").matcher(page);
		if (matcher.find()) {
			imageUrl = matcher.group().substring(5);
		}
		return imageUrl;
	}

	/*public static String getCategory(String page){
		boolean prod = true;
		String category = null;
		Matcher matcher = Pattern.compile("view_cat[.]php[?]city=[0-9]+&cat[0-9=]+").matcher(page);
		if (matcher.find()) {
			String[] m = matcher.group().split("=");
			int size = m.length;
			String d = m[size-2].substring(m[size-2].length()-1);
			if (d.equals("2")){
				prod = false;
			}
			category = m[m.length-1];
		}
		return "";
		if (category!=null){
			if (prod){
				if (category.length()>2){
					for (int prodCat=0; prodCat<prodCats.size(); prodCat++){
						if ((prodCats.get(prodCat).contains(category)) || (category.equals(String.valueOf(prodCat+1)))){
							category = String.valueOf(prodCat+1);
							return category;
						}
					}
				}
			}else{
				if (category.length()>2){
					for (int promCat=0; promCat<promCats.size(); promCat++){
						if ((promCats.get(promCat).contains(category)) || (category.equals(String.valueOf(promCat+1)))){
							category = String.valueOf(promCat+1);
							return category;
						}
					}
				}
			}
		}
	}

	private ArrayList<ArrayList<String>> getCats(String categoryId){
		InternetConnection connection = null;
		StringBuilder allpage = null;
		ArrayList<ArrayList<String>> categories = new ArrayList<>();
		try {
			connection = new InternetConnection("http://mestoskidki.ru/cat_sale.php?city=" + cityId + "&catid=" + categoryId);
			allpage = connection.getPageInStringBuilder();
		} catch (MalformedURLException e) {
			return categories;
		} catch (IOException e) {
			return categories;
		} finally{
			if (connection!=null){
				connection.disconnect();
			}
		}

		if (categoryId.equals("1")){
			categoryId = "";
		}
		Matcher matcher = Pattern.compile("view_cat[.]php[?]city=[0-9]+&cat" + categoryId + "=[0-9]+").matcher(allpage.toString());
		int i=-1;
		while (matcher.find()) {
			String[] m = matcher.group().split("=");
			String categ = m[m.length-1];
			if (categ.length()<3){
				i++;
				if (Integer.valueOf(categ)<(i+1)){
					break;
				}
				categories.add(new ArrayList<String>());
			}else{
				categories.get(i).add(categ);
			}
		}
		return categories;
	}*/
}

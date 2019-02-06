package smg.emgem.haiku.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import smg.emgem.haiku.api.Noun;
import smg.emgem.haiku.api.Word;

@Service
public class HaikuServiceImpl implements HaikuService {

	@Autowired
	ResourceLoader loader;
	
	Logger log = LoggerFactory.getLogger(HaikuServiceImpl.class);
	
	HttpClient client = HttpClientBuilder.create().build();
	
	String externalUID = null;//default val
	
	private Map<String, List<Word>> wordSet = null;
	private Map<String, List<String>> adjectiveMap = null;
	
	@Override
	public String getGeneratedHaiku() {
		try {
			if (wordSet==null) 
				initSlovakWordSet();
			
			String result, pattern;
			
			int randomSeed = Double.valueOf(Math.random()*1000).intValue();
			if (randomSeed%2 == 0) {
				result= load_ANVR_but_NV_Line();
				pattern = "ANVR but NV";
			} else if (randomSeed%3 == 0) {
				result= load_NV_but_ANV_Line();
				pattern = "NV_but_ANV";
			} else if (randomSeed%5 == 0) {
				result = load_NV_NV_ANV_Line();
				pattern = "NV_NV_ANV";
			} else if (randomSeed%7 == 0) {
				result= load_C2_ANVR_Line();
				pattern = "C2_ANVR";
			} else if (randomSeed%11 == 0) {
				result= load_C1_ANV_Line();
				pattern = "C1_ANV";
			} else {
				result= load_ANV_ANVR_Line();
				pattern = "ANV_ANVR";
			}
			
			result = result.substring(0,1).toUpperCase()+result.substring(1);
			log.info(String.format("Creating pattern %s wordweave: %s", pattern, result));
			
			return result;
			
		} catch (IOException e) {
			log.error(e.getMessage());
			return null;
		}
	}

	protected String load_ANV_ANVR_Line() throws IOException {
		//pattern  a n v , a n v r
		String result = createAdjectiveGender(getRndWord("a"), getRndWord("n"))+ " "+
		changeVerbSufix(getRndWord("v"))+ ", "+
		
		createAdjectiveGender(getRndWord("a"), getRndWord("n"))+ " "+
		changeVerbSufix(getRndWord("v"))+ " "+
		getRndWord("r").getValue()+ ".";
		return result;
	}
	
	protected String load_ANVR_but_NV_Line() throws IOException {
		//pattern  a n v r ,but n v
		String result = createAdjectiveGender(getRndWord("a"), getRndWord("n"))+ " "+
		changeVerbSufix(getRndWord("v"))+" "+
		getRndWord("r").getValue()+ ", ale "+
		
		getRndWord("n").getValue()+ " "+
		changeVerbSufix(getRndWord("v"))+ ".";
		
		return result;
	}
	
	protected String load_NV_but_ANV_Line() throws IOException {
		//pattern n v ,but a n v
		String result = getRndWord("n").getValue() + " " + changeVerbSufix(getRndWord("v"))
		+ ", zatial čo "+
		
		createAdjectiveGender(getRndWord("a"), getRndWord("n"))+ " "+
		changeVerbSufix(getRndWord("v"))+ ".";
		
		return result;
	}
	
	protected String load_NV_NV_ANV_Line() throws IOException {
		// pattern nv - nv - anv.
		String result = getRndWord("n").getValue() + " " + changeVerbSufix(getRndWord("v")) + " - " +

		getRndWord("n").getValue() + " " + changeVerbSufix(getRndWord("v")) + " - " +

		createAdjectiveGender(getRndWord("a"), getRndWord("n")) + " " +
		changeVerbSufix(getRndWord("v")) + ".";

		return result;
	}
	
	protected String load_C1_ANV_Line() throws IOException {
		// pattern constant - anv.
		String citation = 		
		createAdjectiveGender(getRndWord("a"), getRndWord("n"))+ " "+
		changeVerbSufix(getRndWord("v"))+ "\" .";

		return  "Múdry muž povedal: \""+citation.substring(0, 1).toUpperCase() + citation.substring(1);
	}
	
	protected String load_C2_ANVR_Line() throws IOException {
		// pattern constant - anvr.
		String result = "Každý vie, že "+
						
		createAdjectiveGender(getRndWord("a"), getRndWord("n"))+ " "+
		changeVerbSufix(getRndWord("v"))+" "+ getRndWord("r").getValue() +  ".";

		return result;
	}
	
	private Word getRndWord(String type) {
		if (wordSet==null) 
			initSlovakWordSet();
		
		List<Word> subset = wordSet.get(type);
		if (subset == null) throw new RuntimeException("Invalid word type "+type);
		
		int radomIndex = Double.valueOf(Math.random()*subset.size()).intValue();
		return subset.get(radomIndex);
	}
	
	private void initExternalUID() {
		try {
			HttpResponse response = client.execute(new HttpGet("http://slovniky.korpus.sk/?d=noundb"));
			String responseStr = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
			
			String uidField = "<input type=\"hidden\" name=\"c\" value=\"";
			if (responseStr.contains(uidField)) {
				int uidIndex = responseStr.indexOf(uidField)+uidField.length();
				externalUID = responseStr.substring(uidIndex, uidIndex+4);
			}
		} catch (Exception e) {
		}
	}
	
	
	private void initSlovakWordSet() {
		log.info("Initializing slovak word database");
		
		wordSet = new HashMap<>();
		wordSet.put("a", new ArrayList<Word>());
		wordSet.put("n", new ArrayList<Word>());
		wordSet.put("v", new ArrayList<Word>());
		wordSet.put("r", new ArrayList<Word>());
		
		Pattern wordPattern = Pattern.compile("(\\d+\\s(\\w)\\s)");
			
		try {
			//OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("C:\\work\\wordDb2.txt"),"UTF-8");
			//BufferedWriter writer = new BufferedWriter(out);
			
			Resource resource = loader.getResource("classpath:static/wordb/wordDb.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
			String line = reader.readLine();
			while (line != null) {

				int labelIndex = Math.min(line.indexOf(";"), line.indexOf("␞"));
				if (labelIndex != -1) {
					String modifiedLine = line;
					Matcher matcher = wordPattern.matcher(line);
					if (matcher.find()) {
						String start = line.substring(0, labelIndex);
						String prefix = matcher.group(0);
						String category = prefix.contains("a") ? "a" : prefix.contains("n") ? "n" : prefix.contains("v") ? "v": "r";
						Word word;
						if ("n".equals(category)) {//getting gender
							//modifiedLine=findGenderExternally(word.getValue())+prefix.substring(prefix.length()-3) + line.substring(prefix.length());
							//modifiedLine=0+prefix.substring(prefix.length()-3) + line.substring(prefix.length());
							int rod=0;
							try {
								rod = Integer.valueOf(prefix.substring(0, prefix.length()-3));
							} catch (Exception e) {
								log.warn("Cannot parse noun input for "+line);
							}
							word = new Noun(start.substring(prefix.length()), rod);
						} else {
							word = new Word(start.substring(prefix.length()));
						}
						wordSet.get(category).add(word);
					}
					
					//writer.append(modifiedLine);
					//writer.append("\n");
				}
				line = reader.readLine();
			}
			
			//writer.close();
		} catch (IOException e) {
			log.error("Problem reading word database:", e.getMessage());
		}
		
	}
	
	private void initAdjectiveMap() {
		adjectiveMap = new HashMap<>();
		
		adjectiveMap.put("ý", Arrays.asList("ý", "á", "é"));
		adjectiveMap.put("y", Arrays.asList("y", "a", "e"));
		adjectiveMap.put("i", Arrays.asList("i", "a", "e"));
		adjectiveMap.put("í", Arrays.asList("í", "ia", "ie"));
		
		adjectiveMap.put("á", Arrays.asList("ý", "á", "é"));
		adjectiveMap.put("a", Arrays.asList("y", "a", "e"));
		
		adjectiveMap.put("é", Arrays.asList("ý", "á", "é"));
		adjectiveMap.put("e", Arrays.asList("y", "a", "e"));
	}
	
	// sets of rules to change from neutral to nominativ
	private String changeVerbSufix(Word verb) {
		String neutral = verb.getValue();
		neutral = neutral.trim();
		log.info("Default neutral:" + neutral);
		String postfix = "";
		String prefix = "";

		if (neutral.endsWith(" sa")) {
			neutral = neutral.substring(0, neutral.indexOf(" sa"));
			prefix = " sa ";
			log.info("Changed neutral to:" + neutral);
		}
		if (neutral.endsWith(" si")) {
			neutral = neutral.substring(0, neutral.indexOf(" si"));
			prefix = " si ";
			log.info("Changed neutral to:" + neutral);
		}
		if (neutral.contains(" ")) {
			postfix = neutral.substring(neutral.indexOf(" "));
			log.info("Postfix changed to:" + postfix);
			neutral = neutral.substring(0, neutral.indexOf(" "));
		}

		if (neutral.endsWith("ovať")) {
			return prefix + neutral.replace("ovať", "uje") + postfix;
		}
		if (neutral.endsWith("aviť")) {
			return prefix + neutral.replace("aviť", "avuje") + postfix;
		}
		if (neutral.endsWith("jiť")) {
			return prefix + neutral.replace("jiť", "juje") + postfix;
		}
		if (neutral.endsWith("núť")) {
			return prefix + neutral.replace("núť", "ne") + postfix;
		}
		if (neutral.endsWith("nuť")) {
			return prefix + neutral.replace("nuť", "ne") + postfix;
		}
		if (neutral.endsWith("návať")) {
			return prefix + neutral.replace("návať", "náva") + postfix;
		}
		if (neutral.endsWith("diť")) {
			return prefix + neutral.replace("diť", "di") + postfix;
		}
		if (neutral.endsWith("rieť")) {
			return prefix + neutral.replace("rieť", "rie") + postfix;
		}
		if (neutral.endsWith("cať")) {
			return prefix + neutral.replace("cať", "ciať") + postfix;
		}
		if (neutral.endsWith("čať")) {
			return prefix + neutral.replace("čať", "čína") + postfix;
		}
		if (neutral.endsWith("brať")) {
			return prefix + neutral.replace("brať", "berie") + postfix;
		}
		if (neutral.endsWith("rať")) {
			return prefix + neutral.replace("rať", "rá") + postfix;
		}
		if (neutral.endsWith("žať")) {
			return prefix + neutral.replace("žať", "ží") + postfix;
		}
		if (neutral.endsWith("ať")) {
			return prefix + neutral.replace("ať", "a") + postfix;
		}
		if (neutral.endsWith("ieť")) {
			return prefix + neutral.replace("ieť", "í") + postfix;
		}
		if (neutral.endsWith("iť")) {
			return prefix + neutral.replace("iť", "í") + postfix;
		}
		return prefix + neutral + postfix;
	}
	
	private String createAdjectiveGender(Word adjective, Word noun) {
		int gender= 0;
		try {
			Noun castNoun = (Noun)noun;
			gender=castNoun.getGender();
			log.debug("Orginal gender info for: "+noun.getValue() +" is "+castNoun.getGender());
		} catch (Exception e) {
			//gulp
		}
		
		if (gender == 0)
		    gender=findGenderExternally(noun.getValue());
		if (gender > 0)
			return changeAdjectiveGender(adjective.getValue(), noun.getValue(), gender);
		return adjective+" "+noun;
	}
	
	private int findGenderExternally(String noun){
		int result = 0; //not found
	    String word = noun.trim();
	    String[] mulitword= word.split(" ");
	    
	    if (mulitword.length>1) {
	    	for (String part:mulitword) {
	    		result = findGenderSingleWord(noun, part);
	    		if (result > 0)
	    			break; //no need to continue
	    	}
	    	
	    	word = mulitword[mulitword.length-1];
	    } else {
	    	result = findGenderSingleWord(noun, word);
	    }
		return result;
	}
	
	private int findGenderSingleWord(String noun, String word) {
		HttpResponse response;
		int result = 0; //not found
		log.info("Input: "+noun+" - Finding gender for "+word);
		try {
			URIBuilder builder = new URIBuilder("http://slovniky.korpus.sk/?s=exact&d=noundb&ie=utf-8&oe=utf-8");
			builder.addParameter("w", word);
			if (externalUID==null)
				initExternalUID();
			builder.addParameter("c", externalUID);
			
			response = client.execute(new HttpGet(builder.toString()));
			String responseStr = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
			
			//refreash UID
			String uidField = "<input type=\"hidden\" name=\"c\" value=\"";
			if (responseStr.contains(uidField)) {
				int uidIndex = responseStr.indexOf(uidField)+uidField.length();
				externalUID = responseStr.substring(uidIndex, uidIndex+4);
			}
			if (responseStr.contains("<b class=\"b0\">"+word+"</b><br>")) {
				String rod = responseStr.substring(responseStr.indexOf("<b class=\"b0\">"+word+"</b><br>")+14+word.length()+9);
				rod=rod.substring(0, rod.indexOf("rod"));
				log.info("Gender found as "+rod);
				if (rod.contains("stredn"))
					return 3;
				if (rod.contains("ensk"))
					return 2;
				if (rod.contains("mu"))
					return 1;
			} else {
				initExternalUID();
			}
		} catch (Exception e) {
			log.error("Error connecting to external dictionary "+e.getMessage());
		}
		return result;
	}
	
	private String changeAdjectiveGender(String adjective, String noun, int gender) {
		if (adjectiveMap == null)
		    initAdjectiveMap(); 
		
		adjective = adjective.trim();
		String adjectiveEnding = adjective.substring(adjective.length()-1);
		
		log.debug("Morphing "+adjective+ " ending with "+adjectiveEnding+ " to gender "+gender);
		
		List<String> properReplaceList = adjectiveMap.get(adjectiveEnding);
		if (properReplaceList != null) {
			adjective = adjective.substring(0, adjective.length()-1)+properReplaceList.get(gender-1);
		}
		String result = adjective+" "+noun;
		
		log.info("Morphing ended with "+result);
		return result;
	}
	
}

package smg.emgem.haiku.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class HaikuServiceImpl implements HaikuService {

	@Autowired
	ResourceLoader loader;
	
	Logger log = LoggerFactory.getLogger(HaikuServiceImpl.class);
	
	private Map<String, List<String>> wordSet = null;
	
	@Override
	public String getGeneratedHaiku() {
		try {
			return loadLine();
		} catch (IOException e) {
			log.error(e.getMessage());
			return null;
		}
	}

	private String loadLine() throws IOException {
		if (wordSet==null) 
			initSlovakWordSet();
		
		//patern  a n v , a n v r
		String result = getRndWord("a")+ " "+
		getRndWord("n")+ " "+
		changeSufix(getRndWord("v"))+ ", "+
		
		getRndWord("a")+ " "+
		getRndWord("n")+ " "+
		changeSufix(getRndWord("v"))+ " "+
		getRndWord("r")+ ".";
		
		result = result.substring(0,1).toUpperCase()+result.substring(1);
		log.info("Creating wordweave: "+ result);
		
		return result;
	}
	
	private String getRndWord(String type) {
		if (wordSet==null) 
			initSlovakWordSet();
		
		List<String> subset = wordSet.get(type);
		if (subset == null) throw new RuntimeException("Invalid word type "+type);
		
		int radomIndex = Double.valueOf(Math.random()*subset.size()).intValue();
		return subset.get(radomIndex);
	}
	
	private void initSlovakWordSet() {
		log.info("Initializing slovak word database");
		
		wordSet = new HashMap<>();
		wordSet.put("a", new ArrayList<String>());
		wordSet.put("n", new ArrayList<String>());
		wordSet.put("v", new ArrayList<String>());
		wordSet.put("r", new ArrayList<String>());
		
		Pattern wordPattern = Pattern.compile("(\\d+\\s(\\w)\\s)");
			
		try {
			Resource resource = loader.getResource("classpath:static/wordb/words-db.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
			String line = reader.readLine();
			while (line != null) {
				int labelIndex = Math.min(line.indexOf(";"), line.indexOf("␞"));
				if (labelIndex != -1) {
					Matcher matcher = wordPattern.matcher(line);
					if (matcher.find()) {
						String start = line.substring(0, labelIndex);
						String prefix = matcher.group(0);
						String category = prefix.contains("a") ? "a" : prefix.contains("n") ? "n" : prefix.contains("v") ? "v": "r";
						String word = start.substring(prefix.length());
//						if ("n".equals(category) && word.trim().endsWith("ť"))
//							continue;
						wordSet.get(category).add(word);
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			log.error("Problem reading word database:", e.getMessage());
		}
		
	}
	
	// sets of rules to change from neutral to nominativ
	private String changeSufix(String neutral) {
		neutral = neutral.trim();
		log.info("Default neutral:" + neutral);
		String postfix = "";

		if (neutral.endsWith(" sa")) {
			neutral = neutral.substring(0, neutral.indexOf(" sa"));
			log.info("Changed neutral to:" + neutral);
		}
		if (neutral.endsWith(" si")) {
			neutral = neutral.substring(0, neutral.indexOf(" si"));
			log.info("Changed neutral to:" + neutral);
		}
		if (neutral.contains(" ")) {
			postfix = neutral.substring(neutral.indexOf(""));
			log.info("Postfix changed to:" + postfix);
			neutral = neutral.substring(0, neutral.indexOf(""));
		}

		if (neutral.endsWith("ovať")) {
			return neutral.replace("ovať", "uje") + postfix;
		}
		if (neutral.endsWith("aviť")) {
			return neutral.replace("aviť", "avuje") + postfix;
		}
		if (neutral.endsWith("núť")) {
			return neutral.replace("núť", "ne") + postfix;
		}
		if (neutral.endsWith("nuť")) {
			return neutral.replace("nuť", "ne") + postfix;
		}
		if (neutral.endsWith("diť")) {
			return neutral.replace("diť", "di") + postfix;
		}
		if (neutral.endsWith("rieť")) {
			return neutral.replace("rieť", "rie") + postfix;
		}
		if (neutral.endsWith("ať")) {
			return neutral.replace("ať", "a") + postfix;
		}
		if (neutral.endsWith("iť")) {
			return neutral.replace("iť", "í") + postfix;
		}
		return neutral + postfix;
	}
}

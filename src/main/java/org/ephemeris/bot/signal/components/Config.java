package org.ephemeris.bot.signal.components;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.ephemeris.bot.signal.Plugin;
import org.ephemeris.bot.signal.api.Method;
import de.thoffbauer.signal4j.store.User;

public class Config {
	
	private static final String PATH = "config.json";
	
	private static Config instance;
	
	@JsonProperty
	private HashMap<String, Boolean> plugins = new HashMap<>();
	
	@JsonProperty
	private HashSet<String> sudoers = new HashSet<>();

    @JsonProperty(required = true)
	private HashMap<String, String>  api = new HashMap<>();

	public static Config getInstance() {
		return instance;
	}
	
	public static void load() throws IOException {
		// load a maybe existing config
		File file = new File(PATH);
		ObjectMapper mapper = new ObjectMapper();
		if(file.exists()) {
			instance = mapper.readValue(file, Config.class);
		} else {
			instance = new Config();
		}
		
		// update it with default config
		Config defaultConfig = mapper.readValue(
				Config.class.getResourceAsStream("defaultConfig.json"), Config.class);

		// Add new plugin entries only
		defaultConfig.plugins.entrySet().stream()
				.filter(v -> !instance.plugins.containsKey(v.getKey()))
				.forEach(v -> instance.plugins.put(v.getKey(), v.getValue()));

		// Add new api endpoints only
		defaultConfig.api.entrySet().stream()
				.filter(v -> !instance.api.containsKey(v.getKey()))
				.forEach(v -> instance.api.put(v.getKey(), v.getValue()));

		// ignore sudoers as there should not be any sudoers inside the default config
		
		// save it
		instance.save();
	}
	
	public void save() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		try {
			mapper.writeValue(new File(PATH), this);
		} catch (IOException e) {
			System.err.println("Could not save config! Changes will be lost: " + e.getMessage() +
					" (" + e.getClass().getSimpleName() + ")");
		}
	}
	
	@JsonIgnore
	public Set<String> getPluginNames() {
		return plugins.keySet();
	}
	
	public boolean isEnabled(Plugin plugin) {
		Boolean boxed = plugins.get(plugin.getName());
		if(boxed != null) {
			return boxed;
		} else {
			throw new IllegalArgumentException(plugin + " not known in config!");
		}
	}
	
	public void setEnabled(String plugin, boolean enabled) {
		plugins.put(plugin, enabled);
		save();
	}
	
	public boolean isSudo(User sender) {
		return sudoers.contains(sender.getNumber());
	}
	
	public void setSudo(User sender, boolean sudo) {
		String phoneNumber = sender.getNumber();
		boolean isSudo = isSudo(sender);
		if(isSudo == sudo) {
			return;
		} else if(isSudo) {
			sudoers.remove(phoneNumber);
			save();
		} else {
			sudoers.add(phoneNumber);
			save();
		}
	}

	public String getAPIEndpoint(Method m) {
        switch (m) {
            case WORD_LOOKUP:
                return api.get("lookupEndpoint");
        }
        return null;
    }

    // Ensure that loaded configuration has no fatal errors preventing the program to run normally
    public boolean validate() {
	    for (Method m : Method.values()) {
	        if (getAPIEndpoint(m) == null) { // api endpoints must be defined for all methods
	            return false;
            }
        }

        return true;
    }
}

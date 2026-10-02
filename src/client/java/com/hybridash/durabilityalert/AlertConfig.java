package com.hybridash.durabilityalert;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** config/durabilityalert.properties */
public class AlertConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("durabilityalert.properties");

	public boolean enabled = true;
	public boolean sound = true;
	/** First warning when this % of durability (or this many uses, whichever is more) is left. */
	public double lowPercent = 10;
	public int lowUses = 20;
	/** Urgent warning at this % (or this many uses). */
	public double criticalPercent = 3;
	public int criticalUses = 5;

	public static AlertConfig load() {
		AlertConfig c = new AlertConfig();
		if (Files.exists(FILE)) {
			Properties p = new Properties();
			try (Reader r = Files.newBufferedReader(FILE)) {
				p.load(r);
				c.enabled = Boolean.parseBoolean(p.getProperty("enabled", "true").trim());
				c.sound = Boolean.parseBoolean(p.getProperty("sound", "true").trim());
				c.lowPercent = Double.parseDouble(p.getProperty("lowPercent", "10").trim());
				c.lowUses = Integer.parseInt(p.getProperty("lowUses", "20").trim());
				c.criticalPercent = Double.parseDouble(p.getProperty("criticalPercent", "3").trim());
				c.criticalUses = Integer.parseInt(p.getProperty("criticalUses", "5").trim());
			} catch (Exception e) {
				DurabilityAlertClient.LOGGER.warn("Couldn't read {}, using defaults", FILE, e);
			}
		}
		c.save();
		return c;
	}

	public void save() {
		Properties p = new Properties();
		p.setProperty("enabled", Boolean.toString(enabled));
		p.setProperty("sound", Boolean.toString(sound));
		p.setProperty("lowPercent", Double.toString(lowPercent));
		p.setProperty("lowUses", Integer.toString(lowUses));
		p.setProperty("criticalPercent", Double.toString(criticalPercent));
		p.setProperty("criticalUses", Integer.toString(criticalUses));
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE)) {
				p.store(w, "DurabilityAlert\nlow*: first warning, critical*: urgent warning. Whichever of percent/uses is bigger is used.");
			}
		} catch (Exception e) {
			DurabilityAlertClient.LOGGER.warn("Couldn't save {}", FILE, e);
		}
	}
}

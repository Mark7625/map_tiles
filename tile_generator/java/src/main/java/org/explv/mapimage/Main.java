package org.explv.mapimage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.MapImageDumper;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Region;
import net.runelite.cache.util.XteaKeyManager;

import org.antlr.v4.runtime.misc.Pair;

@Slf4j
public class Main
{
	private static final List<Pair<String, BiConsumer<MapImageDumper, Boolean>>> mapOptions = List.of(
		new Pair<>("renderMap", MapImageDumper::setRenderMap),
		new Pair<>("renderObjects", MapImageDumper::setRenderObjects),
		new Pair<>("renderIcons", MapImageDumper::setRenderIcons),
		new Pair<>("renderWalls", MapImageDumper::setRenderWalls),
		new Pair<>("renderOverlays", MapImageDumper::setRenderOverlays),
		new Pair<>("renderLabels", MapImageDumper::setRenderLabels),
		new Pair<>("transparency", MapImageDumper::setTransparency)
	);

	public static void main(String[] args) throws IOException
	{
		Map<String, String> cmd = parseArgs(args);

		final String cacheDirectory = cmd.get("cachedir");
		final String xteaJSONPath = cmd.get("xteapath");
		final String outputDirectory = cmd.get("outputdir");

		if (cacheDirectory == null || xteaJSONPath == null || outputDirectory == null)
		{
			System.err.println("Required arguments:");
			System.err.println("--cachedir <path> --xteapath <path> --outputdir <path>");
			System.exit(1);
		}

		XteaKeyManager xteaKeyManager = new XteaKeyManager();
		try (FileInputStream fin = new FileInputStream(xteaJSONPath))
		{
			xteaKeyManager.loadKeys(fin);
		}

		File base = new File(cacheDirectory);
		File outDir = new File(outputDirectory);
		outDir.mkdirs();

		try (Store store = new Store(base))
		{
			store.load();

			MapImageDumper dumper = new MapImageDumper(store, xteaKeyManager);

			// Apply custom render options
			for (Pair<String, BiConsumer<MapImageDumper, Boolean>> mapOption : mapOptions)
			{
				if (cmd.containsKey(mapOption.a))
				{
					String option = cmd.get(mapOption.a);
					boolean value = Boolean.parseBoolean(option);
					mapOption.b.accept(dumper, value);
				}
			}

			dumper.load();

			for (int i = 0; i < Region.Z; ++i)
			{
				BufferedImage image = dumper.drawMap(i);

				File imageFile = new File(outDir, "img-" + i + ".png");

				ImageIO.write(image, "png", imageFile);
				log.info("Wrote image {}", imageFile);
			}
		}
	}

	private static Map<String, String> parseArgs(String[] args)
	{
		Map<String, String> map = new HashMap<>();

		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];

			if (arg.startsWith("--"))
			{
				String key = arg.substring(2);

				if (i + 1 < args.length && !args[i + 1].startsWith("--"))
				{
					map.put(key, args[++i]);
				}
				else
				{
					map.put(key, "true");
				}
			}
		}

		return map;
	}
}

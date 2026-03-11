package org.explv.mapimage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;

import net.runelite.cache.MapImageDumper;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Region;
import net.runelite.cache.util.XteaKeyManager;

public class Main
{
    private static class MapOption
    {
        String name;
        BiConsumer<MapImageDumper, Boolean> setter;

        MapOption(String name, BiConsumer<MapImageDumper, Boolean> setter)
        {
            this.name = name;
            this.setter = setter;
        }
    }

    private static final List<MapOption> mapOptions = Arrays.asList(
        new MapOption("renderMap", MapImageDumper::setRenderMap),
        new MapOption("renderObjects", MapImageDumper::setRenderObjects),
        new MapOption("renderIcons", MapImageDumper::setRenderIcons),
        new MapOption("renderWalls", MapImageDumper::setRenderWalls),
        new MapOption("renderOverlays", MapImageDumper::setRenderOverlays),
        new MapOption("renderLabels", MapImageDumper::setRenderLabels),
        new MapOption("transparency", MapImageDumper::setTransparency)
    );

    public static void main(String[] args) throws IOException
    {
        Map<String, String> cmd = parseArgs(args);

        String cacheDirectory = cmd.get("cachedir");
        String xteaJSONPath = cmd.get("xteapath");
        String outputDirectory = cmd.get("outputdir");

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

            for (MapOption option : mapOptions)
            {
                if (cmd.containsKey(option.name))
                {
                    boolean value = Boolean.parseBoolean(cmd.get(option.name));
                    option.setter.accept(dumper, value);
                }
            }

            dumper.load();

            for (int i = 0; i < Region.Z; i++)
            {
                BufferedImage image = dumper.drawMap(i);

                File imageFile = new File(outDir, "img-" + i + ".png");

                ImageIO.write(image, "png", imageFile);
                System.out.println("Wrote image " + imageFile);
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

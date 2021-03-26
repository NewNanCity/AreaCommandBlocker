package io.github.gk0wk.areacommandblocker.area;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VanillaAreaContainer implements AreaContainer {

    private static final Pattern acceptableAreaNamePattern = Pattern.compile(
            "^(?<world>.+):(?<x1>[+-]?[0-9]+(\\.[0-9]+)?),(?<y1>[+-]?[0-9]+(\\.[0-9]+)?),(?<z1>[+-]?[0-9]+(\\.[0-9]+)?):(?<x2>[+-]?[0-9]+(\\.[0-9]+)?),(?<y2>[+-]?[0-9]+(\\.[0-9]+)?),(?<z2>[+-]?[0-9]+(\\.[0-9]+)?)$"
    );

    // The data-structure and its algorithm is extremely inelegant, but I tried. Using 4-level linked-map is too complex.
    private final Map<World, Map<Area, Point>> areas = new HashMap<>();
    private final Map<Area, Point> anotherPoint = new HashMap<>();

    @Override
    public boolean register(String area, String name, List<String> contents, boolean whitelist) {
        Matcher matcher = acceptableAreaNamePattern.matcher(area);
        if (matcher.find()) {
            Area _area = new Area(name, contents, whitelist);

            // Parse
            World world = Bukkit.getWorld(matcher.group("world"));
            int x1 = Double.valueOf(matcher.group("x1")).intValue();
            int y1 = Double.valueOf(matcher.group("y1")).intValue();
            int z1 = Double.valueOf(matcher.group("z1")).intValue();
            int x2 = Double.valueOf(matcher.group("x2")).intValue();
            int y2 = Double.valueOf(matcher.group("y2")).intValue();
            int z2 = Double.valueOf(matcher.group("z2")).intValue();

            // Swap & Arrange
            int tmp;
            if (x1 > x2) {
                tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            if (y1 > y2) {
                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            if (z1 > z2) {
                tmp = z1;
                z1 = z2;
                z2 = tmp;
            }

            Point p1 = new Point(x1, y1, z1);
            Point p2 = new Point(x2, y2, z2);

            if (!areas.containsKey(world)) {
                areas.put(world, new HashMap<>());
            }

            areas.get(world).put(_area, p1);
            anotherPoint.put(_area, p2);

            return true;
        }
        return false;
    }

    @Override
    public Area check(Location location) {
        Map<Area, Point> tmpMap = areas.get(location.getWorld());
        if (tmpMap != null) {
            for (Map.Entry<Area, Point> entry : tmpMap.entrySet()) {
                Point p1 = entry.getValue();
                if (location.getBlockY() >= p1.y && location.getBlockX() >= p1.x && location.getBlockZ() >= p1.z) {
                    Point p2 = anotherPoint.get(entry.getKey());
                    if (location.getBlockY() <= p2.y && location.getBlockX() <= p2.x && location.getBlockZ() <= p2.z) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Set<Area> getAll() {
        return anotherPoint.keySet();
    }

    @Override
    public void clear() {
        areas.clear();
        anotherPoint.clear();
    }
}

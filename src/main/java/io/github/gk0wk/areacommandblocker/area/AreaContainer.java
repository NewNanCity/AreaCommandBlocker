package io.github.gk0wk.areacommandblocker.area;

import org.bukkit.Location;

import java.util.List;
import java.util.Set;

public interface AreaContainer {
    boolean register(String area, String name, List<String> contents, boolean whitelist);
    Area check(Location location);
    Set<Area> getAll();
    void clear();
}

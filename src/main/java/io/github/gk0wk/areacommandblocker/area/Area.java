package io.github.gk0wk.areacommandblocker.area;

import io.github.gk0wk.areacommandblocker.AreaCommandBlocker;

import java.util.*;

public class Area {
    public final String[][] contents;
    public final String name;
    public final boolean whitelist;
    public Area(String name, List<String> contents, boolean whitelist) {
        this.whitelist = whitelist;
        this.name = name;

        Set<String[]> tmp = new HashSet<>();
        contents.forEach(content -> {
            if (content.charAt(0) == '/') {
                String[] tmpCommand = AreaCommandBlocker.normalizeCommand(content);
                if (tmpCommand != null) {
                    tmp.add(tmpCommand);
                }
            } else {
                String[][] group = AreaCommandBlocker.getInstance().getGroup(content);
                if (group != null) {
                    tmp.addAll(Arrays.asList(group));
                }
            }
        });
        this.contents = tmp.toArray(new String[0][0]);
    }
}

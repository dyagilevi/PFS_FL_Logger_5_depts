package com.example.pfs_flowline_vermanufacture;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;

class MapComparator implements Comparator<Map<String, String>>
{
    private final String key;

    public MapComparator(String key)
    {
        this.key = key;
    }

    public int compare(Map<String, String> first,
                       Map<String, String> second)
    {
        String firstValue = first.get(key);
        String secondValue = second.get(key);

        LocalDateTime ldt_first = LocalDateTime.parse(firstValue, DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"));
        LocalDateTime ldt_second = LocalDateTime.parse(secondValue, DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"));

        //return secondValue.compareTo(firstValue);
        return ldt_second.compareTo(ldt_first);
    }
}

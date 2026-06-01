package com.example.elecount.Hellper;

import com.example.elecount.models.UsageRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * تجميع إحصائيات سجلات الاستخدام
 */
public final class UsageStatsHelper {

    private UsageStatsHelper() {
    }

    public static Map<String, Double> aggregateCostByDevice(ArrayList<UsageRecord> records) {
        Map<String, Double> map = new HashMap<>();
        if (records == null) {
            return map;
        }
        for (UsageRecord record : records) {
            if (record.isSimulation()) {
                continue;
            }
            String deviceId = record.getDeviceId();
            if (deviceId == null || deviceId.isEmpty()) {
                continue;
            }
            map.put(deviceId, map.getOrDefault(deviceId, 0.0) + record.getCost());
        }
        return map;
    }

    public static Map<String, String> aggregateNameByDevice(ArrayList<UsageRecord> records) {
        Map<String, String> map = new HashMap<>();
        if (records == null) {
            return map;
        }
        for (UsageRecord record : records) {
            if (record.isSimulation()) {
                continue;
            }
            String deviceId = record.getDeviceId();
            if (deviceId != null && record.getDeviceName() != null) {
                map.put(deviceId, record.getDeviceName());
            }
        }
        return map;
    }

    public static double totalCost(ArrayList<UsageRecord> records) {
        double total = 0;
        if (records == null) {
            return total;
        }
        for (UsageRecord record : records) {
            if (!record.isSimulation()) {
                total += record.getCost();
            }
        }
        return total;
    }

    public static double totalEnergy(ArrayList<UsageRecord> records) {
        double total = 0;
        if (records == null) {
            return total;
        }
        for (UsageRecord record : records) {
            if (!record.isSimulation()) {
                total += record.getEnergyConsumed();
            }
        }
        return total;
    }

    public static int sessionCount(ArrayList<UsageRecord> records) {
        if (records == null) {
            return 0;
        }
        int count = 0;
        for (UsageRecord record : records) {
            if (!record.isSimulation()) {
                count++;
            }
        }
        return count;
    }

    public static class DeviceRank {
        public String deviceId;
        public String deviceName;
        public double totalCost;
    }

    public static DeviceRank findHighest(ArrayList<UsageRecord> records) {
        Map<String, Double> costs = aggregateCostByDevice(records);
        Map<String, String> names = aggregateNameByDevice(records);
        DeviceRank highest = null;
        for (Map.Entry<String, Double> entry : costs.entrySet()) {
            if (highest == null || entry.getValue() > highest.totalCost) {
                highest = new DeviceRank();
                highest.deviceId = entry.getKey();
                highest.totalCost = entry.getValue();
                highest.deviceName = names.get(entry.getKey());
            }
        }
        return highest;
    }

    public static DeviceRank findLowest(ArrayList<UsageRecord> records) {
        Map<String, Double> costs = aggregateCostByDevice(records);
        Map<String, String> names = aggregateNameByDevice(records);
        DeviceRank lowest = null;
        for (Map.Entry<String, Double> entry : costs.entrySet()) {
            if (lowest == null || entry.getValue() < lowest.totalCost) {
                lowest = new DeviceRank();
                lowest.deviceId = entry.getKey();
                lowest.totalCost = entry.getValue();
                lowest.deviceName = names.get(entry.getKey());
            }
        }
        return lowest;
    }
}

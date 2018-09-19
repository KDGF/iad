package com.kdg.gnome.adx.utils;

import java.util.Map;
import java.util.Random;

/**
 * Created by hbwang on 2018/3/8
 */
public class RandomUtil {

    public static boolean randomChance(int base, int num) throws Exception {
        boolean result = false;
        if (base == num){
            result = true;
        } else if (base > num){
            Random random = new Random();
            int tNum = random.nextInt(base);
            if (tNum < num) {
                result = true;
            }
        }
        return result;
    }

    private static int bidSearch(int[] desArray, double des) {
        int low = 0;
        int high = desArray.length - 1;
        int middle;
        while (low <= high) {
            middle = (low + high) / 2;
            if (des > desArray[middle] && des <= desArray[middle + 1]) {
                return middle;
            } else if (des < desArray[middle]) {
                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }
        return -1;
    }

    // 根据指定的样本数组和样本-权重映射随机抽取一个元素
    public static Object randomByWeight(Object[] elementArray, Map<Object, Integer> weightMap) throws Exception {
        if (elementArray == null || elementArray.length == 0) {
            throw new Exception("ElementArray must not be empty!");
        }
        if (weightMap == null || weightMap.isEmpty()) {
            throw new Exception("WeightMap must not be empty!");
        }
        int[] weightArray = new int[elementArray.length + 1];
        Object[] tempElementArray = new Object[elementArray.length];
        Integer weight;
        for (int i = 0; i < elementArray.length; i++) {
            tempElementArray[i] = elementArray[i];
            weight = weightMap.get(elementArray[i]);
            weightArray[i + 1] = weight + weightArray[i];
        }
        double randomNum = weightArray[weightArray.length - 1] * Math.random();
        int index = bidSearch(weightArray, randomNum);
        return index == -1 ? null : tempElementArray[index];
    }
}

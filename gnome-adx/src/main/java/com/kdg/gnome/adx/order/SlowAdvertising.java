package com.kdg.gnome.adx.order;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Created by hbwang on 2018/7/30
 */
public class SlowAdvertising {

    private static final Logger LOGGER = LogManager.getLogger("ES_OUT_INFO");
    private static ThreadLocal<Random> randomThreadLocal = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random(System.nanoTime());
        }
    };

    static boolean isCamAdvertisingByBudget(double budgetSum, double budgetLim, int ratio4Start, double ratioOfSpeed) {

        int parHasAdvered = (int) (budgetSum * 100 / budgetLim);
        if (parHasAdvered >= ratio4Start) {
            //剩余量(动态减)占 开始生效时 剩余(固定) 的比例

            double a = 100.0 / (budgetLim - (budgetLim * ratio4Start / 100)) / (budgetLim - (budgetLim * ratio4Start / 100));
            int par = (int) (a * (budgetLim - budgetSum) * (budgetLim - budgetSum));

            //投放达到 配置的比例后 开始 缓速投
            return isCamAdvertising((int) (par * ratioOfSpeed));
        } else {
            return true;
        }
    }

    static boolean isCamAdvertisingByLim(int lim, int sum, int ratio4Start, double ratioOfSpeed) {

        int parHasAdvered = (int) (sum * 100 / lim * 1.0);
        if (parHasAdvered >= ratio4Start) {

            //剩余量(动态减)占 开始生效时 剩余(固定) 的比例
            double a = 100.0 / (lim - (lim * ratio4Start / 100.0)) / (lim - (lim * ratio4Start / 100));
            int par = (int) (a * (lim - sum) * (lim - sum));

            //投放达到 配置的比例后 开始 缓速投
            return isCamAdvertising((int) (par * ratioOfSpeed));
        } else {
            return true;
        }

    }

    static boolean isAdvertisingByBalance(double balance, double balanceLim) {

        double a = 100.0 / balanceLim / balanceLim;
        int par = (int) (a * balance * balance);
//        int par = (int) (balance * 100 / balanceLim);

        return isCamAdvertising(par);
    }

    private static boolean isCamAdvertising(int par) {

        int ranPar = randomThreadLocal.get().nextInt(100);
        LOGGER.info("--------------- ramPar = " + ranPar + ", par = " + par);
        return (ranPar <= par);
    }



    public static void main(String[] args) {
        int lim = 1000;
        int sum = 800;

//        int par = (lim - sum) * 100 / (lim * (100 - ratio4Start) / 100);

//        double ratio = lim * ratio4Start / 100 / (lim - lim * ratio4Start / 100);
//        int par = (int) ( ((lim -sum) * 100 / sum ) * ratio);
//
//        System.out.println(ratio);
//        System.out.println(par);
//
//        double p = 1.0 / (2 * (lim - 800));
////        double a = 100 / 200;
//
//        int y = (int) (Math.sqrt(2.0 * p * (lim - sum)) * 100);


//        double a = 100.0 / (lim - 800) / (lim - 800);
//
//        int y = (int) (a * (lim - sum) * (lim - sum));
//
//        System.out.println(a);
////        System.out.println(a);
//        System.out.println(y);
//
//
//        double balanceLim = 1000.0;
//        double balance = 200.0;
//
//        double at = 100.0 / balanceLim / balanceLim;
//        int par = (int) (at * balance * balance);
//
//        System.out.println(at);
//        System.out.println(par);

//        double a = 100.0 / (lim - (lim * ratio4Start / 100.0)) / (lim - (lim * ratio4Start / 100));
//        int par = (int) (a * (lim - sum) * (lim - sum));
//        System.out.println(a);
//        System.out.println(par);


    }

}

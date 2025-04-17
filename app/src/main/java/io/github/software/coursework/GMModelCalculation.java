package io.github.software.coursework;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

import static java.lang.Math.*;

/*
    This class is used to calculate GMM model parameters.

    The main function is GMModelCalculation GMModelCalculator(List<Pair<Double, Triple<Integer, Integer, Integer>>>)

    The input content is the daily expenses of a college student over the past few
    decades. It is recommended to divide them into breakfast, lunch, and dinner. That
    is to say, if it is 20 years, there should be 365 * 20 * 3 quadruples (yi, fi, gi, hi),
    where yi represents the expenditure of one item, and the last three parameters represent
    the time of this expenditure. fi represents which month it is in, gi represents which
    day of the month fi, and hi represents what day of the week it is.

    Of course, it is generated data, and so far we have not been able to collect enough real data.

    The output is a parameter of a GMM model, including several 5-tuple pairs
    (mu_i, sigma_i, F_i, G_i, H_i), where mu and sigma represent the mean and
    variance of a component in the GMM model. F_i is a list of length 12, G_i is a
    list of length 31, and H_i is a list of length 7. For each F_i_j, G_i_j，H_i_j，
    Ensure that sigma i (F_i_j+G_i_j+H_i_j)=1
 */
public class GMModelCalculation {

    public static List<Pair<Double, Triple<Integer, Integer, Integer>>> p;
    public static int[] countMonth = new int[13];
    public static int[] countDay = new int[32];
    public static int[] countWeek = new int[8];
    public static List<Double> t;


    public static List<List<Double>> GMModelCalculator(List<Pair<Double, Triple<Integer, Integer, Integer>>> params) {
        p = params;

        for (int i = 0; i < 13; i++) countMonth[i] = 0;
        for (int i = 0; i < 32; i++) countDay[i] = 0;
        for (int i = 0; i < 8; i++) countWeek[i] = 0;

        for (Pair<Double, Triple<Integer, Integer, Integer>> param : params) {
            countMonth[param.getRight().getLeft()]++;
            countDay[param.getRight().getMiddle()]++;
            countWeek[param.getRight().getRight()]++;

            t.add(param.getLeft());
        }

        List<List<Double>> answer = new ArrayList<>();
        Double Score;

        answer.add(gaussParamsCalculator(params));
        Score = BICCalculator(answer);

        List<Integer>[] kMeans;
        List<Pair<Double, Triple<Integer, Integer, Integer>>> tmpList = new ArrayList<>();

        List<List<Double>> tmpAnswer = new ArrayList<>();
        Double tmpScore;

        for (int k = 2; k <= min(20, params.size()); k++) {
            kMeans = kMeansPlus(k);

            tmpAnswer.clear();

            for (int i = 0; i < k; i++) {
                tmpList.clear();
                for (int j : kMeans[i]) {
                    tmpList.add(p.get(j));
                }

                tmpAnswer.add(gaussParamsCalculator(tmpList));
            }

            tmpScore = BICCalculator(tmpAnswer);

            if (tmpScore < Score) {
                answer = new ArrayList<>(tmpAnswer);
            }
        }

        return answer;
    }

    public static List<Integer>[] kMeansPlus(int k) {
        Double[] centers = new Double[50];
        int ctops = 0;
        centers[ctops++] = t.get(0);

        List<Double> G = new ArrayList();
        for (int i = 1; i < k; i++) {
            G.clear();
            Double Gsum = 0.0;

            for (Double tValue : t) {
                Double maxG = 0.0;
                for (int j = 0; j < ctops; j++) {
                    if (maxG < Math.pow(tValue - centers[j], 2)) {
                        maxG = Math.pow(tValue - centers[j], 2);
                    }
                }

                Gsum += maxG;
                G.add(maxG);
            }

            G.set(0, G.get(0) / Gsum);
            for (int j = 1; j < G.size(); j++) {
                G.set(j, G.get(j) / Gsum + G.get(j - 1));
            }

            double random = Math.random();
            for (int j = 0; j < G.size(); j++) {
                if (G.get(j) > random) {
                    centers[ctops++] = t.get(j);
                    break;
                }
            }
        }

        int[] count = new int[50];
        double[] g = new double[50];
        while (true) {
            boolean changed = false;

            for (int i = 0; i < k; i++) {
                count[i] = 0;
                g[i] = 0.0;
            }

            for (double i : t) {
                int minG = 0;
                for (int j = 1; j < k; j++) {
                    if (Math.abs(i - centers[minG]) > Math.abs(i - centers[j])) {
                        minG = j;
                    }
                }

                count[minG]++;
                g[minG] += i;
            }

            for (int i = 0; i < k; i++) {
                if (centers[i] != g[i] / count[i]) {
                    changed = true;
                    centers[i] = g[i] / count[i];
                }
            }

            if (!changed) {
                break;
            }
        }

        List<Integer>[] re = new List[50];

        for (int i = 0; i < k; i++)
            re[i] = new ArrayList();

        for (int x = 0; x < t.size(); x++) {
            double i = t.get(x);

            int minG = 0;
            for (int j = 1; j < k; j++) {
                if (Math.abs(i - centers[minG]) > Math.abs(i - centers[j])) {
                    minG = j;
                }
            }

            re[minG].add(x);
        }

        return re;
    }

    public static Double BICCalculator(List<List<Double>> GMMParams) {
        Double BIC = Math.log(p.size()) * (GMMParams.size() * 52 - 50);
        Double lnL = 0.0;

        for (Pair<Double, Triple<Integer, Integer, Integer>> param : p) {
            Double P = 0.0;
            for (List<Double> gm : GMMParams) {
                Double GP = Math.exp(-1.0 * Math.pow(param.getLeft() - gm.get(0), 2) /
                                    (2 * Math.pow(gm.get(1), 2))) / (2.0 * Math.pow(PI * 2.0, 0.5) * gm.get(1));
                Double W = gm.get(1 + param.getRight().getLeft())
                         + gm.get(13 + param.getRight().getMiddle())
                         + gm.get(44 + param.getRight().getRight());

                P += GP * W;
            }
            lnL += Math.log(P);
        }

        BIC -= 2.0 * lnL;
        return BIC;
    }

    public static List<Double> gaussParamsCalculator(List<Pair<Double, Triple<Integer, Integer, Integer>>> params) {
        int[] countMonth1 = new int[13];
        int[] countDay1 = new int[32];
        int[] countWeek1 = new int[8];

        for (int i = 0; i < 13; i++) countMonth1[i] = 0;
        for (int i = 0; i < 32; i++) countDay1[i] = 0;
        for (int i = 0; i < 8; i++) countWeek1[i] = 0;

        for (Pair<Double, Triple<Integer, Integer, Integer>> param : params) {
            countMonth[param.getRight().getLeft()]++;
            countDay[param.getRight().getMiddle()]++;
            countWeek[param.getRight().getRight()]++;
        }

        Double mean = 0.0, variance = 0.0;
        for (Pair<Double, Triple<Integer, Integer, Integer>> pair : params) {
            mean += pair.getLeft();
        }
        mean /= params.size();
        for (Pair<Double, Triple<Integer, Integer, Integer>> pair : params) {
            variance += Math.pow(pair.getLeft() - mean, 2);
        }
        variance /= params.size();

        List<Double> re = new ArrayList<>();
        re.add(mean);
        re.add(variance);

        Double tmp1 = 0.0, tmp2 = 0.0;
        for (int i = 1; i < 13; i++) {
            re.add(1.0 * countMonth1[i] / countMonth[i]);
            tmp1 += 1.0 * countMonth1[i] / countMonth[i];
        }
        tmp1 /= 12.0;
        for (int i = 1; i < 32; i++) {
            re.add(1.0 * countDay1[i] / countDay[i] - tmp1);
            tmp2 += 1.0 * countDay1[i] / countDay[i] - tmp1;
        }
        tmp2 /= 31.0;
        for (int i = 1; i < 8; i++) {
            re.add(1.0 * countWeek1[i] / countWeek[i] - tmp1 - tmp2);
        }

        return re;
    }
}

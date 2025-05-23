package io.github.software.coursework.algo.probmodel;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.ml.clustering.*;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

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
    list of length 31, and H_i is a list of length 7. For each F_i_j, G_i_j，H_i_j,
    Ensure that sigma i (F_i_j+G_i_j+H_i_j)=1
 */
public class GMModelCalculation {

    public List<Pair<Double, Triple<Integer, Integer, Integer>>> p;
    public int[] countMonth = new int[13];
    public int[] countDay = new int[32];
    public int[] countWeek = new int[8];
    public List<Double> t;


    public List<List<Double>> GMModelCalculator(List<Pair<Double, Triple<Integer, Integer, Integer>>> params) {

        System.out.println("GMModelCalculator new parameters: " + params.size());

        if (params.isEmpty()) {
            List<List<Double>> answer = new ArrayList<>();
            List<Double> temp = new ArrayList<>();
            temp.add(0.0);
            temp.add(0.01);
            for (int i = 1; i < 13; i++)
                temp.add(0.00000001);
            for (int i = 1; i < 32; i++)
                temp.add(0.00000001);
            for (int i = 1; i < 8; i++)
                temp.add(0.00000001);
            answer.add(temp);
            System.out.println("GMModelCalculator Return 0");
            return answer;
        }

        p = params;
        t = new ArrayList<>();

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
        double Score;

        answer.add(gaussParamsCalculator(params));
        Score = BICCalculator(answer);

        List<Integer>[] kMeans;
        List<Pair<Double, Triple<Integer, Integer, Integer>>> tmpList = new ArrayList<>();

        List<List<Double>> tmpAnswer = new ArrayList<>();
        double tmpScore;

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
                Score = tmpScore;
            }

        }

        return answer;
    }

    static class IndexedDoublePoint implements Clusterable {
        private final double[] point;
        private final int index;

        public IndexedDoublePoint(double value, int index) {
            this.point = new double[]{value};
            this.index = index;
        }

        @Override
        public double[] getPoint() {
            return point;
        }

        public int getIndex() {
            return index;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer>[] kMeansPlus(int k) {

        List<IndexedDoublePoint> points = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            points.add(new IndexedDoublePoint(t.get(i), i));
        }

        KMeansPlusPlusClusterer<IndexedDoublePoint> clusterer = new KMeansPlusPlusClusterer<>(k, 50);
        List<CentroidCluster<IndexedDoublePoint>> clusters = clusterer.cluster(points);

        List<Integer>[] re = new List[50];

        for (int i = 0; i < k; i++)
            re[i] = new ArrayList<>();

        int clusterId = 0;
        for (Cluster<IndexedDoublePoint> cluster : clusters) {
            for (IndexedDoublePoint p : cluster.getPoints()) {
                re[clusterId].add(p.getIndex());
            }
            clusterId++;
        }

        return re;
    }

    public double BICCalculator(List<List<Double>> GMMParams) {
        double BIC = Math.log(p.size()) * (GMMParams.size() * 3 - 1);
        double lnL = 0.0;

        for (Pair<Double, Triple<Integer, Integer, Integer>> param : p) {
            double P = 0.0;
            for (List<Double> gm : GMMParams) {
                double GP = Math.exp(-1.0 * Math.pow(param.getLeft() - gm.get(0), 2) /
                                    (2 * Math.pow(gm.get(1), 2))) / (2.0 * Math.pow(PI * 2.0, 0.5) * gm.get(1));
                double W = gm.get(1 + param.getRight().getLeft())
                         + gm.get(13 + param.getRight().getMiddle())
                         + gm.get(44 + param.getRight().getRight());

                P += GP * W;

//                if (W < 0 || W > 1)
//                        System.out.println("P = " + P + ", W = " + W);

//                System.out.println("GP = " + GP + ", W = " + W);
            }

//            if (P < 0.0 || P >= 1.0)
//                System.out.println("P = " + P);

            lnL += Math.log(P);
        }

        BIC -= 2.0 * lnL;
        return BIC;
    }

    public List<Double> gaussParamsCalculator(List<Pair<Double, Triple<Integer, Integer, Integer>>> params) {

        int[] countMonth1 = new int[13];
        int[] countDay1 = new int[32];
        int[] countWeek1 = new int[8];

        for (int i = 0; i < 13; i++) countMonth1[i] = 0;
        for (int i = 0; i < 32; i++) countDay1[i] = 0;
        for (int i = 0; i < 8; i++) countWeek1[i] = 0;

        for (Pair<Double, Triple<Integer, Integer, Integer>> param : params) {
            countMonth1[param.getRight().getLeft()]++;
            countDay1[param.getRight().getMiddle()]++;
            countWeek1[param.getRight().getRight()]++;
        }

        double mean = 0.0, variance = 0.0;
        for (Pair<Double, Triple<Integer, Integer, Integer>> pair : params) {
            mean += pair.getLeft();
        }
        mean /= params.size();
        for (Pair<Double, Triple<Integer, Integer, Integer>> pair : params) {
            variance += Math.pow(pair.getLeft() - mean, 2);
        }
        variance /= params.size();
        variance = Math.sqrt(variance);

        if (variance < 1e-8) {
            variance = 1.0 / params.size();
        }

        List<Double> re = new ArrayList<>();
        re.add(mean);
        re.add(variance);

        double[] tmpansMonth = new double[13];
        double[] tmpansDay = new double[32];
        double[] tmpansWeek = new double[8];

        double tmp1 = 0.0, tmp2 = 0.0;
        for (int i = 1; i < 13; i++) {
            if (countMonth[i] != 0) {
                tmpansMonth[i] = (1.0 * (countMonth1[i]) / (countMonth[i]));
                tmp1 += 1.0 * (countMonth1[i]) / (countMonth[i]);
            } else {
                tmpansMonth[i] = (0.0000001);
            }
        }
        tmp1 /= 12.0;
        double tmp1_ = tmp1;

        for (int i = 1; i < 32; i++) {
            if (countDay[i] != 0) {
                tmp1 = min(tmp1, 1.0 * countDay1[i] / countDay[i]);
            } else
                tmp1 = 0.0;
        }
        for (int i = 1; i < 13; i++) {
            tmpansMonth[i] = max(0.0000001, tmpansMonth[i] - (tmp1_ - tmp1));
        }
        for (int i = 1; i < 32; i++) {
            if (countDay[i] != 0) {
                tmpansDay[i] = (Math.max(1.0 * (countDay1[i]) / (countDay[i]) - tmp1, 0.0000001));
                tmp2 += Math.max(1.0 * (countDay1[i]) / (countDay[i]) - tmp1, 0.0000001);
            } else {
                tmpansDay[i] = (0.0000001);
            }
        }
        tmp2 /= 31.0;

        tmp1_ = tmp1;
        double tmp2_ = tmp2;
        for (int i = 1; i < 8; i++) {
            if (countWeek[i] != 0) {
                tmp1 = min(tmp1, 1.0 * countWeek1[i] / countWeek[i]);
            } else
                tmp1 = 0.0;
        }
        for (int i = 1; i < 8; i++) {
            if (countWeek[i] != 0) {
                tmp2 = min(tmp2, 1.0 * countWeek1[i] / countWeek[i] - tmp1);
            } else
                tmp2 = 0.0;
        }
        for (int i = 1; i < 13; i++) {
            tmpansMonth[i] = max(0.0000001, tmpansMonth[i] - (tmp1_ - tmp1));
        }
        for (int i = 1; i < 32; i++) {
            tmpansDay[i] = max(0.0000001, tmpansDay[i] - (tmp2_ - tmp2));
        }
        for (int i = 1; i < 8; i++) {
            if (countWeek[i] != 0) {
                tmpansWeek[i] = (Math.max(1.0 * (countWeek1[i]) / (countWeek[i]) - tmp1 - tmp2, 0.0000001));
            } else {
                tmpansWeek[i] = (0.0000001);
            }
        }

        for (int i = 1; i < 13; i++) {
            re.add(tmpansMonth[i]);
        }
        for (int i = 1; i < 32; i++) {
            re.add(tmpansDay[i]);
        }
        for (int i = 1; i < 8; i++) {
            re.add(tmpansWeek[i]);
        }

        return re;
    }
}
